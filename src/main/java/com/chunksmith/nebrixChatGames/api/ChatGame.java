package com.chunksmith.nebrixChatGames.api;

import org.bukkit.entity.Player;

/**
 * Base interface for all chat games
 */
public interface ChatGame {

    /**
     * Get the unique identifier for this game
     * @return game ID
     */
    String getId();

    /**
     * Get the display name for this game
     * @return display name
     */
    String getDisplayName();

    /**
     * Create a new round for this game
     * @return new game round
     */
    GameRound createRound();

    /**
     * Check if the given answer is correct for the current round
     * @param answer the player's answer
     * @param player the player who answered
     * @param round the current round
     * @return true if correct, false otherwise
     */
    boolean isCorrectAnswer(String answer, Player player, GameRound round);

    /**
     * Get the ID of this game (legacy method for compatibility)
     * @deprecated Use getId() instead
     */
    @Deprecated
    default String id() {
        return getId();
    }

    /**
     * Start a new round (legacy method for compatibility)
     * @deprecated Use createRound() instead
     */
    @Deprecated
    default GameRound startRound() {
        return createRound();
    }

    /**
     * Check if answer is correct (legacy method for compatibility)
     * @deprecated Use isCorrectAnswer() instead
     */
    @Deprecated
    default boolean isCorrect(String answer, Player player, GameRound round) {
        return isCorrectAnswer(answer, player, round);
    }
}