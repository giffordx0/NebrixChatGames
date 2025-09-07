package com.chunksmith.nebrixChatGames.command;

import com.chunksmith.nebrixChatGames.NebrixChatGames;
import com.chunksmith.nebrixChatGames.api.ChatGame;
import com.chunksmith.nebrixChatGames.core.GameManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Main command handler for chat games
 */
public class CgCommand implements TabCompleter {

    private final NebrixChatGames plugin;
    private final GameManager gameManager;

    public CgCommand(NebrixChatGames plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        final List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            final String input = args[0].toLowerCase();

            // Add subcommands
            if ("start".startsWith(input)) completions.add("start");
            if ("stop".startsWith(input)) completions.add("stop");
            if ("list".startsWith(input)) completions.add("list");
            if ("reload".startsWith(input)) completions.add("reload");
            if ("status".startsWith(input)) completions.add("status");

        } else if (args.length == 2 && "start".equalsIgnoreCase(args[0])) {
            final String input = args[1].toLowerCase();

            // Add game IDs for start command
            completions.addAll(gameManager.allGames().stream()
                    .map(ChatGame::getId)
                    .filter(gameId -> gameId.toLowerCase().startsWith(input))
                    .collect(Collectors.toList()));
        }

        return completions;
    }
}