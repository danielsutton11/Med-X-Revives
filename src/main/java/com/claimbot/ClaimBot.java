package com.claimbot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.awt.Color;
import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class ClaimBot extends ListenerAdapter {

    private final String targetServerId;
    private final String targetChannelId;
    private final String targetRoleId;
    private final String tornApiKey;
    private final OkHttpClient httpClient;
    private final ConcurrentHashMap<String, ClaimData> activeClaims = new ConcurrentHashMap<>();

    private JDA jda;

    public ClaimBot(String targetServerId, String targetChannelId, String targetRoleId, String tornApiKey) {
        this.targetServerId = targetServerId;
        this.targetChannelId = targetChannelId;
        this.targetRoleId = targetRoleId;
        this.tornApiKey = tornApiKey;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public void setJDA(JDA jda) {
        this.jda = jda;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        // Ignore bot messages
        if (event.getAuthor().isBot()) {
            return;
        }

        // Check for !r command
        String content = event.getMessage().getContentRaw().trim();
        if (content.toLowerCase().startsWith("!r")) {
            handleClaimRequest(event);
        }
    }

    private void handleClaimRequest(MessageReceivedEvent event) {
        String content = event.getMessage().getContentRaw().trim();
        String[] parts = content.split("\\s+");

        String userId;
        String requestorDiscordId = null;
        boolean isManualId = false;

        // Check if user provided a Torn ID
        if (parts.length > 1 && parts[1].matches("\\d+")) {
            userId = parts[1];
            requestorDiscordId = event.getAuthor().getId();
            isManualId = true;
        } else {
            userId = event.getAuthor().getId();
            isManualId = false;
        }

        // Fetch Torn profile
        TornProfile profile = fetchTornProfile(userId);

        if (profile == null) {
            if (isManualId) {
                event.getMessage().reply("❌ Revive request not submitted - Could not identify torn user: " + userId).queue();
            } else {
                event.getMessage().reply("❌ Revive request not submitted - Your discord account is not linked to Torn").queue();
            }
            return;
        }

        // Check if user is revivable
        if (!profile.revivable) {
            event.getMessage().reply("❌ Revive request not submitted - Please switch on your revives and submit a new request").queue();
            return;
        }

        // Check if user is in hospital
        if (!"Hospital".equalsIgnoreCase(profile.statusState)) {
            event.getMessage().reply("❌ Revive request not submitted - User is not in the hospital").queue();
            return;
        }

        // Get requestor name if manual ID was used
        String requestorName = null;
        if (isManualId && requestorDiscordId != null) {
            TornProfile requestorProfile = fetchTornProfile(requestorDiscordId);
            if (requestorProfile != null) {
                requestorName = requestorProfile.name;
            }
        }

        // All checks passed, send the revive request
        sendReviveRequest(event, profile, requestorName);
    }

    private void sendReviveRequest(MessageReceivedEvent event, TornProfile profile, String requestorName) {
        try {
            // Get target guild and channel
            Guild targetGuild = jda.getGuildById(targetServerId);
            if (targetGuild == null) {
                System.err.println("❌ Target server not found!");
                event.getMessage().reply("⚠️ Configuration error. Please contact the bot administrator.").queue();
                return;
            }

            TextChannel targetChannel = targetGuild.getTextChannelById(targetChannelId);
            if (targetChannel == null) {
                System.err.println("❌ Target channel not found!");
                event.getMessage().reply("⚠️ Configuration error. Please contact the bot administrator.").queue();
                return;
            }

            // Create unique claim ID
            String claimId = "claim_" + System.currentTimeMillis() + "_" + profile.id;

            // Create profile URL
            String profileUrl = "https://www.torn.com/profiles.php?XID=" + profile.id;

            // Create embed for the revive request
            EmbedBuilder embedBuilder = new EmbedBuilder()
                    .setColor(Color.decode("#5865F2"))
                    .setTitle("💉 New Revive Request")
                    .addField("👤 User", "[" + profile.name + "](" + profileUrl + ")", true)
                    .addField("🆔 User ID", String.valueOf(profile.id), true)
                    .addField("🏠 Server", event.getGuild().getName(), true)
                    .addField("⏰ Time", "<t:" + Instant.now().getEpochSecond() + ":F>", false)
                    .setTimestamp(Instant.now());

            // Add requestor field if there is one
            if (requestorName != null) {
                TornProfile requestorProfile = fetchTornProfile(event.getAuthor().getId());
                if (requestorProfile != null) {
                    String requestorUrl = "https://www.torn.com/profiles.php?XID=" + requestorProfile.id;
                    embedBuilder.addField("📝 Requestor", "[" + requestorName + "](" + requestorUrl + ")", true);
                }
            }

            // Create claim button that opens the profile
            Button claimButton = Button.link(profileUrl, "✅ Revive");
            Button claimedButton = Button.success("claim_" + claimId, "Mark as Claimed");

            // Send to target channel
            targetChannel.sendMessageEmbeds(embedBuilder.build())
                    .setContent("<@&" + targetRoleId + ">")
                    .setActionRow(claimButton, claimedButton)
                    .queue(claimMessage -> {
                        // Store claim information
                        ClaimData claimData = new ClaimData(
                                String.valueOf(profile.id),
                                profile.name,
                                event.getGuild().getName(),
                                event.getChannel().getName(),
                                claimMessage.getId(),
                                System.currentTimeMillis()
                        );
                        activeClaims.put(claimId, claimData);

                        // Confirm to user (only if not in target server)
                        if (!event.getGuild().getId().equals(targetServerId)) {
                            event.getMessage().reply("✅ Your revive request has been submitted!").queue();
                        }

                        System.out.println("Revive request sent for " + profile.name + " [" + profile.id + "] from " +
                                event.getGuild().getName());

                        // Clean up old claims after 24 hours
                        scheduleClaimCleanup(claimId);
                    }, error -> {
                        System.err.println("Error sending revive message: " + error.getMessage());
                        event.getMessage().reply("⚠️ An error occurred while processing your request.").queue();
                    });

        } catch (Exception e) {
            System.err.println("Error handling revive request: " + e.getMessage());
            e.printStackTrace();
            event.getMessage().reply("⚠️ An error occurred while processing your request.").queue();
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String buttonId = event.getComponentId();

        if (!buttonId.startsWith("claim_")) {
            return;
        }

        String claimId = buttonId.substring(6); // Remove "claim_" prefix
        ClaimData claimData = activeClaims.get(claimId);

        if (claimData == null) {
            event.reply("❌ This revive request is no longer valid.").setEphemeral(true).queue();
            return;
        }

        if (claimData.isClaimed()) {
            event.reply("⚠️ This revive request has already been processed.").setEphemeral(true).queue();
            return;
        }

        // Get claimer's name - try Torn API first, fallback to Discord name
        String claimerDiscordId = event.getUser().getId();
        TornProfile claimerProfile = fetchTornProfile(claimerDiscordId);

        String claimerName;
        String claimerDisplayName;
        if (claimerProfile != null) {
            claimerName = claimerProfile.name;
            String claimerUrl = "https://www.torn.com/profiles.php?XID=" + claimerProfile.id;
            claimerDisplayName = "[" + claimerName + "](" + claimerUrl + ")";
        } else {
            claimerName = event.getUser().getName();
            claimerDisplayName = event.getUser().getName();
        }

        // Mark as claimed
        claimData.setClaimed(true);
        claimData.setClaimedBy(claimerName);
        claimData.setClaimedAt(System.currentTimeMillis());

        // Get the original embed
        Message message = event.getMessage();
        if (message.getEmbeds().isEmpty()) {
            event.reply("❌ Error processing revive.").setEphemeral(true).queue();
            return;
        }

        MessageEmbed originalEmbed = message.getEmbeds().get(0);

        // Create updated embed
        EmbedBuilder updatedEmbed = new EmbedBuilder(originalEmbed)
                .setColor(Color.decode("#57F287"))
                .addField("✅ Claimed By", claimerDisplayName, true)
                .addField("⏰ Claimed At", "<t:" + (claimData.getClaimedAt() / 1000) + ":F>", true);

        // Keep the original link button and disable the claim button
        String profileUrl = "https://www.torn.com/profiles.php?XID=" + claimData.getUserId();
        Button linkButton = Button.link(profileUrl, "✅ Revive");
        Button disabledButton = Button.secondary("claim_" + claimId, "Claimed").asDisabled();

        // Update message
        event.editMessageEmbeds(updatedEmbed.build())
                .setActionRow(linkButton, disabledButton)
                .queue(success -> {
                    System.out.println("Claim " + claimId + " processed by " + claimerName);
                }, error -> {
                    System.err.println("Error updating claim message: " + error.getMessage());
                });
    }

    private TornProfile fetchTornProfile(String userId) {
        String url = "https://api.torn.com/v2/user/" + userId + "/profile?striptags=true";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("accept", "application/json")
                .addHeader("Authorization", "ApiKey " + tornApiKey)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                System.err.println("Torn API request failed: " + response.code());
                return null;
            }

            String responseBody = response.body().string();
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();

            // Check for error in response
            if (json.has("error")) {
                System.err.println("Torn API error: " + json.get("error").getAsJsonObject().get("error").getAsString());
                return null;
            }

            if (!json.has("profile")) {
                System.err.println("No profile data in Torn API response");
                return null;
            }

            JsonObject profileObj = json.getAsJsonObject("profile");
            JsonObject statusObj = profileObj.has("status") ? profileObj.getAsJsonObject("status") : null;

            TornProfile profile = new TornProfile();
            profile.id = profileObj.get("id").getAsInt();
            profile.name = profileObj.get("name").getAsString();
            profile.revivable = profileObj.get("revivable").getAsBoolean();
            profile.statusState = statusObj != null && statusObj.has("state") ?
                    statusObj.get("state").getAsString() : "Unknown";

            return profile;

        } catch (IOException e) {
            System.err.println("Error fetching Torn profile: " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.err.println("Error parsing Torn profile: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private void scheduleClaimCleanup(String claimId) {
        new Thread(() -> {
            try {
                TimeUnit.HOURS.sleep(24);
                activeClaims.remove(claimId);
                System.out.println("Cleaned up claim: " + claimId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private static class TornProfile {
        int id;
        String name;
        boolean revivable;
        String statusState;
    }

    public static void main(String[] args) {
        System.out.println("Discord Claim Bot Starting...");
        System.out.println("=====================================");

        // Get configuration from environment variables
        String token = System.getenv("DISCORD_TOKEN");
        String targetServerId = System.getenv("TARGET_SERVER_ID");
        String targetChannelId = System.getenv("TARGET_CHANNEL_ID");
        String targetRoleId = System.getenv("TARGET_ROLE_ID");
        String tornApiKey = System.getenv("TORN_API_KEY");

        // Validate configuration
        if (token == null || token.isEmpty()) {
            System.err.println("DISCORD_TOKEN environment variable is not set!");
            System.exit(1);
        }
        if (targetServerId == null || targetServerId.isEmpty()) {
            System.err.println("TARGET_SERVER_ID environment variable is not set!");
            System.exit(1);
        }
        if (targetChannelId == null || targetChannelId.isEmpty()) {
            System.err.println("TARGET_CHANNEL_ID environment variable is not set!");
            System.exit(1);
        }
        if (targetRoleId == null || targetRoleId.isEmpty()) {
            System.err.println("TARGET_ROLE_ID environment variable is not set!");
            System.exit(1);
        }
        if (tornApiKey == null || tornApiKey.isEmpty()) {
            System.err.println("TORN_API_KEY environment variable is not set!");
            System.exit(1);
        }

        System.out.println("Configuration validated");
        System.out.println("Connecting to Discord...");

        try {
            ClaimBot bot = new ClaimBot(targetServerId, targetChannelId, targetRoleId, tornApiKey);

            // Create custom HTTP client with longer timeouts
            OkHttpClient httpClient = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();

            JDA jda = JDABuilder.createDefault(token)
                    .setHttpClient(httpClient)
                    .enableIntents(
                            GatewayIntent.GUILD_MESSAGES,
                            GatewayIntent.MESSAGE_CONTENT
                    )
                    .disableCache(CacheFlag.VOICE_STATE, CacheFlag.EMOJI, CacheFlag.STICKER)
                    .setStatus(OnlineStatus.ONLINE)
                    .setActivity(Activity.watching("for !r commands"))
                    .addEventListeners(bot)
                    .build();

            bot.setJDA(jda);

            System.out.println("Waiting for bot to be ready...");
            jda.awaitReady();

            System.out.println("=====================================");
            System.out.println("Bot is ready! Logged in as " + jda.getSelfUser().getAsTag());
            System.out.println("Serving " + jda.getGuilds().size() + " servers");
            System.out.println("Target Server ID: " + targetServerId);
            System.out.println("Target Channel ID: " + targetChannelId);
            System.out.println("=====================================");

        } catch (Exception e) {
            System.err.println("=====================================");
            System.err.println("Failed to start bot: " + e.getClass().getSimpleName());
            System.err.println("Error details: " + e.getMessage());
            System.err.println("=====================================");

            if (e.getMessage() != null && e.getMessage().contains("SocketTimeout")) {
                System.err.println("Troubleshooting tips:");
                System.err.println("  1. Check your network/firewall settings");
                System.err.println("  2. Verify Discord API is accessible");
                System.err.println("  3. Ensure your bot token is valid");
                System.err.println("  4. Check Discord status: https://discordstatus.com");
            }

            e.printStackTrace();
            System.exit(1);
        }
    }
}