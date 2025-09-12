# Root Makefile for data-integration-service

# =============================
# Common variables
# =============================
profile    ?= default
version    ?= 0.0.13-SNAPSHOT
insights_version ?=
debug      ?= false

# if you want Maven to update snapshots/releases, invoke with:
#   make ... MAVEN_UPDATE_SNAPSHOT=-U
MAVEN_UPDATE_SNAPSHOT ?=

ifeq ($(debug),true)
  MAVEN_DEBUG_FLAGS = -e -X
else
  MAVEN_DEBUG_FLAGS = -q
endif

# =============================
# Default target
# =============================
all: insights-common-cleanInstall insights-sdk-cleanInstall insights-cleanInstall

# =============================
# Clean everything
# =============================
clean:
	@echo "Cleaning all submodules..."
	mvn clean -f insights-common/pom.xml -s settings.xml $(MAVEN_DEBUG_FLAGS)
	mvn clean -f insights-sdk/pom.xml 	 -s settings.xml $(MAVEN_DEBUG_FLAGS)
	mvn clean -f insights/pom.xml 		 -s settings.xml $(MAVEN_DEBUG_FLAGS)

	@echo "Clearing Maven cache for insights modules..."
	rm -rf ~/.m2/repository/com/salescode/dis/insights-common
	rm -rf ~/.m2/repository/com/salescode/dis/insights-sdk
	rm -rf ~/.m2/repository/com/salescode/dis/insights

# =============================
# insights-common
# =============================
insights-common-setVersion:
	@echo "Setting version for insights-common..."
	@if [ -z "$(version)" ]; then \
	  echo "Error: version is not set."; \
	  exit 1; \
	fi
	mvn versions:set -DnewVersion=$(version) -f insights-common/pom.xml -s settings.xml $(MAVEN_DEBUG_FLAGS)
	mvn versions:commit -f insights-common/pom.xml -s settings.xml $(MAVEN_DEBUG_FLAGS)

insights-common-cleanInstall: insights-common-setVersion
	@echo "Running clean install for insights-common..."
	export CODEARTIFACT_AUTH_TOKEN=`aws codeartifact get-authorization-token --domain salescode --domain-owner 008136251604 --region ap-south-1 --query authorizationToken --output text` && \
	JAVA_HOME=$$(/usr/libexec/java_home -v 11) \
	mvn clean install -f insights-common/pom.xml -s settings.xml -DskipTests -Psb3 $(MAVEN_DEBUG_FLAGS)

insights-common-deploy: clean insights-common-cleanInstall
	@echo "--- Deploying insights-common ---"
	export CODEARTIFACT_AUTH_TOKEN=`aws codeartifact get-authorization-token --domain salescode --domain-owner 008136251604 --region ap-south-1 --query authorizationToken --output text` && \
	mvn verify -f insights-common/pom.xml -s settings.xml -Psb3 -Dclassifier=sb3 $(MAVEN_DEBUG_FLAGS) && \
	mvn deploy -f insights-common/pom.xml -s settings.xml -Psb3 -Dclassifier=sb3 $(MAVEN_DEBUG_FLAGS)

# =============================
# insights-sdk
# =============================
insights-sdk-setVersion:
	@echo "Setting version for insights-sdk..."
	@if [ -z "$(version)" ]; then \
	  echo "Error: version is not set."; \
	  exit 1; \
	fi
	mvn versions:set -DnewVersion=$(version) -f insights-sdk/pom.xml -s settings.xml $(MAVEN_DEBUG_FLAGS)
	mvn versions:set-property -Dproperty="insights-common.version" -DnewVersion=$(version) -f insights-sdk/pom.xml -s settings.xml $(MAVEN_DEBUG_FLAGS)
	mvn versions:commit -f insights-sdk/pom.xml -s settings.xml $(MAVEN_DEBUG_FLAGS)

insights-sdk-cleanInstall: insights-sdk-setVersion
	@echo "Running clean install for insights-sdk..."
	export CODEARTIFACT_AUTH_TOKEN=`aws codeartifact get-authorization-token --domain salescode --domain-owner 008136251604 --region ap-south-1 --query authorizationToken --output text` && \
	JAVA_HOME=$$(/usr/libexec/java_home -v 11) \
	mvn clean install -f insights-sdk/pom.xml -s settings.xml -DskipTests -Psb3 $(MAVEN_DEBUG_FLAGS)

insights-sdk-deploy: clean insights-sdk-cleanInstall
	@echo "--- Deploying insights-sdk ---"
	export CODEARTIFACT_AUTH_TOKEN=`aws codeartifact get-authorization-token --domain salescode --domain-owner 008136251604 --region ap-south-1 --query authorizationToken --output text` && \
	mvn verify -f insights-sdk/pom.xml -s settings.xml -Psb3 -Dclassifier=sb3 $(MAVEN_DEBUG_FLAGS) && \
	mvn deploy -f insights-sdk/pom.xml -s settings.xml -Psb3 -Dclassifier=sb3 $(MAVEN_DEBUG_FLAGS)

# =============================
# insights
# =============================
insights-setVersion:
	@echo "Setting version for insights..."
	@if [ -z "$(version)" ]; then \
	  echo "Error: version is not set."; \
	  exit 1; \
	fi
	export CODEARTIFACT_AUTH_TOKEN=`aws codeartifact get-authorization-token --domain salescode --domain-owner 008136251604 --region ap-south-1 --query authorizationToken --output text` && \
	([ -n "$$insights_version" ] && mvn versions:set -DnewVersion=$$insights_version -f insights/pom.xml -s settings.xml $(MAVEN_DEBUG_FLAGS) || echo "Skipping mvn versions:set as insights-version is empty") && \
	mvn versions:set-property -Dproperty="insights-common.version" -DnewVersion=$(version) -f insights/pom.xml -s settings.xml $(MAVEN_DEBUG_FLAGS) && \
	mvn versions:commit -f insights/pom.xml -s settings.xml $(MAVEN_DEBUG_FLAGS)

insights-cleanInstall: insights-setVersion
	@echo "Running clean install for insights..."
	export CODEARTIFACT_AUTH_TOKEN=`aws codeartifact get-authorization-token --domain salescode --domain-owner 008136251604 --region ap-south-1 --query authorizationToken --output text` && \
	mvn clean install -f insights/pom.xml -s settings.xml -DskipTests $(MAVEN_DEBUG_FLAGS)

insights-image-push: clean insights-cleanInstall
	@echo "Building Docker image for insights..."
	docker buildx build --progress plain --platform "linux/arm64" --provenance=false -t dis-insights ./insights
	@echo "Pushing Docker image for insights..."
	sh insights/push_image.sh $(profile)


#
# =============================
# Check latest version from CodeArtifact
# =============================
latest-version:
	@echo "Fetching latest version from CodeArtifact..."
	@commons=$$(aws codeartifact list-package-versions --package insights-common \
	  --domain salescode --domain-owner 008136251604 \
	  --repository dis-insights --format maven --namespace com.salescode.dis \
	  --no-cli-pager --sort-by PUBLISHED_TIME --query "$.defaultDisplayVersion" --output text) && \
	echo "insights-common latest version: $$commons"
	@sdk=$$(aws codeartifact list-package-versions --package insights-sdk \
	  --domain salescode --domain-owner 008136251604 \
	  --repository dis-insights --format maven --namespace com.salescode.dis \
	  --no-cli-pager --sort-by PUBLISHED_TIME --query "$.defaultDisplayVersion" --output text) && \
	echo "insights-sdk latest version: $$sdk"

