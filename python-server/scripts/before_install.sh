#!/bin/bash
sudo systemctl stop order101-ai || true
docker stop order101-ai || true
docker rm order101-ai || true
docker rmi $(docker images -q "order101-ai") || true
