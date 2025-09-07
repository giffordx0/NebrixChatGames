package com.chunksmith.nebrixChatGames.ui;

import com.chunksmith.nebrixChatGames.NebrixChatGames;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.Collection;

public final class Ui {
    private static final MiniMessage MM = MiniMessage.miniMessage();

    public static Component mm(String s) { return MM.deserialize(s); }

    public static void broadcast(NebrixChatGames plugin, String miniMessage) {
        Component c = mm(miniMessage);
        for (Player p : Bukkit.getOnlinePlayers()) p.sendMessage(c);
    }

    public static void titleAll(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        Title.Times times = Title.Times.of(Duration.ofMillis(fadeIn), Duration.ofMillis(stay), Duration.ofMillis(fadeOut));
        Title t = Title.title(mm(title), mm(subtitle == null ? "" : subtitle), times);
        for (Player p : Bukkit.getOnlinePlayers()) p.showTitle(t);
    }

    public static void actionBarAll(String miniMessage) {
        Component c = mm(miniMessage);
        for (Player p : Bukkit.getOnlinePlayers()) p.sendActionBar(c);
    }

    public static void bossbarShow(Collection<Player> players, BossBar bar) {
        for (Player p : players) p.showBossBar(bar);
    }
    public static void bossbarHide(Collection<Player> players, BossBar bar) {
        for (Player p : players) p.hideBossBar(bar);
    }
}
