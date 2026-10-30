#!/bin/sh
set -eu

attempt=0
while ! mysql \
    --protocol=TCP \
    --host="$MYSQL_HOST" \
    --user="$MYSQL_USER" \
    --database="$MYSQL_DATABASE" \
    --batch \
    --skip-column-names \
    --execute="SELECT 1 FROM flyway_schema_history WHERE version = '2' AND success = 1 LIMIT 1" \
    2>/dev/null | grep -q '^1$'; do
  attempt=$((attempt + 1))
  if [ "$attempt" -ge 120 ]; then
    echo "Timed out waiting for Flyway migrations to finish in $MYSQL_DATABASE." >&2
    exit 1
  fi
  sleep 5
done

mysql \
  --protocol=TCP \
  --host="$MYSQL_HOST" \
  --user="$MYSQL_USER" \
  --database="$MYSQL_DATABASE" \
  < /seed/local-seed.sql
