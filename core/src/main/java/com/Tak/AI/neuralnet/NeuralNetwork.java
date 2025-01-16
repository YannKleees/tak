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
            weights[i] = WeightInitializer.initializeRandom(previousLayerSize, currentLayerSize);
            biases[i] = WeightInitializer.initializeBias(currentLayerSize);
            previousLayerSize = currentLayerSize;
        }
        // Output layer
        int outputLayerIndex = layerCount - 1;
        weights[outputLayerIndex] = WeightInitializer.initializeRandom(previousLayerSize, config.getOutputSize());
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
        //System.out.println("Starting forward pass...");
        double[] outputs = ForwardPropagator.forward(
                inputs,
                weights,
                biases,
                config.getActivationFunction()
        );
        //System.out.println("Forward pass completed. Output: " + outputs[0]);
        return outputs;
    }

    /**
     * Performs backpropagation and updates the network’s parameters.
     *
     * @param inputs         The input array.
     * @param targets        The desired/target output array.
     * @param learningRate   The learning rate for weight updates.
     */
    public void backpropagate(double[] inputs, double[] targets, double learningRate) {
        System.out.println("Starting backpropagation...");
        ForwardPropagator.ForwardCache cache =
                ForwardPropagator.forwardWithCache(inputs, weights, biases, config.getActivationFunction());

        BackPropagator.backpropagate(
                inputs,
                targets,
                learningRate,
                0.0, // regularization rate
                weights,
                biases,
                cache.layerOutputs,
                config.getActivationFunction()
        );
        System.out.println("Backpropagation completed.");
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
