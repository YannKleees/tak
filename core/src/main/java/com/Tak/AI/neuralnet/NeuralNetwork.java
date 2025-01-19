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
     * and initializes all weights/biases randomly (Xavier).
     */
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
            int currentLayerSize = config.getHiddenLayerSizes()[i];
            weights[i] = WeightInitializer.initializeXavier(previousLayerSize, currentLayerSize);
            biases[i] = WeightInitializer.initializeBias(currentLayerSize);
            previousLayerSize = currentLayerSize;
        }
        
        // Output layer
        int outputLayerIndex = layerCount - 1;
        weights[outputLayerIndex] = WeightInitializer.initializeXavier(previousLayerSize, config.getOutputSize());
        biases[outputLayerIndex] = WeightInitializer.initializeBias(config.getOutputSize());

        System.out.println("NeuralNetwork initialized with Xavier weights and biases.");
    }

    public double[] forward(double[] inputs) {
        return ForwardPropagator.forward(
            inputs,
            weights,
            biases,
            config.getActivationFunction()
        );
    }

    public void backpropagate(double[] inputs, double[] targets, double learningRate, double regularizationRate) {
        ForwardPropagator.ForwardCache cache =
            ForwardPropagator.forwardWithCache(inputs, weights, biases, config.getActivationFunction());

        BackPropagator.backpropagate(
            inputs,
            targets,
            learningRate,
            regularizationRate,
            weights,
            biases,
            cache.layerOutputs,
            config.getActivationFunction()
        );
    }

    // --------------- GETTERS / SETTERS ---------------

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
        System.out.println("Weights updated.");
    }

    public void setBiases(double[][] newBiases) {
        this.biases = newBiases;
        System.out.println("Biases updated.");
    }
}
