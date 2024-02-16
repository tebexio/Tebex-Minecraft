package io.tebex.sdk.analytics.obj;

import com.google.common.collect.Maps;
import io.tebex.analytics.sdk.platform.PlayerType;

import java.util.*;

/**
 * The AnalysePlayer class represents a player on the server platform
 * and holds information and statistics about the player.
 */
public class AnalysePlayer {
    private final String name;
    private final UUID uuid;
    private final Date joinedAt;
    private final String ipAddress;
    private Date firstJoinedAt;
    private Date quitAt;
    private PlayerType type;
    private String country;

    private String domain;

    private final Map<String, Object> statistics;
    private final List<PlayerEvent> events;

    /**
     * Creates a new AnalysePlayer with the provided name, UUID, and IP address.
     *
     * @param name      The name of the player.
     * @param uuid      The UUID of the player.
     * @param ipAddress The IP address of the player.
     */
    public AnalysePlayer(String name, UUID uuid, String ipAddress) {
        this.name = name;
        this.uuid = uuid;
        this.joinedAt = new Date();
        this.quitAt = null;
        this.ipAddress = ipAddress;
        this.statistics = Maps.newHashMap();
        this.events = new ArrayList<>();
        this.type = PlayerType.JAVA;
    }

    /**
     * Retrieves the name of the player.
     *
     * @return The name of the player.
     */
    public String getName() {
        return name;
    }

    /**
     * Retrieves the unique identifier (UUID) of the player.
     *
     * @return The UUID of the player.
     */
    public UUID getUniqueId() {
        return uuid;
    }

    /**
     * Retrieves the date and time the player joined the server.
     *
     * @return The joinedAt date.
     */
    public Date getJoinedAt() {
        return joinedAt;
    }

    /**
     * Retrieves the date and time the player first joined the server.
     *
     * @return The firstJoinedAt date.
     */
    public Date getFirstJoinedAt() {
        return firstJoinedAt;
    }

    /**
     * Retrieves the IP address of the player.
     *
     * @return The IP address of the player.
     */
    public String getIpAddress() {
        return ipAddress;
    }

    /**
     * Retrieves the list of player statistics.
     *
     * @return A list of PlayerStatistic objects.
     */
    public Map<String, Object> getStatistics() {
        return statistics;
    }

    /**
     * Retrieves the list of player events.
     *
     * @return A list of PlayerEvent objects.
     */
    public List<PlayerEvent> getEvents() {
        return events;
    }

    /**
     * Retrieves the player's domain.
     *
     * @return The domain of the player.
     */
    public String getDomain() {
        return domain;
    }

    /**
     * Sets the player's domain.
     *
     * @param domain The domain to be set.
     */
    public void setDomain(String domain) {
        this.domain = domain;
    }


    /**
     * Retrieves the player's client type.
     *
     * @return The PlayerType of the player.
     */
    public PlayerType getType() {
        return type;
    }

    /**
     * Retrieves the player's country.
     *
     * @return The country of the player.
     */
    public String getCountry() {
        return country;
    }

    /**
     * Sets the player's client type.
     *
     * @param type The PlayerType to be set.
     */
    public void setType(PlayerType type) {
        this.type = type;
    }

    /**
     * Sets the player's country.
     *
     * @param country The country to be set.
     */
    public void setCountry(String country) {
        this.country = country;
    }

    /**
     * Calculates the player's session duration.
     *
     * @param quitAt The date the player quit.
     * @return The session duration in seconds.
     */
    public int getDurationInSeconds(Date quitAt) {
        return (int) ((quitAt.getTime() - joinedAt.getTime()) / 1000);
    }

    /**
     * Calculates the player's session duration.
     *
     * @return The session duration in seconds.
     */
    public int getDurationInSeconds() {
        return this.quitAt != null ? getDurationInSeconds(this.quitAt) : getDurationInSeconds(new Date());
    }

    /**
     * Logs out the player and sets the quit date.
     */
    public void logout() {
        this.quitAt = new Date();
    }

    /**
     * Tracks a custom event.
     *
     * @param event The event to track.
     */
    public void track(PlayerEvent... event) {
        Arrays.stream(event).forEach(evt -> getEvents().add(evt));
    }

    /**
     * Sets the date and time the player first joined the server.
     *
     * @param firstJoinedAt The firstJoinedAt date to be set.
     */
    public void setFirstJoinedAt(Date firstJoinedAt) {
        this.firstJoinedAt = firstJoinedAt;
    }

    /**
     * Generates a string representation of the AnalysePlayer object.
     *
     * @return A string representation of the AnalysePlayer object.
     */
    @Override
    public String toString() {
        return "AnalysePlayer{" +
                "name='" + name + '\'' +
                ", uuid=" + uuid +
                ", joinedAt=" + joinedAt +
                ", ipAddress='" + ipAddress + '\'' +
                ", firstJoinedAt=" + firstJoinedAt +
                ", quitAt=" + quitAt +
                ", type=" + type +
                ", domain='" + domain + '\'' +
                ", statistics=" + statistics +
                ", events=" + events +
                '}';
    }
}