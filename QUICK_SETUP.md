# Quick Setup Guide - Persistent Revive Request Buttons

## Step 1: Update Environment Variables

Add this new variable to your Railway project:

```
REQUEST_CHANNEL_ID=123456789012345678
```

To get your channel ID:
1. Enable Developer Mode in Discord (Settings > Advanced > Developer Mode)
2. Right-click the channel you want for revive requests
3. Click "Copy Channel ID"

## Step 2: Deploy Updated Code

```bash
cd your-bot-directory
git add .
git commit -m "Add persistent revive request buttons"
git push
```

Railway will automatically redeploy.

## Step 3: Setup the Channel

Once the bot is online:

1. Go to your revive request channel (the one matching REQUEST_CHANNEL_ID)
2. Type: `/setup-revive-channel`
3. The bot will create two messages:
   - Instructions embed
   - Button panel with "Bust Me!" and "Bust Someone Else"

**Done!** Users can now click the buttons to request revives.

## What Users See

### When clicking "Bust Me!"
- Modal pops up
- Field: "Revive Type" (optional, defaults to full)
- User can type "full" or "partial"
- Submits request for themselves

### When clicking "Bust Someone Else"
- Modal pops up
- Field 1: "The Target(s)" - Enter Torn ID or profile link
- Field 2: "Min-Max Level" (optional)
- Submits request for the target
- Shows who requested it in the embed

## Example User Inputs

### For "Bust Someone Else" - Target field accepts:
✅ `1234567`
✅ `[1234567]`
✅ `https://www.torn.com/profiles.php?XID=1234567`
✅ `PlayerName [1234567]`

### For "Bust Me!" - Revive Type field accepts:
✅ `full` (default if left blank)
✅ `partial`
✅ `Full Revive`
✅ `part`

## Testing

After setup, test both buttons:

1. **Test "Bust Me!"**:
   - Must be linked to Torn
   - Must be in hospital
   - Must have revives enabled

2. **Test "Bust Someone Else"**:
   - Enter a valid Torn ID
   - Target must be in hospital
   - Target must have revives enabled

## Customization

### Change Button Text

Edit `ClaimBot.java` around line 245:
```java
Button reviveMeButton = Button.primary("revive_me", "Your Text Here");
Button reviveSomeoneButton = Button.secondary("revive_someone", "Your Text Here");
```

### Change Instructions

Edit `ClaimBot.java` around line 225, modify the `.setDescription()` content.

### Change Colors

```java
.setColor(Color.decode("#5865F2"))  // Discord Blurple
.setColor(Color.decode("#ED4245"))  // Red
.setColor(Color.decode("#57F287"))  // Green
```

## Files Changed

- `src/main/java/com/claimbot/ClaimBot.java` - Main bot logic
- `pom.xml` - No changes needed
- Environment variables - Added `REQUEST_CHANNEL_ID`

## Old Commands Still Work

The `/r` slash command is still available:
- `/r` - Request for yourself
- `/r userid:1234567` - Request for someone else
- `/r userid:1234567 fullrevive:false` - Partial revive

## Complete Environment Variables

Make sure you have all of these in Railway:

```
DISCORD_TOKEN=your_bot_token
TARGET_SERVER_ID=your_server_id
TARGET_CHANNEL_CONTRACT=contract_channel_id
TARGET_CHANNEL_FULL=full_revive_channel_id
TARGET_CHANNEL_PARTIAL=partial_revive_channel_id
REQUEST_CHANNEL_ID=request_channel_id          ⭐ NEW
TARGET_ROLE_ID_FULL_REVIVE=full_role_id
TARGET_ROLE_ID_PART_REVIVE=partial_role_id
TORN_API_KEY=your_torn_api_key
CONTRACT_FACTION_IDS=123,456,789              (optional)
```

## Support

If something doesn't work:
1. Check Railway logs for errors
2. Verify REQUEST_CHANNEL_ID is correct
3. Make sure you ran `/setup-revive-channel` in the right channel
4. Confirm the bot has permissions to send messages/embeds
