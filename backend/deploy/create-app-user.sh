#!/bin/sh
# One-off, on the EC2 host (as root, through SSM): creates the API's MySQL user on RDS,
# with TLS required and rights on the tripfair database only. Safe to run again.
# Needs a temporary role policy to read /tripfair/admin/MYSQL_MASTER_PASSWORD (see
# docs/deploy.md); the API itself never gets the master password.
set -eu
R=ap-southeast-1
get() { aws ssm get-parameter --region "$R" --name "$1" --with-decryption --query Parameter.Value --output text; }
MASTER=$(get /tripfair/admin/MYSQL_MASTER_PASSWORD)
APP=$(get /tripfair/prod/MYSQL_PASSWORD)
HOST=$(get /tripfair/prod/MYSQL_HOST)
mkdir -p /opt/tripfair/certs
curl -fsSL -o /opt/tripfair/certs/rds-ca.pem "https://truststore.pki.rds.amazonaws.com/$R/$R-bundle.pem"
docker run --rm -e MYSQL_PWD="$MASTER" -v /opt/tripfair/certs:/certs:ro mysql:8.4 \
  mysql --ssl-mode=VERIFY_IDENTITY --ssl-ca=/certs/rds-ca.pem -h "$HOST" -u tripfair_admin -e "
    CREATE USER IF NOT EXISTS 'tripfair'@'%' IDENTIFIED BY '$APP' REQUIRE SSL;
    ALTER USER 'tripfair'@'%' IDENTIFIED BY '$APP' REQUIRE SSL;
    GRANT ALL PRIVILEGES ON tripfair.* TO 'tripfair'@'%';
    SHOW GRANTS FOR 'tripfair'@'%';
    SELECT user, ssl_type FROM mysql.user WHERE user = 'tripfair';"
