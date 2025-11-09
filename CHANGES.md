# Discord Revive Bot - Changes Summary

## What's New

This update adds a dedicated revive request channel with persistent buttons and modal forms, similar to the Bounty Hunters Guild busting system shown in your reference images.

## New Features

### 1. Setup Command: `/setup-revive-channel`

**Admin-only command** that creates two persistent messages in your designated revive request channel:

1. **Instructions Message** - A professional embed with:
   - Customer guide on how to request revives
   - Seller guide on how to complete revives
   - Pricing information
   - Support information

2. **Button Message** - Interactive buttons:
   - **"Bust Me!"** button - For users to request a revive for themselves
   - **"Bust Someone Else"** button - For users to request a revive for another player

### 2. "Bust Me!" Button Flow

When clicked:
1. Opens a modal asking for **Revive Type** (defaults to "full")
2. Validates the user's Discord is linked to Torn
3. Checks if user is in hospital
4. Checks if revives are enabled
5. Submits the revive request to the appropriate channel

### 3. "Bust Someone Else" Button Flow

When clicked:
1. Opens a modal with two fields:
   - **The Target(s)**: Accepts multiple formats:
     - Plain Torn ID: `1234567`
     - Profile link: `https://www.torn.com/profiles.php?XID=1234567`
     - Name with ID: `PlayerName [1234567]`
     - Comma-separated list (only first ID is used currently)
   - **Min-Max Level**: Optional level range field (for display/reference)
2. Extracts the user ID from the input
3. Validates the target user
4. Always defaults to **Full Revive** for "someone else" requests
5. Tracks who requested it (shows as "Requestor" in the embed)

## Configuration Changes

### New Environment Variable Required

Add this to your Railway environment variables:

```
REQUEST_CHANNEL_ID=your_channel_id_here
```

This is the channel ID where you want the persistent revive request buttons to live.

### Updated Environment Variables List

You now need these environment variables:

- `DISCORD_TOKEN` - Your Discord bot token
- `TARGET_SERVER_ID` - Server where revive requests are sent
- `TARGET_CHANNEL_CONTRACT` - Channel for contract faction full revives
- `TARGET_CHANNEL_FULL` - Channel for regular full revives
- `TARGET_CHANNEL_PARTIAL` - Channel for partial revives
- **`REQUEST_CHANNEL_ID`** - ⭐ NEW: Channel where users request revives
- `TARGET_ROLE_ID_FULL_REVIVE` - Role to ping for full revives
- `TARGET_ROLE_ID_PART_REVIVE` - Role to ping for partial revives
- `TORN_API_KEY` - Your Torn API key
- `CONTRACT_FACTION_IDS` - Comma-separated faction IDs (optional)

## Setup Instructions

### 1. Add the New Environment Variable

In Railway:
1. Go to your bot project
2. Click "Variables"
3. Add `REQUEST_CHANNEL_ID` with your channel ID

### 2. Deploy the Updated Code

```bash
git add .
git commit -m "Add persistent revive request buttons with modals"
git push
```

Railway will automatically redeploy.

### 3. Run the Setup Command

Once the bot is online:
1. Go to your designated revive request channel
2. Run: `/setup-revive-channel`
3. The bot will create the two persistent messages

**Important:** This command can only be run by server administrators and must be run in the channel specified by `REQUEST_CHANNEL_ID`.

## How Users Interact

### Requesting for Themselves

1. User clicks "Bust Me!" button
2. Modal appears asking for revive type (optional, defaults to full)
3. User types "full" or "partial" (or leaves blank for full)
4. Bot validates and submits the request

### Requesting for Someone Else

1. User clicks "Bust Someone Else" button  
2. Modal appears with:
   - Target field (required)
   - Level range field (optional)
3. User enters target's Torn ID, profile link, or name [ID]
4. Bot extracts the ID, validates, and submits
5. The embed shows who requested it

## Technical Details

### Modal Handling

The bot now listens for three types of interactions:
1. **Slash commands** - `/r` and `/setup-revive-channel`
2. **Button clicks** - `revive_me`, `revive_someone`, and `claim_*`
3. **Modal submissions** - `revive_me_modal` and `revive_someone_modal`

### User ID Extraction

The `extractFirstUserId()` method handles multiple input formats:
- Plain number: `1234567`
- Bracketed: `[1234567]`
- Profile URL: `https://www.torn.com/profiles.php?XID=1234567`
- Name format: `PlayerName [1234567]`

### Persistent Buttons

Unlike the old `/r` slash command which requires typing, these buttons:
- Never expire
- Can be used by anyone with channel access
- Provide a cleaner, more professional interface
- Match the style of your reference image

## Backwards Compatibility

The original `/r` slash command still works! Users can still use:
- `/r` - Request revive for themselves
- `/r userid:1234567` - Request revive for someone else
- `/r userid:1234567 fullrevive:false` - Request partial revive

The new button system is an **addition**, not a replacement.

## Customization

### Change Button Text

In `handleSetupCommand()`, modify:
```java
Button reviveMeButton = Button.primary("revive_me", "Bust Me!");
Button reviveSomeoneButton = Button.secondary("revive_someone", "Bust Someone Else");
```

### Change Embed Colors

In `handleSetupCommand()`, modify:
```java
.setColor(Color.decode("#5865F2"))  // Discord Blurple
```

### Change Instructions Text

In `handleSetupCommand()`, modify the `instructionsEmbed` description.

## Testing Checklist

- [ ] Bot starts without errors
- [ ] `/setup-revive-channel` creates both messages
- [ ] "Bust Me!" button opens modal
- [ ] "Bust Someone Else" button opens modal
- [ ] Submitting "Bust Me!" validates hospital status
- [ ] Submitting "Bust Someone Else" extracts correct ID
- [ ] Revive requests appear in correct channels
- [ ] Requestor field shows who submitted for someone else
- [ ] Contract factions route to contract channel
- [ ] Full/partial routing works correctly

## Troubleshooting

### "Configuration error" when clicking buttons

**Issue**: `REQUEST_CHANNEL_ID` environment variable not set

**Fix**: Add it to Railway and redeploy

### Modal doesn't open

**Issue**: Discord might have cached old interactions

**Fix**: 
1. Restart Discord client
2. Or run `/setup-revive-channel` again to create fresh buttons

### "This command must be run in the designated revive request channel"

**Issue**: Running setup in wrong channel

**Fix**: Run the command in the channel matching your `REQUEST_CHANNEL_ID`

### User ID not extracted correctly

**Issue**: Input format not recognized

**Fix**: Tell users to use one of these formats:
- `1234567`
- `[1234567]`
- `https://www.torn.com/profiles.php?XID=1234567`

## Next Steps

Potential future enhancements:
1. Support multiple targets in "Bust Someone Else"
2. Add dropdown for revive type instead of text input
3. Add cooldown tracking per user
4. Add statistics tracking (revives per day, etc.)
5. Add completion confirmation button for revivers

## Questions?

If you have any questions or need help with:
- Customizing the embed messages
- Changing the button layout
- Adding new features
- Debugging issues

Just let me know!
