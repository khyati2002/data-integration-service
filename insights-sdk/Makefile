cleanInstall:
	mvn clean install -f pom.xml -s ../settings.xml -DskipTests -Psb3 -e

setVersion:
	@if [ -z "$(version)" ]; then \
		echo "Error: version is not set. Use 'make deploy version=x.y.z'"; \
		exit 1; \
	fi
	mvn versions:set -DnewVersion=$(version) -f pom.xml -s ../settings.xml
	mvn versions:commit -f pom.xml -s ../settings.xml

deploy: cleanInstall setVersion
	export CODEARTIFACT_AUTH_TOKEN=`aws codeartifact get-authorization-token --domain salescode --domain-owner 008136251604 --region ap-south-1 --query authorizationToken --output text`
#	mvn verify -f pom.xml -s ../settings.xml
#	mvn deploy -f pom.xml -s ../settings.xml
	mvn verify -f pom.xml -s ../settings.xml -Psb3 -Dclassifier=sb3
	mvn deploy -f pom.xml -s ../settings.xml -Psb3 -Dclassifier=sb3

profile?=default
version?=0.0.1-SNAPSHOT