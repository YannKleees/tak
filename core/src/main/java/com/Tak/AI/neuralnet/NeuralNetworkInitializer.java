package com.Tak.AI.neuralnet;

/**
 * Creates a NeuralNetworkTrainer with a given architecture.
 */
public class NeuralNetworkInitializer {

    public static NeuralNetworkTrainer initializeTrainer(int[] hiddenLayerSizes, ActivationFunction activationFunction) {
        NeuralNetworkConfig config = NeuralNetworkSetup.createConfig(hiddenLayerSizes, activationFunction);
        NeuralNetwork network = new NeuralNetwork(config);
        double learningRate = 0.01; // You can tweak this value

        return new NeuralNetworkTrainer(network, learningRate);
    }

    public static NeuralNetworkTrainer initializeTrainer() {
        // default hidden size = 20, TANH
        return initializeTrainer(new int[]{20}, ActivationFunction.TANH);
    }
}
