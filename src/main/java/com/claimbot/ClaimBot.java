package com.claimbot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
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

import java.awt.Color;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class ClaimBot extends ListenerAdapter {

    private final String targetServerId;
    private final String targetChannelId;
    private final ConcurrentHashMap<String, ClaimData> activeClaims = new ConcurrentHashMap<>();
    
    private JDA jda;

    public ClaimBot(String targetServerId, String targetChannelId) {
        this.targetServerId = targetServerId;
        this.targetChannelId = targetChannelId;
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
        if (content.equalsIgnoreCase("!r")) {
            handleClaimRequest(event);
        }
    }

    private void handleClaimRequest(MessageReceivedEvent event) {
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
            String claimId = "claim_" + System.currentTimeMillis() + "_" + event.getAuthor().getId();

            // Create embed for the claim
            EmbedBuilder embedBuilder = new EmbedBuilder()
                    .setColor(Color.decode("#5865F2"))
                    .setTitle("🎯 New Claim Request")
                    .setDescription("Someone has requested a claim!")
                    .addField("👤 User", event.getAuthor().getAsTag(), true)
                    .addField("🆔 User ID", event.getAuthor().getId(), true)
                    .addField("🏠 Server", event.getGuild().getName(), true)
                    .addField("📝 Channel", event.getChannel().getName(), true)
                    .addField("⏰ Time", "<t:" + Instant.now().getEpochSecond() + ":F>", false)
                    .setFooter("Claim ID: " + claimId)
                    .setTimestamp(Instant.now());

            // Create claim button
            Button claimButton = Button.success("claim_" + claimId, "✅ Claim");

            // Send to target channel
            targetChannel.sendMessageEmbeds(embedBuilder.build())
                    .setActionRow(claimButton)
                    .queue(claimMessage -> {
                        // Store claim information
                        ClaimData claimData = new ClaimData(
                                event.getAuthor().getId(),
                                event.getAuthor().getAsTag(),
                                event.getGuild().getName(),
                                event.getChannel().getName(),
                                claimMessage.getId(),
                                System.currentTimeMillis()
                        );
                        activeClaims.put(claimId, claimData);

                        // Confirm to user
                        event.getMessage().reply("✅ Your claim request has been submitted!").queue();

                        System.out.println("📤 Claim request sent from " + event.getAuthor().getAsTag() + 
                                         " in " + event.getGuild().getName());

                        // Clean up old claims after 24 hours
                        scheduleClaimCleanup(claimId);
                    }, error -> {
                        System.err.println("❌ Error sending claim message: " + error.getMessage());
                        event.getMessage().reply("⚠️ An error occurred while processing your request.").queue();
                    });

        } catch (Exception e) {
            System.err.println("❌ Error handling claim request: " + e.getMessage());
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
            event.reply("❌ This claim is no longer valid.").setEphemeral(true).queue();
            return;
        }

        if (claimData.isClaimed()) {
            event.reply("⚠️ This claim has already been processed.").setEphemeral(true).queue();
            return;
        }

        // Mark as claimed
        claimData.setClaimed(true);
        claimData.setClaimedBy(event.getUser().getAsTag());
        claimData.setClaimedAt(System.currentTimeMillis());

        // Get the original embed
        Message message = event.getMessage();
        if (message.getEmbeds().isEmpty()) {
            event.reply("❌ Error processing claim.").setEphemeral(true).queue();
            return;
        }

        MessageEmbed originalEmbed = message.getEmbeds().get(0);

        // Create updated embed
        EmbedBuilder updatedEmbed = new EmbedBuilder(originalEmbed)
                .setColor(Color.decode("#57F287"))
                .addField("✅ Claimed By", event.getUser().getAsTag(), true)
                .addField("⏰ Claimed At", "<t:" + (claimData.getClaimedAt() / 1000) + ":F>", true);

        // Disable button
        Button disabledButton = Button.secondary("claim_" + claimId, "✅ Claimed").asDisabled();

        // Update message
        event.editMessageEmbeds(updatedEmbed.build())
                .setActionRow(disabledButton)
                .queue(success -> {
                    System.out.println("✅ Claim " + claimId + " processed by " + event.getUser().getAsTag());
                }, error -> {
                    System.err.println("❌ Error updating claim message: " + error.getMessage());
                });
    }

    private void scheduleClaimCleanup(String claimId) {
        new Thread(() -> {
            try {
                TimeUnit.HOURS.sleep(24);
                activeClaims.remove(claimId);
                System.out.println("🧹 Cleaned up claim: " + claimId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    public static void main(String[] args) {
        // Get configuration from environment variables
        String token = System.getenv("DISCORD_TOKEN");
        String targetServerId = System.getenv("TARGET_SERVER_ID");
        String targetChannelId = System.getenv("TARGET_CHANNEL_ID");

        // Validate configuration
        if (token == null || token.isEmpty()) {
            System.err.println("❌ DISCORD_TOKEN environment variable is not set!");
            System.exit(1);
        }
        if (targetServerId == null || targetServerId.isEmpty()) {
            System.err.println("❌ TARGET_SERVER_ID environment variable is not set!");
            System.exit(1);
        }
        if (targetChannelId == null || targetChannelId.isEmpty()) {
            System.err.println("❌ TARGET_CHANNEL_ID environment variable is not set!");
            System.exit(1);
        }

        try {
            ClaimBot bot = new ClaimBot(targetServerId, targetChannelId);

            JDA jda = JDABuilder.createDefault(token)
                    .enableIntents(
                            GatewayIntent.GUILD_MESSAGES,
                            GatewayIntent.MESSAGE_CONTENT
                    )
                    .disableCache(CacheFlag.VOICE_STATE, CacheFlag.EMOJI, CacheFlag.STICKER)
                    .addEventListeners(bot)
                    .build();

            bot.setJDA(jda);

            jda.awaitReady();
            System.out.println("✅ Bot is ready! Logged in as " + jda.getSelfUser().getAsTag());
            System.out.println("📊 Serving " + jda.getGuilds().size() + " servers");

        } catch (Exception e) {
            System.err.println("❌ Failed to start bot: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
