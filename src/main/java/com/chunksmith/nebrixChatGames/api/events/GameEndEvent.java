package com.chunksmith.nebrixChatGames.api.events;

import com.chunksmith.nebrixChatGames.api.ChatGame;
import com.chunksmith.nebrixChatGames.api.GameRound;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

/**
 * Event fired when a chat game ends
 */
public class GameEndEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final ChatGame game;
    private final GameRound round;
    private final Player winner; // null if timeout

    public GameEndEvent(ChatGame game, GameRound round, @Nullable Player winner) {
        this.game = game;
        this.round = round;
        this.winner = winner;
    }

    public ChatGame getGame() { return game; }
    public GameRound getRound() { return round; }
    public @Nullable Player getWinner() { return winner; }

    public boolean hasWinner() { return winner != null; }
    public boolean wasTimeout() { return winner == null; }

    @Override
    public @NotNull HandlerList getHandlers() { return handlers; }

    public static HandlerList getHandlerList() { return handlers; }
}
