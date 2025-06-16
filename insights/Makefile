# Build the Docker image
build-image:
#	mvn clean install -f ../insights-common/pom.xml -DskipTests -e
	#mvn clean install -f ../insights-common/pom.xml -DskipTests -Psb3 -e
#	mvn clean package -f ../insights-sdk/pom.xml -DskipTests -e
	#mvn clean install -f ../insights-sdk/pom.xml -DskipTests  -Psb3 -e
	JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn clean install -s ../settings.xml -DskipTests -e -U
	#docker buildx build --progress plain --platform "linux/amd64" --provenance=false -t dis-insights .

# Execute the push_image.sh script to push the image with the updated version
image-push: build-image
	sh push_image.sh $(profile)

profile=devuat