package com.chunksmith.nebrixChatGames.core;

import com.chunksmith.nebrixChatGames.NebrixChatGames;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WordList {
    private static List<String> WORDS = new ArrayList<>();

    public static void load(NebrixChatGames plugin) {
        try {
            File f = new File(plugin.getDataFolder(), "words.yml");
            FileConfiguration y = YamlConfiguration.loadConfiguration(f);
            WORDS = y.getStringList("words");
        } catch (Exception ignored) {}
        if (WORDS == null) WORDS = new ArrayList<>();
    }
    public static List<String> words() { return Collections.unmodifiableList(WORDS); }
}