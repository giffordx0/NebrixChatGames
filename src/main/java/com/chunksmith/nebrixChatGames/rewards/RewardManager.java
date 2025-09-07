package com.chunksmith.nebrixChatGames.rewards;

import com.chunksmith.nebrixChatGames.NebrixChatGames;
import com.chunksmith.nebrixChatGames.api.ChatGame;
import com.chunksmith.nebrixChatGames.api.GameRound;
import com.chunksmith.nebrixChatGames.api.events.RewardGrantEvent;
import com.chunksmith.nebrixChatGames.config.ConfigManager;
import com.chunksmith.nebrixChatGames.integration.EconomyIntegration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

/**
 * Manages reward distribution to game winners
 * Supports coins, crystals, items, commands, and bonus rewards
 */
public class RewardManager {

    private final NebrixChatGames plugin;
    private final ConfigManager config;
    private final EconomyIntegration economy;

    public RewardManager(NebrixChatGames plugin, ConfigManager config, EconomyIntegration economy) {
        this.plugin = plugin;
        this.config = config;
        this.economy = economy;
    }

    /**
     * Grant rewards to a game winner
     * @param player The winning player
     * @param game The game that was won
     * @param round The completed round
     */
    public void grantRewards(Player player, ChatGame game, GameRound round) {
        try {
            final RewardSet rewards = calculateRewards(game);

            // Grant economy rewards
            long actualCoins = grantCoins(player, rewards.coins());
            long actualCrystals = grantCrystals(player, rewards.crystals());

            // Execute commands
            executeCommands(player, rewards.commands());

            // Grant items
            grantItems(player, rewards.items());

            // Check for bonus rewards
            checkBonusRewards(player);

            // Fire reward event
            Bukkit.getPluginManager().callEvent(
                    new RewardGrantEvent(player, game, round, actualCoins, actualCrystals)
            );

            plugin.getLogger().info(String.format(
                    "Granted rewards to %s for winning %s: %d coins, %d crystals",
                    player.getName(), game.getId(), actualCoins, actualCrystals
            ));

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE,
                    "Failed to grant rewards to " + player.getName(), e);
        }
    }

    /**
     * Calculate rewards for a specific game
     */
    private RewardSet calculateRewards(ChatGame game) {
        final String gameId = game.getId();

        return new RewardSet(
                config.getGameCoins(gameId),
                config.getGameCrystals(gameId),
                config.getBaseCommands(),
                config.getBaseItems()
        );
    }

    /**
     * Grant coins to a player
     * @return Actual amount granted
     */
    private long grantCoins(Player player, long amount) {
        if (amount <= 0) {
            return 0;
        }

        if (economy.isAvailable()) {
            return economy.depositCoins(player, amount) ? amount : 0;
        } else {
            // Fallback: just show message
            plugin.getMessageManager().sendPrefixedMessage(player,
                    "<green>+</green><yellow>" + amount + "</yellow><green> coins!</green>");
            return amount;
        }
    }

    /**
     * Grant crystals to a player
     * @return Actual amount granted
     */
    private long grantCrystals(Player player, long amount) {
        if (amount <= 0) {
            return 0;
        }

        final String crystalCommand = config.getCrystalCommand();
        if (!crystalCommand.isEmpty()) {
            final String command = crystalCommand
                    .replace("%player%", player.getName())
                    .replace("%amount%", String.valueOf(amount));

            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            return amount;
        } else {
            plugin.getMessageManager().sendPrefixedMessage(player,
                    "<green>+</green><yellow>" + amount + "</yellow><green> crystals!</green>");
            return amount;
        }
    }

    /**
     * Execute reward commands for a player
     */
    private void executeCommands(Player player, List<String> commands) {
        for (String command : commands) {
            try {
                final String processedCommand = command.replace("%player%", player.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCommand);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING,
                        "Failed to execute reward command: " + command, e);
            }
        }
    }

    /**
     * Grant items to a player
     */
    private void grantItems(Player player, List<String> itemSpecs) {
        for (String spec : itemSpecs) {
            try {
                final ItemStack item = parseItemSpec(spec);
                if (item != null) {
                    player.getInventory().addItem(item);
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING,
                        "Failed to parse item spec: " + spec, e);
            }
        }
    }

    /**
     * Parse item specification string
     * Format: MATERIAL:AMOUNT or MATERIAL
     */
    private ItemStack parseItemSpec(String spec) {
        final String[] parts = spec.split(":", 2);

        try {
            final Material material = Material.valueOf(parts[0].toUpperCase());
            final int amount = parts.length > 1 ?
                    Math.max(1, Math.min(64, Integer.parseInt(parts[1]))) : 1;

            return new ItemStack(material, amount);

        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid item specification: " + spec);
            return null;
        }
    }

    /**
     * Check and grant bonus rewards based on chance
     */
    private void checkBonusRewards(Player player) {
        final int bonusChance = config.getBonusChance();

        if (bonusChance > 0 && bonusChance <= 100) {
            if (ThreadLocalRandom.current().nextInt(100) < bonusChance) {
                final List<String> bonusCommands = config.getBonusCommands();
                executeCommands(player, bonusCommands);

                plugin.getMessageManager().sendPrefixedMessage(player,
                        "<gold><bold>BONUS REWARD!</bold></gold>");

                plugin.getLogger().info(player.getName() + " received bonus rewards!");
            }
        }
    }

    /**
     * Record to hold reward configuration
     */
    private record RewardSet(
            long coins,
            long crystals,
            List<String> commands,
            List<String> items
    ) {}
}