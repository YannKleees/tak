package com.Tak.AI.neuralnet;

import java.io.Serializable;

/**
 * <p>Holds configuration details for constructing and running the neural network.</p>
 *
 * <strong>SRP:</strong> This class only stores configuration properties
 * (layer sizes, activation function, etc.) without logic to manipulate them.
 */
public class NeuralNetworkConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int inputSize;
    private final int hiddenSize;
    private final int outputSize;
    private final ActivationFunction activationFunction;

    /**
     * <p>Constructs a NeuralNetworkConfig with the required parameters.</p>
     *
     * @param inputSize           The number of input neurons.
     * @param hiddenSize          The number of hidden-layer neurons.
     * @param outputSize          The number of output neurons.
     * @param activationFunction  The activation function to use (LINEAR, SIGMOID, TANH).
     */
    public NeuralNetworkConfig(int inputSize, int hiddenSize, int outputSize, ActivationFunction activationFunction) {
        this.inputSize = inputSize;
        this.hiddenSize = hiddenSize;
        this.outputSize = outputSize;
        this.activationFunction = activationFunction;
    }

    public int getInputSize() {
        return inputSize;
    }

    public int getHiddenSize() {
        return hiddenSize;
    }

    public int getOutputSize() {
        return outputSize;
    }

    public ActivationFunction getActivationFunction() {
        return activationFunction;
    }
}
