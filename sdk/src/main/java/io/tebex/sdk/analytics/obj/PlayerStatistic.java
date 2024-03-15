package io.tebex.sdk.analytics.obj;

/**
 * The PlayerStatistic class represents a statistic related to a player on the server.
 * It stores information such as a key-value pair describing the specific statistic.
 */
public class PlayerStatistic {
    private final String key;
    private final Object value;

    /**
     * Constructs a PlayerStatistic with the given key and value.
     *
     * @param key   The key for the statistic.
     * @param value The value for the statistic.
     */
    public PlayerStatistic(String key, Object value) {
        this.key = key;
        this.value = value;
    }

    /**
     * Retrieves the key for the statistic.
     *
     * @return The statistic key.
     */
    public String getKey() {
        return key;
    }

    /**
     * Retrieves the value for the statistic.
     *
     * @return The statistic value.
     */
    public Object getValue() {
        return value;
    }
}