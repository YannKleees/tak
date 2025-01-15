package com.Tak.AI.neuralnet;

public class NeuralNetworkSetup {
    public static NeuralNetworkConfig createConfig() {
        // Example config for a 5x5 board with 3 features per cell + 2 global = 77 inputs
        int inputSize = 3 * 5 * 5 + 2;
        int hiddenSize = 20;
        int outputSize = 1;
        ActivationFunction activationFunction = ActivationFunction.TANH;

        return new NeuralNetworkConfig(inputSize, hiddenSize, outputSize, activationFunction);
    }
}
