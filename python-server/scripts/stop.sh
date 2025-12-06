#!/bin/bash
echo "Stopping python-server..."
docker stop python-server || true
docker rm python-server || true
