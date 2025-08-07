#!/bin/bash

# Exit if version is not passed as argument
if [ -z "$1" ]; then
  echo "ERROR: Version is not provided."
  echo "Usage: ./build.sh <version>"
  exit 1
fi

VERSION=$1

echo "Starting build process with version: $version ..."
insights_version=$VERSION make insights-cleanInstall
echo "✅ Build completed successfully."
