package com.Tak.AI.neuralnet;

/**
 * Performs backpropagation through a feedforward network.
 */
public class BackPropagator {

    public static void backpropagate(double[] inputs,
                                     double[] targets,
                                     double learningRate,
                                     double regularizationRate,
                                     double[][][] weights,
                                     double[][] biases,
                                     double[][] layerOutputs,
                                     ActivationFunction activationFunc) {

        int lastLayerIndex = weights.length - 1;

        // 1) Output layer errors
        double[] output = layerOutputs[lastLayerIndex];
        double[] outputErrors = new double[output.length];
        for (int i = 0; i < output.length; i++) {
            double errorTerm = (targets[i] - output[i]);
            double derivative = activateDerivative(output[i], activationFunc);
            outputErrors[i] = errorTerm * derivative;
        }

        double[] layerErrorSignal = outputErrors;

        // 2) Propagate backward
        for (int layerIndex = lastLayerIndex; layerIndex >= 0; layerIndex--) {
            double[] currentLayerOutput = layerOutputs[layerIndex];
            double[] previousLayerOutput = (layerIndex == 0) ? inputs : layerOutputs[layerIndex - 1];

            // (a) Update weights
            for (int inNeuron = 0; inNeuron < previousLayerOutput.length; inNeuron++) {
                for (int outNeuron = 0; outNeuron < currentLayerOutput.length; outNeuron++) {
                    double delta = learningRate * layerErrorSignal[outNeuron] * previousLayerOutput[inNeuron];
                    // L2 regularization
                    weights[layerIndex][inNeuron][outNeuron] += 
                        delta - (regularizationRate * weights[layerIndex][inNeuron][outNeuron]);
                }
            }

            // (b) Update biases
            for (int outNeuron = 0; outNeuron < currentLayerOutput.length; outNeuron++) {
                double delta = learningRate * layerErrorSignal[outNeuron];
                biases[layerIndex][outNeuron] += delta;
            }

            // (c) Compute next error signal if not the first layer
            if (layerIndex > 0) {
                double[] newErrorSignal = new double[previousLayerOutput.length];
                for (int inNeuron = 0; inNeuron < previousLayerOutput.length; inNeuron++) {
                    double sum = 0.0;
                    for (int outNeuron = 0; outNeuron < currentLayerOutput.length; outNeuron++) {
                        sum += layerErrorSignal[outNeuron] * weights[layerIndex][inNeuron][outNeuron];
                    }
                    double prevActivation = previousLayerOutput[inNeuron];
                    double derivative = activateDerivative(prevActivation, activationFunc);
                    newErrorSignal[inNeuron] = sum * derivative;
                }
                layerErrorSignal = newErrorSignal;
            }
        }
    }

    private static double activateDerivative(double activatedValue, ActivationFunction function) {
        switch (function) {
            case LINEAR:
                return 1.0;
            case SIGMOID:
                return activatedValue * (1.0 - activatedValue);
            case TANH:
                return 1.0 - (activatedValue * activatedValue);
            case RELU:
                return (activatedValue > 0.0) ? 1.0 : 0.0;
            default:
                return 1.0;
        }
    }
}
