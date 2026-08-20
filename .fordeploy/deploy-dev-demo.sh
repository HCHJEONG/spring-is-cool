#!/usr/bin/env bash
set -euo pipefail

: "${APP_NAME:=spring-is-cool}"
: "${IMAGE_NAME:=spring-is-cool}"
: "${CONTAINER_NAME:=spring-is-cool}"
: "${REPO_URL:=git@github.com:HCHJEONG/spring-is-cool.git}"
: "${DEPLOY_BRANCH:=main}"
: "${CLEAN_CLONE_ROOT:=${HOME}/hchjeong/deploy_remote_repo}"
: "${TARGET_HOST:=yoga}"
: "${REMOTE_IMAGE_DIR:=/home/hchjeong/docker_images/spring-is-cool}"
: "${REMOTE_APP_DIR:=/home/hchjeong/spring-is-cool}"
: "${REMOTE_ENV_FILE:=${REMOTE_APP_DIR}/.env.local}"
: "${REMOTE_GCP_KEY_FILE:=${REMOTE_APP_DIR}/gcp-key.json}"
: "${HOST_PORT:=2222}"
: "${CONTAINER_PORT:=2222}"
: "${CONFIGURE_UFW:=0}"

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
WORKING_ROOT="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
CLEAN_CLONE_DIR="${CLEAN_CLONE_ROOT}/${APP_NAME}"
IMAGE_TAG="$(date +%Y%m%d%H%M%S)"
IMAGE="${IMAGE_NAME}:${IMAGE_TAG}"
LOCAL_IMAGE_DIR="${CLEAN_CLONE_ROOT}/images"
LOCAL_IMAGE_FILE="${LOCAL_IMAGE_DIR}/${IMAGE_NAME}-${IMAGE_TAG}.tar"
REMOTE_IMAGE_FILE="${REMOTE_IMAGE_DIR}/${IMAGE_NAME}-${IMAGE_TAG}.tar"

log() {
  printf '[spring-is-cool dev-demo] %s\n' "$*"
}

if git -C "$WORKING_ROOT" diff --quiet --exit-code; then
  :
else
  log "WARNING: this working tree has uncommitted changes; deployment uses the clean clone, not these files"
fi

mkdir -p "$CLEAN_CLONE_ROOT" "$LOCAL_IMAGE_DIR"

if [ ! -d "$CLEAN_CLONE_DIR/.git" ]; then
  log "creating clean clone: $CLEAN_CLONE_DIR"
  git clone "$REPO_URL" "$CLEAN_CLONE_DIR"
fi

log "updating clean clone from origin/$DEPLOY_BRANCH"
git -C "$CLEAN_CLONE_DIR" fetch --prune origin
git -C "$CLEAN_CLONE_DIR" checkout "$DEPLOY_BRANCH"
git -C "$CLEAN_CLONE_DIR" reset --hard "origin/${DEPLOY_BRANCH}"
git -C "$CLEAN_CLONE_DIR" clean -fdx

COMMIT_SHA="$(git -C "$CLEAN_CLONE_DIR" rev-parse --short HEAD)"
log "building image from clean clone commit: $COMMIT_SHA"
docker build -t "$IMAGE" "$CLEAN_CLONE_DIR"

log "saving image archive: $LOCAL_IMAGE_FILE"
docker save "$IMAGE" -o "$LOCAL_IMAGE_FILE"
docker rmi "$IMAGE" >/dev/null 2>&1 || true

log "preparing remote directories on $TARGET_HOST"
ssh "$TARGET_HOST" \
  REMOTE_IMAGE_DIR="$REMOTE_IMAGE_DIR" \
  REMOTE_APP_DIR="$REMOTE_APP_DIR" \
  REMOTE_ENV_FILE="$REMOTE_ENV_FILE" \
  HOST_PORT="$HOST_PORT" \
  CONFIGURE_UFW="$CONFIGURE_UFW" \
  bash -s <<'REMOTE_PREP'
set -euo pipefail
mkdir -p "$REMOTE_IMAGE_DIR" "$REMOTE_APP_DIR/runtime/ssh"
chmod 700 "$REMOTE_APP_DIR/runtime/ssh"
if [ ! -f "$REMOTE_ENV_FILE" ]; then
  umask 177
  cat > "$REMOTE_ENV_FILE" <<'EOF'
SPRING_IS_COOL_SSH_ENABLED=true
SPRING_IS_COOL_SSH_HOST=0.0.0.0
SPRING_IS_COOL_SSH_PORT=2222
SPRING_IS_COOL_SSH_DEMO_USER=demo
SPRING_IS_COOL_SSH_DEMO_PASSWORD=demo
SPRING_IS_COOL_SSH_HOST_KEY_PATH=/app/runtime/ssh/hostkey.ser
GOOGLE_CLOUD_PROJECT=
GOOGLE_CLOUD_LOCATION=
VERTEX_AI_MODEL_ID=
GOOGLE_APPLICATION_CREDENTIALS=/app/gcp-key.json
EOF
fi
chmod 600 "$REMOTE_ENV_FILE"
if [ "$CONFIGURE_UFW" = "1" ] && command -v ufw >/dev/null 2>&1; then
  sudo ufw allow "${HOST_PORT}/tcp"
fi
REMOTE_PREP

log "transferring image archive to $TARGET_HOST:$REMOTE_IMAGE_FILE"
scp "$LOCAL_IMAGE_FILE" "$TARGET_HOST:$REMOTE_IMAGE_FILE"
rm -f "$LOCAL_IMAGE_FILE"

log "loading image and replacing container on $TARGET_HOST"
ssh "$TARGET_HOST" \
  REMOTE_IMAGE_FILE="$REMOTE_IMAGE_FILE" \
  REMOTE_APP_DIR="$REMOTE_APP_DIR" \
  REMOTE_ENV_FILE="$REMOTE_ENV_FILE" \
  REMOTE_GCP_KEY_FILE="$REMOTE_GCP_KEY_FILE" \
  IMAGE="$IMAGE" \
  CONTAINER_NAME="$CONTAINER_NAME" \
  HOST_PORT="$HOST_PORT" \
  CONTAINER_PORT="$CONTAINER_PORT" \
  bash -s <<'REMOTE_DEPLOY'
set -euo pipefail

DOCKER=(docker)
if ! docker info >/dev/null 2>&1; then
  DOCKER=(sudo docker)
fi

"${DOCKER[@]}" load -i "$REMOTE_IMAGE_FILE"
rm -f "$REMOTE_IMAGE_FILE"

"${DOCKER[@]}" rm -f "$CONTAINER_NAME" >/dev/null 2>&1 || true

RUN_ARGS=(
  -d
  --restart unless-stopped
  --name "$CONTAINER_NAME"
  -p "0.0.0.0:${HOST_PORT}:${CONTAINER_PORT}"
  --env-file "$REMOTE_ENV_FILE"
  -v "${REMOTE_APP_DIR}/runtime/ssh:/app/runtime/ssh"
)

if [ -f "$REMOTE_GCP_KEY_FILE" ]; then
  RUN_ARGS+=(-v "${REMOTE_GCP_KEY_FILE}:/app/gcp-key.json:ro")
fi

"${DOCKER[@]}" run \
  "${RUN_ARGS[@]}" \
  "$IMAGE"

if ! "${DOCKER[@]}" ps --filter "name=^/${CONTAINER_NAME}$" --filter status=running --format '{{.Names}}' | grep -qx "$CONTAINER_NAME"; then
  echo "container did not enter running state" >&2
  "${DOCKER[@]}" ps -a --filter "name=^/${CONTAINER_NAME}$"
  "${DOCKER[@]}" logs --tail 80 "$CONTAINER_NAME" || true
  exit 1
fi

ready=0
for attempt in $(seq 1 20); do
  if timeout 2 bash -lc "</dev/tcp/127.0.0.1/${HOST_PORT}" >/dev/null 2>&1; then
    ready=1
    break
  fi
  echo "waiting for SSH port ${HOST_PORT} (${attempt}/20)"
  sleep 1
done

if [ "$ready" -ne 1 ]; then
  echo "SSH port did not open on 127.0.0.1:${HOST_PORT}" >&2
  "${DOCKER[@]}" logs --tail 80 "$CONTAINER_NAME" || true
  exit 1
fi

"${DOCKER[@]}" ps --filter "name=^/${CONTAINER_NAME}$" --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'
REMOTE_DEPLOY

log "deploy success: ssh demo@${TARGET_HOST} -p ${HOST_PORT}"
log "LAN clients can connect if the dev-demo host firewall allows TCP ${HOST_PORT}"
