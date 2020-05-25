#!/bin/sh

set -e

exec docker rm -f yum-server 2>/dev/null
