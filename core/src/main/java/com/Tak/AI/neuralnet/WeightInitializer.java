package com.Tak.AI.neuralnet;

import java.util.Random;

/**
 * <p>Randomly initializes the weights and biases in a multi-layer network.</p>
 */
public class WeightInitializer {
    private static final Random RANDOM = new Random();

    /**
     * <p>Initializes a weight matrix with random values in (-0.5, 0.5).</p>
     */
    public static double[][] initializeRandom(int rows, int cols) {
        double[][] matrix = new double[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                matrix[i][j] = RANDOM.nextDouble() - 0.5;
            }
        }
        return matrix;
    }

    /**
     * <p>Initializes a bias array with random values in (-0.5, 0.5).</p>
     */
    public static double[] initializeBias(int size) {
        double[] bias = new double[size];
        for (int i = 0; i < size; i++) {
            bias[i] = RANDOM.nextDouble() - 0.5;
        }
        return bias;
    }
}
