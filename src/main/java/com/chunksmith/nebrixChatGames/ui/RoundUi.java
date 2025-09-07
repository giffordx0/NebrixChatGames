package com.chunksmith.nebrixChatGames.ui;

import com.chunksmith.nebrixChatGames.NebrixChatGames;
import com.chunksmith.nebrixChatGames.api.Round;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import static com.chunksmith.nebrixChatGames.ui.Ui.mm;

public final class RoundUi implements AutoCloseable {
    private final NebrixChatGames plugin;
    private final Round round;
    private final BossBar bossBar;
    private final AtomicReference<BukkitTask> tickTask = new AtomicReference<>();

    public RoundUi(NebrixChatGames plugin, Round round, String barFormat) {
        this.plugin = plugin;
        this.round = round;

        // Create boss bar with initial state (full progress)
        final String label = formatBarLabel(barFormat);
        this.bossBar = BossBar.bossBar(
                mm(label),
                1.0f,
                BossBar.Color.PURPLE,
                BossBar.Overlay.PROGRESS
        );

        showBossBarToAllPlayers();
    }

    public void startTicking() {
        // Cancel any existing task
        stopTicking();

        final BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 0L, 20L);
        tickTask.set(task);
    }

    private void tick() {
        try {
            final long currentTime = System.currentTimeMillis();
            final long endTime = round.startAtMillis + round.timeoutMillis;
            final long remainingMs = Math.max(0L, endTime - currentTime);

            // Update boss bar progress
            final float progress = Math.max(0f, Math.min(1f, remainingMs / (float) round.timeoutMillis));
            bossBar.progress(progress);

            // Update action bar countdown if enabled
            if (plugin.getConfig().getBoolean("settings.round.bossbar", true)) {
                final String actionBarTemplate = plugin.getConfig()
                        .getString("style.actionbar", "<gray>You have <yellow><bold>%seconds%</bold> seconds</yellow> left");

                final int secondsLeft = (int) Math.ceil(remainingMs / 1000.0);
                final String actionBarText = actionBarTemplate.replace("%seconds%", String.valueOf(secondsLeft));

                Ui.actionBarAll(actionBarText);
            }

            // Stop if time is up
            if (remainingMs <= 0) {
                close();
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error during UI tick", e);
            close();
        }
    }

    private void stopTicking() {
        final BukkitTask task = tickTask.getAndSet(null);
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
    }

    @Override
    public void close() {
        try {
            stopTicking();
            hideBossBarFromAllPlayers();
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error during UI cleanup", e);
        }
    }

    private String formatBarLabel(String barFormat) {
        return barFormat
                .replace("%game%", round.gameId)
                .replace("%prompt%", round.prompt)
                .replace("%seconds%", String.valueOf(round.timeoutMillis / 1000));
    }

    private void showBossBarToAllPlayers() {
        final Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
        for (Player player : onlinePlayers) {
            player.showBossBar(bossBar);
        }
    }

    private void hideBossBarFromAllPlayers() {
        final Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
        for (Player player : onlinePlayers) {
            player.hideBossBar(bossBar);
        }
    }
}