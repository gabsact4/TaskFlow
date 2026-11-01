#!/usr/bin/env bash
set -Eeuo pipefail

: "${DB_HOST:=db}"
: "${DB_PORT:=3306}"
: "${DB_NAME:=taskflow}"
: "${DB_USERNAME:=taskflow}"
: "${DB_PASSWORD:?DB_PASSWORD must be configured}"
: "${BACKUP_ENCRYPTION_KEY:?BACKUP_ENCRYPTION_KEY must be configured}"
: "${ATTACHMENTS_DIRECTORY:=/data/attachments}"

if [[ $# -ne 2 ]]; then
  echo "Usage: restore.sh /backups/taskflow-db-<timestamp>.sql.gz.enc /backups/taskflow-attachments-<timestamp>.tar.gz.enc" >&2
  exit 2
fi

db_backup="$1"
attachments_backup="$2"
[[ -f "$db_backup" && -f "$attachments_backup" ]] || { echo "Backup file not found." >&2; exit 2; }

export MYSQL_PWD="$DB_PASSWORD"
mysqladmin ping --silent -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USERNAME"

echo "This will replace the database '$DB_NAME' and all files in '$ATTACHMENTS_DIRECTORY'."
if [[ "${CONFIRM_RESTORE:-}" != "YES" ]]; then
  echo "Set CONFIRM_RESTORE=YES to proceed." >&2
  exit 2
fi

openssl enc -d -aes-256-cbc -pbkdf2 -iter 200000 -pass env:BACKUP_ENCRYPTION_KEY -in "$db_backup" \
  | gzip -dc \
  | mysql --host="$DB_HOST" --port="$DB_PORT" --user="$DB_USERNAME"

find "$ATTACHMENTS_DIRECTORY" -mindepth 1 -maxdepth 1 -exec rm -rf -- {} +
openssl enc -d -aes-256-cbc -pbkdf2 -iter 200000 -pass env:BACKUP_ENCRYPTION_KEY -in "$attachments_backup" \
  | tar -xzf - -C "$ATTACHMENTS_DIRECTORY"

echo "Restore completed. Restart the backend and verify the application and attachment links."
