mvn clean install -f jooq/pom.xml
mvn clean install -f bundle/pom.xml
mkdir -p lib
rm -r lib/*
cp bundle/target/bundle.jar lib/bundle.jar
mvn \
  clean \
    compile \
    install -DskipTests=true -X