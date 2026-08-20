#!/usr/bin/env bash
set -euo pipefail

: "${APP_NAME:=spring-is-cool}"
: "${IMAGE_NAME:=spring-is-cool}"
: "${CONTAINER_NAME:=spring-is-cool}"
: "${REPO_URL:=git@github.com:HCHJEONG/spring-is-cool.git}"
: "${DEPLOY_BRANCH:=main}"
: "${CLEAN_CLONE_ROOT:=${HOME}/hchjeong/deploy_remote_repo}"
: "${AWS_DEMO_HOST:?set AWS_DEMO_HOST, for example ubuntu@example}"
: "${REMOTE_IMAGE_DIR:=/srv/spring-is-cool/images}"
: "${REMOTE_APP_DIR:=/srv/spring-is-cool}"
: "${HOST_PORT:=2222}"
: "${CONTAINER_PORT:=2222}"
: "${DEMO_USER:=demo}"
: "${DEMO_PASSWORD:=demo}"
: "${REMOTE_DOCKER:=sudo docker}"

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
WORKING_ROOT="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
CLEAN_CLONE_DIR="${CLEAN_CLONE_ROOT}/${APP_NAME}"
IMAGE_TAG="$(date +%Y%m%d%H%M%S)"
IMAGE="${IMAGE_NAME}:${IMAGE_TAG}"
LOCAL_IMAGE_DIR="${CLEAN_CLONE_ROOT}/images"
LOCAL_IMAGE_FILE="${LOCAL_IMAGE_DIR}/${IMAGE_NAME}-${IMAGE_TAG}.tar"
REMOTE_IMAGE_FILE="${REMOTE_IMAGE_DIR}/${IMAGE_NAME}-${IMAGE_TAG}.tar"

log() {
  printf '[spring-is-cool aws-demo] %s\n' "$*"
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

log "preparing remote directories on $AWS_DEMO_HOST"
ssh "$AWS_DEMO_HOST" \
  REMOTE_IMAGE_DIR="$REMOTE_IMAGE_DIR" \
  REMOTE_APP_DIR="$REMOTE_APP_DIR" \
  bash -s <<'REMOTE_PREP'
set -euo pipefail
sudo mkdir -p "$REMOTE_IMAGE_DIR" "$REMOTE_APP_DIR/runtime/ssh"
sudo chmod 700 "$REMOTE_APP_DIR/runtime/ssh"
sudo chown -R "$(id -u):$(id -g)" "$REMOTE_IMAGE_DIR"
REMOTE_PREP

log "transferring image archive to $AWS_DEMO_HOST:$REMOTE_IMAGE_FILE"
scp "$LOCAL_IMAGE_FILE" "$AWS_DEMO_HOST:$REMOTE_IMAGE_FILE"
rm -f "$LOCAL_IMAGE_FILE"

log "loading image and replacing container on $AWS_DEMO_HOST"
ssh "$AWS_DEMO_HOST" \
  REMOTE_DOCKER="$REMOTE_DOCKER" \
  REMOTE_IMAGE_FILE="$REMOTE_IMAGE_FILE" \
  REMOTE_APP_DIR="$REMOTE_APP_DIR" \
  IMAGE="$IMAGE" \
  CONTAINER_NAME="$CONTAINER_NAME" \
  HOST_PORT="$HOST_PORT" \
  CONTAINER_PORT="$CONTAINER_PORT" \
  DEMO_USER="$DEMO_USER" \
  DEMO_PASSWORD="$DEMO_PASSWORD" \
  bash -s <<'REMOTE_DEPLOY'
set -euo pipefail

$REMOTE_DOCKER load -i "$REMOTE_IMAGE_FILE"
rm -f "$REMOTE_IMAGE_FILE"

$REMOTE_DOCKER rm -f "$CONTAINER_NAME" >/dev/null 2>&1 || true

$REMOTE_DOCKER run -d \
  --restart unless-stopped \
  --name "$CONTAINER_NAME" \
  -p "0.0.0.0:${HOST_PORT}:${CONTAINER_PORT}" \
  -e SPRING_IS_COOL_SSH_ENABLED=true \
  -e SPRING_IS_COOL_SSH_HOST=0.0.0.0 \
  -e SPRING_IS_COOL_SSH_PORT="$CONTAINER_PORT" \
  -e SPRING_IS_COOL_SSH_DEMO_USER="$DEMO_USER" \
  -e SPRING_IS_COOL_SSH_DEMO_PASSWORD="$DEMO_PASSWORD" \
  -e SPRING_IS_COOL_SSH_HOST_KEY_PATH=/app/runtime/ssh/hostkey.ser \
  -v "${REMOTE_APP_DIR}/runtime/ssh:/app/runtime/ssh" \
  "$IMAGE"

if ! $REMOTE_DOCKER ps --filter "name=^/${CONTAINER_NAME}$" --filter status=running --format '{{.Names}}' | grep -qx "$CONTAINER_NAME"; then
  echo "container did not enter running state" >&2
  $REMOTE_DOCKER ps -a --filter "name=^/${CONTAINER_NAME}$"
  $REMOTE_DOCKER logs --tail 80 "$CONTAINER_NAME" || true
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
  $REMOTE_DOCKER logs --tail 80 "$CONTAINER_NAME" || true
  exit 1
fi

$REMOTE_DOCKER ps --filter "name=^/${CONTAINER_NAME}$" --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'
REMOTE_DEPLOY

log "deploy success: ssh ${DEMO_USER}@${AWS_DEMO_HOST} -p ${HOST_PORT}"
