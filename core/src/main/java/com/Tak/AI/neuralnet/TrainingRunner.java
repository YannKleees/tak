package com.Tak.AI.neuralnet;

public class TrainingRunner {
    public static void main(String[] args) {
        // Initialize the trainer
        NeuralNetworkTrainer nnTrainer = NeuralNetworkInitializer.initializeTrainer();
        System.out.println("Neural network initialized");
        
        // Define the number of training games
        int numberOfGames = 1; // Start with 1 game for a small training session
        System.out.println("Training for " + numberOfGames + " game(s)");
        
        // Initialize the self-play trainer
        SelfPlayMinimaxTrainer trainer = new SelfPlayMinimaxTrainer(nnTrainer, numberOfGames);
        System.out.println("Self-play trainer initialized");
        
        // Start training
        trainer.runTraining();
        System.out.println("Training completed");
        
        // Save the trained network with metadata
        String networkName = "TanH"; // Ensure this matches the name used in NeuralNetManager
        String savePath = "networks/" + networkName + ".txt"; // Save within the 'networks' directory
        nnTrainer.saveNetwork(savePath);
        
        System.out.println("Training completed and network saved to " + savePath);
    }
}
