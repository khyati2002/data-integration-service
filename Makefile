# Java 11 configuration for macOS
# Get the path by running: /usr/libexec/java_home -v 11
JAVA_HOME := $(shell /usr/libexec/java_home -v 11 2>/dev/null || echo "/Library/Java/JavaVirtualMachines/openjdk-11.jdk/Contents/Home")

init:
	@if [ -d "./bundle/target/" ]; then \
	   echo "Directory exists, skipping install."; \
	   exit 0; \
	else \
	   echo "Initializing"; \
	   echo "Using JAVA_HOME: $(JAVA_HOME)"; \
	   if [ ! -d "$(JAVA_HOME)" ]; then \
	       echo "ERROR: Java 11 not found. Install it with: brew install openjdk@11"; \
	       exit 1; \
	   fi; \
	   export JAVA_HOME=$(JAVA_HOME); \
	   export PATH=$(JAVA_HOME)/bin:$$PATH; \
	   java -version; \
	   export CODEARTIFACT_AUTH_TOKEN=`aws codeartifact get-authorization-token --domain salescode --domain-owner 008136251604 --region ap-south-1 --query authorizationToken --output text`; \
	   mvn clean install -f jooq/pom.xml -s settings.xml; \
	   JAVA_HOME=$(/usr/libexec/java_home -v 11) mvn clean install -DskipTests=true -s settings.xml; \
	   JAVA_HOME=$(/usr/libexec/java_home -v 11) mvn clean install -f bundle/pom.xml -s settings.xml; \
	fi

setup-submodule:
	@if [ -z "$(BRANCH)" ]; then \
	   echo "Error: BRANCH is not set."; \
	   exit 1; \
	elif [ "$(BRANCH)" = "main" ]; then \
	   echo "Skipping submodule setup for main branch."; \
	else \
	   echo "Adding submodule with branch: $(BRANCH)"; \
	   git submodule add -b $(BRANCH) https://applicatetech.git.beanstalkapp.com/data-integration-bundles.git bundle; \
	   git submodule update --init --recursive; \
	fi

remove-submodule:
	@echo "Attempting to clean up any existing submodule information for 'bundle'..."
	[ -d ".git/modules/bundle" ] && git submodule deinit -f bundle || true
	[ -d "bundle" ] && git rm -rf --cached bundle || true
	[ -d ".git/modules/bundle" ] && rm -rf .git/modules/bundle || true
	[ -d "bundle" ] && rm -rf bundle || true

generate-bundle:
	@echo "Generating bundle with AWS CodeArtifact authentication..."
	@echo "Using JAVA_HOME: $(JAVA_HOME)"; \
	if [ ! -d "$(JAVA_HOME)" ]; then \
	    echo "ERROR: Java 11 not found. Install it with: brew install openjdk@11"; \
	    exit 1; \
	fi; \
	export JAVA_HOME=$(JAVA_HOME); \
	export PATH=$(JAVA_HOME)/bin:$$PATH; \
	java -version; \
	export CODEARTIFACT_AUTH_TOKEN=`aws codeartifact get-authorization-token --domain salescode --domain-owner 008136251604 --region ap-south-1 --query authorizationToken --output text`; \
	mvn clean install -f bundle/pom.xml -s settings.xml
	@mkdir -p lib
	@rm -r lib/* || true
	cp bundle/target/bundle.jar lib/bundle.jar

generate-dis-jar:
	@echo "Generating DIS JAR with AWS CodeArtifact authentication..."
	@echo "Using JAVA_HOME: $(JAVA_HOME)"; \
	if [ ! -d "$(JAVA_HOME)" ]; then \
	    echo "ERROR: Java 11 not found. Install it with: brew install openjdk@11"; \
	    exit 1; \
	fi; \
	export JAVA_HOME=$(JAVA_HOME); \
	export PATH=$(JAVA_HOME)/bin:$$PATH; \
	java -version; \
	export CODEARTIFACT_AUTH_TOKEN=`aws codeartifact get-authorization-token --domain salescode --domain-owner 008136251604 --region ap-south-1 --query authorizationToken --output text`; \
	mvn clean compile install -DskipTests=true -s settings.xml

generate-all: remove-submodule setup-submodule init generate-bundle generate-dis-jar

generate-bundle-only: init generate-bundle

generate-project-jar:
	@echo "Generating project JAR with AWS CodeArtifact authentication..."
	@echo "Using JAVA_HOME: $(JAVA_HOME)"; \
	if [ ! -d "$(JAVA_HOME)" ]; then \
	    echo "ERROR: Java 11 not found. Install it with: brew install openjdk@11"; \
	    exit 1; \
	fi; \
	export JAVA_HOME=$(JAVA_HOME); \
	export PATH=$(JAVA_HOME)/bin:$$PATH; \
	java -version; \
	export CODEARTIFACT_AUTH_TOKEN=`aws codeartifact get-authorization-token --domain salescode --domain-owner 008136251604 --region ap-south-1 --query authorizationToken --output text`; \
	mvn clean install -f jooq/pom.xml -s settings.xml; \
	mvn clean compile install -DskipTests=true -s settings.xml

.PHONY: init setup-submodule remove-submodule generate-bundle generate-dis-jar generate-all generate-bundle-only generate-project-jar