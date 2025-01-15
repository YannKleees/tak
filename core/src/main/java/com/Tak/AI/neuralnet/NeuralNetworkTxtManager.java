package com.Tak.AI.neuralnet;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>A dedicated manager for handling .txt file operations related to Neural Networks.</p>
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
     */
    public static void saveTrainer(NeuralNetworkTrainer trainer, String filePath, int numberOfGames, double lastOutput)
        throws IOException {

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
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

            double[][][] weights = trainer.getNetwork().getWeights();
            double[][] biases = trainer.getNetwork().getBiases();
            for (int layerIndex = 0; layerIndex < weights.length; layerIndex++) {
                writer.write("[WeightsLayer" + layerIndex + "]");
                writer.newLine();
                for (double[] row : weights[layerIndex]) {
                    writer.write(joinArray(row, " "));
                    writer.newLine();
                }
                writer.newLine();

                writer.write("[BiasLayer" + layerIndex + "]");
                writer.newLine();
                writer.write(joinArray(biases[layerIndex], " "));
                writer.newLine();
                writer.newLine();
            }

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
     */
    public static NeuralNetworkLoadData loadTrainer(String filePath) throws IOException {
        NeuralNetworkConfig config = null;
        double[][][] weights = null;
        double[][] biases = null;
        int numberOfGamesTrained = 0;
        double lastOutput = 0.0;

        int inputSize = 0;
        int numHiddenLayers = 0;
        List<Integer> hiddenSizes = new ArrayList<>();
        int outputSize = 0;
        ActivationFunction activationFunction = ActivationFunction.TANH;

        int currentLayerIndex = -1;
        List<double[]> currentLayerWeights = new ArrayList<>();
        List<Double> currentLayerBiases = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (line.startsWith("[Configuration]")) {
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("[")) {
                            // Step back so next loop handles bracket
                            break;
                        }
                        String[] parts = line.split("=");
                        if (parts.length != 2) continue;
                        String key = parts[0];
                        String value = parts[1];

                        switch (key) {
                            case "inputSize":
                                inputSize = Integer.parseInt(value);
                                break;
                            case "numHiddenLayers":
                                numHiddenLayers = Integer.parseInt(value);
                                break;
                            case "outputSize":
                                outputSize = Integer.parseInt(value);
                                break;
                            case "activationFunction":
                                activationFunction = ActivationFunction.valueOf(value);
                                break;
                            default:
                                if (key.startsWith("hiddenSize")) {
                                    hiddenSizes.add(Integer.parseInt(value));
                                }
                                break;
                        }
                    }
                    int totalLayers = numHiddenLayers + 1;
                    weights = new double[totalLayers][][];
                    biases = new double[totalLayers][];
                }

                else if (line.startsWith("[WeightsLayer")) {
                    // If we were reading biases for a previous layer, store them
                    if (currentLayerIndex != -1 && !currentLayerBiases.isEmpty()) {
                        biases[currentLayerIndex] = currentLayerBiases
                            .stream().mapToDouble(Double::doubleValue).toArray();
                        currentLayerBiases.clear();
                    }
                    String idxStr = line.substring("[WeightsLayer".length(), line.indexOf("]"));
                    currentLayerIndex = Integer.parseInt(idxStr);
                    currentLayerWeights.clear();
                }

                else if (line.startsWith("[BiasLayer")) {
                    // Store the weights for this layer
                    if (!currentLayerWeights.isEmpty()) {
                        weights[currentLayerIndex] = currentLayerWeights
                            .toArray(new double[0][]);
                        currentLayerWeights.clear();
                    }
                    String idxStr = line.substring("[BiasLayer".length(), line.indexOf("]"));
                    currentLayerIndex = Integer.parseInt(idxStr);
                    currentLayerBiases.clear();
                }

                else if (line.startsWith("[Metadata]")) {
                    // If there's still un-stored biases, store them
                    if (currentLayerIndex >= 0 && !currentLayerBiases.isEmpty()) {
                        biases[currentLayerIndex] = currentLayerBiases
                            .stream().mapToDouble(Double::doubleValue).toArray();
                        currentLayerBiases.clear();
                    }
                    // Now read metadata
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("[")) {
                            break;
                        }
                        String[] parts = line.split("=");
                        if (parts.length != 2) continue;
                        String key = parts[0];
                        String value = parts[1];
                        switch (key) {
                            case "numberOfGamesTrained":
                                numberOfGamesTrained = Integer.parseInt(value);
                                break;
                            case "lastOutput":
                                lastOutput = Double.parseDouble(value);
                                break;
                            default:
                                break;
                        }
                    }
                    break; // done reading file
                }

                else {
                    // We are either reading weights or biases
                    if (weights == null || biases == null) {
                        continue;
                    }
                    // If we haven't set weights[currentLayerIndex] yet, we are reading weight rows
                    if (weights[currentLayerIndex] == null) {
                        String[] tokens = line.split(" ");
                        double[] row = new double[tokens.length];
                        for (int i = 0; i < tokens.length; i++) {
                            row[i] = parseDoubleOrZero(tokens[i]);
                        }
                        currentLayerWeights.add(row);
                    } else {
                        // We must be reading bias line
                        String[] tokens = line.split(" ");
                        for (String tok : tokens) {
                            currentLayerBiases.add(parseDoubleOrZero(tok));
                        }
                    }
                }
            }
        }

        // if last layer's biases never got stored
        if (currentLayerIndex >= 0 && biases != null && !currentLayerBiases.isEmpty()) {
            biases[currentLayerIndex] = currentLayerBiases
                .stream().mapToDouble(Double::doubleValue).toArray();
            currentLayerBiases.clear();
        }

        NeuralNetworkConfig configFinal = new NeuralNetworkConfig(
            inputSize,
            hiddenSizes.stream().mapToInt(i -> i).toArray(),
            outputSize,
            activationFunction
        );
        NeuralNetwork net = new NeuralNetwork(configFinal);
        net.setWeights(weights);
        net.setBiases(biases);

        return new NeuralNetworkLoadData(net, numberOfGamesTrained, lastOutput);
    }

    private static double parseDoubleOrZero(String s) {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private static String joinArray(double[] array, String delimiter) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            sb.append(array[i]);
            if (i < array.length - 1) {
                sb.append(delimiter);
            }
        }
        return sb.toString();
    }

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
