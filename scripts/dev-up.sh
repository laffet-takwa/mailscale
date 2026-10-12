#!/bin/sh
# Starts the full MailScale stack (infrastructure + API + worker) and waits
# for the API to report healthy.
set -e

echo "Starting MailScale infrastructure..."
docker compose up -d postgres redis kafka mailpit

echo "Waiting for PostgreSQL..."
until docker compose exec -T postgres pg_isready -U mailscale -d mailscale >/dev/null 2>&1; do
  sleep 1
done

echo "Waiting for Kafka..."
until docker compose exec -T kafka kafka-topics.sh --bootstrap-server localhost:9092 --list >/dev/null 2>&1; do
  sleep 1
done

echo "Infrastructure ready."
cat <<'EOF'

Next steps:
  1. API     : ./mvnw -f backend-java/pom.xml spring-boot:run
  2. Worker  : ./gradlew -p worker-kotlin run
  3. Frontend: npm --prefix frontend run dev

Log in with admin@mailscale.local / demo1234
EOF
