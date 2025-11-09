package com.claimbot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
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
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class ClaimBot extends ListenerAdapter {

    private final String targetServerId;
    private final String targetChannelContract;
    private final String targetChannelFull;
    private final String targetChannelPartial;
    private final String targetRoleFullRevive;
    private final String targetRolePartRevive;
    private final String tornApiKey;
    private final Set<String> contractFactionIds;
    private final OkHttpClient httpClient;
    private final ConcurrentHashMap<String, ClaimData> activeClaims = new ConcurrentHashMap<>();

    private JDA jda;

    public ClaimBot(String targetServerId, String targetChannelContract, String targetChannelFull,
                    String targetChannelPartial, String targetRoleFullRevive, 
                    String targetRolePartRevive, String tornApiKey, Set<String> contractFactionIds) {
        this.targetServerId = targetServerId;
        this.targetChannelContract = targetChannelContract;
        this.targetChannelFull = targetChannelFull;
        this.targetChannelPartial = targetChannelPartial;
        this.targetRoleFullRevive = targetRoleFullRevive;
        this.targetRolePartRevive = targetRolePartRevive;
        this.tornApiKey = tornApiKey;
        this.contractFactionIds = contractFactionIds;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public void setJDA(JDA jda) {
        this.jda = jda;
    }

    @Override
    public void onReady(ReadyEvent event) {
        // Register slash commands globally
        event.getJDA().updateCommands().addCommands(
                Commands.slash("setup-revive-channel", "Setup the revive request channel (Admin only)")
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
        ).queue();

        System.out.println("✅ Slash commands registered!");
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equals("setup-revive-channel")) {
            handleSetupCommand(event);
            return;
        }
    }

    private void handleSetupCommand(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();

        try {
            TextChannel channel = event.getChannel().asTextChannel();

            // Create the instructions embed
            EmbedBuilder instructionsEmbed = new EmbedBuilder()
                    .setColor(Color.decode("#5865F2"))
                    .setTitle("🏥 Med X Revive Service - Reviving Guide")
                    .setDescription("""
                            **Customers Guide**
                            • When in Hospital, click the 'Revive Me' button
                            • Fill in the Modal
                            • Click Submit and be patient
                            • Once complete, pay the Reviver 2 Xanax unless under contract.
                            
                            *If any issues please contact Dsuttz [1561637]*""")
                    .setTimestamp(Instant.now());

            channel.sendMessageEmbeds(instructionsEmbed.build()).queue();

            // Create the button message
            EmbedBuilder buttonEmbed = new EmbedBuilder()
                    .setColor(Color.decode("#5865F2"))
                    .setTitle("🏥 Med X Revive Service - Request Revive")
                    .setDescription("To request a revive, click the button below!")
                    .setFooter("Click the button below to request a revive!");

            Button reviveMeButton = Button.primary("revive_me", "Revive Me");
            Button reviveSomeoneButton = Button.secondary("revive_someone", "Revive Someone Else");

            channel.sendMessageEmbeds(buttonEmbed.build())
                    .setActionRow(reviveMeButton, reviveSomeoneButton)
                    .queue(success -> {
                        event.getHook().editOriginal("✅ Revive request channel setup complete!").queue();
                    }, error -> {
                        event.getHook().editOriginal("❌ Error setting up channel: " + error.getMessage()).queue();
                    });

        } catch (Exception e) {
            event.getHook().editOriginal("❌ Error: " + e.getMessage()).queue();
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String buttonId = event.getComponentId();

        // Handle the persistent revive request buttons
        if (buttonId.equals("revive_me")) {
            handleReviveMeButton(event);
            return;
        }

        if (buttonId.equals("revive_someone")) {
            handleReviveSomeoneButton(event);
            return;
        }

        // Handle claim buttons (existing functionality)
        if (buttonId.startsWith("claim_")) {
            handleClaimButton(event);
        }
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        if (event.getComponentId().equals("revive_type_select")) {
            handleReviveTypeSelect(event);
        }
    }

    private void handleReviveTypeSelect(StringSelectInteractionEvent event) {
        // Get the selected revive type
        String reviveType = event.getValues().get(0); // "full" or "partial"
        
        // Create modal for entering Torn ID
        TextInput userIdInput = TextInput.create("target_userid", "The Target", TextInputStyle.SHORT)
                .setPlaceholder("Torn User ID or Profile Link")
                .setRequired(true)
                .build();

        // Store the revive type in the modal ID so we can retrieve it later
        Modal modal = Modal.create("revive_someone_modal_" + reviveType, "Request Revive for Someone")
                .addActionRow(userIdInput)
                .build();

        event.replyModal(modal).queue();
    }

    private void handleReviveMeButton(ButtonInteractionEvent event) {
        // Defer to show we're processing
        event.deferReply(true).queue();
        
        // Get the user's Discord ID and fetch their Torn profile
        String userId = event.getUser().getId();
        TornProfile profile = fetchTornProfile(userId);

        if (profile == null) {
            event.getHook().editOriginal("❌ Revive request not submitted - Your discord account is not linked to Torn").queue();
            return;
        }

        if (!profile.revivable && profile.id != 1561637) {
            event.getHook().editOriginal("❌ Revive request not submitted - Please switch on your revives and submit a new request").queue();
            return;
        }

        if (!"Hospital".equalsIgnoreCase(profile.statusState)) {
            event.getHook().editOriginal("❌ Revive request not submitted - You are not in the hospital").queue();
            return;
        }

        // All validation passed, process the revive request with full revive by default
        processReviveRequestFromButton(event, profile, null, true);
    }

    private void handleReviveSomeoneButton(ButtonInteractionEvent event) {
        // Show a dropdown menu to select revive type
        StringSelectMenu selectMenu = StringSelectMenu.create("revive_type_select")
                .setPlaceholder("Select Revive Type")
                .addOption("Full Revive (Defensive)", "full", "Request a full revive")
                .addOption("Partial Revive (Offensive)", "partial", "Request a partial revive")
                .build();

        event.reply("Please select the revive type:")
                .addActionRow(selectMenu)
                .setEphemeral(true)
                .queue();
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        if (event.getModalId().startsWith("revive_someone_modal")) {
            handleReviveSomeoneModal(event);
        }
    }

    private void handleReviveSomeoneModal(ModalInteractionEvent event) {
        event.deferReply(true).queue();

        String targetUserIdInput = Objects.requireNonNull(event.getValue("target_userid")).getAsString().trim();
        
        // Extract revive type from modal ID (format: "revive_someone_modal_full" or "revive_someone_modal_partial")
        String modalId = event.getModalId();
        boolean fullRevive = modalId.contains("_full");
        
        // Extract just the first user ID from the input
        String targetUserId = extractFirstUserId(targetUserIdInput);

        if (targetUserId == null || !targetUserId.matches("\\d+")) {
            event.getHook().editOriginal("❌ Could not extract a valid Torn user ID from: " + targetUserIdInput).queue();
            return;
        }

        // Validate target user immediately
        TornProfile profile = fetchTornProfile(targetUserId);

        if (profile == null) {
            event.getHook().editOriginal("❌ Revive request not submitted - Could not identify torn user: " + targetUserId).queue();
            return;
        }

        if (!profile.revivable && profile.id != 1561637) {
            event.getHook().editOriginal("❌ Revive request not submitted - Target user does not have revives enabled").queue();
            return;
        }

        if (!"Hospital".equalsIgnoreCase(profile.statusState)) {
            event.getHook().editOriginal("❌ Revive request not submitted - User is not in the hospital").queue();
            return;
        }

        // Get requestor name
        String requestorDiscordId = event.getUser().getId();
        TornProfile requestorProfile = fetchTornProfile(requestorDiscordId);
        String requestorName = requestorProfile != null ? requestorProfile.name : null;

        // Use the revive type from modal ID
        processReviveRequestFromModal(event, profile, requestorName, fullRevive);
    }

    private String extractFirstUserId(String input) {
        // Try different patterns to extract user ID
        
        // Pattern 1: Plain number at start
        if (input.matches("^\\d+.*")) {
            return input.split("[,\\s]")[0];
        }
        
        // Pattern 2: [ID] format
        if (input.contains("[") && input.contains("]")) {
            int start = input.indexOf("[") + 1;
            int end = input.indexOf("]", start);
            if (end > start) {
                String extracted = input.substring(start, end).trim();
                if (extracted.matches("\\d+")) {
                    return extracted;
                }
            }
        }
        
        // Pattern 3: Profile link
        if (input.contains("XID=")) {
            String[] parts = input.split("XID=");
            if (parts.length > 1) {
                String idPart = parts[1].split("[&\\s,]")[0];
                if (idPart.matches("\\d+")) {
                    return idPart;
                }
            }
        }
        
        return null;
    }

    private void processReviveRequestFromButton(ButtonInteractionEvent event, TornProfile profile, 
                                               String requestorName, boolean fullRevive) {
        try {
            Guild targetGuild = jda.getGuildById(targetServerId);
            if (targetGuild == null) {
                event.getHook().editOriginal("⚠️ Configuration error. Please contact the bot administrator.").queue();
                return;
            }

            String targetChannelId;
            String targetRoleId;
            String channelType;

            // Debug logging
            System.out.println("DEBUG: User faction ID: " + profile.factionId);
            System.out.println("DEBUG: Contract faction IDs: " + contractFactionIds);
            System.out.println("DEBUG: Checking if " + profile.factionId + " is in contract list");
            
            boolean isContractFaction = contractFactionIds.contains(String.valueOf(profile.factionId));
            System.out.println("DEBUG: Is contract faction? " + isContractFaction);

            if (fullRevive && isContractFaction) {
                targetChannelId = targetChannelContract;
                targetRoleId = targetRoleFullRevive;
                channelType = "Contract";
            } else if (fullRevive) {
                targetChannelId = targetChannelFull;
                targetRoleId = targetRoleFullRevive;
                channelType = "Full Revive";
            } else {
                targetChannelId = targetChannelPartial;
                targetRoleId = targetRolePartRevive;
                channelType = "Partial Revive";
            }

            System.out.println("DEBUG: Routing to channel type: " + channelType);

            TextChannel targetChannel = targetGuild.getTextChannelById(targetChannelId);
            if (targetChannel == null) {
                event.getHook().editOriginal("⚠️ Configuration error. Please contact the bot administrator.").queue();
                return;
            }

            String claimId = "claim_" + System.currentTimeMillis() + "_" + profile.id;
            String profileUrl = "https://www.torn.com/profiles.php?XID=" + profile.id;
            Color embedColor = fullRevive ? Color.decode("#ED4245") : Color.decode("#5865F2");

            EmbedBuilder embedBuilder = new EmbedBuilder()
                    .setColor(embedColor)
                    .setTitle("💉 New Revive Request")
                    .addField("👤 User", "[" + profile.name + "](" + profileUrl + ")", true)
                    .addField("🆔 User ID", String.valueOf(profile.id), true)
                    .addField("🏥 Full Revive", fullRevive ? "✅ Yes" : "❌ No", true);

            if (isContractFaction && fullRevive) {
                embedBuilder.addField("⭐ Type", "Contract Faction", true);
            }

            embedBuilder.addField("🏠 Server", Objects.requireNonNull(event.getGuild()).getName(), true)
                    .addField("⏰ Time", "<t:" + Instant.now().getEpochSecond() + ":F>", false)
                    .setTimestamp(Instant.now());

            if (requestorName != null) {
                TornProfile requestorProfile = fetchTornProfile(event.getUser().getId());
                if (requestorProfile != null) {
                    String requestorUrl = "https://www.torn.com/profiles.php?XID=" + requestorProfile.id;
                    embedBuilder.addField("📝 Requestor", "[" + requestorName + "](" + requestorUrl + ")", true);
                }
            }

            Button claimButton = Button.link(profileUrl, "✅ Revive");
            Button claimedButton = Button.success("claim_" + claimId, "Mark as Claimed");

            targetChannel.sendMessageEmbeds(embedBuilder.build())
                    .setContent("<@&" + targetRoleId + ">")
                    .setActionRow(claimButton, claimedButton)
                    .queue(claimMessage -> {
                        ClaimData claimData = new ClaimData(
                                String.valueOf(profile.id),
                                profile.name,
                                event.getGuild().getName(),
                                event.getChannel().getName(),
                                claimMessage.getId(),
                                System.currentTimeMillis(),
                                fullRevive
                        );
                        activeClaims.put(claimId, claimData);

                        String reviveTypeText = isContractFaction && fullRevive ? " (Contract - Full Revive)"
                                : fullRevive ? " (Full Revive requested)"
                                : " (Partial Revive requested)";
                        event.getHook().editOriginal("✅ Your revive request has been submitted!" + reviveTypeText).queue();

                        System.out.println("Revive request sent for " + profile.name + " [" + profile.id + "] from " +
                                event.getGuild().getName() + " to " + channelType + " channel" +
                                (fullRevive ? " (Full Revive)" : " (Partial Revive)"));

                        scheduleClaimCleanup(claimId);
                    }, error -> {
                        event.getHook().editOriginal("⚠️ An error occurred while processing your request.").queue();
                    });

        } catch (Exception e) {
            event.getHook().editOriginal("⚠️ An error occurred while processing your request.").queue();
            e.printStackTrace();
        }
    }

    private void processReviveRequestFromModal(ModalInteractionEvent event, TornProfile profile, 
                                              String requestorName, boolean fullRevive) {
        try {
            Guild targetGuild = jda.getGuildById(targetServerId);
            if (targetGuild == null) {
                event.getHook().editOriginal("⚠️ Configuration error. Please contact the bot administrator.").queue();
                return;
            }

            String targetChannelId;
            String targetRoleId;
            String channelType;

            // Debug logging
            System.out.println("DEBUG: User faction ID: " + profile.factionId);
            System.out.println("DEBUG: Contract faction IDs: " + contractFactionIds);
            System.out.println("DEBUG: Checking if " + profile.factionId + " is in contract list");
            
            boolean isContractFaction = contractFactionIds.contains(String.valueOf(profile.factionId));
            System.out.println("DEBUG: Is contract faction? " + isContractFaction);

            if (fullRevive && isContractFaction) {
                targetChannelId = targetChannelContract;
                targetRoleId = targetRoleFullRevive;
                channelType = "Contract";
            } else if (fullRevive) {
                targetChannelId = targetChannelFull;
                targetRoleId = targetRoleFullRevive;
                channelType = "Full Revive";
            } else {
                targetChannelId = targetChannelPartial;
                targetRoleId = targetRolePartRevive;
                channelType = "Partial Revive";
            }

            System.out.println("DEBUG: Routing to channel type: " + channelType);

            TextChannel targetChannel = targetGuild.getTextChannelById(targetChannelId);
            if (targetChannel == null) {
                event.getHook().editOriginal("⚠️ Configuration error. Please contact the bot administrator.").queue();
                return;
            }

            String claimId = "claim_" + System.currentTimeMillis() + "_" + profile.id;
            String profileUrl = "https://www.torn.com/profiles.php?XID=" + profile.id;
            Color embedColor = fullRevive ? Color.decode("#ED4245") : Color.decode("#5865F2");

            EmbedBuilder embedBuilder = new EmbedBuilder()
                    .setColor(embedColor)
                    .setTitle("💉 New Revive Request")
                    .addField("👤 User", "[" + profile.name + "](" + profileUrl + ")", true)
                    .addField("🆔 User ID", String.valueOf(profile.id), true)
                    .addField("🏥 Full Revive", fullRevive ? "✅ Yes" : "❌ No", true);

            if (isContractFaction && fullRevive) {
                embedBuilder.addField("⭐ Type", "Contract", true);
            }

            embedBuilder.addField("🏠 Server", Objects.requireNonNull(event.getGuild()).getName(), true)
                    .addField("⏰ Time", "<t:" + Instant.now().getEpochSecond() + ":F>", false)
                    .setTimestamp(Instant.now());

            if (requestorName != null) {
                TornProfile requestorProfile = fetchTornProfile(event.getUser().getId());
                if (requestorProfile != null) {
                    String requestorUrl = "https://www.torn.com/profiles.php?XID=" + requestorProfile.id;
                    embedBuilder.addField("📝 Requestor", "[" + requestorName + "](" + requestorUrl + ")", true);
                }
            }

            Button claimButton = Button.link(profileUrl, "✅ Revive");
            Button claimedButton = Button.success("claim_" + claimId, "Mark as Claimed");

            targetChannel.sendMessageEmbeds(embedBuilder.build())
                    .setContent("<@&" + targetRoleId + ">")
                    .setActionRow(claimButton, claimedButton)
                    .queue(claimMessage -> {
                        ClaimData claimData = new ClaimData(
                                String.valueOf(profile.id),
                                profile.name,
                                event.getGuild().getName(),
                                event.getChannel().getName(),
                                claimMessage.getId(),
                                System.currentTimeMillis(),
                                fullRevive
                        );
                        activeClaims.put(claimId, claimData);

                        String reviveTypeText = isContractFaction && fullRevive ? " (Contract - Full Revive)"
                                : fullRevive ? " (Full Revive requested)"
                                : " (Partial Revive requested)";
                        event.getHook().editOriginal("✅ Your revive request has been submitted!" + reviveTypeText).queue();

                        System.out.println("Revive request sent for " + profile.name + " [" + profile.id + "] from " +
                                event.getGuild().getName() + " to " + channelType + " channel" +
                                (fullRevive ? " (Full Revive)" : " (Partial Revive)"));

                        scheduleClaimCleanup(claimId);
                    }, error -> {
                        event.getHook().editOriginal("⚠️ An error occurred while processing your request.").queue();
                    });

        } catch (Exception e) {
            event.getHook().editOriginal("⚠️ An error occurred while processing your request.").queue();
            e.printStackTrace();
        }
    }

    private void handleClaimButton(ButtonInteractionEvent event) {
        String buttonId = event.getComponentId();
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

        claimData.setClaimed(true);
        claimData.setClaimedBy(claimerName);
        claimData.setClaimedAt(System.currentTimeMillis());

        Message message = event.getMessage();
        if (message.getEmbeds().isEmpty()) {
            event.reply("❌ Error processing revive.").setEphemeral(true).queue();
            return;
        }

        MessageEmbed originalEmbed = message.getEmbeds().get(0);

        EmbedBuilder updatedEmbed = new EmbedBuilder(originalEmbed)
                .setColor(Color.decode("#57F287"))
                .addField("✅ Claimed By", claimerDisplayName, true)
                .addField("⏰ Claimed At", "<t:" + (claimData.getClaimedAt() / 1000) + ":F>", true);

        String profileUrl = "https://www.torn.com/profiles.php?XID=" + claimData.getUserId();
        Button linkButton = Button.link(profileUrl, "✅ Revive");
        Button disabledButton = Button.secondary("claim_" + claimId, "Claimed").asDisabled();

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

            assert response.body() != null;
            String responseBody = response.body().string();
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();

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

            // Handle null faction_id (players not in a faction)
            if (profileObj.has("faction_id") && !profileObj.get("faction_id").isJsonNull()) {
                profile.factionId = profileObj.get("faction_id").getAsInt();
            } else {
                profile.factionId = 0; // Default to 0 if no faction
            }

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
        int factionId;
    }

    public static void main(String[] args) {
        System.out.println("Discord Claim Bot Starting...");
        System.out.println("=====================================");

        // Get configuration from environment variables
        String token = System.getenv("DISCORD_TOKEN");
        String targetServerId = System.getenv("TARGET_SERVER_ID");
        String targetChannelContract = System.getenv("TARGET_CHANNEL_CONTRACT");
        String targetChannelFull = System.getenv("TARGET_CHANNEL_FULL");
        String targetChannelPartial = System.getenv("TARGET_CHANNEL_PARTIAL");
        String targetRoleFullRevive = System.getenv("TARGET_ROLE_ID_FULL_REVIVE");
        String targetRolePartRevive = System.getenv("TARGET_ROLE_ID_PART_REVIVE");
        String tornApiKey = System.getenv("TORN_API_KEY");
        String contractFactionIdsStr = System.getenv("CONTRACT_FACTION_IDS");

        // Validate configuration
        if (token == null || token.isEmpty()) {
            System.err.println("❌ DISCORD_TOKEN environment variable is not set!");
            System.exit(1);
        }
        if (targetServerId == null || targetServerId.isEmpty()) {
            System.err.println("❌ TARGET_SERVER_ID environment variable is not set!");
            System.exit(1);
        }
        if (targetChannelContract == null || targetChannelContract.isEmpty()) {
            System.err.println("❌ TARGET_CHANNEL_CONTRACT environment variable is not set!");
            System.exit(1);
        }
        if (targetChannelFull == null || targetChannelFull.isEmpty()) {
            System.err.println("❌ TARGET_CHANNEL_FULL environment variable is not set!");
            System.exit(1);
        }
        if (targetChannelPartial == null || targetChannelPartial.isEmpty()) {
            System.err.println("❌ TARGET_CHANNEL_PARTIAL environment variable is not set!");
            System.exit(1);
        }
        if (targetRoleFullRevive == null || targetRoleFullRevive.isEmpty()) {
            System.err.println("❌ TARGET_ROLE_ID_FULL_REVIVE environment variable is not set!");
            System.exit(1);
        }
        if (targetRolePartRevive == null || targetRolePartRevive.isEmpty()) {
            System.err.println("❌ TARGET_ROLE_ID_PART_REVIVE environment variable is not set!");
            System.exit(1);
        }
        if (tornApiKey == null || tornApiKey.isEmpty()) {
            System.err.println("❌ TORN_API_KEY environment variable is not set!");
            System.exit(1);
        }

        // Parse contract faction IDs
        Set<String> contractFactionIds = new HashSet<>();
        if (contractFactionIdsStr != null && !contractFactionIdsStr.isEmpty()) {
            String[] ids = contractFactionIdsStr.split(",");
            for (String id : ids) {
                String trimmedId = id.trim();
                if (!trimmedId.isEmpty() && !trimmedId.equals("0")) {
                    contractFactionIds.add(trimmedId);
                }
            }
            System.out.println("✅ Loaded " + contractFactionIds.size() + " contract faction IDs: " + contractFactionIds);
        } else {
            System.out.println("⚠️ No contract faction IDs configured");
        }

        System.out.println("✅ Configuration validated");
        System.out.println("Connecting to Discord...");

        try {
            ClaimBot bot = new ClaimBot(targetServerId, targetChannelContract, targetChannelFull,
                    targetChannelPartial, targetRoleFullRevive, targetRolePartRevive,
                    tornApiKey, contractFactionIds);

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
                    .setActivity(Activity.watching("for revive requests"))
                    .addEventListeners(bot)
                    .build();

            bot.setJDA(jda);

            System.out.println("Waiting for bot to be ready...");
            jda.awaitReady();

            System.out.println("=====================================");
            System.out.println("✅ Bot is ready! Logged in as " + jda.getSelfUser().getAsTag());
            System.out.println("📊 Serving " + jda.getGuilds().size() + " servers");
            System.out.println("🎯 Target Server ID: " + targetServerId);
            System.out.println("📢 Contract Channel: " + targetChannelContract);
            System.out.println("📢 Full Revive Channel: " + targetChannelFull);
            System.out.println("📢 Partial Revive Channel: " + targetChannelPartial);
            System.out.println("=====================================");

        } catch (Exception e) {
            System.err.println("=====================================");
            System.err.println("❌ Failed to start bot: " + e.getClass().getSimpleName());
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
