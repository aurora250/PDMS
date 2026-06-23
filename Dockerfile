FROM eclipse-temurin:21-jre-alpine

ARG JAR_FILE
COPY ${JAR_FILE} app.jar

RUN echo '#!/bin/sh' > /entrypoint.sh && \
    echo 'echo "127.0.0.1 $(hostname)" >> /etc/hosts' >> /entrypoint.sh && \
    echo 'exec java -XX:+UseZGC -XX:+ZGenerational -XX:+EnableDynamicAgentLoading -jar /app.jar' >> /entrypoint.sh && \
    chmod +x /entrypoint.sh

ENTRYPOINT ["/entrypoint.sh"]
