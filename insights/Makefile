# Build the Docker image
build-image:
	mvn clean install -f ../insights-common/pom.xml -DskipTests -e
	mvn clean install -f ../insights-common/pom.xml -DskipTests -Psb3 -e
	mvn clean install -f ../insights-sdk/pom.xml -DskipTests -e
	mvn clean install -f ../insights-sdk/pom.xml -DskipTests  -Psb3 -e
	JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn clean install -DskipTests -e -X

# Execute the push_image.sh script to push the image with the updated version
image-push: build-image
	sh push_image.sh $(profile)

profile=devuat