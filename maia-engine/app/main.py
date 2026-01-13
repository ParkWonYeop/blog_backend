import os
import shutil
import subprocess
import sys
import threading
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

app = FastAPI(title="Maia Engine Bridge")
engine_lock = threading.Lock()
engines: dict[str, chess.engine.SimpleEngine] = {}


class StateRequest(BaseModel):
    moves: list[str] = Field(default_factory=list)
    white: str | None = None
    black: str | None = None
    event: str = "Maia3"


