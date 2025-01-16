package com.Tak.AI.neuralnet;

/**
 * Utility class for setting up NeuralNetwork configurations.
 */
public class NeuralNetworkSetup {
    /**
     * Creates a NeuralNetworkConfig with specified hidden layer sizes and activation function.
     *
     * @param hiddenLayerSizes  An array specifying the number of neurons in each hidden layer.
     * @param activationFunction The activation function to use.
     * @return A NeuralNetworkConfig instance.
     */
    public static NeuralNetworkConfig createConfig(int[] hiddenLayerSizes, ActivationFunction activationFunction) {
        int inputSize = 3 * 5 * 5 + 2; // Example: 3 features per cell for a 5x5 board + 2 global features
        int outputSize = 1; // Assuming a single output for evaluation score

        return new NeuralNetworkConfig(inputSize, hiddenLayerSizes, outputSize, activationFunction);
    }
}
