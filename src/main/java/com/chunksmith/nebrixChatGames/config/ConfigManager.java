package com.chunksmith.nebrixChatGames.config;

import com.chunksmith.nebrixChatGames.NebrixChatGames;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

/**
 * Manages plugin configuration with validation, caching, and type safety
 * Follows Bukkit best practices for configuration management
 */
public class ConfigManager {

    private final NebrixChatGames plugin;
    private FileConfiguration config;

    // Configuration cache for frequently accessed values
    private volatile ConfigCache cache;

    public ConfigManager(NebrixChatGames plugin) {
        this.plugin = plugin;
    }

    /**
     * Load configuration from file with validation
     * @return true if loaded successfully
     */
    public boolean loadConfig() {
        try {
            // Save default config if it doesn't exist
            plugin.saveDefaultConfig();

            // Reload from disk
            plugin.reloadConfig();
            this.config = plugin.getConfig();

            // Validate configuration
            if (!validateConfig()) {
                plugin.getLogger().severe("Configuration validation failed");
                return false;
            }

            // Build cache
            this.cache = buildConfigCache();

            plugin.getLogger().info("Configuration loaded and validated successfully");
            return true;

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load configuration", e);
            return false;
        }
    }

    /**
     * Validate all configuration values
     * @return true if valid
     */
    private boolean validateConfig() {
        boolean valid = true;

        // Validate timeout settings
        final int timeout = config.getInt("settings.round.timeout-seconds", 30);
        if (timeout <= 0 || timeout > 300) {
            plugin.getLogger().warning("Invalid timeout-seconds: " + timeout + " (using default: 30)");
        }

        // Validate warmup settings
        final int warmup = config.getInt("settings.round.answer-warmup-ms", 500);
        if (warmup < 0 || warmup > 5000) {
            plugin.getLogger().warning("Invalid answer-warmup-ms: " + warmup + " (using default: 500)");
        }

        // Validate minimum players
        final int minPlayers = config.getInt("settings.min-players-online", 1);
        if (minPlayers < 0 || minPlayers > 100) {
            plugin.getLogger().warning("Invalid min-players-online: " + minPlayers + " (using default: 1)");
        }

        // Validate scheduler settings
        if (isSchedulerEnabled()) {
            final int interval = config.getInt("settings.scheduler.interval-seconds", 300);
            if (interval < 60) {
                plugin.getLogger().warning("Scheduler interval too short: " + interval + "s (minimum: 60s)");
            }
        }

        // Validate rate limiting
        final int rateLimit = config.getInt("settings.anti-cheat.rate-limit-per-sec", 3);
        if (rateLimit <= 0 || rateLimit > 20) {
            plugin.getLogger().warning("Invalid rate-limit-per-sec: " + rateLimit + " (using default: 3)");
        }

        return valid;
    }

    /**
     * Build configuration cache for performance
     */
    private ConfigCache buildConfigCache() {
        return new ConfigCache(
                // General settings
                config.getString("settings.prefix", "<gradient:#FF5EDF:#00C9FF><bold>CHAT GAMES</bold></gradient> <gray>»</gray>"),
                config.getStringList("settings.disabled-worlds"),
                config.getBoolean("settings.use-permission-toplay", false),
                Math.max(1, config.getInt("settings.min-players-online", 1)),

                // Game settings
                Math.max(10, config.getInt("settings.round.timeout-seconds", 30)),
                Math.max(0, config.getInt("settings.round.answer-warmup-ms", 500)),
                config.getBoolean("settings.round.case-sensitive-default", false),

                // Scheduler settings
                "rotate".equalsIgnoreCase(config.getString("settings.scheduler.mode", "manual")),
                Math.max(60, config.getInt("settings.scheduler.interval-seconds", 300)),

                // Anti-cheat settings
                Math.max(1, Math.min(20, config.getInt("settings.anti-cheat.rate-limit-per-sec", 3))),
                config.getBoolean("settings.anti-cheat.strip-colors", true),
                config.getBoolean("settings.anti-cheat.normalize-homoglyphs", true),

                // UI settings
                config.getBoolean("settings.round.play-sound", true),
                config.getString("settings.round.sound", "entity.player.levelup"),
                config.getBoolean("settings.round.bossbar", true),
                config.getBoolean("settings.round.titles", true),

                // Integration settings
                config.getBoolean("integration.economy.enabled", true),
                config.getString("integration.economy.provider", "vault"),
                config.getString("integration.crystals.award-command", "eco give %player% %amount%")
        );
    }

    // Cached getters for frequently accessed values
    public String getPrefix() { return cache.prefix; }
    public List<String> getDisabledWorlds() { return cache.disabledWorlds; }
    public boolean requiresPermissionToPlay() { return cache.requiresPermissionToPlay; }
    public int getMinPlayersOnline() { return cache.minPlayersOnline; }
    public int getGameTimeout() { return cache.gameTimeout; }
    public int getAnswerWarmup() { return cache.answerWarmup; }
    public boolean isDefaultCaseSensitive() { return cache.defaultCaseSensitive; }
    public boolean isSchedulerEnabled() { return cache.schedulerEnabled; }
    public int getSchedulerInterval() { return cache.schedulerInterval; }
    public int getRateLimit() { return cache.rateLimit; }
    public boolean shouldStripColors() { return cache.stripColors; }
    public boolean shouldNormalizeHomoglyphs() { return cache.normalizeHomoglyphs; }
    public boolean shouldPlaySounds() { return cache.playSounds; }
    public String getStartSound() { return cache.startSound; }
    public boolean shouldShowBossbar() { return cache.showBossbar; }
    public boolean shouldShowTitles() { return cache.showTitles; }
    public boolean isEconomyEnabled() { return cache.economyEnabled; }
    public String getEconomyProvider() { return cache.economyProvider; }
    public String getCrystalCommand() { return cache.crystalCommand; }

    /**
     * Check if a specific game is enabled
     * @param gameId The game ID to check
     * @return true if enabled
     */
    public boolean isGameEnabled(String gameId) {
        return config.getBoolean("games." + gameId + ".enabled", true);
    }

    /**
     * Get game-specific weight for scheduler
     * @param gameId The game ID
     * @return The weight (default: 1)
     */
    public int getGameWeight(String gameId) {
        return Math.max(0, config.getInt("settings.scheduler.rotate-weight." + gameId, 1));
    }

    /**
     * Get all configured game weights
     * @return Map of game IDs to weights
     */
    public Set<String> getConfiguredGameWeights() {
        final ConfigurationSection weightsSection = config.getConfigurationSection("settings.scheduler.rotate-weight");
        return weightsSection != null ? weightsSection.getKeys(false) : Collections.emptySet();
    }

    /**
     * Get Levenshtein distance tolerance for a game type
     * @param gameType The game type
     * @return Maximum allowed edit distance
     */
    public int getLevenshteinTolerance(String gameType) {
        return Math.max(0, config.getInt("settings.anti-cheat.levenshtein-tolerance." + gameType, 0));
    }

    // Reward configuration methods
    public long getBaseCoins() {
        return Math.max(0, config.getLong("rewards.base.coins", 100));
    }

    public long getBaseCrystals() {
        return Math.max(0, config.getLong("rewards.base.crystals", 2));
    }

    public List<String> getBaseCommands() {
        return config.getStringList("rewards.base.commands");
    }

    public List<String> getBaseItems() {
        return config.getStringList("rewards.base.items");
    }

    public int getBonusChance() {
        return Math.max(0, Math.min(100, config.getInt("rewards.bonus-chance.percent", 15)));
    }

    public List<String> getBonusCommands() {
        return config.getStringList("rewards.bonus-chance.commands");
    }

    public long getGameCoins(String gameId) {
        return config.getLong("rewards.per-game." + gameId + ".coins", getBaseCoins());
    }

    public long getGameCrystals(String gameId) {
        return config.getLong("rewards.per-game." + gameId + ".crystals", getBaseCrystals());
    }

    // UI Configuration methods
    public String getBossbarFormat() {
        return config.getString("style.bossbar.format",
                "<gradient:#FF5EDF:#00C9FF><bold>CHAT GAMES</bold></gradient> <gray>•</gray> <white>%game%</white>");
    }

    public String getActionbarFormat() {
        return config.getString("style.actionbar",
                "<gray>⏳ <yellow><bold>%seconds%s</bold></yellow> remaining</gray>");
    }

    public String getTitleMain() {
        return config.getString("style.title.main",
                "<gradient:#FF5EDF:#00C9FF><bold>CHAT GAMES</bold></gradient>");
    }

    public String getTitleSubtitle(String gameType) {
        return config.getString("style.title.subtitle." + gameType,
                "<gray>Get ready!</gray>");
    }

    public int getTitleFadeIn() {
        return Math.max(0, config.getInt("style.title.fade-in-ms", 500));
    }

    public int getTitleStay() {
        return Math.max(0, config.getInt("style.title.stay-ms", 2000));
    }

    public int getTitleFadeOut() {
        return Math.max(0, config.getInt("style.title.fade-out-ms", 500));
    }

    // Game-specific configuration methods
    public int getUnscrambleMinLength() {
        return Math.max(3, config.getInt("games.unscramble.min-length", 4));
    }

    public int getUnscrambleMaxLength() {
        return Math.max(getUnscrambleMinLength(), config.getInt("games.unscramble.max-length", 8));
    }

    public String getUnscrambleSwaps() {
        return config.getString("games.unscramble.scramble-swaps", "3-6");
    }

    public int getMathDifficulty() {
        return Math.max(1, Math.min(5, config.getInt("games.math.difficulty", 2)));
    }

    public List<String> getMathOperators() {
        final List<String> operators = config.getStringList("games.math.operators");
        return operators.isEmpty() ? List.of("+", "-", "*") : operators;
    }

    public boolean shouldUseDivisionIntegers() {
        return config.getBoolean("games.math.division-integers-only", true);
    }

    // Debug settings
    public boolean isDebugEnabled() {
        return config.getBoolean("debug.verbose", false);
    }

    public boolean shouldLogAnswers() {
        return config.getBoolean("debug.log-answers", false) && isDebugEnabled();
    }

    public boolean shouldLogPerformance() {
        return config.getBoolean("debug.log-performance", false) && isDebugEnabled();
    }

    /**
     * Get raw configuration for advanced access
     * @return The FileConfiguration instance
     */
    public FileConfiguration getRawConfig() {
        return config;
    }

    /**
     * Immutable configuration cache for performance
     */
    private record ConfigCache(
            String prefix,
            List<String> disabledWorlds,
            boolean requiresPermissionToPlay,
            int minPlayersOnline,
            int gameTimeout,
            int answerWarmup,
            boolean defaultCaseSensitive,
            boolean schedulerEnabled,
            int schedulerInterval,
            int rateLimit,
            boolean stripColors,
            boolean normalizeHomoglyphs,
            boolean playSounds,
            String startSound,
            boolean showBossbar,
            boolean showTitles,
            boolean economyEnabled,
            String economyProvider,
            String crystalCommand
    ) {
        public ConfigCache {
            // Make defensive copies of mutable collections
            disabledWorlds = List.copyOf(disabledWorlds);
        }
    }
}