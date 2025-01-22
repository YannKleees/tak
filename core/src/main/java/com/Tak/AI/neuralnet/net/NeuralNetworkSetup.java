package com.Tak.AI.neuralnet.net;

/**
 * Utility for building a NeuralNetworkConfig with default input size (130) and 2 outputs.
 */
public class NeuralNetworkSetup {

    public static NeuralNetworkConfig createConfig(int[] hiddenLayerSizes, ActivationFunction activationFunction) {
        int inputSize = 130;   // From BoardToInputsConverter
        int outputSize = 2;    // (scoreBLUE, scoreGREEN)
        return new NeuralNetworkConfig(inputSize, hiddenLayerSizes, outputSize, activationFunction);
    }
}
