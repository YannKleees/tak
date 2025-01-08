package com.Tak.AI.neuralnet;

public class NetworkLoader {
    public static NeuralNetworkTrainer loadTrainer(String filePath, double learningRate) {
        NeuralNetworkTrainer nnTrainer = new NeuralNetworkTrainer(null, learningRate);
        nnTrainer.loadNetwork(filePath);
        return nnTrainer;
    }
}
