import os
import subprocess
import threading
from datetime import datetime, timezone

import chess
import chess.engine
import chess.pgn
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field


DEFAULT_MODEL = os.getenv("MAIA_MODEL", "5m").removeprefix("maia3-")
DEFAULT_DEVICE = os.getenv("MAIA_DEVICE", "cpu")
DEFAULT_USE_AMP = os.getenv("MAIA_USE_AMP", "false").lower() == "true"

app = FastAPI(title="Maia Engine Bridge")
engine_lock = threading.Lock()
engines: dict[str, chess.engine.SimpleEngine] = {}


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


def engine_command(model: str) -> list[str]:
    command = [
        "maia3-uci",
        "--model",
        model_alias(model),
        "--use-uci-history",
        "--device",
        DEFAULT_DEVICE,
    ]
    if not DEFAULT_USE_AMP:
        command.append("--no-use-amp")
    return command


def get_engine(model: str) -> chess.engine.SimpleEngine:
    if model not in engines:
        try:
            engines[model] = chess.engine.SimpleEngine.popen_uci(engine_command(model))
        except FileNotFoundError as exc:
            raise HTTPException(status_code=503, detail="maia3-uci executable was not found") from exc
        except (chess.engine.EngineError, subprocess.SubprocessError, OSError) as exc:
            raise HTTPException(status_code=503, detail="failed to start Maia engine") from exc
    return engines[model]


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


@app.get("/health")
def health() -> dict:
    return {"status": "ok"}


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

    with engine_lock:
        maia = get_engine(request.model)
        try:
            maia.configure(
                {
                    "Elo": request.rating,
                    "Temperature": request.temperature,
                    "TopP": request.topP,
                }
            )
            result = maia.play(board, chess.engine.Limit(nodes=1))
        except chess.engine.EngineError as exc:
            raise HTTPException(status_code=503, detail="Maia engine failed to select a move") from exc

    if result.move is None:
        raise HTTPException(status_code=503, detail="Maia engine returned no move")

    board.push(result.move)
    return {"move": result.move.uci(), **board_payload(board, request)}


@app.on_event("shutdown")
def shutdown() -> None:
    for maia in engines.values():
        maia.quit()
    engines.clear()
