package com.chunksmith.nebrixChatGames.games;

import com.chunksmith.nebrixChatGames.NebrixChatGames;
import com.chunksmith.nebrixChatGames.api.AbstractChatGame;
import com.chunksmith.nebrixChatGames.api.GameRound;
import org.bukkit.entity.Player;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Math game implementation
 */
public class MathGame extends AbstractChatGame {

    public MathGame(NebrixChatGames plugin) {
        super(plugin);
    }

    @Override
    public String getId() {
        return "math";
    }

    @Override
    public String getDisplayName() {
        return "Math Challenge";
    }

    @Override
    public GameRound createRound() {
        final ThreadLocalRandom random = ThreadLocalRandom.current();

        // Generate two numbers based on difficulty
        final int difficulty = config.getMathDifficulty();
        final int maxNumber = difficulty * 10;

        final int num1 = random.nextInt(1, maxNumber + 1);
        final int num2 = random.nextInt(1, maxNumber + 1);
        final String operator = getRandomOperator(random);

        final int result = calculateResult(num1, num2, operator);
        final String prompt = num1 + " " + operator + " " + num2 + " = ?";

          return new GameRound(
                  getId(),
                  getDisplayName(),
                  prompt,
                  String.valueOf(result),
                  config.getGameTimeout() * 1000L,
                  config.getAnswerWarmup(),
                  isCaseSensitive()
          );
      }

    @Override
    public boolean isCorrectAnswer(String answer, Player player, GameRound round) {
        try {
            final int playerAnswer = Integer.parseInt(answer.trim());
            final int correctAnswer = Integer.parseInt(round.getCorrectAnswer());
            return playerAnswer == correctAnswer;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String getRandomOperator(ThreadLocalRandom random) {
        final String[] operators = {"+", "-", "*"};
        return operators[random.nextInt(operators.length)];
    }

    private int calculateResult(int num1, int num2, String operator) {
        switch (operator) {
            case "+":
                return num1 + num2;
            case "-":
                return num1 - num2;
            case "*":
                return num1 * num2;
            default:
                throw new IllegalArgumentException("Unknown operator: " + operator);
        }
    }
}