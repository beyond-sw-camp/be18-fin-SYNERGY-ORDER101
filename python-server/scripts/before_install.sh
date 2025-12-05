#!/bin/bash

echo "Stopping existing python-server container (if exist)..."

docker stop order101-ai || true
docker rm order101-ai || true

echo "Cleanup old images..."
docker system prune -af || true
