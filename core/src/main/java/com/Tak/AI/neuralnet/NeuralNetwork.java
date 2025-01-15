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
     * weights[layerIndex] = 2D array: [numNeuronsInPreviousLayer][numNeuronsInThisLayer]
     * biases[layerIndex] = 1D array: [numNeuronsInThisLayer]
     */
    private double[][][] weights;
    private double[][] biases;

    public NeuralNetwork(NeuralNetworkConfig config) {
        this.config = config;
        initializeWeights();
    }

    private void initializeWeights() {
        int layerCount = config.getHiddenLayerSizes().length + 1;
        weights = new double[layerCount][][];
        biases = new double[layerCount][];

        int previousLayerSize = config.getInputSize();
        for (int i = 0; i < config.getHiddenLayerSizes().length; i++) {
            int thisLayerSize = config.getHiddenLayerSizes()[i];
            weights[i] = WeightInitializer.initializeRandom(previousLayerSize, thisLayerSize);
            biases[i] = WeightInitializer.initializeBias(thisLayerSize);
            previousLayerSize = thisLayerSize;
        }
        // final layer
        int outputLayerIndex = layerCount - 1;
        weights[outputLayerIndex] = WeightInitializer.initializeRandom(previousLayerSize, config.getOutputSize());
        biases[outputLayerIndex] = WeightInitializer.initializeBias(config.getOutputSize());
    }

    public double[] forward(double[] inputs) {
        return ForwardPropagator.forward(
            inputs,
            weights,
            biases,
            config.getActivationFunction()
        );
    }

    public void backpropagate(double[] inputs, double[] targets, double learningRate) {
        // forward pass with cache
        ForwardPropagator.ForwardCache cache =
            ForwardPropagator.forwardWithCache(inputs, weights, biases, config.getActivationFunction());

        // pass everything to BackPropagator
        BackPropagator.backpropagate(
            inputs,
            targets,
            learningRate,
            0.0, // you can change this to a nonzero if you want L2
            weights,
            biases,
            cache.layerOutputs,
            config.getActivationFunction()
        );
    }

    // Getters / Setters
    public NeuralNetworkConfig getConfig() {
        return config;
    }

    public double[][][] getWeights() {
        return weights;
    }

    public double[][] getBiases() {
        return biases;
    }

    public void setWeights(double[][][] newWeights) {
        this.weights = newWeights;
    }

    public void setBiases(double[][] newBiases) {
        this.biases = newBiases;
    }
}
