package com.Tak.AI.neuralnet;

public class TrainingRunner {
    public static void main(String[] args) {
        // Initialize the trainer
        NeuralNetworkTrainer nnTrainer = NeuralNetworkInitializer.initializeTrainer();
        System.out.println("Neural network initialized");

        // Define the number of training games
        int numberOfGames = 1; // Adjust based on your computational resources
        System.out.println("Training for " + numberOfGames + " games");

        // Initialize the self-play trainer
        SelfPlayMinimaxTrainer trainer = new SelfPlayMinimaxTrainer(nnTrainer, numberOfGames);
        System.out.println("Self-play trainer initialized");

        // Start training
        trainer.runTraining();
        System.out.println("Training completed");

        // Save the trained network with metadata
        String savePath = "trained_network.txt";
        nnTrainer.saveNetwork(savePath);

        System.out.println("Training completed and network saved to " + savePath);
    }
}
