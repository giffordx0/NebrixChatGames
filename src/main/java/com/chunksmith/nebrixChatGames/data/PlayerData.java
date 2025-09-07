package com.chunksmith.nebrixChatGames.data;

import java.util.UUID;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Public PlayerData class for storing player statistics
 */
public class PlayerData {

    private final UUID playerId;
    private int gamesWon;
    private int gamesPlayed;
    private long totalRewards;
    private long lastPlayed;
    private final Map<String, Integer> wins = new HashMap<>();

    public PlayerData(UUID playerId) {
        this.playerId = playerId;
        this.gamesWon = 0;
        this.gamesPlayed = 0;
        this.totalRewards = 0;
        this.lastPlayed = System.currentTimeMillis();
    }

    public PlayerData(UUID playerId, int gamesWon, int gamesPlayed, long totalRewards, long lastPlayed) {
        this.playerId = playerId;
        this.gamesWon = gamesWon;
        this.gamesPlayed = gamesPlayed;
        this.totalRewards = totalRewards;
        this.lastPlayed = lastPlayed;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public int getGamesWon() {
        return gamesWon;
    }

    public void setGamesWon(int gamesWon) {
        this.gamesWon = gamesWon;
    }

    public void incrementGamesWon() {
        this.gamesWon++;
    }

    public void incrementWins(String gameId) {
        incrementGamesWon();
        wins.merge(gameId, 1, Integer::sum);
    }

    public int getGamesPlayed() {
        return gamesPlayed;
    }

    public void setGamesPlayed(int gamesPlayed) {
        this.gamesPlayed = gamesPlayed;
    }

    public void incrementGamesPlayed() {
        this.gamesPlayed++;
    }

    public long getTotalRewards() {
        return totalRewards;
    }

    public void setTotalRewards(long totalRewards) {
        this.totalRewards = totalRewards;
    }

    public void addReward(long reward) {
        this.totalRewards += reward;
    }

    public long getLastPlayed() {
        return lastPlayed;
    }

    public void setLastPlayed(long lastPlayed) {
        this.lastPlayed = lastPlayed;
    }

    public void updateLastPlayed() {
        this.lastPlayed = System.currentTimeMillis();
    }

    public double getWinRate() {
        return gamesPlayed > 0 ? (double) gamesWon / gamesPlayed : 0.0;
    }

    public Map<String, Integer> getWins() {
        return Collections.unmodifiableMap(wins);
    }

    public static PlayerData fromConfig(UUID playerId, FileConfiguration config) {
        final int won = config.getInt("games-won", 0);
        final int played = config.getInt("games-played", 0);
        final long rewards = config.getLong("total-rewards", 0);
        final long last = config.getLong("last-played", System.currentTimeMillis());
        PlayerData data = new PlayerData(playerId, won, played, rewards, last);
        ConfigurationSection winsSection = config.getConfigurationSection("wins");
        if (winsSection != null) {
            for (String key : winsSection.getKeys(false)) {
                data.wins.put(key, winsSection.getInt(key, 0));
            }
        }
        return data;
    }

    public void saveToConfig(FileConfiguration config) {
        config.set("games-won", gamesWon);
        config.set("games-played", gamesPlayed);
        config.set("total-rewards", totalRewards);
        config.set("last-played", lastPlayed);
        ConfigurationSection section = config.createSection("wins");
        for (Map.Entry<String, Integer> entry : wins.entrySet()) {
            section.set(entry.getKey(), entry.getValue());
        }
    }
}