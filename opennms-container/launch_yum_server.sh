#!/usr/bin/env bash

# Exit script if a statement returns a non-true return value.
set -o errexit

# Use the error status of the first failure, rather than that of the last item in a pipeline.
set -o pipefail


RPMDIR="$1"; shift || :
PORT="$1"; shift || :
CONTAINER_NAME="yum-repo"
OCI="opennms/yum-repo:1.0.0-b4609"

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

docker run --rm --detach --name "${CONTAINER_NAME}" --volume "${RPMDIR}:/repo" --network bridge --publish "${PORT}:${PORT}" "${OCI}"

echo "waiting for server to be available..."
COUNT=0
while [ "$COUNT" -lt 30 ]; do
  COUNT="$((COUNT+1))"
  if curl --fail --silent "http://${MYIP}:${PORT}/repodata/repomd.xml" >/dev/null 2>&1; then
    echo "ready"
    break
  fi
  sleep 1
done

if [ "$COUNT" -eq 30 ]; then
  echo "gave up waiting for server"
  echo "docker logs:"
  echo ""
  docker logs "${CONTAINER_NAME}"
  exit 1
fi
