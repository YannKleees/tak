package com.Tak.AI.experiments;

import com.Tak.AI.evaluation.EvaluationFunction;
import com.Tak.Logic.models.Board;
import com.Tak.Logic.models.BluePlayer;
import com.Tak.Logic.models.BluePlayer;
import com.Tak.Logic.models.GreenPlayer;
import com.Tak.Logic.models.Player;

import java.util.List;

public class Main {
    private final Player GREENPlayer;
    private final Player BLUEPlayer;

    public Main() {
        this.GREENPlayer = new GreenPlayer(21, 1); // Example piece counts
        this.BLUEPlayer = new BluePlayer(21, 1);
        this.GREENPlayer.setOpponent(this.BLUEPlayer);
        this.BLUEPlayer.setOpponent(this.GREENPlayer);
    }

    public static void main(String[] args) {
        // Initialize the board, players, and evaluation function.
        Board board = new Board(5, 3); // Example 5x5 board.
        Main mainInstance = new Main();
        Player GREENPlayer = mainInstance.GREENPlayer;
        Player BLUEPlayer = mainInstance.BLUEPlayer;

        EvaluationFunction evaluationFunction = new EvaluationFunction();

        // Create and run the depth experiment.
        DepthExperiment experiment = new DepthExperiment(board, evaluationFunction, GREENPlayer, BLUEPlayer, 5, 0);
        List<DepthExperiment.DepthExperimentResult> results = experiment.runExperiment();

        // Print the results or pass them to a graphing library.
        System.out.println("Depth Experiment Results:");
        for (DepthExperiment.DepthExperimentResult result : results) {
            System.out.println(result);
        }

        // Use the results to create graphs, e.g., time taken vs depth.
    }
}
