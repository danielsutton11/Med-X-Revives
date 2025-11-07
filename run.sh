#!/bin/bash

# Discord Claim Bot - Run Script
# This script builds and runs the bot with environment variables

echo "🤖 Discord Claim Bot - Startup Script"
echo "======================================"
echo ""

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "❌ Java is not installed. Please install Java 17 or higher."
    exit 1
fi

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo "❌ Java 17 or higher is required. Current version: $JAVA_VERSION"
    exit 1
fi

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven is not installed. Please install Maven 3.6+."
    exit 1
fi

# Check environment variables
if [ -z "$DISCORD_TOKEN" ]; then
    echo "❌ DISCORD_TOKEN environment variable is not set!"
    echo "   Set it with: export DISCORD_TOKEN='your_token_here'"
    exit 1
fi

if [ -z "$TARGET_SERVER_ID" ]; then
    echo "❌ TARGET_SERVER_ID environment variable is not set!"
    echo "   Set it with: export TARGET_SERVER_ID='your_server_id'"
    exit 1
fi

if [ -z "$TARGET_CHANNEL_ID" ]; then
    echo "❌ TARGET_CHANNEL_ID environment variable is not set!"
    echo "   Set it with: export TARGET_CHANNEL_ID='your_channel_id'"
    exit 1
fi

echo "✅ Environment variables are set"
echo ""

# Check if JAR exists
if [ ! -f "target/discord-claim-bot-1.0.0.jar" ]; then
    echo "📦 Building project..."
    mvn clean package
    
    if [ $? -ne 0 ]; then
        echo "❌ Build failed!"
        exit 1
    fi
    echo "✅ Build successful"
    echo ""
fi

# Run the bot
echo "🚀 Starting Discord Claim Bot..."
echo ""
java -jar target/discord-claim-bot-1.0.0.jar
