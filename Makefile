init:
	@echo "Initializing"; \
	export CODEARTIFACT_AUTH_TOKEN=`aws codeartifact get-authorization-token --domain salescode --domain-owner 008136251604 --region ap-south-1 --query authorizationToken --output text`; \
	mvn clean install -f jooq/pom.xml -s settings.xml; \
	mvn clean install -DskipTests=true -s settings.xml; \
	mvn clean install -f bundle/pom.xml -s settings.xml

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
	@export CODEARTIFACT_AUTH_TOKEN=`aws codeartifact get-authorization-token --domain salescode --domain-owner 008136251604 --region ap-south-1 --query authorizationToken --output text`; \
	mvn clean install -f bundle/pom.xml -s settings.xml
	@mkdir -p lib
	@rm -rf lib/* || true
	@cp bundle/target/bundle.jar lib/bundle.jar

generate-dis-jar:
	mvn clean compile install -DskipTests=true

generate-all: remove-submodule setup-submodule init generate-bundle generate-dis-jar

generate-bundle-only: init generate-bundle

generate-project-jar:
	@echo "Generating project JAR with AWS CodeArtifact authentication..."
	export CODEARTIFACT_AUTH_TOKEN=`aws codeartifact get-authorization-token --domain salescode --domain-owner 008136251604 --region ap-south-1 --query authorizationToken --output text`; \
	mvn clean install -f jooq/pom.xml -s settings.xml; \
	mvn clean compile install -DskipTests=true -s settings.xml

