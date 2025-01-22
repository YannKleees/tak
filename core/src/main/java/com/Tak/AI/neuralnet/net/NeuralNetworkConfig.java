package com.Tak.AI.neuralnet.net;

import java.io.Serializable;

public class NeuralNetworkConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int inputSize;
    private final int[] hiddenLayerSizes;
    private final int outputSize;
    private final ActivationFunction activationFunction;

    public NeuralNetworkConfig(int inputSize, int[] hiddenLayerSizes, int outputSize, ActivationFunction activationFunction) {
        this.inputSize = inputSize;
        this.hiddenLayerSizes = hiddenLayerSizes;
        this.outputSize = outputSize;
        this.activationFunction = activationFunction;
    }

    public int getInputSize() {
        return inputSize;
    }

    public int[] getHiddenLayerSizes() {
        return hiddenLayerSizes;
    }

    public int getOutputSize() {
        return outputSize;
    }

    public ActivationFunction getActivationFunction() {
        return activationFunction;
    }
}
