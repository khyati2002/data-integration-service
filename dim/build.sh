
mvn clean install -f jooq/pom.xml
mvn clean \
    compile \
    install -DskipTests=true -X