#!/usr/bin/env bash

set -e

RPMDIR="$1"; shift || :
PORT="$1"; shift || :

if [ -z "$RPMDIR" ]; then
  echo "usage: $0 <rpmdir> [port]"
  echo ""
  exit 1
fi
RPMDIR="$(cd "$RPMDIR"; pwd -P)"

if [ -z "$PORT" ]; then
  PORT=19990
fi

MYDIR="$(dirname "$0")"
MYDIR="$(cd "$MYDIR"; pwd -P)"

cd "$MYDIR"

MYIP="$(./get_ip.sh)"

./build_yum_server.sh
./stop_yum_server.sh

docker run --detach --name yum-server --volume "${RPMDIR}:/repo" --network bridge --publish "${PORT}:${PORT}" opennms-yum-server

echo "waiting for server to be available..."
COUNT=0
while [ "$COUNT" -lt 30 ]; do
  COUNT="$((COUNT+1))"
  if curl --silent "http://${MYIP}:${PORT}/repodata/repomd.xml" >/dev/null 2>&1; then
    echo "ready"
    break
  fi
  sleep 1
done

if [ "$COUNT" -eq 30 ]; then
  echo "gave up waiting for server"
  echo "docker logs:"
  echo ""
  docker logs yum-server
  exit 1
fi
