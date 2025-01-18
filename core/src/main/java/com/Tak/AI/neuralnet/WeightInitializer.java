package com.Tak.AI.neuralnet;

import java.util.Random;

/**
 * Randomly (or Xavier) initializes the weights/biases in a multi-layer network.
 */
public class WeightInitializer {

    private static final Random RANDOM = new Random();

    /**
     * Initializes a weight matrix in [-0.5, 0.5].
     * If you want a different range or method, update accordingly.
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
     * Initializes a bias array in [-0.5, 0.5].
     */
    public static double[] initializeBias(int size) {
        double[] bias = new double[size];
        for (int i = 0; i < size; i++) {
            bias[i] = RANDOM.nextDouble() - 0.5;
        }
        return bias;
    }

    /**
     * Xavier/Glorot uniform initialization:
     * range = sqrt(6 / (fan_in + fan_out)).
     */
    public static double[][] initializeXavier(int fanIn, int fanOut) {
        double limit = Math.sqrt(6.0 / (fanIn + fanOut));
        double[][] matrix = new double[fanIn][fanOut];
        for (int r = 0; r < fanIn; r++) {
            for (int c = 0; c < fanOut; c++) {
                matrix[r][c] = (RANDOM.nextDouble() * 2.0 - 1.0) * limit;
            }
        }
        return matrix;
    }
}
