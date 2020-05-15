#!/bin/bash -e

pushd yum-server
	./build_yum_server.sh
popd

for file in */build_container_image.sh; do
	DIR="$(dirname "$file")"
	pushd "$DIR"
		./build_container_image.sh
	popd
done
