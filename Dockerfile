FROM eclipse-temurin:17-jdk AS builder

WORKDIR /workspace
COPY . .
RUN chmod +x ./gradlew && ./gradlew :backend:installDist --no-daemon

FROM eclipse-temurin:17-jre

WORKDIR /app
COPY --from=builder /workspace/backend/build/install/backend /app/backend

ENV PORT=8080
EXPOSE 8080

CMD ["/app/backend/bin/backend"]
