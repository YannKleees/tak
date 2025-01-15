package com.Tak.AI.neuralnet;

/**
 * <p>Handles the backpropagation logic in a multi-layer feedforward network.</p>
 *
 * Basic steps:
 *  1) Compute output layer errors
 *  2) Propagate errors backward through each hidden layer
 *  3) Update weights and biases
 */
public class BackPropagator {

    /**
     * <p>Performs backprop across all layers in reverse order.</p>
     *
     * @param inputs         The original network inputs.
     * @param targets        The desired network outputs.
     * @param learningRate   The learning rate for updates.
     * @param regularizationRate The L2 regularization rate (if used).
     * @param weights        The full array of weight matrices (one per layer).
     * @param biases         The full array of bias arrays (one per layer).
     * @param layerOutputs   layerOutputs[i] = output of layer i (where i=0 is first hidden, last is the final output).
     * @param activationFunc The activation used in all layers (for derivative).
     */
    public static void backpropagate(double[] inputs,
                                     double[] targets,
                                     double learningRate,
                                     double regularizationRate,
                                     double[][][] weights,
                                     double[][] biases,
                                     double[][] layerOutputs,
                                     ActivationFunction activationFunc) {

        int lastLayerIndex = weights.length - 1;

        // 1) Output layer error
        double[] output = layerOutputs[lastLayerIndex];
        double[] outputErrors = new double[output.length];
        for (int i = 0; i < output.length; i++) {
            double errorTerm = (targets[i] - output[i]);
            double derivative = activateDerivative(output[i], activationFunc);
            outputErrors[i] = errorTerm * derivative;
        }

        // We'll keep a running "error signal" from layer to layer
        double[] layerErrorSignal = outputErrors;

        // 2) Propagate backward through hidden layers
        for (int layerIndex = lastLayerIndex; layerIndex >= 0; layerIndex--) {
            double[] currentLayerOutput = layerOutputs[layerIndex];
            double[] previousLayerOutput = (layerIndex == 0)
                ? inputs
                : layerOutputs[layerIndex - 1];

            // a) Update weights
            for (int inNeuron = 0; inNeuron < previousLayerOutput.length; inNeuron++) {
                for (int outNeuron = 0; outNeuron < currentLayerOutput.length; outNeuron++) {
                    // Gradient step
                    double delta = learningRate * layerErrorSignal[outNeuron] * previousLayerOutput[inNeuron];
                    // Optional L2 regularization
                    weights[layerIndex][inNeuron][outNeuron] +=
                        delta - (regularizationRate * weights[layerIndex][inNeuron][outNeuron]);
                }
            }

            // b) Update biases
            for (int outNeuron = 0; outNeuron < currentLayerOutput.length; outNeuron++) {
                double delta = learningRate * layerErrorSignal[outNeuron];
                biases[layerIndex][outNeuron] += delta;
            }

            // c) Compute error signal for the next (previous) layer, if any
            if (layerIndex > 0) {
                double[] newErrorSignal = new double[previousLayerOutput.length];
                for (int inNeuron = 0; inNeuron < previousLayerOutput.length; inNeuron++) {
                    double sum = 0.0;
                    for (int outNeuron = 0; outNeuron < currentLayerOutput.length; outNeuron++) {
                        sum += layerErrorSignal[outNeuron] * weights[layerIndex][inNeuron][outNeuron];
                    }
                    // multiply by derivative wrt the previous layer's activation
                    double prevActivation = previousLayerOutput[inNeuron];
                    double derivative = activateDerivative(prevActivation, activationFunc);
                    newErrorSignal[inNeuron] = sum * derivative;
                }
                layerErrorSignal = newErrorSignal;
            }
        }
    }

    /**
     * <p>Applies the derivative of the chosen activation function
     * to a neuron's post-activation output value.</p>
     */
    private static double activateDerivative(double activatedValue, ActivationFunction function) {
        switch (function) {
            case LINEAR:
                return 1.0;
            case SIGMOID:
                // d/dx(sigmoid) = sigmoid * (1 - sigmoid)
                return activatedValue * (1.0 - activatedValue);
            case TANH:
                // d/dx(tanh) = 1 - tanh^2
                return 1.0 - (activatedValue * activatedValue);
            default:
                return 1.0;
        }
    }
}
