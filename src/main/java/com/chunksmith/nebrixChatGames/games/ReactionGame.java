package com.chunksmith.nebrixChatGames.games;
import com.chunksmith.nebrixChatGames.NebrixChatGames;
import com.chunksmith.nebrixChatGames.api.AbstractChatGame;
import com.chunksmith.nebrixChatGames.api.GameRound;
import com.chunksmith.nebrixChatGames.util.WordProvider;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Fast reaction game where players type the displayed word
 */
public class ReactionGame extends AbstractChatGame {

    private static final String GAME_ID = "reaction";
    private static final String DISPLAY_NAME = "Reaction";
    private static final String DEFAULT_WORD = "react";

    private final WordProvider wordProvider;

    public ReactionGame(NebrixChatGames plugin, WordProvider wordProvider) {
        super(plugin);
        this.wordProvider = wordProvider;
    }

    @Override
    public String getId() {
        return GAME_ID;
    }

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    public GameRound createRound() {
        final String selectedWord = selectReactionWord();

        return new GameRound(
                getId(),
                getDisplayName(),
                selectedWord,
                selectedWord,
                getTimeoutDuration(),
                getWarmupDuration(),
                isCaseSensitive()
        );
    }

    @Override
    public boolean isCorrectAnswer(String answer, Player player, GameRound round) {
        return checkSimpleAnswer(answer, round.getCorrectAnswer(), round.isCaseSensitive());
    }

    /**
     * Select a word for reaction game (typically shorter words)
     */
    private String selectReactionWord() {
        final List<String> availableWords = wordProvider.getWords();

        if (availableWords.isEmpty()) {
            return DEFAULT_WORD;
        }

        // Prefer shorter words for reaction games
        final List<String> shortWords = availableWords.stream()
                .filter(word -> word.length() >= 3 && word.length() <= 8)
                .toList();

        if (!shortWords.isEmpty()) {
            return shortWords.get(ThreadLocalRandom.current().nextInt(shortWords.size())).toLowerCase();
        }

        // Fallback to any word
        return availableWords.get(ThreadLocalRandom.current().nextInt(availableWords.size())).toLowerCase();
    }
}