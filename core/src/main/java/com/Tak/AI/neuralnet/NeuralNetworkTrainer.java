package com.Tak.AI.neuralnet;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>A trainer for a multi-layer NeuralNetwork, 
 * providing single-sample or batch training, plus save/load methods.</p>
 */
public class NeuralNetworkTrainer implements Serializable {
    private static final long serialVersionUID = 1L;

    private NeuralNetwork network;
    private double learningRate;

    public NeuralNetworkTrainer(NeuralNetwork network, double learningRate) {
        this.network = network;
        this.learningRate = learningRate;
    }

    /**
     * Trains the network on a single example.
     */
    public void train(double[] inputs, double[] targets) {
        network.backpropagate(inputs, targets, learningRate);
    }

    /**
     * Trains on a batch of examples.
     */
    public void trainBatch(List<double[]> inputBatch, List<double[]> targetBatch) {
        for (int i = 0; i < inputBatch.size(); i++) {
            train(inputBatch.get(i), targetBatch.get(i));
        }
    }

    /**
     * <p>**Reintroduced** convenience method to handle a list of
     * SelfPlayMinimaxTrainer.TrainingExample objects (if your code calls it).</p>
     */
    public void trainAll(List<SelfPlayMinimaxTrainer.TrainingExample> gameData) {
        for (SelfPlayMinimaxTrainer.TrainingExample ex : gameData) {
            train(ex.getInput(), ex.getTarget());
        }
    }

    /**
     * Saves the network to a structured text file.
     */
    public void saveNetwork(String filePath) {
        NeuralNetworkConfig cfg = network.getConfig();
        double[][][] w = network.getWeights();
        double[][] b = network.getBiases();

        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            // [Configuration]
            writer.println("[Configuration]");
            writer.println("inputSize=" + cfg.getInputSize());
            writer.println("numHiddenLayers=" + cfg.getHiddenLayerSizes().length);

            // hiddenSizes
            for (int i = 0; i < cfg.getHiddenLayerSizes().length; i++) {
                writer.println("hiddenSize" + i + "=" + cfg.getHiddenLayerSizes()[i]);
            }

            writer.println("outputSize=" + cfg.getOutputSize());
            writer.println("activationFunction=" + cfg.getActivationFunction());
            writer.println();

            // Weights/Biases
            for (int layerIndex = 0; layerIndex < w.length; layerIndex++) {
                writer.println("[WeightsLayer" + layerIndex + "]");
                double[][] layerWeights = w[layerIndex];
                for (double[] row : layerWeights) {
                    writer.println(joinArray(row, " "));
                }
                writer.println();

                writer.println("[BiasLayer" + layerIndex + "]");
                double[] layerBias = b[layerIndex];
                writer.println(joinArray(layerBias, " "));
                writer.println();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads a network from a structured text file.
     */
    public void loadNetwork(String filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            int inputSize = 0;
            int numHiddenLayers = 0;
            List<Integer> hiddenSizes = new ArrayList<>();
            int outputSize = 0;
            ActivationFunction actFunc = ActivationFunction.TANH; // default

            double[][][] loadedWeights = null;
            double[][] loadedBiases = null;

            boolean configRead = false;
            int layerCount = 0;
            int currentWeightLayer = -1;
            int rowRead = 0;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                if (line.startsWith("[Configuration]")) {
                    while (true) {
                        reader.mark(10000);
                        String cLine = reader.readLine();
                        if (cLine == null || cLine.trim().isEmpty() || cLine.trim().startsWith("[")) {
                            reader.reset();
                            break;
                        }
                        cLine = cLine.trim();
                        String[] parts = cLine.split("=");
                        if (parts.length != 2) throw new IOException("Invalid config line: " + cLine);

                        switch (parts[0]) {
                            case "inputSize":
                                inputSize = Integer.parseInt(parts[1]);
                                break;
                            case "numHiddenLayers":
                                numHiddenLayers = Integer.parseInt(parts[1]);
                                break;
                            case "outputSize":
                                outputSize = Integer.parseInt(parts[1]);
                                break;
                            case "activationFunction":
                                actFunc = ActivationFunction.valueOf(parts[1]);
                                break;
                            default:
                                if (parts[0].startsWith("hiddenSize")) {
                                    hiddenSizes.add(Integer.parseInt(parts[1]));
                                } else {
                                    throw new IOException("Unknown config parameter: " + parts[0]);
                                }
                                break;
                        }
                    }
                    layerCount = numHiddenLayers + 1;
                    loadedWeights = new double[layerCount][][];
                    loadedBiases = new double[layerCount][];
                    configRead = true;
                }
                else if (line.startsWith("[WeightsLayer")) {
                    if (!configRead) {
                        throw new IOException("Encountered WeightsLayer before reading [Configuration].");
                    }
                    String idxStr = line.substring("[WeightsLayer".length(), line.indexOf("]"));
                    currentWeightLayer = Integer.parseInt(idxStr);

                    int rows = 0, cols = 0;
                    if (currentWeightLayer == 0) {
                        rows = inputSize;
                        cols = hiddenSizes.get(0);
                    } else if (currentWeightLayer == layerCount - 1) {
                        rows = hiddenSizes.get(hiddenSizes.size() - 1);
                        cols = outputSize;
                    } else {
                        rows = hiddenSizes.get(currentWeightLayer - 1);
                        cols = hiddenSizes.get(currentWeightLayer);
                    }
                    loadedWeights[currentWeightLayer] = new double[rows][cols];

                    rowRead = 0;
                    while (rowRead < rows) {
                        reader.mark(5000);
                        String wLine = reader.readLine();
                        if (wLine == null || wLine.trim().isEmpty() || wLine.trim().startsWith("[")) {
                            reader.reset();
                            break;
                        }
                        String[] tokens = wLine.trim().split(" ");
                        if (tokens.length != cols) {
                            throw new IOException("Invalid number of columns for WeightsLayer " + currentWeightLayer);
                        }
                        for (int c = 0; c < cols; c++) {
                            loadedWeights[currentWeightLayer][rowRead][c] = Double.parseDouble(tokens[c]);
                        }
                        rowRead++;
                    }
                }
                else if (line.startsWith("[BiasLayer")) {
                    if (!configRead) {
                        throw new IOException("Encountered BiasLayer before reading [Configuration].");
                    }
                    String idxStr = line.substring("[BiasLayer".length(), line.indexOf("]"));
                    currentWeightLayer = Integer.parseInt(idxStr);

                    int size = (currentWeightLayer == layerCount - 1)
                            ? outputSize
                            : hiddenSizes.get(currentWeightLayer);

                    loadedBiases[currentWeightLayer] = new double[size];
                    String bLine = reader.readLine();
                    if (bLine == null) {
                        throw new IOException("Missing biases line for layer " + currentWeightLayer);
                    }
                    String[] tokens = bLine.trim().split(" ");
                    if (tokens.length != size) {
                        throw new IOException("Invalid number of biases in layer " + currentWeightLayer);
                    }
                    for (int i = 0; i < size; i++) {
                        loadedBiases[currentWeightLayer][i] = Double.parseDouble(tokens[i]);
                    }
                }
            }

            // Rebuild network
            NeuralNetworkConfig newCfg = new NeuralNetworkConfig(
                    inputSize,
                    hiddenSizes.stream().mapToInt(Integer::intValue).toArray(),
                    outputSize,
                    actFunc
            );
            this.network = new NeuralNetwork(newCfg);
            this.network.setWeights(loadedWeights);
            this.network.setBiases(loadedBiases);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String joinArray(double[] array, String delimiter) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            sb.append(array[i]);
            if (i < array.length - 1) {
                sb.append(delimiter);
            }
        }
        return sb.toString();
    }

    // ----------- GETTERS / SETTERS -----------
    public NeuralNetwork getNetwork() {
        return network;
    }

    public double getLearningRate() {
        return learningRate;
    }

    public void setLearningRate(double lr) {
        this.learningRate = lr;
    }

    public void decayLearningRate(double factor) {
        this.learningRate *= factor;
    }
}
