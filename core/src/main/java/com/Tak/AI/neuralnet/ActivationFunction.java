package com.Tak.AI.neuralnet;

/**
 * Defines the available activation functions for the placeholder neural network.
 * <p>
 * <strong>Usage Notes:</strong>
 * <ul>
 *   <li>LINEAR: Outputs the input as-is. Useful for debugging or certain regression tasks.</li>
 *   <li>SIGMOID: Outputs a value in (0, 1). Often used in simpler feedforward networks for binary classification.</li>
 *   <li>TANH:    Outputs a value in (-1, 1). Can be beneficial for zero-centered data or certain RL setups.</li>
 *   <li>RELU:    Rectified Linear Unit. Outputs max(0, x). Common for deep networks.</li>
 * </ul>
 */
public enum ActivationFunction {
    LINEAR,
    SIGMOID,
    TANH,
    RELU
}
