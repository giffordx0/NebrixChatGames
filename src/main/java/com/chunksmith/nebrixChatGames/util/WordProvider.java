package com.chunksmith.nebrixChatGames.util;

import com.chunksmith.nebrixChatGames.NebrixChatGames;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

/**
 * Manages word lists for games that require vocabulary
 * Thread-safe with automatic reloading capabilities
 */
public class WordProvider {

    private final NebrixChatGames plugin;
    private volatile List<String> words = new ArrayList<>();
    private volatile long lastLoadTime = 0;

    // Default words as fallback
    private static final List<String> DEFAULT_WORDS = List.of(
            "minecraft", "building", "adventure", "explore", "crafting",
            "diamond", "emerald", "redstone", "portal", "village",
            "castle", "dragon", "treasure", "dungeon", "warrior",
            "wizard", "knight", "archer", "potion", "enchant",
            "forest", "mountain", "ocean", "desert", "plains",
            "survival", "creative", "peaceful", "hostile", "neutral"
    );

    public WordProvider(NebrixChatGames plugin) {
        this.plugin = plugin;
    }

    /**
     * Load words from configuration file
     * Creates default file if it doesn't exist
     */
    public void loadWords() {
        try {
            final File wordsFile = new File(plugin.getDataFolder(), "words.yml");

            // Create default file if it doesn't exist
            if (!wordsFile.exists()) {
                createDefaultWordsFile(wordsFile);
            }

            final FileConfiguration wordsConfig = YamlConfiguration.loadConfiguration(wordsFile);
            final List<String> loadedWords = wordsConfig.getStringList("words");

            if (loadedWords.isEmpty()) {
                plugin.getLogger().warning("No words found in words.yml, using defaults");
                this.words = new ArrayList<>(DEFAULT_WORDS);
            } else {
                // Filter and validate words
                this.words = loadedWords.stream()
                        .filter(word -> word != null && !word.trim().isEmpty())
                        .map(String::trim)
                        .map(String::toLowerCase)
                        .distinct()
                        .toList();

                plugin.getLogger().info("Loaded " + this.words.size() + " words from words.yml");
            }

            this.lastLoadTime = System.currentTimeMillis();

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load words, using defaults", e);
            this.words = new ArrayList<>(DEFAULT_WORDS);
        }
    }

    /**
     * Get immutable list of all words
     * @return List of words
     */
    public List<String> getWords() {
        return Collections.unmodifiableList(words);
    }

    /**
     * Get words filtered by length
     * @param minLength Minimum word length
     * @param maxLength Maximum word length
     * @return Filtered list of words
     */
    public List<String> getWordsByLength(int minLength, int maxLength) {
        return words.stream()
                .filter(word -> word.length() >= minLength && word.length() <= maxLength)
                .toList();
    }

    /**
     * Get the number of loaded words
     * @return Word count
     */
    public int getWordCount() {
        return words.size();
    }

    /**
     * Check if words were loaded recently (for caching)
     * @return true if loaded within last 5 minutes
     */
    public boolean isRecentlyLoaded() {
        return (System.currentTimeMillis() - lastLoadTime) < 300000; // 5 minutes
    }

    /**
     * Create default words.yml file
     */
    private void createDefaultWordsFile(File wordsFile) throws IOException {
        plugin.getDataFolder().mkdirs();

        // Try to copy from plugin resources first
        try (InputStream resourceStream = plugin.getResource("words.yml")) {
            if (resourceStream != null) {
                Files.copy(resourceStream, wordsFile.toPath());
                plugin.getLogger().info("Created default words.yml from plugin resources");
                return;
            }
        } catch (Exception e) {
            plugin.getLogger().fine("Could not copy words.yml from resources: " + e.getMessage());
        }

        // Create file with default words
        final FileConfiguration config = new YamlConfiguration();
        config.set("words", DEFAULT_WORDS);
        config.save(wordsFile);

        plugin.getLogger().info("Created default words.yml with " + DEFAULT_WORDS.size() + " words");
    }
}