@echo off
REM Discord Claim Bot - Windows Run Script

echo.
echo Discord Claim Bot - Startup Script
echo ======================================
echo.

REM Check if Java is installed
where java >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Java is not installed. Please install Java 17 or higher.
    pause
    exit /b 1
)

REM Check if Maven is installed
where mvn >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Maven is not installed. Please install Maven 3.6+.
    pause
    exit /b 1
)

REM Check environment variables
if "%DISCORD_TOKEN%"=="" (
    echo ERROR: DISCORD_TOKEN environment variable is not set!
    echo    Set it with: set DISCORD_TOKEN=your_token_here
    pause
    exit /b 1
)

if "%TARGET_SERVER_ID%"=="" (
    echo ERROR: TARGET_SERVER_ID environment variable is not set!
    echo    Set it with: set TARGET_SERVER_ID=your_server_id
    pause
    exit /b 1
)

if "%TARGET_CHANNEL_ID%"=="" (
    echo ERROR: TARGET_CHANNEL_ID environment variable is not set!
    echo    Set it with: set TARGET_CHANNEL_ID=your_channel_id
    pause
    exit /b 1
)

echo Environment variables are set
echo.

REM Check if JAR exists
if not exist "target\discord-claim-bot-1.0.0.jar" (
    echo Building project...
    call mvn clean package
    
    if %ERRORLEVEL% NEQ 0 (
        echo ERROR: Build failed!
        pause
        exit /b 1
    )
    echo Build successful
    echo.
)

REM Run the bot
echo Starting Discord Claim Bot...
echo.
java -jar target\discord-claim-bot-1.0.0.jar

pause
