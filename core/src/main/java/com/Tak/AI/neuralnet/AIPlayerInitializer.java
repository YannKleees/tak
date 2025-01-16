package com.Tak.AI.neuralnet;

import com.Tak.AI.players.MinimaxAgent;
import com.Tak.Logic.models.Player;
import com.Tak.Logic.models.Player.Color;

import java.util.Arrays;
import java.util.List;

public class AIPlayerInitializer {
    public static List<Player> createAIPlayers(NeuralNetworkTrainer nnTrainer) {
        Player player1 = new MinimaxAgent(
            Color.BLUE, 21, 21, 1, 3, false,
            new NeuralNetworkEvaluator(nnTrainer.getNetwork())
        );
        Player player2 = new MinimaxAgent(
            Color.GREEN, 21, 21, 1, 3, false,
            new NeuralNetworkEvaluator(nnTrainer.getNetwork())
        );
        
        return Arrays.asList(player1, player2);
    }
}
