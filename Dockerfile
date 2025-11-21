FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

COPY daily-topic-tracker.jar .
COPY application.properties .

RUN echo 'export JAVA_OPTS="-XX:+UseSerialGC -Xms128m -Xmx512m -XX:MaxMetaspaceSize=128m -XX:+ExitOnOutOfMemoryError"' >> /etc/profile

ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar daily-topic-tracker.jar"]

EXPOSE 8443