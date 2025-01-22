package com.Tak.AI.neuralnet.net;

import com.Tak.AI.neuralnet.trainer.NeuralNetworkTrainer;

public class NeuralNetworkInitializer {

    public static NeuralNetworkTrainer initializeTrainer(int[] hiddenLayerSizes, ActivationFunction activationFunction){
        NeuralNetworkConfig cfg = NeuralNetworkSetup.createConfig(hiddenLayerSizes, activationFunction);
        NeuralNetwork net = new NeuralNetwork(cfg);

        // default LR
        double learningRate = 0.001;

        NeuralNetworkTrainer trainer = new NeuralNetworkTrainer(net, learningRate);
        trainer.setShuffleData(true);
        trainer.setMiniBatchSize(32);
        trainer.setRegularizationRate(1e-4);
        return trainer;
    }

    public static NeuralNetworkTrainer initializeTrainer(){
        return initializeTrainer(new int[]{64}, ActivationFunction.TANH);
    }
}
