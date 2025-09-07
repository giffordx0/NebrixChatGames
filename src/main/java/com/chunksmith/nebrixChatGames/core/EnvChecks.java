package com.chunksmith.nebrixChatGames.core;

import com.chunksmith.nebrixChatGames.NebrixChatGames;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class EnvChecks {
    public static boolean canStart(NebrixChatGames plugin) {
        int min = plugin.getConfig().getInt("settings.min-players-online",1);
        if (onlineCount() < min) return false;
        return true;
    }
    public static int onlineCount() { return Bukkit.getOnlinePlayers().size(); }

    public static boolean playerEligible(NebrixChatGames plugin, Player p) {
        if (plugin.getConfig().getBoolean("settings.use-permission-toplay", false))
            if (!p.hasPermission("nebrix.cg.play")) return false;
        if (plugin.getConfig().getStringList("settings.disabled-worlds").contains(p.getWorld().getName()))
            return false;
        return true;
    }
}