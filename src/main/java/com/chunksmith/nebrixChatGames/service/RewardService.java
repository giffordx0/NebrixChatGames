package com.chunksmith.nebrixChatGames.service;

import com.chunksmith.nebrixChatGames.api.Round;
import org.bukkit.entity.Player;

public interface RewardService {
    void grantWinner(Player player, Round round);
}