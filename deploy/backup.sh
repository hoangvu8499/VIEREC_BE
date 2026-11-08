#!/bin/sh
# Daily backup of the database and uploaded files. Run from cron, e.g.:
#   0 2 * * * /opt/vierec/VIEREC_BE/deploy/backup.sh >> /var/log/vierec-backup.log 2>&1
# Backups sit on the same disk: copy them off the VPS (rclone, scp...) to survive a lost server.
set -eu

cd "$(dirname "$0")"
BACKUP_DIR="${BACKUP_DIR:-/opt/vierec-backups}"
KEEP_DB_DAYS="${KEEP_DB_DAYS:-7}"
KEEP_UPLOAD_DAYS="${KEEP_UPLOAD_DAYS:-2}"
STAMP="$(date +%Y-%m-%d_%H%M)"
mkdir -p "$BACKUP_DIR"

docker compose exec -T mysql sh -c \
  'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction --routines --triggers vierec' \
  | gzip > "$BACKUP_DIR/db_$STAMP.sql.gz"

docker compose exec -T app tar czf - -C /app uploads > "$BACKUP_DIR/uploads_$STAMP.tar.gz"

find "$BACKUP_DIR" -name 'db_*.sql.gz' -mtime +"$KEEP_DB_DAYS" -delete
find "$BACKUP_DIR" -name 'uploads_*.tar.gz' -mtime +"$KEEP_UPLOAD_DAYS" -delete
echo "$(date) backup ok: $STAMP"
