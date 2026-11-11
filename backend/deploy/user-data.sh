#!/bin/bash
# First boot of the TripFair API host (Amazon Linux 2023 ships the AWS CLI and SSM agent).
set -eu
dnf install -y docker
systemctl enable --now docker
mkdir -p /opt/tripfair
