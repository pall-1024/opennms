#!/usr/bin/env bash

# Exit script if a statement returns a non-true return value.
set -o errexit

# Use the error status of the first failure, rather than that of the last item in a pipeline.
set -o pipefail

CONTAINER_NAME="yum-repo"

docker rm -f "${CONTAINER_NAME}" 2>/dev/null
