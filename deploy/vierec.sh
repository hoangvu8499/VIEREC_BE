#!/usr/bin/env bash
# Manage the VIEREC stack on the VPS. Expected layout (PROJECT_DIR, default /opt/vierec):
#   /opt/vierec/VIEREC_BE   <- this repo (git clone)
#   /opt/vierec/VIEREC-_FE  <- FE repo (git clone, or ./vierec.sh setup clones it)
# Usage: ./vierec.sh {setup|cert|start|stop|restart|update|status|logs|backup|db-import}
set -euo pipefail

DEPLOY_DIR="$(cd "$(dirname "$0")" && pwd)"
BE_DIR="$(dirname "$DEPLOY_DIR")"
PROJECT_DIR="$(dirname "$BE_DIR")"
FE_DIR="$PROJECT_DIR/VIEREC-_FE"
FE_REPO="${FE_REPO:-https://github.com/hoangvu8499/VIEREC-_FE.git}"
BRANCH="${BRANCH:-main}"

cd "$DEPLOY_DIR"
dc() { docker compose "$@"; }

info() { printf '\033[1;32m==> %s\033[0m\n' "$*"; }
fail() { printf '\033[1;31mERROR: %s\033[0m\n' "$*" >&2; exit 1; }

domain() { grep -E '^DOMAIN=' .env | cut -d= -f2- | tr -d '"[:space:]'; }

require_env() {
  [ -f .env ] || fail "missing $DEPLOY_DIR/.env (cp .env.example .env, then fill it in)"
  local key
  for key in DOMAIN MYSQL_ROOT_PASSWORD DB_PASSWORD JWT_SECRET; do
    grep -qE "^$key=.+" .env || fail "$key is empty in .env"
  done
}

require_fe() {
  [ -d "$FE_DIR/.git" ] || fail "FE repo not found at $FE_DIR (run ./vierec.sh setup)"
}

has_cert() {
  dc run --rm --no-deps --entrypoint sh certbot -c "test -f /etc/letsencrypt/live/$(domain)/fullchain.pem" \
    >/dev/null 2>&1
}

cmd_setup() {
  if [ -d "$FE_DIR/.git" ]; then
    info "FE repo already at $FE_DIR"
  else
    info "Cloning FE into $FE_DIR"
    git clone --branch "$BRANCH" "$FE_REPO" "$FE_DIR"
  fi
  if [ ! -f .env ]; then
    cp .env.example .env
    chmod 600 .env
    info "Created .env: fill in the passwords and secrets (openssl rand -base64 48), then run ./vierec.sh cert"
  fi
}

cmd_cert() {
  require_env
  local email="${1:-}" d
  [ -n "$email" ] || fail "usage: ./vierec.sh cert you@example.com"
  d="$(domain)"
  info "Requesting Let's Encrypt certificate for $d and www.$d (port 80 must be free)"
  dc stop web >/dev/null 2>&1 || true
  dc run --rm -p 80:80 --entrypoint certbot certbot certonly --standalone \
    -d "$d" -d "www.$d" --email "$email" --agree-tos --no-eff-email --non-interactive
  info "Certificate ready. Next: ./vierec.sh start"
}

cmd_start() {
  require_env
  require_fe
  has_cert || fail "no HTTPS certificate yet: run ./vierec.sh cert you@example.com first"
  info "Building and starting containers"
  dc up -d --build
  docker builder prune -af >/dev/null
  docker image prune -f >/dev/null
  dc ps
  info "Started. Check https://$(domain) (the API needs about a minute to become healthy)"
}

cmd_stop() {
  info "Stopping containers (database, uploads and certificates are kept)"
  dc down
}

cmd_restart() {
  require_env
  info "Restarting containers ${*:-(all)}"
  dc restart "$@"
  dc ps
}

cmd_update() {
  require_env
  require_fe
  info "Pulling latest code ($BRANCH)"
  git -C "$BE_DIR" pull --ff-only origin "$BRANCH"
  git -C "$FE_DIR" pull --ff-only origin "$BRANCH"
  info "Reminder: run new SQL files from VIEREC_BE/db/ by hand before this if the schema changed"
  cmd_start
}

cmd_status() {
  dc ps
  echo
  docker stats --no-stream --format 'table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}'
  echo
  df -h / | tail -1 | awk '{print "Disk: " $3 " used / " $2 " (" $5 ")"}'
}

cmd_logs() {
  dc logs -f --tail 200 "$@"
}

cmd_backup() {
  "$DEPLOY_DIR/backup.sh"
}

cmd_db_import() {
  local file="${1:-}"
  [ -f "$file" ] || fail "usage: ./vierec.sh db-import file.sql"
  require_env
  dc up -d mysql
  info "Waiting for MySQL"
  until [ "$(docker inspect -f '{{.State.Health.Status}}' "$(dc ps -q mysql)")" = healthy ]; do sleep 2; done
  info "Importing $file into vierec"
  dc exec -T mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" vierec' < "$file"
  info "Done"
}

usage() {
  cat <<EOF
Usage: ./vierec.sh <command>

  setup              clone the FE repo next to VIEREC_BE, create .env from .env.example
  cert <email>       get the HTTPS certificate (once, after DNS points to this VPS)
  db-import <file>   import a .sql file into the vierec database
  start              build and start everything
  stop               stop everything (data is kept)
  restart [service]  restart all or one service (mysql | app | web | certbot)
  update             git pull both repos, rebuild and restart
  status             containers, RAM/CPU, disk usage
  logs [service]     follow logs (Ctrl+C to quit)
  backup             back up the database and uploads now
EOF
}

case "${1:-}" in
  setup) cmd_setup ;;
  cert) shift; cmd_cert "$@" ;;
  start) cmd_start ;;
  stop) cmd_stop ;;
  restart) shift; cmd_restart "$@" ;;
  update) cmd_update ;;
  status) cmd_status ;;
  logs) shift; cmd_logs "$@" ;;
  backup) cmd_backup ;;
  db-import) shift; cmd_db_import "$@" ;;
  *) usage; [ -z "${1:-}" ] || exit 1 ;;
esac
