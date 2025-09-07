package com.chunksmith.nebrixChatGames.api.events;

import com.chunksmith.nebrixChatGames.api.ChatGame;
import com.chunksmith.nebrixChatGames.api.GameRound;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Event fired when a player submits an answer
 */
public class PlayerAnswerEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled = false;

    private final Player player;
    private final ChatGame game;
    private final GameRound round;
    private final String answer;

    public PlayerAnswerEvent(Player player, ChatGame game, GameRound round, String answer) {
        this.player = player;
        this.game = game;
        this.round = round;
        this.answer = answer;
    }

    public Player getPlayer() { return player; }
    public ChatGame getGame() { return game; }
    public GameRound getRound() { return round; }
    public String getAnswer() { return answer; }

    @Override
    public boolean isCancelled() { return cancelled; }

    @Override
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }

    @Override
    public @NotNull HandlerList getHandlers() { return handlers; }

    public static HandlerList getHandlerList() { return handlers; }
}