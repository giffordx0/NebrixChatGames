package com.chunksmith.nebrixChatGames.util;

import com.chunksmith.nebrixChatGames.NebrixChatGames;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;

import java.text.Normalizer;
import java.util.regex.Pattern;

public final class Text {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER =
            LegacyComponentSerializer.legacyAmpersand();

    // Pattern for invisible characters commonly used to bypass filters
    private static final Pattern INVISIBLE_CHARS = Pattern.compile("[\u200B\u200C\u200D\u2060\uFEFF]");

    private Text() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Gets the formatted prefix from config, supporting both legacy and MiniMessage formats
     */
    public static String prefix(NebrixChatGames plugin) {
        final String configPrefix = plugin.getConfig().getString("settings.prefix", "&7[ChatGames]");

        // Try to parse as MiniMessage first, fallback to legacy
        try {
            Component component = MINI_MESSAGE.deserialize(configPrefix);
            return LegacyComponentSerializer.legacySection().serialize(component) + ChatColor.RESET + " ";
        } catch (Exception e) {
            // Fallback to legacy format
            return ChatColor.translateAlternateColorCodes('&', configPrefix) + ChatColor.RESET + " ";
        }
    }

    /**
     * Normalizes user input for answer checking
     */
    public static String normalize(String input, NebrixChatGames plugin, boolean caseSensitive) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        String normalized = input;

        // Case sensitivity
        if (!caseSensitive) {
            normalized = normalized.toLowerCase();
        }

        // Strip color codes if configured
        if (plugin.getConfig().getBoolean("settings.anti-cheat.strip-colors", true)) {
            normalized = ChatColor.stripColor(normalized);
            // Also strip MiniMessage tags
            normalized = stripMiniMessageTags(normalized);
        }

        // Remove invisible characters
        normalized = INVISIBLE_CHARS.matcher(normalized).replaceAll("");

        // Normalize unicode characters if configured
        if (plugin.getConfig().getBoolean("settings.anti-cheat.normalize-homoglyphs", true)) {
            normalized = normalizeHomoglyphs(normalized);
        }

        return normalized.trim();
    }

    /**
     * Converts MiniMessage string to Component
     */
    public static Component miniMessage(String text) {
        try {
            return MINI_MESSAGE.deserialize(text);
        } catch (Exception e) {
            // Fallback to legacy parsing if MiniMessage fails
            return LEGACY_SERIALIZER.deserialize(text);
        }
    }

    /**
     * Strips MiniMessage tags from text
     */
    private static String stripMiniMessageTags(String text) {
        // Basic regex to remove common MiniMessage tags
        return text.replaceAll("<[^>]*>", "");
    }

    /**
     * Normalizes homoglyphs and similar-looking characters
     */
    private static String normalizeHomoglyphs(String text) {
        // Normalize unicode to decomposed form, then remove combining characters
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        normalized = normalized.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");

        // Replace common homoglyphs
        normalized = normalized
                .replace("０１２３４５６７８９", "0123456789") // Fullwidth digits
                .replace("ＡＢＣＤＥＦＧＨＩＪＫＬＭＮＯＰＱＲＳＴＵＶＷＸＹＺ",
                        "ABCDEFGHIJKLMNOPQRSTUVWXYZ") // Fullwidth uppercase
                .replace("ａｂｃｄｅｆｇｈｉｊｋｌｍｎｏｐｑｒｓｔｕｖｗｘｙｚ",
                        "abcdefghijklmnopqrstuvwxyz") // Fullwidth lowercase
                .replace("ⅰⅱⅲⅳⅴⅵⅶⅷⅸⅹ", "iiiiiivvviviiviiiixxxiiixx") // Roman numerals
                .replace("①②③④⑤⑥⑦⑧⑨⑩", "1234567890"); // Circled numbers

        return normalized;
    }
}