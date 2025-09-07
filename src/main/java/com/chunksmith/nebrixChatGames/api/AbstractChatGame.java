/**
 * Abstract base class for chat games with common functionality
 */
package com.chunksmith.nebrixChatGames.api;

import com.chunksmith.nebrixChatGames.NebrixChatGames;
import com.chunksmith.nebrixChatGames.config.ConfigManager;

public abstract class AbstractChatGame implements ChatGame {

    protected final NebrixChatGames plugin;
    protected final ConfigManager config;

    protected AbstractChatGame(NebrixChatGames plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
    }

    @Override
    public boolean isEnabled() {
        return config.isGameEnabled(getId());
    }

    /**
     * Get the warmup duration from config
     * @return Warmup duration in milliseconds
     */
    protected long getWarmupDuration() {
        return config.getAnswerWarmup();
    }

    /**
     * Get the timeout duration from config
     * @return Timeout duration in milliseconds
     */
    protected long getTimeoutDuration() {
        return config.getGameTimeout() * 1000L;
    }

    /**
     * Get case sensitivity setting from config
     * @return true if answers should be case-sensitive
     */
    protected boolean isCaseSensitive() {
        return config.getRawConfig().getBoolean(getConfigPrefix() + ".case-sensitive",
                config.isDefaultCaseSensitive());
    }

    /**
     * Utility method for simple string-based answer checking
     * @param playerAnswer The player's normalized answer
     * @param correctAnswer The correct answer
     * @param caseSensitive Whether comparison should be case-sensitive
     * @return true if answers match
     */
    protected boolean checkSimpleAnswer(String playerAnswer, String correctAnswer, boolean caseSensitive) {
        if (playerAnswer == null || correctAnswer == null) {
            return false;
        }

        if (caseSensitive) {
            return playerAnswer.equals(correctAnswer);
        } else {
            return playerAnswer.equalsIgnoreCase(correctAnswer);
        }
    }
}