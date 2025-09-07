package com.chunksmith.nebrixChatGames.core;

import com.chunksmith.nebrixChatGames.NebrixChatGames;
import com.chunksmith.nebrixChatGames.api.ChatGame;
import org.bukkit.Bukkit;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class Scheduler {
    private final NebrixChatGames plugin;
    private final GameManager manager;
    private int taskId = -1;

    public Scheduler(NebrixChatGames plugin, GameManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    public void maybeStart() {
        if (!"rotate".equalsIgnoreCase(plugin.getConfig().getString("settings.scheduler.mode","rotate"))) return;
        int delay = plugin.getConfig().getInt("settings.scheduler.interval-seconds", 1500);
        if (delay <= 0) return;
        if (taskId != -1) Bukkit.getScheduler().cancelTask(taskId);
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::tick, delay*20L, delay*20L);
    }

    public void stop() {
        if (taskId != -1) Bukkit.getScheduler().cancelTask(taskId);
        taskId = -1;
    }

    private void tick() {
        if (manager == null) return;
        if (EnvChecks.onlineCount() < plugin.getConfig().getInt("settings.min-players-online",1)) return;

        // choose weighted random enabled game
        Map<String,Integer> weights = new HashMap<>();
        var sec = plugin.getConfig().getConfigurationSection("settings.scheduler.rotate-weight");
        if (sec != null) {
            for (String k : sec.getKeys(false)) {
                var g = manager.allGames().stream().anyMatch(x -> x.getId().equalsIgnoreCase(k));
                if (g) weights.put(k, plugin.getConfig().getInt("settings.scheduler.rotate-weight."+k,1));
            }
        }
        String pick = pickWeighted(weights);
        if (pick == null) return;
        manager.start(pick);
    }

    private String pickWeighted(Map<String,Integer> map) {
        if (map.isEmpty()) return null;
        int sum = 0;
        for (int w : map.values()) sum += Math.max(0,w);
        if (sum <= 0) return null;
        int r = ThreadLocalRandom.current().nextInt(sum);
        int acc = 0;
        for (var e : map.entrySet()) {
            acc += Math.max(0,e.getValue());
            if (r < acc) return e.getKey();
        }
        return null;
    }
}