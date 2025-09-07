package com.chunksmith.nebrixChatGames.api.events;

import com.chunksmith.nebrixChatGames.api.ChatGame;
import com.chunksmith.nebrixChatGames.api.GameRound;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Event fired when rewards are granted to a player
 */
public class RewardGrantEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final Player player;
    private final ChatGame game;
    private final GameRound round;
    private final long coins;
    private final long crystals;

    public RewardGrantEvent(Player player, ChatGame game, GameRound round, long coins, long crystals) {
        this.player = player;
        this.game = game;
        this.round = round;
        this.coins = coins;
        this.crystals = crystals;
    }

    public Player getPlayer() { return player; }
    public ChatGame getGame() { return game; }
    public GameRound getRound() { return round; }
    public long getCoins() { return coins; }
    public long getCrystals() { return crystals; }

    @Override
    public @NotNull HandlerList getHandlers() { return handlers; }

    public static HandlerList getHandlerList() { return handlers; }
}