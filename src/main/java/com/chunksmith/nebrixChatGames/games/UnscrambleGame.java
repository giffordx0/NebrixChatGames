package com.chunksmith.nebrixChatGames.games;
import com.chunksmith.nebrixChatGames.NebrixChatGames;
import com.chunksmith.nebrixChatGames.api.AbstractChatGame;
import com.chunksmith.nebrixChatGames.api.GameRound;
import com.chunksmith.nebrixChatGames.config.ConfigManager;
import com.chunksmith.nebrixChatGames.util.WordProvider;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

import static org.bukkit.Bukkit.getLogger;

/**
 * Word unscrambling game with configurable difficulty
 */
public class UnscrambleGame extends AbstractChatGame {

    private static final String GAME_ID = "unscramble";
    private static final String DISPLAY_NAME = "Unscramble";
    private static final String DEFAULT_WORD = "minecraft";
    private static final int MAX_WORD_SELECTION_ATTEMPTS = 50;

    private final WordProvider wordProvider;

    public UnscrambleGame(NebrixChatGames plugin, WordProvider wordProvider, ConfigManager configManager) {
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
        try {
            final String selectedWord = selectWord();
            final String scrambledWord = scrambleWord(selectedWord);

            // Ensure we actually scrambled the word
            if (scrambledWord.equals(selectedWord)) {
                // Try one more scramble
                final String rescrambled = scrambleWord(selectedWord);
                if (!rescrambled.equals(selectedWord)) {
                    return createRoundWithWords(rescrambled, selectedWord);
                }
                // If still the same, add some indication it's already scrambled
                getLogger().fine("Selected word '" + selectedWord + "' could not be scrambled effectively");
            }

            return createRoundWithWords(scrambledWord, selectedWord);

        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Failed to create unscramble round, using fallback", e);
            return createRoundWithWords(DEFAULT_WORD, DEFAULT_WORD);
        }
    }

    private GameRound createRoundWithWords(String prompt, String answer) {
        return new GameRound(
                getId(),
                prompt,
                answer,
                System.currentTimeMillis(),
                getWarmupDuration(),
                getTimeoutDuration(),
                isCaseSensitive()
        );
    }

    @Override
    public boolean isCorrectAnswer(String answer, Player player, GameRound round) {
        return checkSimpleAnswer(answer, round.getCorrectAnswer(), round.isCaseSensitive());
    }

    @Override
    public String getCorrectAnswer(GameRound round) {
        return round.getCorrectAnswer();
    }

    /**
     * Select an appropriate word for unscrambling
     */
    private String selectWord() {
        final List<String> availableWords = wordProvider.getWords();

        if (availableWords.isEmpty()) {
            getLogger().warning("No words available in word list, using default");
            return DEFAULT_WORD;
        }

        final int minLength = config.getUnscrambleMinLength();
        final int maxLength = config.getUnscrambleMaxLength();

        // Try to find a word within length constraints
        for (int attempt = 0; attempt < MAX_WORD_SELECTION_ATTEMPTS; attempt++) {
            final String candidate = availableWords.get(
                    ThreadLocalRandom.current().nextInt(availableWords.size())
            );

            if (candidate.length() >= minLength && candidate.length() <= maxLength) {
                return candidate.toLowerCase();
            }
        }

        // Fallback: use any word
        getLogger().fine("Could not find word within length constraints, using random word");
        return availableWords.get(ThreadLocalRandom.current().nextInt(availableWords.size())).toLowerCase();
    }

    /**
     * Scramble a word using character swapping
     */
    private String scrambleWord(String word) {
        if (word.length() <= 2) {
            return word; // Can't meaningfully scramble very short words
        }

        final char[] chars = word.toCharArray();
        final SwapRange swapRange = parseSwapRange();
        final ThreadLocalRandom random = ThreadLocalRandom.current();

        final int swaps = random.nextInt(swapRange.min(), swapRange.max() + 1);

        for (int i = 0; i < swaps; i++) {
            final int pos1 = random.nextInt(chars.length);
            final int pos2 = random.nextInt(chars.length);

            if (pos1 != pos2) {
                final char temp = chars[pos1];
                chars[pos1] = chars[pos2];
                chars[pos2] = temp;
            }
        }

        return new String(chars);
    }

    /**
     * Parse swap range from configuration
     */
    private SwapRange parseSwapRange() {
        final String rangeString = config.getUnscrambleSwaps();

        try {
            final String[] parts = rangeString.split("-", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid range format");
            }

            final int min = Math.max(1, Integer.parseInt(parts[0].trim()));
            final int max = Math.max(min, Integer.parseInt(parts[1].trim()));

            return new SwapRange(min, max);

        } catch (Exception e) {
            getLogger().warning("Invalid scramble-swaps config: " + rangeString + ", using default 3-6");
            return new SwapRange(3, 6);
        }
    }

    private record SwapRange(int min, int max) {}
}