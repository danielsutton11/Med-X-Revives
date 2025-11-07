# Railway Deployment Guide - Java Discord Bot

## Prerequisites

Before deploying to Railway, you need:

1. **Discord Bot Token**
   - Created at [Discord Developer Portal](https://discord.com/developers/applications)
   - MESSAGE CONTENT INTENT enabled

2. **Server and Channel IDs**
   - Enable Developer Mode in Discord
   - Right-click server → Copy Server ID
   - Right-click channel → Copy Channel ID

3. **GitHub Account**
   - Code must be in a GitHub repository

## Step-by-Step Deployment

### 1. Prepare Your Repository

```bash
# Initialize git (if not already done)
git init

# Add all files
git add .

# Commit
git commit -m "Initial commit - Java Discord Claim Bot"

# Create repository on GitHub, then:
git remote add origin https://github.com/YOUR_USERNAME/YOUR_REPO_NAME.git
git branch -M main
git push -u origin main
```

### 2. Deploy to Railway

1. Go to [Railway.app](https://railway.app) and sign in
2. Click **"New Project"**
3. Select **"Deploy from GitHub repo"**
4. Authorize Railway to access your GitHub
5. Select your bot repository
6. Railway will automatically:
   - Detect it's a Maven project
   - Run `mvn clean package`
   - Start the application

### 3. Configure Environment Variables

In your Railway project:

1. Click on your service
2. Go to **"Variables"** tab
3. Click **"+ New Variable"**
4. Add each of these:

```
DISCORD_TOKEN=your_bot_token_here
TARGET_SERVER_ID=your_server_id_here
TARGET_CHANNEL_ID=your_channel_id_here
```

**Important:** After adding variables, the service will automatically redeploy.

### 4. Verify Deployment

#### Check Build Logs

In the "Deployments" tab, you should see:
```
[INFO] Building claim-bot 1.0.0
[INFO] BUILD SUCCESS
```

#### Check Runtime Logs

Click "View Logs" - you should see:
```
✅ Bot is ready! Logged in as YourBot#1234
📊 Serving X servers
🌐 Health check server running on port XXXX
```

#### Test Health Endpoint

1. Go to **Settings** → find your Railway URL
2. Visit the URL in browser - should show:
```json
{
  "status": "online",
  "bot": "YourBot#1234",
  "servers": 1,
  "uptime": 12345
}
```

### 5. Invite Bot to Servers

Generate OAuth2 URL:

1. Discord Developer Portal → Your Application
2. OAuth2 → URL Generator
3. **Scopes:** `bot`
4. **Permissions:**
   - Read Messages/View Channels
   - Send Messages
   - Embed Links
   - Read Message History

5. Copy the generated URL
6. Invite to:
   - Your central server (where claims appear)
   - Any test servers

### 6. Test the Bot

1. In a server with the bot, type: `!r`
2. Check your designated channel - claim message should appear
3. Click the "✅ Claim" button
4. Message should update showing who claimed it

## Troubleshooting

### Build Fails

**Error:** `Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin`

**Solution:**
- Check `pom.xml` has correct Java version (17)
- Verify all dependencies are accessible
- Check Railway logs for specific error

**Try:** In Railway settings, set build command:
```
mvn clean package -DskipTests
```

### Bot is Offline

**Check:**
1. Railway logs for errors
2. DISCORD_TOKEN is correct (no spaces)
3. Bot token wasn't regenerated
4. MESSAGE CONTENT INTENT is enabled

**Solution:**
- Redeploy from Railway dashboard
- Verify environment variables are set

### Configuration Error Messages

**Error:** "Configuration error. Please contact the bot administrator."

**Check:**
1. TARGET_SERVER_ID is correct
2. TARGET_CHANNEL_ID is correct
3. Bot is actually in your target server
4. Bot has permissions to send messages in target channel

### Railway-Specific Issues

**Port Already in Use:**
- Railway automatically sets `PORT` environment variable
- Bot uses this for health check server
- Don't manually set PORT

**App Crashes After Deploy:**
- Check if JAR file was created: `target/claim-bot-1.0.0.jar`
- Verify `Procfile` points to correct JAR name
- Check for Java version mismatch

**Out of Memory:**
- Railway default is 512MB RAM
- Increase in Settings → Resources if needed

## Railway Configuration

### Build Settings

Railway auto-detects Maven and runs:
```bash
mvn clean package
```

### Start Command

Defined in `Procfile`:
```
web: java -jar target/claim-bot-1.0.0.jar
```

### Health Checks

Railway monitors the HTTP endpoint on assigned PORT. Bot provides:
- `/` - Status JSON
- `/health` - Health check JSON

## Updating Your Bot

### Deploy New Version

```bash
# Make your changes
git add .
git commit -m "Update: description of changes"
git push

# Railway automatically deploys on push to main branch
```

### Manual Redeploy

In Railway dashboard:
1. Go to Deployments
2. Click latest deployment
3. Click **"Redeploy"**

### Rollback

1. Go to Deployments
2. Find previous working deployment
3. Click **"Redeploy"**

## Monitoring

### View Logs

**Real-time:**
```bash
# Install Railway CLI
npm i -g @railway/cli

# Login
railway login

# View logs
railway logs
```

**In Dashboard:**
- Click your service → "View Logs"

### Check Status

Visit your Railway URL:
- Shows bot online status
- Number of servers
- Uptime

### Alerts

Railway can notify you via:
- Email
- Slack
- Discord webhook

Set up in: Project Settings → Notifications

## Cost & Usage

### Railway Pricing

- **Free Plan:** $5 credit/month
- **Pro Plan:** $20/month (includes $20 credit)
- **Usage-based:** ~$0.002/hour (~$1.50/month)

### Typical Bot Usage

A Discord bot typically uses:
- **RAM:** 200-400 MB
- **CPU:** 0.01-0.05 vCPU
- **Cost:** $0.50-2.00/month

The free $5 credit covers most Discord bots!

### Monitor Usage

Railway Dashboard → Project → Usage
- See RAM/CPU usage
- Track costs
- Set usage alerts

## Environment Variables Reference

| Variable | Required | Description | Example |
|----------|----------|-------------|---------|
| `DISCORD_TOKEN` | Yes | Bot token from Discord Developer Portal | `MTIzNDU2Nzg5...` |
| `TARGET_SERVER_ID` | Yes | Server ID where claims are sent | `123456789012345678` |
| `TARGET_CHANNEL_ID` | Yes | Channel ID for claim messages | `987654321098765432` |
| `PORT` | No | Auto-set by Railway for health check | `8080` |

## Best Practices

### Security
✅ Never commit tokens to Git  
✅ Use Railway's encrypted variables  
✅ Rotate tokens if exposed  
✅ Use least-privilege permissions  

### Performance
✅ Monitor RAM usage  
✅ Implement rate limiting  
✅ Clean up old claims  
✅ Use async operations  

### Reliability
✅ Check health endpoint regularly  
✅ Set up Railway notifications  
✅ Keep dependencies updated  
✅ Test before deploying  

## Need Help?

- **Railway Discord:** https://discord.gg/railway
- **Railway Docs:** https://docs.railway.app
- **JDA Discord:** https://discord.gg/jda
- **JDA Wiki:** https://jda.wiki

## Quick Reference

### Useful Commands

```bash
# Build locally
mvn clean package

# Run locally
java -jar target/claim-bot-1.0.0.jar

# Railway CLI login
railway login

# View Railway logs
railway logs

# Link to Railway project
railway link
```

### Important URLs

- Discord Developer Portal: https://discord.com/developers/applications
- Railway Dashboard: https://railway.app/dashboard
- JDA Documentation: https://ci.dv8tion.net/job/JDA5/javadoc/
- Maven Repository: https://mvnrepository.com/

---

🎉 **Your bot is now deployed and running 24/7 on Railway!**
