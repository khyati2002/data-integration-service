#!/bin/bash

VERSION=${VERSION:-0.0.7-SNAPSHOT}

export version=$VERSION

echo "Starting build process ...."
make clean
make all

echo "Build completed successfully."
