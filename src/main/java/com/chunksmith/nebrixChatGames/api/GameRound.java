package com.chunksmith.nebrixChatGames.api;

/**
 * Represents a single round of a chat game
 */
public class GameRound {

    private final String gameId;
    private final String gameName;
    private final String prompt;
    private final String correctAnswer;
    private final long timeoutDuration;
    private final long warmupDuration;
    private final long startTime;
    private final boolean caseSensitive;

    public GameRound(String gameId, String gameName, String prompt, String correctAnswer,
                     long timeoutDuration, long warmupDuration) {
        this(gameId, gameName, prompt, correctAnswer, timeoutDuration, warmupDuration, false);
    }

    public GameRound(String gameId, String gameName, String prompt, String correctAnswer,
                     long timeoutDuration, long warmupDuration, boolean caseSensitive) {
        this.gameId = gameId;
        this.gameName = gameName;
        this.prompt = prompt;
        this.correctAnswer = correctAnswer;
        this.timeoutDuration = timeoutDuration;
        this.warmupDuration = warmupDuration;
        this.caseSensitive = caseSensitive;
        this.startTime = System.currentTimeMillis();
    }

    public String getGameId() {
        return gameId;
    }

    public String getGameName() {
        return gameName;
    }

    public String getPrompt() {
        return prompt;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public long getTimeoutDuration() {
        return timeoutDuration;
    }

    public long getWarmupDuration() {
        return warmupDuration;
    }

    public long getStartTime() {
        return startTime;
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - startTime > timeoutDuration;
    }

    public long getRemainingTime() {
        return Math.max(0, (startTime + timeoutDuration) - System.currentTimeMillis());
    }

    public boolean isWarmupPeriod() {
        return System.currentTimeMillis() - startTime < warmupDuration;
    }
}