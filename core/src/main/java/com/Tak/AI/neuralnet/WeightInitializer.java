package com.Tak.AI.neuralnet;

import java.util.Random;

/**
 * <p>Responsible for initializing the weights of the neural network.</p>
 *
 * <p><strong>SRP:</strong> This class only deals with generating initial weight values
 * for the input-to-hidden and hidden-to-output layers (or further layers if extended).</p>
 */
public class WeightInitializer {
    
    private static final Random RANDOM = new Random();
    
    /**
     * <p>Initializes a weight matrix with random values in range (-0.5, 0.5).</p>
     *
     * @param rows The number of rows (e.g., input neurons).
     * @param cols The number of columns (e.g., hidden or output neurons).
     * @return A 2D array of shape [rows][cols] with random values.
     */
    public static double[][] initializeRandom(int rows, int cols) {
        double[][] matrix = new double[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                // Uniform distribution in [-0.5, 0.5)
                matrix[i][j] = RANDOM.nextDouble() - 0.5;
            }
        }
        return matrix;
    }
    
    /**
     * <p>Initializes a bias array with random values in range (-0.5, 0.5).</p>
     *
     * @param size The size of the bias array.
     * @return An array of length 'size' with random bias values.
     */
    public static double[] initializeBias(int size) {
        double[] bias = new double[size];
        for (int i = 0; i < size; i++) {
            bias[i] = RANDOM.nextDouble() - 0.5;
        }
        return bias;
    }
}
