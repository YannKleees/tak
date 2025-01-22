package com.Tak.AI.neuralnet.trainer;

import com.Tak.AI.neuralnet.net.NeuralNetworkInitializer;

/**
 * Example main class that runs self-play training (or you can adapt it to iterative training).
 */
public class TrainingRunner {

    public static void main(String[] args) {
        // 1) Create default trainer: hidden=64, TANH, LR=0.001
        NeuralNetworkTrainer nnTrainer = NeuralNetworkInitializer.initializeTrainer();
        System.out.println("Neural network initialized with TANH final layer, 64 hidden units.");

        // 2) Customize
        nnTrainer.setShuffleData(true);
        nnTrainer.setMiniBatchSize(32);
        nnTrainer.setLearningRateDecay(0.99);
        nnTrainer.setRegularizationRate(1e-4);

        // 3) Decide whether you want iterative training vs. heuristic or self-play:
        //    For iterative training:
        //       IterativeNetVsHeuristicTrainer it = new IterativeNetVsHeuristicTrainer(nnTrainer, 5, 10, 3, false);
        //       it.runIterativeTraining();
        //
        //    For self-play:
        int numberOfGames = 20;
        int validationFrequency = 5;
        int patience = 3;

        SelfPlayMinimaxTrainer trainer = new SelfPlayMinimaxTrainer(
            nnTrainer,
            numberOfGames,
            validationFrequency,
            patience
        );

        // Example: switch to Net-vs-Random
        //trainer.setOpponentType(ANNManager.OpponentType.NET_VS_RANDOM);

        // Possibly set truncation
        // trainer.setTruncationLimit(30);

        // 4) Start training
        trainer.runTraining();
        System.out.println("Training completed!");

        // 5) Save the network
        String networkName = "TANH_Net";
        String savePath = "networks/" + networkName + ".txt";
        nnTrainer.saveNetwork(savePath);

        System.out.println("Network saved to " + savePath);
    }
}
