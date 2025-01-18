package com.Tak.AI.neuralnet;

import java.io.Serializable;

/**
 * Represents a feedforward neural network with multiple hidden layers.
 */
public class NeuralNetwork implements Serializable {
    private static final long serialVersionUID = 1L;

    private final NeuralNetworkConfig config;

    /**
     * weights[layerIndex][inNeuron][outNeuron]
     * biases[layerIndex][outNeuron]
     */
    private double[][][] weights;
    private double[][] biases;

    /**
     * Constructs a multi-layer NeuralNetwork with the specified config,
     * and initializes all weights/biases randomly.
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

        // Initialize weights and biases for each layer
        int previousLayerSize = config.getInputSize();
        for (int i = 0; i < config.getHiddenLayerSizes().length; i++) {
            int currentLayerSize = config.getHiddenLayerSizes()[i];
            weights[i] = WeightInitializer.initializeXavier(previousLayerSize, currentLayerSize);
            biases[i] = WeightInitializer.initializeBias(currentLayerSize);
            previousLayerSize = currentLayerSize;
        }
        // Output layer
        int outputLayerIndex = layerCount - 1;
        weights[outputLayerIndex] = WeightInitializer.initializeXavier(previousLayerSize, config.getOutputSize());
        biases[outputLayerIndex] = WeightInitializer.initializeBias(config.getOutputSize());

        System.out.println("NeuralNetwork initialized with weights and biases.");
    }

    /**
     * Performs a forward pass through all hidden layers plus the output layer.
     *
     * @param inputs The network input array.
     * @return The output array (size = outputSize).
     */
    public double[] forward(double[] inputs) {
        double[] outputs = ForwardPropagator.forward(
            inputs, 
            weights, 
            biases, 
            config.getActivationFunction()
        );
        return outputs;
    }

    /**
     * Performs backpropagation and updates the network’s parameters.
     *
     * @param inputs             The input array.
     * @param targets            The desired/target output array.
     * @param learningRate       The learning rate for weight updates.
     * @param regularizationRate The L2 regularization rate.
     */
    public void backpropagate(double[] inputs, double[] targets, double learningRate, double regularizationRate) {
        ForwardPropagator.ForwardCache cache =
            ForwardPropagator.forwardWithCache(inputs, weights, biases, config.getActivationFunction());

        BackPropagator.backpropagate(
            inputs,
            targets,
            learningRate,
            regularizationRate,     // <-- Pass regularization rate here
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
     * Setter for weights (all layers at once). 
     * Make sure to pass an array that matches the shape needed by the config.
     */
    public void setWeights(double[][][] newWeights) {
        this.weights = newWeights;
        System.out.println("Weights updated.");
    }

    /**
     * Setter for biases (all layers at once).
     * Make sure it matches the shape [layerCount][neuronsInLayer].
     */
    public void setBiases(double[][] newBiases) {
        this.biases = newBiases;
        System.out.println("Biases updated.");
    }
}
