# Discord Claim Bot (Java)

A Java Discord bot using JDA that allows users in any server to send claim requests to your central server using the `!r` command.

## Features

- ✅ Simple `!r` command to request claims
- 🎯 Forwards claim requests to your designated server/channel
- 🔘 Interactive claim button system
- 📊 Shows user info, server, channel, and timestamp
- 🔒 Prevents duplicate claims
- 📝 Tracks who claimed each request
- 🌐 Built-in health check endpoint for Railway

## Prerequisites

- Java 17 or higher
- Maven 3.6 or higher
- Discord Bot Token

## Local Setup

### 1. Create a Discord Bot

1. Go to [Discord Developer Portal](https://discord.com/developers/applications)
2. Click "New Application" and give it a name
3. Go to the "Bot" section
4. Click "Add Bot"
5. Under "Privileged Gateway Intents", enable:
   - MESSAGE CONTENT INTENT
   - SERVER MEMBERS INTENT (optional)
6. Click "Reset Token" and copy your bot token

### 2. Get Server and Channel IDs

1. Enable Developer Mode in Discord (User Settings > Advanced > Developer Mode)
2. Right-click your server icon → "Copy Server ID"
3. Right-click the channel where claims should appear → "Copy Channel ID"

### 3. Set Environment Variables

**On Windows:**
```cmd
set DISCORD_TOKEN=your_bot_token_here
set TARGET_SERVER_ID=your_server_id
set TARGET_CHANNEL_ID=your_channel_id
```

**On Linux/Mac:**
```bash
export DISCORD_TOKEN=your_bot_token_here
export TARGET_SERVER_ID=your_server_id
export TARGET_CHANNEL_ID=your_channel_id
```

### 4. Build and Run

```bash
# Build the project
mvn clean package

# Run the bot
java -jar target/claim-bot-1.0.0.jar
```

You should see:
```
✅ Bot is ready! Logged in as YourBot#1234
📊 Serving 1 servers
🌐 Health check server running on port 8080
```

## Railway Deployment

### Quick Deploy

1. **Push to GitHub**
   ```bash
   git init
   git add .
   git commit -m "Initial commit"
   git remote add origin https://github.com/YOUR_USERNAME/YOUR_REPO.git
   git push -u origin main
   ```

2. **Deploy to Railway**
   - Go to [Railway.app](https://railway.app)
   - Click "New Project" → "Deploy from GitHub repo"
   - Select your repository
   - Railway will auto-detect Maven and build

3. **Set Environment Variables**
   In Railway dashboard, add:
   - `DISCORD_TOKEN` - Your bot token
   - `TARGET_SERVER_ID` - Your server ID
   - `TARGET_CHANNEL_ID` - Your channel ID

4. **Deploy**
   - Railway will automatically build and deploy
   - Check logs for "✅ Bot is ready!"

### Build Configuration

Railway automatically detects Maven projects and runs:
```bash
mvn clean package
java -jar target/claim-bot-1.0.0.jar
```

The `Procfile` specifies the startup command.

## Bot Permissions

When generating your OAuth2 invite URL:

**Scopes:** `bot`

**Bot Permissions:**
- Read Messages/View Channels
- Send Messages
- Embed Links
- Read Message History
- Use Slash Commands (optional)

## Usage

1. Invite bot to your central server (where claims will be sent)
2. Invite bot to any servers where users should use `!r`
3. User types `!r` in any channel
4. Claim message appears in your designated channel
5. Click "✅ Claim" button to process

## Example Claim Message

```
🎯 New Claim Request
Someone has requested a claim!

👤 User: JohnDoe#1234
🆔 User ID: 123456789012345678
🏠 Server: Gaming Community
📝 Channel: general
⏰ Time: January 1, 2025 3:45 PM

[✅ Claim Button]
```

## Project Structure

```
discord-claim-bot-java/
├── src/main/java/com/discord/claimbot/
│   ├── ClaimBot.java           # Main bot class
│   └── HealthCheckServer.java  # HTTP health check server
├── pom.xml                      # Maven dependencies
├── Procfile                     # Railway startup command
└── README.md
```

## Dependencies

- **JDA 5.0.0-beta.20** - Java Discord API
- **SLF4J 2.0.9** - Logging framework

## Troubleshooting

**Bot not responding:**
- Verify MESSAGE CONTENT INTENT is enabled
- Check bot has read message permissions
- Ensure environment variables are set correctly

**Build fails:**
- Verify Java 17+ and Maven 3.6+ are installed
- Check `pom.xml` for dependency issues
- Try `mvn clean install`

**Can't send to target channel:**
- Verify TARGET_SERVER_ID and TARGET_CHANNEL_ID
- Ensure bot is in your target server
- Check bot has send message permissions

**Railway deployment issues:**
- Check Railway logs for errors
- Verify all environment variables are set
- Ensure `Procfile` points to correct JAR

## Development

**Build without running:**
```bash
mvn clean compile
```

**Run tests:**
```bash
mvn test
```

**Create JAR:**
```bash
mvn package
```

## Advanced Customization

### Change Command

Edit `ClaimBot.java` line 74:
```java
if (event.getMessage().getContentRaw().equalsIgnoreCase("!r")) {
```

### Add More Fields

Edit the embed around line 103:
```java
.addField("Custom Field", "Custom Value", true)
```

### Add Cooldowns

Track last usage per user with a `Map<String, Long>`:
```java
private final Map<String, Long> cooldowns = new ConcurrentHashMap<>();
```

## Security Notes

- Never commit bot tokens to Git
- Use environment variables for sensitive data
- Implement rate limiting for production
- Validate all user input

## License

MIT

## Support

For issues or questions:
- Discord.js Guide: https://discord.com/developers/docs
- JDA Wiki: https://jda.wiki
- Railway Docs: https://docs.railway.app
