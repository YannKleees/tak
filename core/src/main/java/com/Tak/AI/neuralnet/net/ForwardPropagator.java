package com.Tak.AI.neuralnet.net;

/**
 * Handles the forward pass in a multi-layer feedforward neural network.
 */
public class ForwardPropagator {

    public static class ForwardCache {
        public double[][] layerOutputs;
    }

    public static double[] forward(double[] inputs,
                                   double[][][] weights,
                                   double[][] biases,
                                   ActivationFunction activationFunction) {
        double[] currentActivations = inputs;

        for (int layerIndex = 0; layerIndex < weights.length; layerIndex++) {
            currentActivations = computeLayerOutput(
                    currentActivations,
                    weights[layerIndex],
                    biases[layerIndex],
                    activationFunction
            );
        }
        return currentActivations;
    }

    public static ForwardCache forwardWithCache(double[] inputs,
                                                double[][][] weights,
                                                double[][] biases,
                                                ActivationFunction activationFunction) {
        ForwardCache cache = new ForwardCache();
        cache.layerOutputs = new double[weights.length][];

        double[] currentActivations = inputs;
        for (int layerIndex = 0; layerIndex < weights.length; layerIndex++) {
            currentActivations = computeLayerOutput(
                    currentActivations,
                    weights[layerIndex],
                    biases[layerIndex],
                    activationFunction
            );
            cache.layerOutputs[layerIndex] = currentActivations;
        }
        return cache;
    }

    private static double[] computeLayerOutput(double[] input,
                                               double[][] layerWeights,
                                               double[] layerBias,
                                               ActivationFunction func) {
        int inSize = layerWeights.length;
        int outSize = layerWeights[0].length;
        double[] output = new double[outSize];

        for (int o = 0; o < outSize; o++) {
            double sum = 0.0;
            for (int i = 0; i < inSize; i++) {
                sum += input[i] * layerWeights[i][o];
            }
            sum += layerBias[o];
            output[o] = applyActivation(sum, func);
        }
        return output;
    }

    public static double applyActivation(double value, ActivationFunction function) {
        switch (function) {
            case LINEAR:
                return value;
            case SIGMOID:
                return 1.0 / (1.0 + Math.exp(-value));
            case TANH:
                return Math.tanh(value);
            case RELU:
                return Math.max(0, value);
            default:
                return value;
        }
    }
}
