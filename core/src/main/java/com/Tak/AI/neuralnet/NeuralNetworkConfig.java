package com.Tak.AI.neuralnet;

import java.io.Serializable;

/**
 * <p>Holds configuration details for constructing and running a multi-layer neural network.</p>
 *
 * <strong>Key Changes:</strong>
 * - Allows either multiple hidden layers via an int[] or
 *   a single hidden layer via a simpler constructor.
 */
public class NeuralNetworkConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int inputSize;
    private final int[] hiddenLayerSizes;
    private final int outputSize;
    private final ActivationFunction activationFunction;

    /**
     * <p>Construct a NeuralNetworkConfig with variable hidden-layer sizes.</p>
     *
     * @param inputSize           Number of input neurons.
     * @param hiddenLayerSizes    An array specifying how many neurons in each hidden layer.
     * @param outputSize          Number of output neurons.
     * @param activationFunction  Activation function (LINEAR, SIGMOID, TANH).
     */
    public NeuralNetworkConfig(int inputSize, int[] hiddenLayerSizes, int outputSize, ActivationFunction activationFunction) {
        this.inputSize = inputSize;
        this.hiddenLayerSizes = hiddenLayerSizes;
        this.outputSize = outputSize;
        this.activationFunction = activationFunction;
    }

    /**
     * <p>Convenience constructor for a single hidden layer (as used in older code).</p>
     *
     * @param inputSize           Number of input neurons.
     * @param hiddenSize          Number of neurons in the (single) hidden layer.
     * @param outputSize          Number of output neurons.
     * @param activationFunction  Activation function.
     */
    public NeuralNetworkConfig(int inputSize, int hiddenSize, int outputSize, ActivationFunction activationFunction) {
        this(
            inputSize,
            new int[]{ hiddenSize }, // single layer array
            outputSize,
            activationFunction
        );
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
