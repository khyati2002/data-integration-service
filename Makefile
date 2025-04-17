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
		echo "Attempting to clean up any existing submodule information for 'bundle'..."; \
		git submodule deinit -f bundle || true; \
		git rm -rf --cached bundle || true; \
		rm -rf .git/modules/bundle; \
		if [ -d "bundle" ]; then \
			echo "Double-checking and deleting 'bundle' directory..."; \
			rm -rf bundle; \
		fi; \
		echo "Adding submodule with branch: $(BRANCH)"; \
		git submodule add -b $(BRANCH) https://applicatetech.git.beanstalkapp.com/data-integration-bundles.git bundle; \
		git submodule update --init --recursive; \
	fi



generate-bundle:
	mvn clean install -f bundle/pom.xml
	@mkdir -p lib
	@rm -r lib/* || true
	cp bundle/target/bundle.jar lib/bundle.jar

generate-dis-jar:
	mvn clean compile install -DskipTests=true

generate-all: setup-submodule init generate-bundle generate-dis-jar

generate-bundle-only: setup-submodule init generate-bundle