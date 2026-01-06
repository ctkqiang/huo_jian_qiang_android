#!/usr/bin/env bash
set -e

echo "📦 Installing debug build..."
./gradlew installDebug

echo "🔍 Finding JDWP processes..."
PIDS=$(adb jdwp)

if [ -z "$PIDS" ]; then
  echo "❌ No debuggable app found. Is the app running?"
  exit 1
fi

PID=$(echo "$PIDS" | tail -n 1)

echo "🎯 Using PID: $PID"
echo "🔗 Forwarding tcp:8700 → jdwp:$PID"

adb forward tcp:8700 jdwp:$PID

echo "✅ Ready"
echo "👉 Now start '🧠 Attach to Android App' in VS Code"
