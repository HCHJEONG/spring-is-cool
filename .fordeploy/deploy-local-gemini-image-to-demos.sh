#!/usr/bin/env bash
set -euo pipefail

: "${IMAGE:=spring-is-cool:gemini-2026082012}"
: "${IMAGE_FILE:=/home/hchjeong/hchjeong/deploy_remote_repo/images/spring-is-cool-gemini-2026082012.tar}"

deploy_target() {
  local host="$1"
  local app_dir="$2"
  local image_dir="$3"
  local session_id="$4"
  local remote_image_file="${image_dir}/$(basename "$IMAGE_FILE")"

  printf '[spring-is-cool gemini deploy] preparing %s\n' "$host"
  ssh "$host" \
    APP_DIR="$app_dir" \
    IMAGE_DIR="$image_dir" \
    SESSION_ID="$session_id" \
    bash -s <<'REMOTE_PREP'
set -euo pipefail

mkdir -p "$IMAGE_DIR" "$APP_DIR/runtime/ssh" "$APP_DIR/data"
chmod 700 "$APP_DIR/runtime/ssh" "$APP_DIR/data"
touch "$APP_DIR/data/world.sqlite"
chmod 600 "$APP_DIR/data/world.sqlite"

ENV_FILE="$APP_DIR/.env.local"
test -f "$ENV_FILE"
test -f "$APP_DIR/gcp-key.json"

set_key() {
  local key="$1"
  local value="$2"
  if grep -q "^${key}=" "$ENV_FILE"; then
    sed -i "s|^${key}=.*|${key}=${value}|" "$ENV_FILE"
  else
    printf '%s=%s\n' "$key" "$value" >> "$ENV_FILE"
  fi
}

set_key SPRING_IS_COOL_SSH_ENABLED true
set_key SPRING_IS_COOL_SSH_HOST 0.0.0.0
set_key SPRING_IS_COOL_SSH_PORT 2222
set_key SPRING_IS_COOL_SSH_DEMO_USER demo
set_key SPRING_IS_COOL_SSH_DEMO_PASSWORD demo
set_key SPRING_IS_COOL_SSH_HOST_KEY_PATH /app/runtime/ssh/hostkey.ser
set_key SPRING_IS_COOL_AI_ENABLED true
set_key SPRING_IS_COOL_AI_PROVIDER gemini
set_key GOOGLE_CLOUD_LOCATION global
set_key VERTEX_AI_MODEL_ID gemini-2.5-flash-lite
set_key GOOGLE_APPLICATION_CREDENTIALS /app/gcp-key.json
set_key SPRING_IS_COOL_PERSISTENCE_ENABLED true
set_key SPRING_IS_COOL_PERSISTENCE_TYPE sqlite
set_key SPRING_IS_COOL_PERSISTENCE_SQLITE_PATH /app/data/world.sqlite
set_key SPRING_IS_COOL_PERSISTENCE_SESSION_ID "$SESSION_ID"
set_key SERVER_PORT 8080
chmod 600 "$ENV_FILE"

grep -E '^(SPRING_IS_COOL_AI_ENABLED|SPRING_IS_COOL_AI_PROVIDER|GOOGLE_CLOUD_LOCATION|VERTEX_AI_MODEL_ID|GOOGLE_APPLICATION_CREDENTIALS)=' "$ENV_FILE"
REMOTE_PREP

  printf '[spring-is-cool gemini deploy] transferring image to %s\n' "$host"
  scp "$IMAGE_FILE" "${host}:${remote_image_file}"

  printf '[spring-is-cool gemini deploy] replacing container on %s\n' "$host"
  ssh "$host" \
    APP_DIR="$app_dir" \
    IMAGE="$IMAGE" \
    REMOTE_IMAGE_FILE="$remote_image_file" \
    bash -s <<'REMOTE_DEPLOY'
set -euo pipefail

DOCKER=(docker)
if ! docker info >/dev/null 2>&1; then
  DOCKER=(sudo docker)
fi

"${DOCKER[@]}" load -i "$REMOTE_IMAGE_FILE"
rm -f "$REMOTE_IMAGE_FILE"
"${DOCKER[@]}" rm -f spring-is-cool >/dev/null 2>&1 || true

RUN_ARGS=(
  -d
  --restart unless-stopped
  --name spring-is-cool
  -p 0.0.0.0:2222:2222
  -p 0.0.0.0:8080:8080
  --env-file "$APP_DIR/.env.local"
  -v "$APP_DIR/runtime/ssh:/app/runtime/ssh"
  -v "$APP_DIR/data:/app/data"
  -v "$APP_DIR/gcp-key.json:/app/gcp-key.json:ro"
)

"${DOCKER[@]}" run "${RUN_ARGS[@]}" "$IMAGE"

for attempt in $(seq 1 30); do
  if "${DOCKER[@]}" ps --filter 'name=^/spring-is-cool$' --filter status=running --format '{{.Names}}' | grep -qx spring-is-cool; then
    if timeout 2 bash -lc '</dev/tcp/127.0.0.1/8080' >/dev/null 2>&1; then
      "${DOCKER[@]}" ps --filter 'name=^/spring-is-cool$' --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'
      exit 0
    fi
  fi
  sleep 1
done

"${DOCKER[@]}" ps -a --filter 'name=^/spring-is-cool$'
"${DOCKER[@]}" logs --tail 100 spring-is-cool || true
exit 1
REMOTE_DEPLOY
}

deploy_target yoga /home/hchjeong/spring-is-cool /home/hchjeong/docker_images/spring-is-cool dev-demo-office
deploy_target aws-demo /home/ubuntu/spring-is-cool /home/ubuntu/docker_images/spring-is-cool aws-demo-office
