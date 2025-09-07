package com.chunksmith.nebrixChatGames.util;

import com.chunksmith.nebrixChatGames.NebrixChatGames;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.text.Normalizer;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Text processing utilities with Adventure API support
 * Follows Minecraft 1.21.8 best practices
 */
public final class TextUtils {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER =
            LegacyComponentSerializer.legacyAmpersand();

    // Pattern for invisible characters and various Unicode tricks
    private static final Pattern INVISIBLE_CHARS = Pattern.compile(
            "[\u200B\u200C\u200D\u2060\uFEFF\u00AD\u034F\u061C\u115F\u1160\u17B4\u17B5\u180E]+"
    );

    // Pattern to strip MiniMessage tags
    private static final Pattern MINIMESSAGE_TAGS = Pattern.compile("<[^<>]*>");

    private TextUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Parse text as MiniMessage with fallback to legacy format
     * @param text The text to parse
     * @return Parsed Component
     */
    public static Component parseMessage(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        try {
            return MINI_MESSAGE.deserialize(text);
        } catch (Exception e) {
            // Fallback to legacy parsing
            try {
                return LEGACY_SERIALIZER.deserialize(text);
            } catch (Exception fallback) {
                // Ultimate fallback to plain text
                return Component.text(ChatColor.stripColor(text));
            }
        }
    }

    /**
     * Get formatted prefix from plugin configuration
     * @param plugin The plugin instance
     * @return Formatted prefix component
     */
    public static Component getPrefix(NebrixChatGames plugin) {
        final String configPrefix = plugin.getConfigManager().getPrefix();
        return parseMessage(configPrefix);
    }

    /**
     * Normalize player input for answer checking
     * Applies various anti-cheat measures and standardization
     *
     * @param input The raw player input
     * @param plugin The plugin instance for configuration
     * @param caseSensitive Whether to preserve case
     * @return Normalized string ready for comparison
     */
    public static String normalizeAnswer(String input, NebrixChatGames plugin, boolean caseSensitive) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        String normalized = input.trim();

        // Apply case normalization first if needed
        if (!caseSensitive) {
            normalized = normalized.toLowerCase();
        }

        // Strip color codes if configured
        if (plugin.getConfigManager().shouldStripColors()) {
            normalized = stripAllColorCodes(normalized);
        }

        // Remove invisible characters
        normalized = INVISIBLE_CHARS.matcher(normalized).replaceAll("");

        // Normalize Unicode homoglyphs if configured
        if (plugin.getConfigManager().shouldNormalizeHomoglyphs()) {
            normalized = normalizeHomoglyphs(normalized);
        }

        // Remove extra whitespace
        normalized = normalized.replaceAll("\\s+", " ").trim();

        return normalized;
    }

    /**
     * Strip both legacy and MiniMessage color codes
     * @param text Input text
     * @return Text with color codes removed
     */
    private static String stripAllColorCodes(String text) {
        // Strip legacy color codes first
        String stripped = ChatColor.stripColor(text);

        // Strip MiniMessage tags
        stripped = MINIMESSAGE_TAGS.matcher(stripped).replaceAll("");

        return stripped;
    }

    /**
     * Normalize Unicode homoglyphs and similar-looking characters
     * This helps prevent players from using lookalike characters to bypass answers
     *
     * @param text Input text
     * @return Normalized text
     */
    private static String normalizeHomoglyphs(String text) {
        // Normalize to NFD (canonical decomposition) and remove combining marks
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

        // Replace common homoglyphs with ASCII equivalents
        normalized = normalized
                // Fullwidth characters to ASCII
                .replace("０１２３４５６７８９", "0123456789")
                .replace("ＡＢＣＤＥＦＧＨＩＪＫＬＭＮＯＰＱＲＳＴＵＶＷＸＹＺ", "ABCDEFGHIJKLMNOPQRSTUVWXYZ")
                .replace("ａｂｃｄｅｆｇｈｉｊｋｌｍｎｏｐｑｒｓｔｕｖｗｘｙｚ", "abcdefghijklmnopqrstuvwxyz")

                // Cyrillic lookalikes
                .replace("А", "A").replace("В", "B").replace("С", "C").replace("Е", "E")
                .replace("Н", "H").replace("І", "I").replace("Ј", "J").replace("К", "K")
                .replace("М", "M").replace("О", "O").replace("Р", "P").replace("Т", "T")
                .replace("Х", "X").replace("У", "Y")

                .replace("а", "a").replace("е", "e").replace("о", "o").replace("р", "p")
                .replace("с", "c").replace("х", "x").replace("у", "y")

                // Greek lookalikes
                .replace("Α", "A").replace("Β", "B").replace("Ε", "E").replace("Ζ", "Z")
                .replace("Η", "H").replace("Ι", "I").replace("Κ", "K").replace("Μ", "M")
                .replace("Ν", "N").replace("Ο", "O").replace("Ρ", "P").replace("Τ", "T")
                .replace("Υ", "Y").replace("Χ", "X")

                .replace("α", "a").replace("ο", "o").replace("ρ", "p").replace("υ", "y")

                // Mathematical symbols
                .replace("∅", "0").replace("⊘", "0").replace("∘", "o").replace("⦻", "0")

                // Roman numerals (basic ones)
                .replace("Ⅰ", "I").replace("Ⅱ", "II").replace("Ⅲ", "III").replace("Ⅳ", "IV")
                .replace("Ⅴ", "V").replace("Ⅵ", "VI").replace("Ⅶ", "VII").replace("Ⅷ", "VIII")
                .replace("Ⅸ", "IX").replace("Ⅹ", "X")

                // Circled numbers
                .replace("①②③④⑤⑥⑦⑧⑨⑩", "12345678910")

                // Superscript numbers
                .replace("⁰¹²³⁴⁵⁶⁷⁸⁹", "0123456789")

                // Subscript numbers
                .replace("₀₁₂₃₄₅₆₇₈₉", "0123456789");

        return normalized;
    }

    /**
     * Check if two strings are similar using Levenshtein distance
     * Useful for typo tolerance in answers
     *
     * @param s1 First string
     * @param s2 Second string
     * @param maxDistance Maximum allowed edit distance
     * @return true if strings are within the distance threshold
     */
    public static boolean isSimilar(String s1, String s2, int maxDistance) {
        if (maxDistance <= 0) {
            return s1.equals(s2);
        }

        return levenshteinDistance(s1, s2) <= maxDistance;
    }

    /**
     * Calculate Levenshtein distance between two strings
     * @param s1 First string
     * @param s2 Second string
     * @return Edit distance
     */
    private static int levenshteinDistance(String s1, String s2) {
        if (s1.equals(s2)) {
            return 0;
        }

        if (s1.isEmpty()) {
            return s2.length();
        }

        if (s2.isEmpty()) {
            return s1.length();
        }

        int[] previousRow = new int[s2.length() + 1];
        int[] currentRow = new int[s2.length() + 1];

        for (int i = 0; i <= s2.length(); i++) {
            previousRow[i] = i;
        }

        for (int i = 1; i <= s1.length(); i++) {
            currentRow[0] = i;

            for (int j = 1; j <= s2.length(); j++) {
                int insertCost = currentRow[j - 1] + 1;
                int deleteCost = previousRow[j] + 1;
                int replaceCost = previousRow[j - 1] + (s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1);

                currentRow[j] = Math.min(Math.min(insertCost, deleteCost), replaceCost);
            }

            int[] temp = previousRow;
            previousRow = currentRow;
            currentRow = temp;
        }

        return previousRow[s2.length()];
    }
}