package com.Tak.AI.neuralnet;

public class NeuralNetworkInitializer {
    /**
     * Initializes the NeuralNetworkTrainer with specified hidden layer sizes and activation function.
     *
     * @param hiddenLayerSizes  An array specifying the number of neurons in each hidden layer.
     * @param activationFunction The activation function to use.
     * @return An instance of NeuralNetworkTrainer.
     */
    public static NeuralNetworkTrainer initializeTrainer(int[] hiddenLayerSizes, ActivationFunction activationFunction) {
        NeuralNetworkConfig config = NeuralNetworkSetup.createConfig(hiddenLayerSizes, activationFunction);
        NeuralNetwork network = new NeuralNetwork(config);
        double learningRate = 0.01; // You can tweak this value

        return new NeuralNetworkTrainer(network, learningRate);
    }

    /**
     * Overloaded method for backward compatibility with default settings.
     *
     * @return An instance of NeuralNetworkTrainer with default hidden layer sizes and activation function.
     */
    public static NeuralNetworkTrainer initializeTrainer() {
        return initializeTrainer(new int[]{20}, ActivationFunction.TANH);
    }
}
