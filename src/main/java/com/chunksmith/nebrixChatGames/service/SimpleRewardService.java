package com.chunksmith.nebrixChatGames.service;

import com.chunksmith.nebrixChatGames.NebrixChatGames;
import com.chunksmith.nebrixChatGames.api.Round;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

public final class SimpleRewardService implements RewardService {
    private final NebrixChatGames plugin;

    public SimpleRewardService(NebrixChatGames plugin) {
        this.plugin = plugin;
    }

    @Override
    public void grantWinner(Player player, Round round) {
        try {
            final RewardConfig rewards = loadRewardConfig(round);

            // Grant economy rewards
            grantEconomyRewards(player, rewards);

            // Execute reward commands
            executeRewardCommands(player, rewards.commands());

            // Give items
            grantItems(player, rewards.items());

            // Handle bonus chance rewards
            handleBonusRewards(player);

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE,
                    String.format("Failed to grant rewards to player %s for game %s",
                            player.getName(), round.gameId), e);
        }
    }

    private RewardConfig loadRewardConfig(Round round) {
        // Load base rewards
        long coins = plugin.getConfig().getLong("rewards.base.coins", 0);
        long crystals = plugin.getConfig().getLong("rewards.base.crystals", 0);
        List<String> commands = plugin.getConfig().getStringList("rewards.base.commands");
        List<String> items = plugin.getConfig().getStringList("rewards.base.items");

        // Apply per-game overrides
        final ConfigurationSection gameSection = plugin.getConfig()
                .getConfigurationSection("rewards.per-game." + round.gameId);

        if (gameSection != null) {
            coins = gameSection.getLong("coins", coins);
            crystals = gameSection.getLong("crystals", crystals);

            // Allow game-specific commands and items to override or extend base ones
            if (gameSection.contains("commands")) {
                commands = gameSection.getStringList("commands");
            }
            if (gameSection.contains("items")) {
                items = gameSection.getStringList("items");
            }
        }

        return new RewardConfig(coins, crystals, commands, items);
    }

    private void grantEconomyRewards(Player player, RewardConfig rewards) {
        // Grant coins through economy
        if (rewards.coins() > 0) {
            grantCoins(player, rewards.coins());
        }

        // Grant crystals through command
        if (rewards.crystals() > 0) {
            final String crystalCommand = plugin.getConfig()
                    .getString("integration.crystals.award-command", "");

            if (!crystalCommand.isEmpty()) {
                final String formattedCommand = crystalCommand
                        .replace("%player%", player.getName())
                        .replace("%amount%", String.valueOf(rewards.crystals()));

                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), formattedCommand);
            } else {
                plugin.getLogger().warning(
                        "Crystal reward configured but no award command specified in config");
            }
        }
    }

    private void grantCoins(Player player, long amount) {
        try {
            // Try to use Vault economy if available
            final var economyRegistration = Bukkit.getServicesManager()
                    .getRegistration(net.milkbowl.vault.economy.Economy.class);

            if (economyRegistration != null) {
                final var economy = economyRegistration.getProvider();
                economy.depositPlayer(player, amount);
                plugin.getLogger().info(
                        String.format("Granted %d coins to %s via Vault", amount, player.getName()));
            } else {
                // Fallback message if Vault is not available
                player.sendMessage(String.format("§6+%d coins (economy not linked)", amount));
                plugin.getLogger().warning("Vault economy not available for coin rewards");
            }
        } catch (Exception e) {
            // Final fallback
            player.sendMessage(String.format("§6+%d coins", amount));
            plugin.getLogger().log(Level.WARNING, "Failed to grant coins via Vault", e);
        }
    }

    private void executeRewardCommands(Player player, List<String> commands) {
        commands.stream()
                .map(command -> command.replace("%player%", player.getName()))
                .forEach(command -> {
                    try {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.WARNING,
                                String.format("Failed to execute reward command: %s", command), e);
                    }
                });
    }

    private void grantItems(Player player, List<String> itemSpecs) {
        for (String spec : itemSpecs) {
            try {
                final ItemStack item = parseItemSpec(spec);
                if (item != null) {
                    player.getInventory().addItem(item);
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING,
                        String.format("Failed to parse item spec: %s", spec), e);
            }
        }
    }

    private ItemStack parseItemSpec(String spec) {
        final String[] parts = spec.split(":", 3);

        try {
            final Material material = Material.valueOf(parts[0].toUpperCase());
            final int amount = parts.length > 1 ?
                    Integer.parseInt(parts[1]) : 1;

            // Future: Could add support for NBT data in parts[2]
            return new ItemStack(material, Math.max(1, Math.min(64, amount)));

        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning(
                    String.format("Invalid item specification: %s", spec));
            return null;
        }
    }

    private void handleBonusRewards(Player player) {
        final int bonusChance = plugin.getConfig().getInt("rewards.bonus-chance.percent", 0);

        if (bonusChance > 0 && bonusChance <= 100) {
            final boolean wonBonus = ThreadLocalRandom.current().nextInt(100) < bonusChance;

            if (wonBonus) {
                final List<String> bonusCommands = plugin.getConfig()
                        .getStringList("rewards.bonus-chance.commands");

                executeRewardCommands(player, bonusCommands);

                plugin.getLogger().info(
                        String.format("Player %s won bonus rewards (%d%% chance)",
                                player.getName(), bonusChance));
            }
        }
    }

    /**
     * Immutable record to hold reward configuration
     */
    private record RewardConfig(
            long coins,
            long crystals,
            List<String> commands,
            List<String> items
    ) {}
}