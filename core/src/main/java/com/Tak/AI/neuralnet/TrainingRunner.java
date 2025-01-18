package com.Tak.AI.neuralnet;

public class TrainingRunner {

    public static void main(String[] args) {
        // 1) Create default trainer (hidden=20) now with RELU
        NeuralNetworkTrainer nnTrainer = NeuralNetworkInitializer.initializeTrainer();
        System.out.println("Neural network initialized.");

        // 2) Customize training hyperparameters
        nnTrainer.setShuffleData(true);
        nnTrainer.setMiniBatchSize(32);
        nnTrainer.setLearningRateDecay(0.99);
        nnTrainer.setRegularizationRate(1e-4);

        // 3) Set the number of training games, validation frequency, and patience
        int numberOfGames = 10; 
        int validationFrequency = 2;
        int patience = 5;  

        // 4) Create the self-play trainer
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
        String networkName = "RELU_Net"; 
        String savePath = "networks/" + networkName + ".txt"; 
        nnTrainer.saveNetwork(savePath);

        System.out.println("Training completed and network saved to " + savePath);
    }
}
