package com.chunksmith.nebrixChatGames.data;

import java.util.UUID;

/**
 * Public PlayerData class for storing player statistics
 */
public class PlayerData {

    private final UUID playerId;
    private int gamesWon;
    private int gamesPlayed;
    private long totalRewards;
    private long lastPlayed;

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
}