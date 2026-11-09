#!/bin/sh
set -e

JAR="$1"
DIR="$2"
PORT="${3:-8081}"

if [ -z "$JAR" ] || [ -z "$DIR" ]; then
    echo "Usage: deploy.sh <jar> <deploy-dir> [port]"
    exit 1
fi

mkdir -p "$DIR"

if [ -f "$DIR/app.pid" ] && kill -0 "$(cat "$DIR/app.pid")" 2>/dev/null; then
    echo "Stopping old instance $(cat "$DIR/app.pid")"
    kill "$(cat "$DIR/app.pid")" || true
    sleep 5
fi

cp "$JAR" "$DIR/clinicbook.jar"
cd "$DIR"

BUILD_ID=dontKillMe JENKINS_NODE_COOKIE=dontKillMe \
    nohup java -jar clinicbook.jar --server.port="$PORT" > clinicbook.log 2>&1 &
echo $! > app.pid
echo "Started ClinicBook (pid $(cat app.pid)) on port $PORT, log: $DIR/clinicbook.log"
