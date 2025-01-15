package com.Tak.AI.neuralnet;

import java.io.Serializable;

/**
 * <p>Represents a feedforward neural network with multiple hidden layers.</p>
 *
 * <strong>Key Changes:</strong>
 * - Instead of a single hidden layer, we store an array of weight/bias pairs:
 *   weights[0] = input-to-first-hidden
 *   weights[1] = first-hidden-to-second-hidden
 *   ...
 *   weights[L] = last-hidden-to-output
 *
 * - The forward/backprop logic loops through these layers in sequence.
 */
public class NeuralNetwork implements Serializable {
    private static final long serialVersionUID = 1L;

    private final NeuralNetworkConfig config;

    /**
     * weights[layerIndex] is a 2D array: [numNeuronsInPreviousLayer][numNeuronsInThisLayer]
     * biases[layerIndex] is a 1D array: [numNeuronsInThisLayer]
     *
     * For example, if hiddenLayers = {20, 10}:
     *   - weights[0]: shape [inputSize, 20]
     *   - weights[1]: shape [20, 10]
     *   - weights[2]: shape [10, outputSize] // final layer
     */
    private double[][][] weights;
    private double[][] biases;

    /**
     * <p>Constructs a multi-layer NeuralNetwork with the specified config,
     * and initializes all weights/biases randomly.</p>
     *
     * @param config The network configuration (layer sizes, activation function, etc.)
     */
    public NeuralNetwork(NeuralNetworkConfig config) {
        this.config = config;
        initializeWeights();
    }

    /**
     * Allocates and randomly initializes all weight matrices & bias arrays.
     */
    private void initializeWeights() {
        int layerCount = config.getHiddenLayerSizes().length + 1; // hidden layers + output layer
        weights = new double[layerCount][][];
        biases = new double[layerCount][];

        // ----- Build layer shapes -----
        // layer 0: input -> hiddenLayer[0]
        int previousLayerSize = config.getInputSize();
        for (int i = 0; i < config.getHiddenLayerSizes().length; i++) {
            int thisLayerSize = config.getHiddenLayerSizes()[i];
            weights[i] = WeightInitializer.initializeRandom(previousLayerSize, thisLayerSize);
            biases[i] = WeightInitializer.initializeBias(thisLayerSize);
            previousLayerSize = thisLayerSize;
        }
        // final layer: last hidden -> output
        int outputLayerIndex = layerCount - 1;
        weights[outputLayerIndex] = WeightInitializer.initializeRandom(previousLayerSize, config.getOutputSize());
        biases[outputLayerIndex] = WeightInitializer.initializeBias(config.getOutputSize());
    }

    /**
     * <p>Performs a forward pass through all hidden layers plus the output layer.</p>
     *
     * @param inputs The network input array.
     * @return The output array (size = outputSize).
     */
    public double[] forward(double[] inputs) {
        return ForwardPropagator.forward(
                inputs,
                weights,
                biases,
                config.getActivationFunction()
        );
    }

    /**
     * <p>Performs backpropagation and updates the network’s parameters.</p>
     *
     * @param inputs       The input array.
     * @param targets      The desired/target output array.
     * @param learningRate The learning rate for weight updates.
     */
    public void backpropagate(double[] inputs, double[] targets, double learningRate) {
        // We'll do a forward pass that captures all layer activations, so we don't recalc them.
        ForwardPropagator.ForwardCache cache =
                ForwardPropagator.forwardWithCache(inputs, weights, biases, config.getActivationFunction());

        // Then backprop across all layers
        BackPropagator.backpropagate(
                inputs,
                targets,
                learningRate,
                0.0, // Add the missing double argument
                weights,
                biases,
                cache.layerOutputs,
                config.getActivationFunction()
        );
    }

    // ------------------------ GETTERS / SETTERS ---------------------------

    public NeuralNetworkConfig getConfig() {
        return config;
    }

    public double[][][] getWeights() {
        return weights;
    }

    public double[][] getBiases() {
        return biases;
    }

    /**
     * <p>Setter for weights (all layers at once). 
     * Make sure to pass an array that matches the shape needed by the config.</p>
     */
    public void setWeights(double[][][] newWeights) {
        this.weights = newWeights;
    }

    /**
     * <p>Setter for biases (all layers at once).
     * Make sure it matches the shape [layerCount][neuronsInLayer].</p>
     */
    public void setBiases(double[][] newBiases) {
        this.biases = newBiases;
    }
}
