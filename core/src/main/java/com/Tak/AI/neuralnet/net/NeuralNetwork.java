package com.Tak.AI.neuralnet.net;

import java.io.Serializable;

/**
 * Represents a feedforward neural network with multiple hidden layers.
 */
public class NeuralNetwork implements Serializable {
    private static final long serialVersionUID = 1L;

    private final NeuralNetworkConfig config;

    // Standard weights/biases
    private double[][][] weights;
    private double[][] biases;

    // ADDED FEATURE: track last-deltas for momentum
    private double[][][] lastWeightDeltas;
    private double[][] lastBiasDeltas;

    public NeuralNetwork(NeuralNetworkConfig config) {
        this.config = config;
        initializeWeights();
        initializeLastDeltas();
    }

    private void initializeWeights() {
        int layerCount = config.getHiddenLayerSizes().length + 1;
        weights = new double[layerCount][][];
        biases = new double[layerCount][];

        int prevLayerSize = config.getInputSize();
        for(int i=0; i<config.getHiddenLayerSizes().length; i++){
            int currentSize = config.getHiddenLayerSizes()[i];
            weights[i] = WeightInitializer.initializeXavier(prevLayerSize, currentSize);
            biases[i] = WeightInitializer.initializeBias(currentSize);
            prevLayerSize = currentSize;
        }
        // output layer
        int outLayerIdx = layerCount -1;
        weights[outLayerIdx] = WeightInitializer.initializeXavier(prevLayerSize, config.getOutputSize());
        biases[outLayerIdx] = WeightInitializer.initializeBias(config.getOutputSize());

        System.out.println("NeuralNetwork initialized (Xavier).");
    }

    // Prepare same shape arrays for momentum
    private void initializeLastDeltas() {
        lastWeightDeltas = new double[weights.length][][];
        lastBiasDeltas   = new double[biases.length][];

        for(int l=0; l<weights.length; l++){
            int inSize = weights[l].length;
            int outSize = weights[l][0].length;
            lastWeightDeltas[l] = new double[inSize][outSize];
            lastBiasDeltas[l] = new double[outSize];
        }
    }

    public double[] forward(double[] inputs) {
        return ForwardPropagator.forward(
            inputs, weights, biases, config.getActivationFunction()
        );
    }

    public void backpropagate(double[] inputs,
                              double[] targets,
                              double learningRate,
                              double regularRate,
                              double momentum) {
        ForwardPropagator.ForwardCache cache =
            ForwardPropagator.forwardWithCache(inputs, weights, biases, config.getActivationFunction());

        BackPropagator.backpropagate(
            inputs,
            targets,
            learningRate,
            regularRate,
            momentum,
            weights,
            lastWeightDeltas, // pass references
            biases,
            lastBiasDeltas,
            cache.layerOutputs,
            config.getActivationFunction()
        );
    }

    // getters / setters

    public NeuralNetworkConfig getConfig() {
        return config;
    }

    public double[][][] getWeights() {
        return weights;
    }

    public double[][] getBiases() {
        return biases;
    }

    public void setWeights(double[][][] w) {
        this.weights = w;
        initializeLastDeltas(); // re-init momentum arrays if needed
    }

    public void setBiases(double[][] b) {
        this.biases = b;
        initializeLastDeltas();
    }
}
