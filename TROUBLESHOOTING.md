# Railway Deployment Troubleshooting

## Common Deployment Crashes and Fixes

### 1. Check Your Railway Logs

First, go to your Railway dashboard and check the deployment logs:
1. Railway Dashboard → Your Project → Deployments
2. Click on the failed deployment
3. Look for error messages

### 2. Most Common Issues

#### Issue: "No such file or directory" or "Cannot find JAR"

**Cause**: Railway can't find the built JAR file

**Fix**: The JAR file name must match exactly. Check these files:
- `nixpacks.toml` → `cmd = "java -jar target/discord-claim-bot-1.0.0.jar"`
- `railway.json` → `"startCommand": "java -jar target/discord-claim-bot-1.0.0.jar"`

The JAR name is: `{artifactId}-{version}.jar`
From pom.xml:
- artifactId: `discord-claim-bot`
- version: `1.0.0`
- Result: `discord-claim-bot-1.0.0.jar`

#### Issue: "Main class not found"

**Cause**: pom.xml has wrong main class reference

**Fix**: In pom.xml, line 72 should be:
```xml
<mainClass>com.claimbot.ClaimBot</mainClass>
```

NOT:
- `com.discord.claimbot.ClaimBot` ❌
- `claimbot.ClaimBot` ❌
- `ClaimBot` ❌

#### Issue: "Environment variable not set"

**Cause**: Missing Discord bot configuration

**Fix**: In Railway dashboard, add these variables:
```
DISCORD_TOKEN=your_actual_bot_token
TARGET_SERVER_ID=your_server_id_number
TARGET_CHANNEL_ID=your_channel_id_number
```

Make sure:
- No quotes around values
- No spaces before/after values
- Token is valid (copy fresh from Discord Developer Portal)

#### Issue: "Build failed" during Maven

**Cause**: Dependency download issues or Java version mismatch

**Fix**: 
1. Ensure `nixpacks.toml` has:
```toml
[phases.setup]
nixPkgs = ["maven", "jdk17"]
```

2. Ensure pom.xml has:
```xml
<properties>
    <maven.compiler.source>17</maven.compiler.source>
    <maven.compiler.target>17</maven.compiler.target>
</properties>
```

#### Issue: Bot crashes immediately after starting

**Possible causes**:
1. **Invalid Discord token**
   - Get a fresh token from Discord Developer Portal
   - Click "Reset Token" and copy the new one
   
2. **MESSAGE CONTENT intent not enabled**
   - Go to Discord Developer Portal
   - Your Bot → Bot Settings
   - Enable "MESSAGE CONTENT INTENT"
   - Save changes
   
3. **Bot not in target server**
   - Make sure bot is invited to the server specified in TARGET_SERVER_ID
   
4. **Invalid server/channel IDs**
   - Right-click server → Copy Server ID (make sure Developer Mode is on)
   - Right-click channel → Copy Channel ID
   - IDs should be 18-19 digit numbers

### 3. Step-by-Step Debugging

#### Step 1: Verify Environment Variables

In Railway logs, you should see at startup:
```
✅ Bot is ready! Logged in as YourBot#1234
📊 Serving X servers
```

If you see:
```
❌ DISCORD_TOKEN environment variable is not set!
```
→ Add the environment variable in Railway dashboard

#### Step 2: Check Build Logs

Look for:
```
[INFO] Building discord-claim-bot 1.0.0
[INFO] BUILD SUCCESS
```

If you see BUILD FAILURE, check the error message above it.

#### Step 3: Check Start Command

In Railway logs, look for what command is being run:
```
java -jar target/discord-claim-bot-1.0.0.jar
```

Should match your JAR file name exactly.

### 4. Manual Testing Locally

Before deploying to Railway, test locally:

```bash
# Set environment variables
export DISCORD_TOKEN="your_token"
export TARGET_SERVER_ID="your_server_id"
export TARGET_CHANNEL_ID="your_channel_id"

# Build
mvn clean package

# Check JAR was created
ls -lh target/*.jar

# Run locally
java -jar target/discord-claim-bot-1.0.0.jar
```

If it works locally, the issue is Railway-specific.

### 5. Railway-Specific Fixes

#### Force Rebuild

Sometimes Railway caches old builds:
1. Railway Dashboard → Settings
2. Scroll to "Danger Zone"
3. Click "Restart Deployment"

Or redeploy:
```bash
git commit --allow-empty -m "Force rebuild"
git push
```

#### Check Java Version

Add this to your code temporarily to debug:
```java
System.out.println("Java version: " + System.getProperty("java.version"));
```

Should show: `17.x.x`

### 6. Quick Fix Checklist

- [ ] pom.xml mainClass is `com.claimbot.ClaimBot`
- [ ] pom.xml artifactId is `discord-claim-bot`
- [ ] Java files are in `src/main/java/com/claimbot/`
- [ ] nixpacks.toml references correct JAR name
- [ ] railway.json references correct JAR name
- [ ] All 3 environment variables are set in Railway
- [ ] Discord token is valid and fresh
- [ ] MESSAGE CONTENT intent is enabled
- [ ] Bot is invited to target server
- [ ] Server ID and Channel ID are correct

### 7. Still Not Working?

Share your Railway deployment logs. Look for these key sections:
1. **Build logs** - Shows Maven build process
2. **Start logs** - Shows what command is being run
3. **Application logs** - Shows your bot's output

Common log locations in Railway:
- Deployments → Click deployment → View Logs
- Look for red error messages
- Copy the full error and search for it

### 8. Emergency: Start Fresh

If nothing works:
1. Delete the Railway project
2. Create new Railway project
3. Connect to GitHub repo
4. Add environment variables
5. Deploy

### Contact Support

If you're still stuck, I can help! Provide:
1. Full error message from Railway logs
2. Your pom.xml file
3. Your project structure (output of `ls -R`)
