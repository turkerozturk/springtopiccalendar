# TARGETPLATFORM ile mimari ayırt edilir
ARG TARGETPLATFORM

# ARMv7 → Ubuntu Jammy
FROM eclipse-temurin:17-jdk-jammy AS base_arm

# AMD64 → Alpine
FROM eclipse-temurin:17-jdk-alpine AS base_amd

# ------------------------------------------------
# Mimariye göre uygun base image seçilir
# ------------------------------------------------
FROM base_${TARGETPLATFORM##*/} AS final

WORKDIR /app

COPY daily-topic-tracker.jar .
COPY application.properties .

# Platforma özel JVM ayarları
ARG TARGETPLATFORM
RUN if [ "$TARGETPLATFORM" = "linux/arm/v7" ]; then \
        echo 'export JAVA_OPTS="-XX:+UseSerialGC -Xms128m -Xmx512m -XX:MaxMetaspaceSize=128m -XX:+ExitOnOutOfMemoryError"' >> /etc/profile; \
    else \
        echo 'export JAVA_OPTS="-Xms256m -Xmx512m"' >> /etc/profile; \
    fi

ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar daily-topic-tracker.jar"]

EXPOSE 8443
