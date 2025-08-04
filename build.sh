#!/bin/bash

# Exit if VERSION is not set
if [[ -z "$VERSION" ]]; then
  echo "ERROR: VERSION is not set. Please pass it as an environment variable."
  echo "Usage: VERSION=1.2.3 ./build.sh"
  exit 1
fi

export version=$VERSION

echo "Starting build process with version: $version ..."
make clean
make all

echo "✅ Build completed successfully."
