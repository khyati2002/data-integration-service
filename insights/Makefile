# Build the Docker image
build-image:
	docker buildx build --progress plain --platform "linux/amd64" --provenance=false -t dis-insights .

# Execute the push_image.sh script to push the image with the updated version
image-push: build-image
	sh push_image.sh $(profile)

profile=devuat