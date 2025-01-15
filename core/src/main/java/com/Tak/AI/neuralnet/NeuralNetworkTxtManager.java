package com.Tak.AI.neuralnet;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>A dedicated manager for handling .txt file operations related to Neural Networks.
 * It adheres to the SOLID principles by encapsulating all file-related functionalities.</p>
 *
 * <strong>Responsibilities:</strong>
 * <ul>
 *   <li>Saving neural network configurations and weights to .txt files.</li>
 *   <li>Loading neural network configurations and weights from .txt files.</li>
 *   <li>Managing metadata such as the number of games trained and performance outputs.</li>
 * </ul>
 */
public class NeuralNetworkTxtManager {

    /**
     * Saves the neural network along with metadata to a specified .txt file.
     *
     * @param trainer        The NeuralNetworkTrainer instance to save.
     * @param filePath       The path to the .txt file.
     * @param numberOfGames  The number of games (or states) trained on.
     * @param lastOutput     The last output or performance metric of the network.
     * @throws IOException If an I/O error occurs during saving.
     */
    public static void saveTrainer(NeuralNetworkTrainer trainer, String filePath, int numberOfGames, double lastOutput) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            // [Configuration]
            NeuralNetworkConfig cfg = trainer.getNetwork().getConfig();
            writer.write("[Configuration]");
            writer.newLine();
            writer.write("inputSize=" + cfg.getInputSize());
            writer.newLine();
            writer.write("numHiddenLayers=" + cfg.getHiddenLayerSizes().length);
            writer.newLine();
            for (int i = 0; i < cfg.getHiddenLayerSizes().length; i++) {
                writer.write("hiddenSize" + i + "=" + cfg.getHiddenLayerSizes()[i]);
                writer.newLine();
            }
            writer.write("outputSize=" + cfg.getOutputSize());
            writer.newLine();
            writer.write("activationFunction=" + cfg.getActivationFunction());
            writer.newLine();
            writer.newLine();

            // [WeightsLayerX] and [BiasLayerX]
            double[][][] weights = trainer.getNetwork().getWeights();
            double[][] biases = trainer.getNetwork().getBiases();
            for (int layerIndex = 0; layerIndex < weights.length; layerIndex++) {
                // Weights
                writer.write("[WeightsLayer" + layerIndex + "]");
                writer.newLine();
                for (double[] row : weights[layerIndex]) {
                    writer.write(joinArray(row, " "));
                    writer.newLine();
                }
                writer.newLine();

                // Biases
                writer.write("[BiasLayer" + layerIndex + "]");
                writer.newLine();
                writer.write(joinArray(biases[layerIndex], " "));
                writer.newLine();
                writer.newLine();
            }

            // [Metadata]
            writer.write("[Metadata]");
            writer.newLine();
            writer.write("numberOfGamesTrained=" + numberOfGames);
            writer.newLine();
            writer.write("lastOutput=" + lastOutput);
            writer.newLine();
        }
    }

    /**
     * Loads the neural network and metadata from a specified .txt file.
     *
     * @param filePath The path to the .txt file.
     * @return A NeuralNetworkLoadData instance containing the network and metadata.
     * @throws IOException If an I/O error occurs during loading.
     */
    public static NeuralNetworkLoadData loadTrainer(String filePath) throws IOException {
        NeuralNetworkConfig config = null;
        double[][][] weights = null;
        int numberOfGamesTrained = 0;
        double lastOutput = 0.0;

        // Initialize variables to store configuration
        int inputSize = 0;
        int numHiddenLayers = 0;
        List<Integer> hiddenSizes = new ArrayList<>();
        int outputSize = 0;
        ActivationFunction activationFunction = ActivationFunction.TANH;

        double[][] biases = null;
        int currentLayerIndex = -1;
        List<Double> currentLayerBiases = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            List<double[]> currentLayerWeights = new ArrayList<>();

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (line.startsWith("[Configuration]")) {
                    System.out.println("Parsing [Configuration] section...");
                    // Read config lines until a blank or new bracket section
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("[")) break;

                        String[] parts = line.split("=");
                        if (parts.length != 2) {
                            System.out.println("Invalid config line: " + line);
                            continue;
                        }
                        String key = parts[0];
                        String value = parts[1];

                        switch (key) {
                            case "inputSize":
                                inputSize = Integer.parseInt(value);
                                System.out.println("inputSize: " + inputSize);
                                break;
                            case "numHiddenLayers":
                                numHiddenLayers = Integer.parseInt(value);
                                System.out.println("numHiddenLayers: " + numHiddenLayers);
                                break;
                            case "outputSize":
                                outputSize = Integer.parseInt(value);
                                System.out.println("outputSize: " + outputSize);
                                break;
                            case "activationFunction":
                                activationFunction = ActivationFunction.valueOf(value);
                                System.out.println("activationFunction: " + activationFunction);
                                break;
                            default:
                                if (key.startsWith("hiddenSize")) {
                                    hiddenSizes.add(Integer.parseInt(value));
                                    System.out.println(key + ": " + value);
                                }
                                break;
                        }
                    }
                    // Initialize arrays
                    int totalLayers = numHiddenLayers + 1; // hidden + output
                    weights = new double[totalLayers][][];
                    biases = new double[totalLayers][];
                    System.out.println("Initialized weights and biases arrays for " + totalLayers + " layers.");
                }

                else if (line.startsWith("[WeightsLayer")) {
                    // Before starting a new layer, assign biases for the previous layer if available
                    if (currentLayerIndex != -1 && !currentLayerBiases.isEmpty()) {
                        biases[currentLayerIndex] = currentLayerBiases.stream().mapToDouble(Double::doubleValue).toArray();
                        System.out.println("Loaded Biases for Layer " + currentLayerIndex + " with " + biases[currentLayerIndex].length + " neurons.");
                        currentLayerBiases = new ArrayList<>();
                    }

                    // Extract layer index
                    String idxStr = line.substring("[WeightsLayer".length(), line.indexOf("]"));
                    currentLayerIndex = Integer.parseInt(idxStr);
                    currentLayerWeights = new ArrayList<>();
                    System.out.println("Parsing [WeightsLayer" + currentLayerIndex + "]...");
                }

                else if (line.startsWith("[BiasLayer")) {
                    // Assign weights for the current layer
                    if (!currentLayerWeights.isEmpty()) {
                        weights[currentLayerIndex] = currentLayerWeights.toArray(new double[0][]);
                        System.out.println("Loaded Weights for Layer " + currentLayerIndex + " with " + weights[currentLayerIndex].length + " neurons.");
                        currentLayerWeights = new ArrayList<>();
                    }

                    // Extract bias layer index
                    String idxStr = line.substring("[BiasLayer".length(), line.indexOf("]"));
                    currentLayerIndex = Integer.parseInt(idxStr);
                    currentLayerBiases = new ArrayList<>();
                    System.out.println("Parsing [BiasLayer" + currentLayerIndex + "]...");
                }

                else if (line.startsWith("[Metadata]")) {
                    // Assign biases for the last layer before metadata
                    if (!currentLayerBiases.isEmpty()) {
                        biases[currentLayerIndex] = currentLayerBiases.stream().mapToDouble(Double::doubleValue).toArray();
                        System.out.println("Loaded Biases for Layer " + currentLayerIndex + " with " + biases[currentLayerIndex].length + " neurons.");
                        currentLayerBiases = new ArrayList<>();
                    }

                    // Now read metadata lines
                    System.out.println("Parsing [Metadata] section...");
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("[")) break;
                        String[] parts = line.split("=");
                        if (parts.length != 2) {
                            System.out.println("Invalid metadata line: " + line);
                            continue;
                        }
                        String key = parts[0];
                        String value = parts[1];
                        switch (key) {
                            case "numberOfGamesTrained":
                                numberOfGamesTrained = Integer.parseInt(value);
                                System.out.println("numberOfGamesTrained: " + numberOfGamesTrained);
                                break;
                            case "lastOutput":
                                lastOutput = Double.parseDouble(value);
                                System.out.println("lastOutput: " + lastOutput);
                                break;
                            default:
                                System.out.println("Unknown metadata key: " + key);
                                break;
                        }
                    }
                    // End reading
                    break;
                }

                else {
                    // Now parse the lines as either weights or biases
                    // If we haven't encountered [BiasLayer..], we are reading weights
                    if (currentLayerIndex >= 0 && weights != null && biases != null) {
                        if (weights[currentLayerIndex] == null) {
                            // We are reading weight rows
                            String[] tokens = line.split(" ");
                            double[] row = new double[tokens.length];
                            for (int i = 0; i < tokens.length; i++) {
                                try {
                                    row[i] = Double.parseDouble(tokens[i]);
                                } catch (NumberFormatException e) {
                                    System.out.println("Invalid weight value: " + tokens[i]);
                                    row[i] = 0.0; // or handle appropriately
                                }
                            }
                            currentLayerWeights.add(row);
                        }
                        else {
                            // We must be reading bias lines
                            String[] tokens = line.split(" ");
                            for (String tok : tokens) {
                                try {
                                    currentLayerBiases.add(Double.parseDouble(tok));
                                } catch (NumberFormatException e) {
                                    System.out.println("Invalid bias value: " + tok);
                                    currentLayerBiases.add(0.0); // or handle appropriately
                                }
                            }
                        }
                    }
                }
            }

            // Assign the last layer's biases if not already assigned
            if (currentLayerIndex >= 0 && biases != null && !currentLayerBiases.isEmpty()) {
                biases[currentLayerIndex] = currentLayerBiases.stream().mapToDouble(Double::doubleValue).toArray();
                System.out.println("Loaded Biases for Layer " + currentLayerIndex + " with " + biases[currentLayerIndex].length + " neurons.");
            }

            // Build the NeuralNetworkConfig
            NeuralNetworkConfig configFinal = new NeuralNetworkConfig(
                    inputSize,
                    hiddenSizes.stream().mapToInt(i -> i).toArray(),
                    outputSize,
                    activationFunction
            );

            // Build the neural net
            NeuralNetwork net = new NeuralNetwork(configFinal);
            net.setWeights(weights);
            net.setBiases(biases);

            System.out.println("Neural network successfully loaded with configuration: ");
            System.out.println("Input Size: " + inputSize);
            System.out.println("Hidden Layers: " + hiddenSizes);
            System.out.println("Output Size: " + outputSize);
            System.out.println("Activation Function: " + activationFunction);

                return new NeuralNetworkLoadData(net, numberOfGamesTrained, lastOutput);
            }
        }

    /**
     * Converts a double[] to a space-separated string.
     */
    private static String joinArray(double[] array, String delimiter) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            sb.append(array[i]);
            if (i < array.length - 1) sb.append(delimiter);
        }
        return sb.toString();
    }

    /**
     * Data structure for loading the neural network and metadata.
     */
    public static class NeuralNetworkLoadData {
        public NeuralNetwork network;
        public int numberOfGamesTrained;
        public double lastOutput;

        NeuralNetworkLoadData(NeuralNetwork network, int numberOfGamesTrained, double lastOutput) {
            this.network = network;
            this.numberOfGamesTrained = numberOfGamesTrained;
            this.lastOutput = lastOutput;
        }
    }
}
