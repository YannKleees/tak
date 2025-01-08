package com.Tak.AI.neuralnet;

public class NeuralNetworkSetup {
    public static NeuralNetworkConfig createConfig() {
        int inputSize = 3 * 5 * 5 + 2; // Example: 3 features per cell for a 5x5 board + 2 global features
        int hiddenSize = 20; // You can adjust based on experimentation
        int outputSize = 1; // Assuming a single output for evaluation score
        ActivationFunction activationFunction = ActivationFunction.TANH; // Choose based on your preference
        
        return new NeuralNetworkConfig(inputSize, hiddenSize, outputSize, activationFunction);
    }
}
