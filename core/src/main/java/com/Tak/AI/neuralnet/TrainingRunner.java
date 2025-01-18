package com.Tak.AI.neuralnet;

/**
 * Simple runner that initializes a trainer, runs a small self-play training, and saves the network.
 */
public class TrainingRunner {

    public static void main(String[] args) {
        // 1) Create default trainer (hidden=20, TANH)
        NeuralNetworkTrainer nnTrainer = NeuralNetworkInitializer.initializeTrainer();
        System.out.println("Neural network initialized.");

        // 2) Set the number of training games, validation frequency, and patience
        int numberOfGames = 10; 
        int validationFrequency = 2;
        int patience = 5;  

        // 3) Create the self-play trainer
        SelfPlayMinimaxTrainer trainer = new SelfPlayMinimaxTrainer(
            nnTrainer,
            numberOfGames,
            validationFrequency,
            patience
        );
        System.out.println("Self-play trainer initialized.");

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
