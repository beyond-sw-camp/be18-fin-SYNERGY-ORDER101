#!/bin/bash
echo "Stopping python-server..."

set +e

docker stop python-server || true
docker rm python-server || true

exit 0
