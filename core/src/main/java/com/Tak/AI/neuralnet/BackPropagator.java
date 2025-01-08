package com.Tak.AI.neuralnet;

/**
 * <p>Handles the backpropagation logic and weight updates for a feedforward network.</p>
 *
 * <p><strong>Single Responsibility (SRP):</strong>
 * This class focuses solely on calculating gradients (errors) and applying
 * them to the weight matrices for a network with one hidden layer.</p>
 */
public class BackPropagator {

    /**
     * <p>Conducts the backpropagation and weight update in a single hidden-layer network.</p>
     *
     * @param inputs                 The input array used in the forward pass.
     * @param targets                The target output array (ground truth).
     * @param learningRate           The learning rate for weight updates.
     * @param weightsInputHidden     The input-to-hidden weights ([inputSize][hiddenSize]).
     * @param weightsHiddenOutput    The hidden-to-output weights ([hiddenSize][outputSize]).
     * @param biasHidden             The bias array for the hidden layer ([hiddenSize]).
     * @param biasOutput             The bias array for the output layer ([outputSize]).
     * @param hiddenLayerActivations The hidden layer neuron outputs from the forward pass (post-activation).
     * @param outputActivations      The output layer neuron outputs from the forward pass (post-activation).
     * @param activationFunction     The activation function used for both hidden and output neurons.
     *
     * <p><strong>Implementation Outline:</strong></p>
     * <ol>
     *   <li>Compute output layer errors (<code>errOut[o] = (targets[o] - outputActivations[o]) * derivative(outputActivations[o])</code>).</li>
     *   <li>Compute hidden layer errors (sum the weighted output errors, then multiply by derivative(hiddenActivations[h])).</li>
     *   <li>Update each <code>weightsHiddenOutput[h][o]</code> and <code>biasOutput[o]</code>.</li>
     *   <li>Update each <code>weightsInputHidden[i][h]</code> and <code>biasHidden[h]</code>.</li>
     * </ol>
     */
    public static void backpropagate(
            double[] inputs,
            double[] targets,
            double learningRate,
            double[][] weightsInputHidden,
            double[][] weightsHiddenOutput,
            double[] biasHidden,
            double[] biasOutput,
            double[] hiddenLayerActivations,
            double[] outputActivations,
            ActivationFunction activationFunction
    ) {
        // 1) Compute output errors
        int outputSize = outputActivations.length;
        double[] errOut = new double[outputSize];

        for (int o = 0; o < outputSize; o++) {
            double errorTerm = (targets[o] - outputActivations[o]);
            double derivative = activateDerivative(outputActivations[o], activationFunction);
            errOut[o] = errorTerm * derivative;
        }

        // 2) Compute hidden layer errors
        int hiddenSize = hiddenLayerActivations.length;
        double[] errHidden = new double[hiddenSize];

        for (int h = 0; h < hiddenSize; h++) {
            double sum = 0.0;
            for (int o = 0; o < outputSize; o++) {
                sum += errOut[o] * weightsHiddenOutput[h][o];
            }
            double derivativeHidden = activateDerivative(hiddenLayerActivations[h], activationFunction);
            errHidden[h] = sum * derivativeHidden;
        }

        // 3) Update weights hidden->output and biasOutput
        for (int h = 0; h < hiddenSize; h++) {
            for (int o = 0; o < outputSize; o++) {
                double delta = learningRate * errOut[o] * hiddenLayerActivations[h];
                weightsHiddenOutput[h][o] += delta;
            }
        }
        // Also update the output biases
        for (int o = 0; o < outputSize; o++) {
            double delta = learningRate * errOut[o];
            biasOutput[o] += delta;
        }

        // 4) Update weights input->hidden and biasHidden
        int inputSize = inputs.length; 
        for (int i = 0; i < inputSize; i++) {
            for (int h = 0; h < hiddenSize; h++) {
                double delta = learningRate * errHidden[h] * inputs[i];
                weightsInputHidden[i][h] += delta;
            }
        }
        // Also update the hidden biases
        for (int h = 0; h < hiddenSize; h++) {
            double delta = learningRate * errHidden[h];
            biasHidden[h] += delta;
        }
    }

    /**
     * <p>Applies the derivative of the chosen activation function to a single value (needed for backprop).</p>
     *
     * @param activatedValue The neuron's post-activation output (if using SIGMOID or TANH).
     * @param function       The ActivationFunction used.
     * @return The derivative of the activation function at that value.
     */
    public static double activateDerivative(double activatedValue, ActivationFunction function) {
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
                // If more act. functions are added, handle them here
                return 1.0;
        }
    }
}
