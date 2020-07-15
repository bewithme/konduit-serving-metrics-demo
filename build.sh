#!/usr/bin/env bash

echo "Building Docker image."
nvidia-docker build -t local/konduit-demo:v2 .

echo "Finished build."
echo
echo "Start demo by running: docker-compose up"
