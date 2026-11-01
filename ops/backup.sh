#!/usr/bin/env bash
set -Eeuo pipefail

: "${DB_HOST:=db}"
: "${DB_PORT:=3306}"
: "${DB_NAME:=taskflow}"
: "${DB_USERNAME:=taskflow}"
: "${DB_PASSWORD:?DB_PASSWORD must be configured}"
: "${BACKUP_RETENTION_DAYS:=14}"
: "${BACKUP_ENCRYPTION_KEY:?BACKUP_ENCRYPTION_KEY must be configured}"
: "${BACKUP_DIRECTORY:=/backups}"
: "${ATTACHMENTS_DIRECTORY:=/data/attachments}"

mkdir -p "$BACKUP_DIRECTORY"
export MYSQL_PWD="$DB_PASSWORD"

log() { printf '[backup] %s %s\n' "$(date -u '+%Y-%m-%dT%H:%M:%SZ')" "$*"; }

wait_for_database() {
  until mysqladmin ping --silent -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USERNAME"; do
    log "Waiting for MySQL..."
    sleep 10
  done
}

run_backup() {
  local stamp db_tmp files_tmp db_file files_file manifest
  stamp="$(date -u '+%Y%m%dT%H%M%SZ')"
  db_tmp="$BACKUP_DIRECTORY/.taskflow-db-$stamp.sql.gz.enc.tmp"
  files_tmp="$BACKUP_DIRECTORY/.taskflow-attachments-$stamp.tar.gz.enc.tmp"
  db_file="$BACKUP_DIRECTORY/taskflow-db-$stamp.sql.gz.enc"
  files_file="$BACKUP_DIRECTORY/taskflow-attachments-$stamp.tar.gz.enc"
  manifest="$BACKUP_DIRECTORY/taskflow-$stamp.sha256"

  log "Starting encrypted database and attachment backup"
    if ! mysqldump --host="$DB_HOST" --port="$DB_PORT" --user="$DB_USERNAME" \
      --single-transaction --routines --triggers --events --no-tablespaces --add-drop-database --databases "$DB_NAME" \
      | gzip -c \
      | openssl enc -aes-256-cbc -salt -pbkdf2 -iter 200000 -pass env:BACKUP_ENCRYPTION_KEY -out "$db_tmp"; then
    rm -f "$db_tmp" "$files_tmp"
    log "Database dump failed"
    return 1
  fi
  if ! tar -czf - -C "$ATTACHMENTS_DIRECTORY" . \
      | openssl enc -aes-256-cbc -salt -pbkdf2 -iter 200000 -pass env:BACKUP_ENCRYPTION_KEY -out "$files_tmp"; then
    rm -f "$db_tmp" "$files_tmp"
    log "Attachment archive failed"
    return 1
  fi

  mv "$db_tmp" "$db_file"
  mv "$files_tmp" "$files_file"
  (cd "$BACKUP_DIRECTORY" && sha256sum "${db_file##*/}" "${files_file##*/}" > "$manifest")
  find "$BACKUP_DIRECTORY" -type f -name 'taskflow-*' -mtime "+$BACKUP_RETENTION_DAYS" -delete
  log "Backup completed: ${db_file##*/}, ${files_file##*/}"
}

wait_for_database
while true; do
  now="$(date -u +%s)"
  next_run="$(date -u -d 'today 02:00' +%s)"
  if (( next_run <= now )); then next_run="$(date -u -d 'tomorrow 02:00' +%s)"; fi
  delay=$((next_run - now))
  log "Next scheduled backup at $(date -u -d "@$next_run" '+%Y-%m-%dT%H:%M:%SZ')"
  sleep "$delay"
  if ! run_backup; then log "Backup failed; will retry at the next scheduled run"; fi
done
