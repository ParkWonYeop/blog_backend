import logging
import os
import shutil
import subprocess
import sys
import threading
import time
from contextlib import asynccontextmanager
from dataclasses import dataclass, field
from datetime import datetime, timezone
from pathlib import Path

import chess
import chess.engine
import chess.pgn
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field


DEFAULT_MODEL = os.getenv("MAIA_MODEL", "23m").removeprefix("maia3-")
DEFAULT_DEVICE = os.getenv("MAIA_DEVICE", "cpu")
DEFAULT_USE_AMP = os.getenv("MAIA_USE_AMP", "false").lower() == "true"
# 동시에 띄워 둘 maia3-uci 프로세스 수. 각 프로세스가 torch + 모델을 따로 올리므로 메모리 상한을 결정한다.
MAX_ENGINES = max(1, int(os.getenv("MAIA_MAX_ENGINES", "2")))
# 이 시간 동안 쓰이지 않은 엔진 프로세스는 종료해 메모리를 반환한다.
ENGINE_IDLE_SECONDS = int(os.getenv("MAIA_ENGINE_IDLE_SECONDS", "900"))
# 엔진은 한 번에 한 수만 계산하므로, 이 시간 안에 차례가 오지 않으면 503으로 거절해 요청이 쌓이는 것을 막는다.
ENGINE_WAIT_SECONDS = float(os.getenv("MAIA_ENGINE_WAIT_SECONDS", "30"))
ENGINE_START_TIMEOUT_SECONDS = float(os.getenv("MAIA_ENGINE_START_TIMEOUT_SECONDS", "120"))
REAPER_INTERVAL_SECONDS = 60

log = logging.getLogger("maia-engine")


@dataclass
class LoadedEngine:
    engine: chess.engine.SimpleEngine
    started_at: float = field(default_factory=time.monotonic)
    last_used_at: float = field(default_factory=time.monotonic)

    def pid(self) -> int | None:
        transport = getattr(self.engine.protocol, "transport", None)
        return transport.get_pid() if transport is not None else None


engine_lock = threading.Lock()
engines: dict[str, LoadedEngine] = {}


class StateRequest(BaseModel):
    moves: list[str] = Field(default_factory=list)
    white: str | None = None
    black: str | None = None
    event: str = "Maia3"


class PlayRequest(StateRequest):
    rating: int = Field(default=1500, ge=600, le=2600)
    model: str = Field(default=DEFAULT_MODEL, pattern="^(3m|5m|23m|79m)$")
    temperature: float = Field(default=0.8, ge=0.0, le=2.0)
    topP: float = Field(default=0.95, ge=0.0, le=1.0)


def model_alias(model: str) -> str:
    return f"maia3-{model}"


def engine_executable() -> str:
    executable_name = "maia3-uci.exe" if os.name == "nt" else "maia3-uci"
    venv_executable = Path(sys.executable).with_name(executable_name)
    if venv_executable.exists():
        return str(venv_executable)

    path_executable = shutil.which("maia3-uci")
    if path_executable:
        return path_executable

    return "maia3-uci"


def engine_command(model: str) -> list[str]:
    command = [
        engine_executable(),
        "--model",
        model_alias(model),
        "--use-uci-history",
        "--device",
        DEFAULT_DEVICE,
    ]
    if not DEFAULT_USE_AMP:
        command.append("--no-use-amp")
    return command


def process_rss_mb(pid: int | None) -> float | None:
    if pid is None:
        return None
    try:
        with open(f"/proc/{pid}/status", encoding="utf-8") as status:
            for line in status:
                if line.startswith("VmRSS:"):
                    return round(int(line.split()[1]) / 1024, 1)
    except OSError:
        return None
    return None


def drop_engine(model: str, reason: str) -> None:
    """engine_lock을 잡은 상태에서 호출한다."""
    loaded = engines.pop(model, None)
    if loaded is None:
        return
    log.info("stopping maia3-%s engine (%s)", model, reason)
    try:
        loaded.engine.quit()
    except chess.engine.EngineError:
        loaded.engine.close()
    except Exception:  # noqa: BLE001 - 종료 실패로 서비스가 죽으면 안 된다
        log.exception("failed to stop maia3-%s cleanly", model)


def spawn_engine(model: str) -> LoadedEngine:
    try:
        engine = chess.engine.SimpleEngine.popen_uci(engine_command(model), timeout=ENGINE_START_TIMEOUT_SECONDS)
    except FileNotFoundError as exc:
        raise HTTPException(status_code=503, detail="maia3-uci executable was not found") from exc
    except (chess.engine.EngineError, subprocess.SubprocessError, OSError, TimeoutError) as exc:
        log.exception("failed to start maia3-%s", model)
        raise HTTPException(status_code=503, detail="failed to start Maia engine") from exc
    log.info("started maia3-%s engine", model)
    return LoadedEngine(engine=engine)


def get_engine(model: str) -> chess.engine.SimpleEngine:
    """engine_lock을 잡은 상태에서 호출한다."""
    loaded = engines.get(model)
    if loaded is None:
        while len(engines) >= MAX_ENGINES:
            oldest = min(engines, key=lambda name: engines[name].last_used_at)
            drop_engine(oldest, "evicted for maia3-%s" % model)
        loaded = spawn_engine(model)
        engines[model] = loaded
    loaded.last_used_at = time.monotonic()
    return loaded.engine


def reap_idle_engines() -> None:
    now = time.monotonic()
    with engine_lock:
        for model, loaded in list(engines.items()):
            if now - loaded.last_used_at >= ENGINE_IDLE_SECONDS:
                drop_engine(model, "idle for %ds" % int(now - loaded.last_used_at))


def reaper_loop(stop: threading.Event) -> None:
    while not stop.wait(REAPER_INTERVAL_SECONDS):
        try:
            reap_idle_engines()
        except Exception:  # noqa: BLE001 - 리퍼 스레드는 죽지 않아야 한다
            log.exception("idle engine reaper failed")


def build_board(moves: list[str]) -> chess.Board:
    board = chess.Board()
    for move in moves:
        try:
            board.push_uci(move)
        except ValueError as exc:
            raise HTTPException(status_code=400, detail=f"invalid move: {move}") from exc
    return board


def pgn_payload(board: chess.Board, request: StateRequest, result: str | None) -> str:
    game = chess.pgn.Game.from_board(board)
    game.headers["Event"] = request.event
    game.headers["Site"] = "blog-backend"
    game.headers["Date"] = datetime.now(timezone.utc).strftime("%Y.%m.%d")
    if request.white:
        game.headers["White"] = request.white
    if request.black:
        game.headers["Black"] = request.black
    game.headers["Result"] = result or "*"

    exporter = chess.pgn.StringExporter(headers=True, variations=False, comments=False)
    return game.accept(exporter)


def board_payload(board: chess.Board, request: StateRequest) -> dict:
    outcome = board.outcome(claim_draw=True)
    if outcome is None:
        status = "IN_PROGRESS"
        result = None
    else:
        status = outcome.termination.name
        result = outcome.result()

    return {
        "fen": board.fen(),
        "turn": "white" if board.turn == chess.WHITE else "black",
        "status": status,
        "result": result,
        "pgn": pgn_payload(board, request, result),
    }


def select_move(request: PlayRequest, board: chess.Board) -> chess.Move | None:
    options = {"Elo": request.rating, "Temperature": request.temperature, "TopP": request.topP}
    # 엔진 프로세스가 죽어 있으면(OOM 등) 한 번 새로 띄워 재시도한다.
    for attempt in (1, 2):
        maia = get_engine(request.model)
        try:
            maia.configure(options)
            return maia.play(board, chess.engine.Limit(nodes=1)).move
        except chess.engine.EngineTerminatedError:
            drop_engine(request.model, "terminated unexpectedly")
            if attempt == 2:
                raise HTTPException(status_code=503, detail="Maia engine terminated unexpectedly")
        except chess.engine.EngineError as exc:
            raise HTTPException(status_code=503, detail="Maia engine failed to select a move") from exc
    return None


@asynccontextmanager
async def lifespan(_: FastAPI):
    stop = threading.Event()
    reaper = threading.Thread(target=reaper_loop, args=(stop,), name="maia-engine-reaper", daemon=True)
    reaper.start()
    try:
        yield
    finally:
        stop.set()
        with engine_lock:
            for model in list(engines):
                drop_engine(model, "shutdown")


app = FastAPI(title="Maia Engine Bridge", lifespan=lifespan)


@app.get("/health")
def health() -> dict:
    now = time.monotonic()
    with engine_lock:
        loaded = {
            model: {
                "pid": entry.pid(),
                "rssMb": process_rss_mb(entry.pid()),
                "idleSeconds": int(now - entry.last_used_at),
                "uptimeSeconds": int(now - entry.started_at),
            }
            for model, entry in engines.items()
        }
    return {
        "status": "ok",
        "engines": loaded,
        "limits": {
            "maxEngines": MAX_ENGINES,
            "idleSeconds": ENGINE_IDLE_SECONDS,
            "waitSeconds": ENGINE_WAIT_SECONDS,
            "ompThreads": os.getenv("OMP_NUM_THREADS"),
        },
    }


@app.post("/maia/state")
def state(request: StateRequest) -> dict:
    board = build_board(request.moves)
    return board_payload(board, request)


@app.post("/maia/move")
def move(request: PlayRequest) -> dict:
    board = build_board(request.moves)
    current = board_payload(board, request)
    if current["status"] != "IN_PROGRESS":
        return {"move": None, **current}

    if not engine_lock.acquire(timeout=ENGINE_WAIT_SECONDS):
        raise HTTPException(status_code=503, detail="Maia engine is busy")
    try:
        selected = select_move(request, board)
    finally:
        engine_lock.release()

    if selected is None:
        raise HTTPException(status_code=503, detail="Maia engine returned no move")

    board.push(selected)
    return {"move": selected.uci(), **board_payload(board, request)}
