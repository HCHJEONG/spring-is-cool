#!/usr/bin/env bash
set -euo pipefail

: "${TARGET_HOST:=yoga}"
: "${REMOTE_APP_DIR:=/home/hchjeong/spring-is-cool}"
: "${REMOTE_IMAGE_DIR:=/home/hchjeong/docker_images/spring-is-cool}"
: "${REMOTE_ENV_FILE:=${REMOTE_APP_DIR}/.env.local}"
: "${REMOTE_DATA_DIR:=${REMOTE_APP_DIR}/data}"
: "${REMOTE_SQLITE_FILE:=${REMOTE_DATA_DIR}/world.sqlite}"

log() {
  printf '[spring-is-cool dev-demo init] %s\n' "$*"
}

log "initializing runtime files on ${TARGET_HOST}"
ssh "$TARGET_HOST" \
  REMOTE_IMAGE_DIR="$REMOTE_IMAGE_DIR" \
  REMOTE_APP_DIR="$REMOTE_APP_DIR" \
  REMOTE_ENV_FILE="$REMOTE_ENV_FILE" \
  REMOTE_DATA_DIR="$REMOTE_DATA_DIR" \
  REMOTE_SQLITE_FILE="$REMOTE_SQLITE_FILE" \
  bash -s <<'REMOTE_INIT'
set -euo pipefail

mkdir -p "$REMOTE_IMAGE_DIR" "$REMOTE_APP_DIR/runtime/ssh" "$REMOTE_DATA_DIR"
chmod 700 "$REMOTE_APP_DIR/runtime/ssh"
chmod 700 "$REMOTE_DATA_DIR"

if [ ! -f "$REMOTE_SQLITE_FILE" ]; then
  : > "$REMOTE_SQLITE_FILE"
fi
chmod 600 "$REMOTE_SQLITE_FILE"

if [ ! -f "$REMOTE_ENV_FILE" ]; then
  umask 177
  cat > "$REMOTE_ENV_FILE" <<'EOF'
SPRING_IS_COOL_SSH_ENABLED=true
SPRING_IS_COOL_SSH_HOST=0.0.0.0
SPRING_IS_COOL_SSH_PORT=2222
SPRING_IS_COOL_SSH_DEMO_USER=demo
SPRING_IS_COOL_SSH_DEMO_PASSWORD=demo
SPRING_IS_COOL_SSH_HOST_KEY_PATH=/app/runtime/ssh/hostkey.ser
SPRING_IS_COOL_AI_ENABLED=false
SPRING_IS_COOL_AI_PROVIDER=static
SPRING_IS_COOL_PERSISTENCE_ENABLED=true
SPRING_IS_COOL_PERSISTENCE_TYPE=sqlite
SPRING_IS_COOL_PERSISTENCE_SQLITE_PATH=/app/data/world.sqlite
SPRING_IS_COOL_PERSISTENCE_SESSION_ID=dev-demo-office
SERVER_PORT=8080
GOOGLE_CLOUD_PROJECT=
GOOGLE_CLOUD_LOCATION=
VERTEX_AI_MODEL_ID=
GOOGLE_APPLICATION_CREDENTIALS=/app/gcp-key.json
EOF
fi
chmod 600 "$REMOTE_ENV_FILE"
ensure_env_key() {
  local key="$1"
  local value="$2"
  if ! grep -q "^${key}=" "$REMOTE_ENV_FILE"; then
    printf '%s=%s\n' "$key" "$value" >> "$REMOTE_ENV_FILE"
  fi
}
ensure_env_key SPRING_IS_COOL_AI_ENABLED false
ensure_env_key SPRING_IS_COOL_AI_PROVIDER static
ensure_env_key SPRING_IS_COOL_PERSISTENCE_ENABLED true
ensure_env_key SPRING_IS_COOL_PERSISTENCE_TYPE sqlite
ensure_env_key SPRING_IS_COOL_PERSISTENCE_SQLITE_PATH /app/data/world.sqlite
ensure_env_key SPRING_IS_COOL_PERSISTENCE_SESSION_ID dev-demo-office
ensure_env_key SERVER_PORT 8080

printf 'runtime ready: %s\n' "$REMOTE_APP_DIR"
ls -ld "$REMOTE_APP_DIR" "$REMOTE_APP_DIR/runtime/ssh" "$REMOTE_DATA_DIR"
ls -l "$REMOTE_ENV_FILE" "$REMOTE_SQLITE_FILE"
REMOTE_INIT
