#!/bin/sh
# Runs once, when the mysql-data volume is first created.
# Creates the shadow database used by `prisma migrate dev` and grants the app user
# access to that database only, so it never needs a global CREATE DATABASE privilege.
set -e

mysql --user=root --password="$MYSQL_ROOT_PASSWORD" <<SQL
CREATE DATABASE IF NOT EXISTS \`${MYSQL_DATABASE}_shadow\`;
GRANT ALL PRIVILEGES ON \`${MYSQL_DATABASE}_shadow\`.* TO '${MYSQL_USER}'@'%';
SQL
