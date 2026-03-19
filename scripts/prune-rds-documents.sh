#!/usr/bin/env bash
set -euo pipefail

APP_DIR="${APP_DIR:-/home/ec2-user/policymind-src}"
ENV_FILE="${ENV_FILE:-$APP_DIR/.env.production}"
PSQL_RUNNER_CONTAINER="${PSQL_RUNNER_CONTAINER:-policymind_postgres}"
DB_THRESHOLD_PERCENT="${DB_THRESHOLD_PERCENT:-50}"
DB_ALLOCATED_GIB="${DB_ALLOCATED_GIB:-20}"
MIN_AGE_DAYS="${MIN_AGE_DAYS:-30}"
MAX_DELETE_PER_RUN="${MAX_DELETE_PER_RUN:-200}"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Missing env file: $ENV_FILE" >&2
  exit 1
fi

set -a
source "$ENV_FILE"
set +a

DB_HOST="${DB_HOST:-}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${POSTGRES_DB:-policymind}"
DB_USER="${POSTGRES_USER:-}"
DB_PASSWORD="${POSTGRES_PASSWORD:-}"

if [[ -z "$DB_HOST" || -z "$DB_USER" || -z "$DB_PASSWORD" ]]; then
  echo "DB_HOST, POSTGRES_USER, and POSTGRES_PASSWORD must be set in $ENV_FILE" >&2
  exit 1
fi

threshold_bytes=$(( DB_ALLOCATED_GIB * 1024 * 1024 * 1024 * DB_THRESHOLD_PERCENT / 100 ))

psql_query() {
  local database="$1"
  local sql="$2"
  docker exec -e PGPASSWORD="$DB_PASSWORD" "$PSQL_RUNNER_CONTAINER" \
    psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$database" -tA -c "$sql"
}

db_size_bytes() {
  psql_query postgres "select pg_database_size('$DB_NAME');" | tr -d '[:space:]'
}

pick_oldest_document_id() {
  psql_query "$DB_NAME" "
    select id
    from documents
    where status in ('COMPLETED', 'FAILED')
      and coalesce(completed_at, updated_at, created_at) < now() - interval '${MIN_AGE_DAYS} days'
    order by coalesce(completed_at, updated_at, created_at) asc, id asc
    limit 1;
  " | tr -d '[:space:]'
}

delete_document() {
  local document_id="$1"
  psql_query "$DB_NAME" "
    begin;
    delete from document_chunk where document_id = ${document_id};
    delete from documents where id = ${document_id};
    commit;
  " >/dev/null
}

current_size="$(db_size_bytes)"
if [[ -z "$current_size" ]]; then
  echo "Could not determine RDS database size." >&2
  exit 1
fi

echo "Current DB size bytes: $current_size"
echo "Threshold bytes: $threshold_bytes"

if (( current_size <= threshold_bytes )); then
  echo "Database size is below threshold. Nothing to prune."
  exit 0
fi

deleted_count=0
while (( current_size > threshold_bytes )) && (( deleted_count < MAX_DELETE_PER_RUN )); do
  document_id="$(pick_oldest_document_id)"
  if [[ -z "$document_id" ]]; then
    echo "No eligible old documents found for pruning."
    break
  fi

  echo "Pruning oldest eligible document id=$document_id"
  delete_document "$document_id"
  deleted_count=$((deleted_count + 1))
  current_size="$(db_size_bytes)"
  echo "Database size after delete bytes: $current_size"
done

echo "Prune run complete. Deleted documents: $deleted_count"
