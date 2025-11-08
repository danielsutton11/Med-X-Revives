package com.claimbot;

public class ClaimData {
    private final String userId;
    private final String userTag;
    private final String guildName;
    private final String channelName;
    private final String messageId;
    private final long timestamp;
    private final boolean fullRevive;

    private boolean claimed;
    private String claimedBy;
    private long claimedAt;

    public ClaimData(String userId, String userTag, String guildName,
                     String channelName, String messageId, long timestamp, boolean fullRevive) {
        this.userId = userId;
        this.userTag = userTag;
        this.guildName = guildName;
        this.channelName = channelName;
        this.messageId = messageId;
        this.timestamp = timestamp;
        this.fullRevive = fullRevive;
        this.claimed = false;
    }

    // Getters
    public String getUserId() {
        return userId;
    }

    public String getUserTag() {
        return userTag;
    }

    public String getGuildName() {
        return guildName;
    }

    public String getChannelName() {
        return channelName;
    }

    public String getMessageId() {
        return messageId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isFullRevive() {
        return fullRevive;
    }

    public boolean isClaimed() {
        return claimed;
    }

    public String getClaimedBy() {
        return claimedBy;
    }

    public long getClaimedAt() {
        return claimedAt;
    }

    // Setters
    public void setClaimed(boolean claimed) {
        this.claimed = claimed;
    }

    public void setClaimedBy(String claimedBy) {
        this.claimedBy = claimedBy;
    }

    public void setClaimedAt(long claimedAt) {
        this.claimedAt = claimedAt;
    }
}
