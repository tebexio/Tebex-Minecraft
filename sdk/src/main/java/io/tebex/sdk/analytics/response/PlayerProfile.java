package io.tebex.sdk.analytics.response;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Represents a player's profile, including their name, UUID, and statistics.
 */
public class PlayerProfile {
    private final String name;
    private final UUID uuid;
    private final Date createdAt;
    private final Date updatedAt;
    private final Date firstJoinedAt;
    private final Date lastLoggedInAt;
    private final int totalSessionTime;
    private final List<Statistic> statistics;

    /**
     * Constructs a PlayerProfile instance.
     *
     * @param name The player's name.
     * @param uuid The player's UUID.
     * @param createdAt The player's creation date.
     * @param updatedAt The player's last updated date.
     * @param firstJoinedAt The player's first join date.
     * @param lastLoggedInAt The player's last login date.
     * @param totalSessionTime The player's total session time in minutes.
     * @param statistics The player's list of statistics.
     */
    public PlayerProfile(String name, UUID uuid, Date createdAt, Date updatedAt, Date firstJoinedAt, Date lastLoggedInAt, int totalSessionTime, List<Statistic> statistics) {
        this.name = name;
        this.uuid = uuid;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.firstJoinedAt = firstJoinedAt;
        this.lastLoggedInAt = lastLoggedInAt;
        this.totalSessionTime = totalSessionTime;
        this.statistics = statistics;
    }

    /**
     * Returns the player's name.
     *
     * @return The player's name.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the player's UUID.
     *
     * @return The player's UUID.
     */
    public UUID getUuid() {
        return uuid;
    }

    /**
     * Returns the player's creation date.
     *
     * @return The player's creation date.
     */
    public Date getCreatedAt() {
        return createdAt;
    }

    /**
     * Returns the player's last updated date.
     *
     * @return The player's last updated date.
     */
    public Date getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Returns the player's first join date.
     *
     * @return The player's first join date.
     */
    public Date getFirstJoinedAt() {
        return firstJoinedAt;
    }

    /**
     * Returns the player's last login date.
     *
     * @return The player's last login date.
     */
    public Date getLastLoggedInAt() {
        return lastLoggedInAt;
    }

    /**
     * Returns the player's total session time in minutes.
     *
     * @return The player's total session time in minutes.
     */
    public int getTotalSessionTime() {
        return totalSessionTime;
    }

    /**
     * Returns the player's list of statistics.
     *
     * @return The player's list of statistics.
     */
    public List<Statistic> getStatistics() {
        return statistics;
    }

    /**
     * Represents a player's statistic.
     */
    public static class Statistic {
        private final String nickname;
        private final String placeholder;
        private final Object value;

        /**
         * Constructs a Statistic instance.
         *
         * @param nickname The statistic's nickname.
         * @param placeholder The statistic's placeholder.
         * @param value The statistic's value.
         */
        public Statistic(String nickname, String placeholder, Object value) {
            this.nickname = nickname;
            this.placeholder = placeholder;
            this.value = value;
        }

        /**
         * Returns the statistic's nickname.
         *
         * @return The statistic's nickname.
         */
        public String getNickname() {
            return nickname;
        }

        /**
         * Returns the statistic's placeholder.
         *
         * @return The statistic's placeholder.
         */
        public String getPlaceholder() {
            return placeholder;
        }

        /**
         * Returns the statistic's value.
         *
         * @return The statistic's value.
         */
        public Object getValue() {
            return value;
        }
    }
}