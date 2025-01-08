package com.Tak.AI.neuralnet;

public class NeuralNetworkInitializer {
    public static NeuralNetworkTrainer initializeTrainer() {
        NeuralNetworkConfig config = NeuralNetworkSetup.createConfig();
        NeuralNetwork network = new NeuralNetwork(config);
        double learningRate = 0.01; // You can tweak this value
        
        return new NeuralNetworkTrainer(network, learningRate);
    }
}
