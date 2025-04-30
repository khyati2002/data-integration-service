FROM amazoncorretto:21
COPY target/insights*.jar /usr/app/insights.jar
WORKDIR /usr/app
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "insights.jar"]