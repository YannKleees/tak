package com.Tak.AI.neuralnet;

/**
 * <p>Handles the forward pass logic for a feedforward neural network.</p>
 *
 * <p><strong>SRP:</strong> Only focuses on computing hidden layer activations and
 * output layer activations from given inputs & weights, for a single-hidden-layer design.</p>
 */
public class ForwardPropagator {

    /**
     * <p>Performs a forward pass on a single-layer hidden network.</p>
     *
     * @param inputs               The input array.
     * @param weightsInputHidden   The input-to-hidden weight matrix ([inputSize][hiddenSize]).
     * @param weightsHiddenOutput  The hidden-to-output weight matrix ([hiddenSize][outputSize]).
     * @param biasHidden           The bias array for the hidden layer ([hiddenSize]).
     * @param biasOutput           The bias array for the output layer ([outputSize]).
     * @param activationFunction   The activation function to apply for hidden and output neurons.
     * @return The output array from the network (size = number of output neurons).
     */
    public static double[] forward(double[] inputs,
                                   double[][] weightsInputHidden,
                                   double[][] weightsHiddenOutput,
                                   double[] biasHidden,
                                   double[] biasOutput,
                                   ActivationFunction activationFunction) {
        // Hidden layer
        int hiddenSize = weightsInputHidden[0].length;
        double[] hiddenActivations = new double[hiddenSize];

        for (int h = 0; h < hiddenSize; h++) {
            double sum = 0.0;
            for (int i = 0; i < inputs.length; i++) {
                sum += inputs[i] * weightsInputHidden[i][h];
            }
            sum += biasHidden[h];
            hiddenActivations[h] = applyActivation(sum, activationFunction);
        }

        // Output layer
        int outputSize = weightsHiddenOutput[0].length;
        double[] outputActivations = new double[outputSize];

        for (int o = 0; o < outputSize; o++) {
            double sum = 0.0;
            for (int h2 = 0; h2 < hiddenSize; h2++) {
                sum += hiddenActivations[h2] * weightsHiddenOutput[h2][o];
            }
            sum += biasOutput[o];
            outputActivations[o] = applyActivation(sum, activationFunction);
        }

        return outputActivations;
    }

    /**
     * <p>Applies the chosen activation function to a single value.</p>
     *
     * @param value     The input sum to activate (z).
     * @param function  The chosen ActivationFunction.
     * @return The activated value f(z).
     */
    public static double applyActivation(double value, ActivationFunction function) {
        switch (function) {
            case LINEAR:
                return value;
            case SIGMOID:
                return 1.0 / (1.0 + Math.exp(-value));
            case TANH:
                return Math.tanh(value);
            default:
                return value;
        }
    }

    /**
     * <p>Optional helper class that holds hidden + output activations if you want them for debugging/backprop.</p>
     */
    public static class ForwardResults {
        private final double[] hiddenActivations;
        private final double[] outputActivations;

        public ForwardResults(double[] hiddenActivations, double[] outputActivations) {
            this.hiddenActivations = hiddenActivations;
            this.outputActivations = outputActivations;
        }

        public double[] getHiddenActivations() {
            return hiddenActivations;
        }

        public double[] getOutputActivations() {
            return outputActivations;
        }
    }
}
