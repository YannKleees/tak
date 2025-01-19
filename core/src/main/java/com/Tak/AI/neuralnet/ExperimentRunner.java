package com.Tak.AI.neuralnet;

import java.io.File;

import com.Tak.AI.evaluation.HeuristicEvaluator;
import com.Tak.AI.experiments.IterativeNetVsHeuristicTrainer;

/**
 * A central place to store, organize, and run all your test experiments
 * (activation functions, hidden layer sizes, self-play, continuing from existing net, etc.).
 */
public class ExperimentRunner {

    /**
     * Runs a series of sample experiments automatically.
     * Called by ANNManager when user selects "Run Experiments".
     */
    public static void runAllExperiments() {
        System.out.println("\n--- Running Automatic Experiments ---");

        // A) Activation function experiment
        runActivationFunctionExperiment();

        // B) Hidden layer sizes experiment
        runHiddenLayerExperiment();

        // C) Self-play demonstration
        runSelfPlayExperiment();

        // D) Continuing from existing net
        runContinueFromExistingExperiment();

        System.out.println("\nAll experiments done!");
    }

    // -----------------------------------------------------
    // Experiment A: Activation function
    // -----------------------------------------------------
    private static void runActivationFunctionExperiment() {
        System.out.println("Experiment A: Activation functions");

        // We test three common activation funcs
        ActivationFunction[] afs = {
            ActivationFunction.SIGMOID,
            ActivationFunction.TANH,
            ActivationFunction.RELU
        };

        for (ActivationFunction af : afs) {
            System.out.println("\n--- Testing AF = " + af + " ---");

            // Build net with single hidden layer of 64 units
            NeuralNetworkTrainer trainer = NeuralNetworkInitializer.initializeTrainer(
                new int[]{64},
                af
            );

            // A small iterative training
            int boardSize = 5;
            int gamesPerRound = 5;
            int totalRounds = 2;
            boolean printBoard = false;

            IterativeNetVsHeuristicTrainer it =
                new IterativeNetVsHeuristicTrainer(trainer, boardSize, gamesPerRound, totalRounds, printBoard);
            it.runIterativeTraining();

            // Possibly measure performance or do final checks
            System.out.println("ActivationFunc=" + af + " experiment complete.\n");
        }
    }

    // -----------------------------------------------------
    // Experiment B: Hidden Layer Sizes
    // -----------------------------------------------------
    private static void runHiddenLayerExperiment() {
        System.out.println("\nExperiment B: Hidden layers");

        int[][] layerConfigs = { {64}, {128}, {64,64} };
        for (int[] config : layerConfigs) {
            System.out.println("\n--- Testing hidden config=" + java.util.Arrays.toString(config) + " ---");

            NeuralNetworkTrainer trainer = NeuralNetworkInitializer.initializeTrainer(
                config,
                ActivationFunction.TANH
            );

            int boardSize = 5;
            int gamesPerRound = 5;
            int totalRounds = 2;
            boolean printBoard = false;

            IterativeNetVsHeuristicTrainer it =
                new IterativeNetVsHeuristicTrainer(trainer, boardSize, gamesPerRound, totalRounds, printBoard);
            it.runIterativeTraining();

            System.out.println("Hidden config " + java.util.Arrays.toString(config) + " experiment complete.\n");
        }
    }

    // -----------------------------------------------------
    // Experiment C: Simple Self-Play demonstration
    // -----------------------------------------------------
    private static void runSelfPlayExperiment() {
        System.out.println("\nExperiment C: Self-play demonstration");

        // Build a default trainer
        NeuralNetworkTrainer nnTrainer = NeuralNetworkInitializer.initializeTrainer();

        // Let's do ~10 self-play games
        int numberOfGames = 10;
        int validationFrequency = 5;
        int patience = 2;

        SelfPlayMinimaxTrainer selfPlay =
            new SelfPlayMinimaxTrainer(nnTrainer, numberOfGames, validationFrequency, patience);

        selfPlay.runTraining();

        // We won't bother saving it here, but you could:
        // nnTrainer.saveNetwork("networks/SelfPlayExperiment.txt");
        System.out.println("Self-play experiment done.\n");
    }

    // -----------------------------------------------------
    // Experiment D: Continuing from existing net
    // -----------------------------------------------------
    private static void runContinueFromExistingExperiment() {
        System.out.println("\nExperiment D: Continue training an existing net (if available).");

        // Suppose we look for a file "MyNet.txt" in networks/
        String existingFile = "networks/MyNet.txt";
        File f = new File(existingFile);

        if (!f.exists()) {
            System.out.println("   - 'MyNet.txt' not found. Skipping this experiment.");
            return;
        }

        // Load it
        NeuralNetworkTrainer trainer = NeuralNetworkInitializer.initializeTrainer();
        trainer.loadNetwork(existingFile);

        // Now do a small iterative or self-play
        int boardSz = 5;
        int gamesPerRound = 5;
        int totalRounds = 1; 
        boolean printBoard = false;

        // We'll do iterative vs. heuristic as a demonstration
        IterativeNetVsHeuristicTrainer iterTrain =
            new IterativeNetVsHeuristicTrainer(trainer, boardSz, gamesPerRound, totalRounds, printBoard);

        iterTrain.runIterativeTraining();

        // Save it back
        trainer.saveNetwork(existingFile);

        System.out.println("Continuing from existing net done. Updated MyNet.txt stored.\n");
    }
}
