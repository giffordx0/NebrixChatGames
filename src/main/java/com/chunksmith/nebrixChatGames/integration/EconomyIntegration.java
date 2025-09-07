package com.chunksmith.nebrixChatGames.integration;

import com.chunksmith.nebrixChatGames.NebrixChatGames;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.logging.Level;

/**
 * Integration with Vault economy systems
 * Provides fallback behavior when Vault is not available
 */
public class EconomyIntegration {

    private final NebrixChatGames plugin;
    private Economy economy;
    private boolean available = false;

    public EconomyIntegration(NebrixChatGames plugin) {
        this.plugin = plugin;
    }

    /**
     * Initialize economy integration
     */
    public void initialize() {
        if (!plugin.getConfigManager().isEconomyEnabled()) {
            plugin.getLogger().info("Economy integration disabled in configuration");
            return;
        }

        if (!Bukkit.getPluginManager().isPluginEnabled("Vault")) {
            plugin.getLogger().info("Vault not found - economy features disabled");
            return;
        }

        try {
            final RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager()
                    .getRegistration(Economy.class);

            if (rsp != null) {
                economy = rsp.getProvider();
                available = true;
                plugin.getLogger().info("Economy integration enabled via " + economy.getName());
            } else {
                plugin.getLogger().warning("No economy provider found");
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to initialize economy integration", e);
        }
    }

    /**
     * Check if economy is available
     * @return true if economy operations are possible
     */
    public boolean isAvailable() {
        return available && economy != null;
    }

    /**
     * Deposit coins to a player's account
     * @param player The player
     * @param amount Amount to deposit
     * @return true if successful
     */
    public boolean depositCoins(Player player, long amount) {
        if (!isAvailable() || amount <= 0) {
            return false;
        }

        try {
            final var response = economy.depositPlayer(player, amount);
            return response.transactionSuccess();
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING,
                    "Failed to deposit coins for " + player.getName(), e);
            return false;
        }
    }

    /**
     * Get player's current balance
     * @param player The player
     * @return Balance, or 0 if unavailable
     */
    public double getBalance(Player player) {
        if (!isAvailable()) {
            return 0;
        }

        try {
            return economy.getBalance(player);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING,
                    "Failed to get balance for " + player.getName(), e);
            return 0;
        }
    }

    /**
     * Clean up economy integration
     */
    public void cleanup() {
        this.economy = null;
        this.available = false;
    }
}