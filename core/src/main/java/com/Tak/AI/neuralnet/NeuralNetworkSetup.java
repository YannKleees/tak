package com.Tak.AI.neuralnet;

/**
 * Utility for building a NeuralNetworkConfig with default input size = 3*(5*5) + 2, output = 1.
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
        // For a 5x5 board with 3 features/cell + 2 global features = 75 + 2 = 77
        // Adjust if you want to handle variable board sizes in a more dynamic approach.
        int inputSize = 130; // 77
        int outputSize = 1;
        return new NeuralNetworkConfig(inputSize, hiddenLayerSizes, outputSize, activationFunction);
    }
}
