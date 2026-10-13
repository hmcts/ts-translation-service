#!/usr/bin/env sh

set -eu

repo_root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
env_file=${1:-"$repo_root/.env.local"}

if [ -f "$env_file" ]; then
  echo "$env_file already exists; keeping the existing credentials."
  exit 0
fi

umask 077
password="$(openssl rand -hex 24)"

{
  printf '%s\n' "TS_TRANSLATION_SERVICE_DB_NAME=ts_translation_service"
  printf '%s\n' "TS_TRANSLATION_SERVICE_DB_USERNAME=ts"
  printf '%s\n' "TS_TRANSLATION_SERVICE_DB_PASSWORD=$password"
} > "$env_file"

chmod 600 "$env_file"
echo "Generated $env_file"
