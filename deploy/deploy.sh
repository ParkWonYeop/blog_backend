#!/bin/bash
set -euo pipefail

mkdir -p "$REMOTE_DIR"

MAIA_DIR="$REMOTE_DIR/maia-engine"
MAIA_NEW_DIR="$REMOTE_DIR/maia-engine.new"
MAIA_VENV_DIR="$REMOTE_DIR/maia-engine-venv"
MAIA_CACHE_DIR="$REMOTE_DIR/.cache/huggingface"
MAIA_REQUIREMENTS_HASH_FILE="$MAIA_VENV_DIR/.requirements.sha256"

python_bin=""
if command -v python3.11 >/dev/null 2>&1; then
  python_bin="$(command -v python3.11)"
elif command -v python3 >/dev/null 2>&1; then
  python_bin="$(command -v python3)"
fi

if [ -z "$python_bin" ]; then
  echo "python3.11 or python3 is required on the deploy server"
  exit 1
fi

install_apt_package() {
  package_name="$1"
  if [ "$(id -u)" -eq 0 ]; then
    DEBIAN_FRONTEND=noninteractive apt-get install -y "$package_name"
  elif command -v sudo >/dev/null 2>&1; then
    sudo DEBIAN_FRONTEND=noninteractive apt-get install -y "$package_name"
  else
    return 1
  fi
}

ensure_python_venv() {
  probe_dir="$(mktemp -d)"
  if "$python_bin" -m venv "$probe_dir" >/dev/null 2>&1; then
    rm -rf "$probe_dir"
    return 0
  fi
  rm -rf "$probe_dir"

  if ! command -v apt-get >/dev/null 2>&1; then
    echo "Python venv support is missing, and apt-get is not available on the deploy server."
    return 1
  fi

  echo "Python venv support is missing. Installing python venv package..."
  if [ "$(id -u)" -eq 0 ]; then
    DEBIAN_FRONTEND=noninteractive apt-get update
  elif command -v sudo >/dev/null 2>&1; then
    sudo DEBIAN_FRONTEND=noninteractive apt-get update
  else
    echo "Cannot install python venv package because deploy user is not root and sudo is unavailable."
    return 1
  fi

  python_version="$("$python_bin" -c 'import sys; print(f"{sys.version_info.major}.{sys.version_info.minor}")')"
  for package_name in "python${python_version}-venv" "python3-venv"; do
    if install_apt_package "$package_name"; then
      probe_dir="$(mktemp -d)"
      if "$python_bin" -m venv "$probe_dir" >/dev/null 2>&1; then
        rm -rf "$probe_dir"
        return 0
      fi
      rm -rf "$probe_dir"
    fi
  done

  echo "failed to install a working python venv package."
  return 1
}

if ! command -v git >/dev/null 2>&1; then
  echo "git is required on the deploy server because maia3 is installed from GitHub"
  exit 1
fi

rm -rf "$MAIA_NEW_DIR"
mkdir -p "$MAIA_NEW_DIR" "$MAIA_CACHE_DIR"
tar -xzf "$MAIA_ARCHIVE_TMP" -C "$MAIA_NEW_DIR" --strip-components=1

maia_backup_path=""
if [ -d "$MAIA_DIR" ]; then
  maia_backup_path="$MAIA_DIR.$(date +%Y%m%d%H%M%S).bak"
  mv "$MAIA_DIR" "$maia_backup_path"
fi
mv "$MAIA_NEW_DIR" "$MAIA_DIR"

rollback_maia() {
  if [ -n "$maia_backup_path" ] && [ -d "$maia_backup_path" ]; then
    rm -rf "$MAIA_DIR"
    mv "$maia_backup_path" "$MAIA_DIR"
    if systemctl cat "$MAIA_SERVICE_NAME" >/dev/null 2>&1; then
      systemctl restart "$MAIA_SERVICE_NAME" || true
    fi
  fi
}

if ! ensure_python_venv; then
  rollback_maia
  echo "failed to prepare Python venv support on the deploy server."
  exit 1
fi

if [ ! -x "$MAIA_VENV_DIR/bin/python" ] || ! "$MAIA_VENV_DIR/bin/python" -m pip --version >/dev/null 2>&1; then
  echo "Creating Maia Python venv..."
  rm -rf "$MAIA_VENV_DIR"
  if ! "$python_bin" -m venv "$MAIA_VENV_DIR"; then
    rollback_maia
    echo "failed to create Python venv. Install python3-venv on the deploy server."
    exit 1
  fi
fi

if ! "$MAIA_VENV_DIR/bin/python" -m pip --version >/dev/null 2>&1; then
  if ! "$MAIA_VENV_DIR/bin/python" -m ensurepip --upgrade; then
    rollback_maia
    echo "failed to bootstrap pip in the Maia Python venv."
    exit 1
  fi
fi

if ! "$MAIA_VENV_DIR/bin/python" -m pip install --upgrade pip; then
  rollback_maia
  exit 1
fi
requirements_hash="$(sha256sum "$MAIA_DIR/requirements.txt" | awk '{print $1}')"
saved_requirements_hash=""
if [ -f "$MAIA_REQUIREMENTS_HASH_FILE" ]; then
  saved_requirements_hash="$(cat "$MAIA_REQUIREMENTS_HASH_FILE")"
fi

if [ "$requirements_hash" != "$saved_requirements_hash" ]; then
  if ! "$MAIA_VENV_DIR/bin/python" -m pip install -r "$MAIA_DIR/requirements.txt"; then
    rollback_maia
    exit 1
  fi
  printf '%s\n' "$requirements_hash" > "$MAIA_REQUIREMENTS_HASH_FILE"
fi

if ! "$MAIA_VENV_DIR/bin/python" -m py_compile "$MAIA_DIR/app/main.py"; then
  rollback_maia
  exit 1
fi

cp "$MAIA_SERVICE_TMP" "/etc/systemd/system/$MAIA_SERVICE_NAME"
chmod 0644 "/etc/systemd/system/$MAIA_SERVICE_NAME"
systemctl daemon-reload
systemctl enable "$MAIA_SERVICE_NAME"

if ! systemctl restart "$MAIA_SERVICE_NAME"; then
  rollback_maia
  journalctl -u "$MAIA_SERVICE_NAME" -n 120 --no-pager
  exit 1
fi

sleep 5

if ! systemctl is-active --quiet "$MAIA_SERVICE_NAME"; then
  rollback_maia
  journalctl -u "$MAIA_SERVICE_NAME" -n 120 --no-pager
  exit 1
fi

if command -v curl >/dev/null 2>&1; then
  if ! curl -fsS http://127.0.0.1:8000/health >/dev/null; then
    rollback_maia
    journalctl -u "$MAIA_SERVICE_NAME" -n 120 --no-pager
    exit 1
  fi
else
  if ! "$python_bin" -c "import urllib.request; urllib.request.urlopen('http://127.0.0.1:8000/health', timeout=5).read()"; then
    rollback_maia
    journalctl -u "$MAIA_SERVICE_NAME" -n 120 --no-pager
    exit 1
  fi
fi

if [ -n "$maia_backup_path" ]; then
  rm -rf "$maia_backup_path"
fi

cd "$REMOTE_DIR"

backup_path=""
if [ -f "$JAR_NAME" ]; then
  backup_path="$JAR_NAME.$(date +%Y%m%d%H%M%S).bak"
  cp "$JAR_NAME" "$backup_path"
fi

mv "$REMOTE_TMP" "$JAR_NAME"
chown root:root "$JAR_NAME"
chmod 0644 "$JAR_NAME"

rollback_backend() {
  if [ -n "$backup_path" ] && [ -f "$backup_path" ]; then
    cp "$backup_path" "$JAR_NAME"
    systemctl restart "$SERVICE_NAME" || true
  fi
}

if ! systemctl restart "$SERVICE_NAME"; then
  rollback_backend
  journalctl -u "$SERVICE_NAME" -n 120 --no-pager
  exit 1
fi

api_health_url="http://127.0.0.1:8080/actuator/health"
api_healthy=0
for _ in $(seq 1 45); do
  sleep 2
  if ! systemctl is-active --quiet "$SERVICE_NAME"; then
    break
  fi
  if command -v curl >/dev/null 2>&1; then
    if curl -fsS "$api_health_url" >/dev/null 2>&1; then
      api_healthy=1
      break
    fi
  elif "$python_bin" -c "import urllib.request; urllib.request.urlopen('$api_health_url', timeout=5).read()" >/dev/null 2>&1; then
    api_healthy=1
    break
  fi
done

if [ "$api_healthy" -ne 1 ]; then
  rollback_backend
  journalctl -u "$SERVICE_NAME" -n 120 --no-pager
  exit 1
fi

systemctl is-active "$MAIA_SERVICE_NAME"
systemctl is-active "$SERVICE_NAME"
