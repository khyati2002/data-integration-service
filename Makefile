init:
	@if [ -d "./bundle/target/" ]; then \
		echo "Directory exists, skipping install."; \
		exit 0; \
	else \
	  	echo "Initializing"; \
		mvn clean install -f jooq/pom.xml; \
		mvn clean install -DskipTests=true; \
		mvn clean install -f bundle/pom.xml; \
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
	@if [ -z "$(BRANCH)" ]; then \
		echo "Error: BRANCH is not set." ; \
		exit 1 ; \
	elif [ "$(BRANCH)" = "main" ]; then \
		echo "Skipping submodule setup for main branch." ; \
	else \
		echo "Attempting to clean up any existing submodule information for 'bundle'..." ; \
		[ -d ".git/modules/bundle" ] && git submodule deinit -f bundle || true ; \
		[ -d "bundle" ] && git rm -rf --cached bundle || true ; \
		[ -d ".git/modules/bundle" ] && rm -rf .git/modules/bundle || true ; \
		[ -d "bundle" ] && rm -rf bundle || true ; \
	fi



generate-bundle:
	mvn clean install -f bundle/pom.xml
	@mkdir -p lib
	@rm -r lib/* || true
	cp bundle/target/bundle.jar lib/bundle.jar

generate-dis-jar:
	mvn clean compile install -DskipTests=true

generate-all: setup-submodule init generate-bundle generate-dis-jar

generate-bundle-only: init generate-bundle

generate-project-jar:
	mvn clean install -f bundle/pom.xml
	mvn clean compile install -DskipTests=true
