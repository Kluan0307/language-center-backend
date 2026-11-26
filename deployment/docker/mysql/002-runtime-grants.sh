#!/bin/bash
# Invoked by the official MySQL entrypoint only when the data directory is new.
set -eu
if [[ ! "${MYSQL_USER:-}" =~ ^[A-Za-z][A-Za-z0-9_]{0,31}$ ]] || [[ "$MYSQL_USER" == root ]]; then
  echo 'MYSQL_USER must be a dedicated application account' >&2
  exit 1
fi
if [[ "${MYSQL_DATABASE:-}" != language_center_db ]]; then
  echo 'Unexpected application database' >&2
  exit 1
fi
MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql --protocol=socket --user=root <<SQL
REVOKE ALL PRIVILEGES, GRANT OPTION FROM '$MYSQL_USER'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON \`language_center_db\`.* TO '$MYSQL_USER'@'%';
SQL
