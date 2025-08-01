# Root Makefile for data-integration-service

# Common variables
profile?=default
version?=0.0.7-SNAPSHOT # Default version, can be overridden

# Clean all generated files in all submodules
clean:
	@echo "Cleaning all submodules..."
	mvn clean -f insights-common/pom.xml -s settings.xml
	mvn clean -f insights-sdk/pom.xml -s settings.xml
	mvn clean -f insights/pom.xml -s settings.xml

# Build all submodules
all: insights-common-cleanInstall insights-sdk-cleanInstall insights-cleanInstall

# --- insights-common targets ---

insights-common-cleanInstall: insights-common-setVersion
	@echo "Running clean install for insights-common..."
	mvn clean install -f insights-common/pom.xml -s settings.xml -DskipTests -Psb3 -e -X

insights-common-setVersion:
	@echo "Setting version for insights-common..."
	@if [ -z "$(version)" ]; then \
		echo "Error: version is not set."; \
		exit 1; \
	fi
	mvn versions:set -DnewVersion=$(version) -f insights-common/pom.xml -s settings.xml
	mvn versions:commit -f insights-common/pom.xml -s settings.xml

insights-common-deploy: insights-common-cleanInstall
	@echo "Deploying insights-common..."
	export CODEARTIFACT_AUTH_TOKEN=`aws codeartifact get-authorization-token --domain salescode --domain-owner 008136251604 --region ap-south-1 --query authorizationToken --output text`
	mvn verify -f insights-common/pom.xml -s settings.xml -Psb3 -Dclassifier=sb3
	mvn deploy -f insights-common/pom.xml -s settings.xml -Psb3 -Dclassifier=sb3

# --- insights-sdk targets ---

insights-sdk-cleanInstall: insights-sdk-setVersion
	@echo "Running clean install for insights-sdk..."
	mvn clean install -f insights-sdk/pom.xml -s settings.xml -DskipTests -Psb3 -e

insights-sdk-setVersion:
	@echo "Setting version for insights-sdk..."
	@if [ -z "$(version)" ]; then \
		echo "Error: version is not set."; \
		exit 1; \
	fi
	mvn versions:set -DnewVersion=$(version) -f insights-sdk/pom.xml -s settings.xml
	mvn versions:commit -f insights-sdk/pom.xml -s settings.xml

insights-sdk-deploy: insights-sdk-cleanInstall
	@echo "Deploying insights-sdk..."
	export CODEARTIFACT_AUTH_TOKEN=`aws codeartifact get-authorization-token --domain salescode --domain-owner 008136251604 --region ap-south-1 --query authorizationToken --output text`
	mvn verify -f insights-sdk/pom.xml -s settings.xml -Psb3 -Dclassifier=sb3
	mvn deploy -f insights-sdk/pom.xml -s settings.xml -Psb3 -Dclassifier=sb3

# --- insights targets ---

insights-cleanInstall: insights-common-cleanInstall insights-sdk-cleanInstall
	@echo "Building Docker image for insights..."
	JAVA_HOME=$(/usr/libexec/java_home -v 17) mvn clean install -f insights/pom.xml -s settings.xml -DskipTests -e -U

insights-image-push: insights-build-image
	docker buildx build --progress plain --platform "linux/amd64" --provenance=false -t dis-insights .
	@echo "Pushing Docker image for insights..."
	sh insights/push_image.sh $(profile)

