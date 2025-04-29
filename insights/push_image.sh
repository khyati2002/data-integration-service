#!/bin/bash

# Define repository name and region as variables
repo_name="dis-insights"
region="ap-south-1"

# Set default AWS profile to 'default' if not provided
profile="${1:-default}"

# Echo the profile being used
echo "Using AWS profile: $profile"

# Fetch AWS Account ID
accountId=$(aws sts get-caller-identity --profile "$profile" --query "Account" --output text)
echo "AWS Account ID: $accountId"

# Fetch the latest image tag from ECR using variables
latest_version=$(aws ecr describe-images --repository-name "$repo_name" --region "$region" --profile "$profile" --query "imageDetails | sort_by(@, &imagePushedAt) | [0].imageTags[0]" --output text)
echo "Latest version from ECR: $latest_version"

# Check if the latest version is empty or doesn't match the expected format
if [[ -z "$latest_version" || ! "$latest_version" =~ ^v?[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "Latest version is empty or not in major.minor.patch format. Setting to 0.0.0"
  version="0.0.0"
else
# Extract the version number (remove the 'v' prefix if it exists)
version=$(echo "$latest_version" | sed "s/^v//")
echo "Extracted version: $version"
fi

# Split the version into major, minor, and patch components
if [[ "$version" =~ ^([0-9]+)\.([0-9]+)\.([0-9]+)$ ]]; then
  major=${BASH_REMATCH[1]}
  minor=${BASH_REMATCH[2]}
  patch=${BASH_REMATCH[3]}
echo "Major: $major, Minor: $minor, Patch: $patch"
else
  echo "Version does not match major.minor.patch format. Setting to 0.0.1"
  major=0
  minor=0
  patch=0
fi


# Increment the patch version
new_patch=$((patch + 1))
echo "New patch version: $new_patch"

# Construct the new version tag
new_version="$major.$minor.$new_patch"
echo "New version tag: $new_version"

# Log in to AWS ECR
echo "Logging in to ECR..."
login_result=$(aws ecr get-login-password --region "$region" --profile "$profile" | docker login --username AWS --password-stdin "$accountId.dkr.ecr.$region.amazonaws.com")
echo "ECR Login Result: $login_result"

# Tag the Docker image with the new version
echo "Tagging Docker image..."
docker tag "$repo_name":latest "$accountId.dkr.ecr.$region.amazonaws.com/$repo_name:$new_version"
echo "Docker image tagged as: $accountId.dkr.ecr.$region.amazonaws.com/$repo_name:$new_version"

# Push the Docker image to ECR
echo "Pushing Docker image to ECR..."
echo "Docker image push command: docker push \"$accountId.dkr.ecr.$region.amazonaws.com/$repo_name:$new_version\""
docker push "$accountId.dkr.ecr.$region.amazonaws.com/$repo_name:$new_version"
