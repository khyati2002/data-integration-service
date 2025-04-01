init:
	@if [ -d "$$HOME/.m2/repository/com/salescode/dim/" ]; then \
		echo "Directory exists, skipping install."; \
		exit 0; \
	else \
	  	echo "Initializing"; \
		mvn clean install -f jooq/pom.xml; \
		mvn clean install -DskipTests=true; \
		mvn clean install -f bundle/pom.xml; \
	fi

generate-bundle:
	mvn clean install -f bundle/pom.xml
	@mkdir -p lib
	@rm -r lib/*
	cp bundle/target/bundle.jar lib/bundle.jar

generate-dis-jar:
	mvn clean compile install -DskipTests=true

generate-all: init generate-bundle generate-dis-jar