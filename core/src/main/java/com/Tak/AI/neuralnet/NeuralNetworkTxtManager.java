package com.Tak.AI.neuralnet;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles saving/loading a NeuralNetworkTrainer to/from a .txt file.
 * 
 * Only recommended for smaller networks. For bigger networks, consider a binary approach.
 */
public class NeuralNetworkTxtManager {

    public static void saveTrainer(NeuralNetworkTrainer trainer,
                                   String filePath,
                                   int numberOfGames,
                                   double lastOutput) throws IOException {
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

            // [Weights] & [Biases]
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

            // [Metadata]
            writer.write("[Metadata]");
            writer.newLine();
            writer.write("numberOfGamesTrained=" + numberOfGames);
            writer.newLine();
            writer.write("lastOutput=" + lastOutput);
            writer.newLine();
        }
    }

    public static NeuralNetworkLoadData loadTrainer(String filePath) throws IOException {
        NeuralNetworkConfig config;
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
                    // parse config lines
                    while ((line = reader.readLine()) != null && !line.startsWith("[")) {
                        if (line == null) break;
                        line = line.trim();
                        if (line.isEmpty()) continue;

                        String[] parts = line.split("=");
                        if (parts.length != 2) continue;

                        String key = parts[0];
                        String val = parts[1];
                        switch (key) {
                            case "inputSize":
                                inputSize = Integer.parseInt(val);
                                break;
                            case "numHiddenLayers":
                                numHiddenLayers = Integer.parseInt(val);
                                break;
                            case "outputSize":
                                outputSize = Integer.parseInt(val);
                                break;
                            case "activationFunction":
                                activationFunction = ActivationFunction.valueOf(val);
                                break;
                            default:
                                if (key.startsWith("hiddenSize")) {
                                    hiddenSizes.add(Integer.parseInt(val));
                                }
                                break;
                        }
                    }

                    int totalLayers = numHiddenLayers + 1;
                    weights = new double[totalLayers][][];
                    biases = new double[totalLayers][];
                }

                if (line == null) break;

                if (line.startsWith("[WeightsLayer")) {
                    if (currentLayerIndex != -1 && !currentLayerWeights.isEmpty()) {
                        weights[currentLayerIndex] =
                            currentLayerWeights.toArray(new double[0][]);
                        currentLayerWeights.clear();
                    }
                    String idxStr = line.substring("[WeightsLayer".length(), line.indexOf("]"));
                    currentLayerIndex = Integer.parseInt(idxStr);
                }
                else if (line.startsWith("[BiasLayer")) {
                    if (currentLayerIndex != -1 && !currentLayerWeights.isEmpty()) {
                        weights[currentLayerIndex] =
                            currentLayerWeights.toArray(new double[0][]);
                        currentLayerWeights.clear();
                    }
                    String idxStr = line.substring("[BiasLayer".length(), line.indexOf("]"));
                    currentLayerIndex = Integer.parseInt(idxStr);

                    // read the next line of biases
                    line = reader.readLine();
                    if (line != null) {
                        line = line.trim();
                        String[] tokens = line.split(" ");
                        currentLayerBiases.clear();
                        for (String tok : tokens) {
                            currentLayerBiases.add(Double.parseDouble(tok));
                        }
                        biases[currentLayerIndex] = currentLayerBiases.stream().mapToDouble(Double::doubleValue).toArray();
                    }
                }
                else if (line.startsWith("[Metadata]")) {
                    if (currentLayerIndex != -1 && !currentLayerWeights.isEmpty()) {
                        weights[currentLayerIndex] =
                            currentLayerWeights.toArray(new double[0][]);
                        currentLayerWeights.clear();
                    }
                    // read metadata lines
                    while ((line = reader.readLine()) != null && !line.startsWith("[")) {
                        line = line.trim();
                        if (line.isEmpty()) continue;
                        String[] parts = line.split("=");
                        if (parts.length == 2) {
                            String k = parts[0];
                            String v = parts[1];
                            if (k.equals("numberOfGamesTrained")) {
                                numberOfGamesTrained = Integer.parseInt(v);
                            } else if (k.equals("lastOutput")) {
                                lastOutput = Double.parseDouble(v);
                            }
                        }
                    }
                    // done reading
                    break;
                }
                else {
                    // parse weight row
                    String[] tokens = line.split(" ");
                    double[] row = new double[tokens.length];
                    for (int i = 0; i < tokens.length; i++) {
                        row[i] = Double.parseDouble(tokens[i]);
                    }
                    currentLayerWeights.add(row);
                }
            }
        }

        // Assign last layer weights if needed
        if (currentLayerIndex != -1 && !currentLayerWeights.isEmpty()) {
            weights[currentLayerIndex] = currentLayerWeights.toArray(new double[0][]);
        }

        NeuralNetworkConfig cfg = new NeuralNetworkConfig(
            inputSize,
            hiddenSizes.stream().mapToInt(i -> i).toArray(),
            outputSize,
            activationFunction
        );
        NeuralNetwork net = new NeuralNetwork(cfg);
        net.setWeights(weights);
        net.setBiases(biases);

        // Validate dimensions if needed

        return new NeuralNetworkLoadData(net, numberOfGamesTrained, lastOutput);
    }

    private static String joinArray(double[] array, String delimiter) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            sb.append(array[i]);
            if (i < array.length - 1) sb.append(delimiter);
        }
        return sb.toString();
    }

    public static class NeuralNetworkLoadData {
        public NeuralNetwork network;
        public int numberOfGamesTrained;
        public double lastOutput;

        public NeuralNetworkLoadData(NeuralNetwork network, int numberOfGamesTrained, double lastOutput) {
            this.network = network;
            this.numberOfGamesTrained = numberOfGamesTrained;
            this.lastOutput = lastOutput;
        }
    }
}
