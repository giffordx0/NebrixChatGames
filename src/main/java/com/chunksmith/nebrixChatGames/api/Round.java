package com.chunksmith.nebrixChatGames.api;

/**
 * Legacy Round class for backward compatibility
 * @deprecated Use GameRound instead
 */
@Deprecated
public class Round extends GameRound {

    // Legacy field names for compatibility
    public final String gameId;
    public final String prompt;
    public final String answer;
    public final long timeoutMillis;
    public final long warmupMillis;
    public final long startAtMillis;
    public final boolean caseSensitive;

    public Round(String gameId, String prompt, String answer, long timeoutMillis,
                 long warmupMillis, boolean caseSensitive) {
        super(gameId, gameId, prompt, answer, timeoutMillis, warmupMillis, caseSensitive);

        // Initialize legacy fields
        this.gameId = gameId;
        this.prompt = prompt;
        this.answer = answer;
        this.timeoutMillis = timeoutMillis;
        this.warmupMillis = warmupMillis;
        this.startAtMillis = getStartTime();
        this.caseSensitive = caseSensitive;
    }

    public Round(String gameId, String prompt, String answer, long timeoutMillis, long warmupMillis) {
        this(gameId, prompt, answer, timeoutMillis, warmupMillis, false);
    }
}