package com.Tak.AI.neuralnet;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>A trainer for the NeuralNetwork, providing methods for
 * single-sample or batch training, also used by SelfPlayMinimaxTrainer.</p>
 *
 * <p><strong>SRP:</strong> Focuses on orchestrating weight updates by calling
 * {@link NeuralNetwork#backpropagate(double[], double[], double)} 
 * for each training example or batch.</p>
 */
public class NeuralNetworkTrainer implements Serializable {
    private static final long serialVersionUID = 1L;

    private NeuralNetwork network;
    private double learningRate;

    /**
     * <p>Constructs a NeuralNetworkTrainer with the given network and learning rate.</p>
     *
     * @param network      The neural network to be trained.
     * @param learningRate The rate at which weights should be updated during backprop.
     */
    public NeuralNetworkTrainer(NeuralNetwork network, double learningRate) {
        this.network = network;
        this.learningRate = learningRate;
    }

    /**
     * <p>Trains the network on a single example (inputs -> target).</p>
     *
     * @param inputs  The input array representing a single board state or scenario.
     * @param targets The desired output array (e.g. [1.0] for a winning state).
     */
    public void train(double[] inputs, double[] targets) {
        network.backpropagate(inputs, targets, learningRate);
        // Optionally, track error or logs here
    }

    /**
     * <p>Trains the network on a batch of examples in one pass.</p>
     *
     * @param inputBatch  A list of input arrays (multiple states).
     * @param targetBatch A list of target output arrays (same size as inputBatch).
     */
    public void trainBatch(List<double[]> inputBatch, List<double[]> targetBatch) {
        for (int i = 0; i < inputBatch.size(); i++) {
            train(inputBatch.get(i), targetBatch.get(i));
        }
    }

    /**
     * <p>(Optional) Additional convenience method for training data from entire game
     * if you store them in a wrapper class.</p>
     *
     * @param gameData A list of training examples from a single game or multiple games.
     */
    public void trainAll(List<SelfPlayMinimaxTrainer.TrainingExample> gameData) {
        for (SelfPlayMinimaxTrainer.TrainingExample ex : gameData) {
            train(ex.getInput(), ex.getTarget());
        }
    }

    /**
     * <p>Saves the trained network to a structured text file for later reuse.</p>
     */
    public void saveNetwork(String filePath) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            NeuralNetworkConfig cfg = network.getConfig();
            
            // Write Configuration
            writer.println("[Configuration]");
            writer.println("inputSize=" + cfg.getInputSize());
            writer.println("hiddenSize=" + cfg.getHiddenSize());
            writer.println("outputSize=" + cfg.getOutputSize());
            writer.println("activationFunction=" + cfg.getActivationFunction());
            writer.println();

            // Write Weights Input to Hidden
            writer.println("[WeightsInputHidden]");
            double[][] wIH = network.getWeightsInputHidden();
            for (double[] row : wIH) {
                writer.println(joinArray(row, " "));
            }
            writer.println();

            // Write Weights Hidden to Output
            writer.println("[WeightsHiddenOutput]");
            double[][] wHO = network.getWeightsHiddenOutput();
            for (double[] row : wHO) {
                writer.println(joinArray(row, " "));
            }
            writer.println();

            // Write Biases Hidden
            writer.println("[BiasHidden]");
            double[] biasHidden = network.getBiasHidden();
            writer.println(joinArray(biasHidden, " "));
            writer.println();

            // Write Biases Output
            writer.println("[BiasOutput]");
            double[] biasOutput = network.getBiasOutput();
            writer.println(joinArray(biasOutput, " "));
            writer.println();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * <p>Loads a previously saved network from a structured text file.</p>
     */
    public void loadNetwork(String filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            NeuralNetworkConfig newCfg = null;
            double[][] wIH = null;
            double[][] wHO = null;
            double[] biasHidden = null;
            double[] biasOutput = null;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (line.startsWith("[Configuration]")) {
                    // Read configuration
                    int inputSize = 0, hiddenSize = 0, outputSize = 0;
                    ActivationFunction actFunc = ActivationFunction.TANH; // default
                    for (int i = 0; i < 4; i++) { // expecting four config lines
                        line = reader.readLine();
                        if (line == null) throw new IOException("Incomplete configuration section.");
                        line = line.trim();
                        String[] parts = line.split("=");
                        if (parts.length != 2) throw new IOException("Invalid configuration line: " + line);
                        switch (parts[0]) {
                            case "inputSize":
                                inputSize = Integer.parseInt(parts[1]);
                                break;
                            case "hiddenSize":
                                hiddenSize = Integer.parseInt(parts[1]);
                                break;
                            case "outputSize":
                                outputSize = Integer.parseInt(parts[1]);
                                break;
                            case "activationFunction":
                                actFunc = ActivationFunction.valueOf(parts[1]);
                                break;
                            default:
                                throw new IOException("Unknown configuration parameter: " + parts[0]);
                        }
                    }
                    newCfg = new NeuralNetworkConfig(inputSize, hiddenSize, outputSize, actFunc);
                    network = new NeuralNetwork(newCfg);
                } else if (line.startsWith("[WeightsInputHidden]")) {
                    // Read Weights Input to Hidden
                    wIH = new double[network.getConfig().getInputSize()][network.getConfig().getHiddenSize()];
                    for (int i = 0; i < network.getConfig().getInputSize(); i++) {
                        line = reader.readLine();
                        if (line == null) throw new IOException("Unexpected end of file in WeightsInputHidden.");
                        String[] tokens = line.trim().split(" ");
                        if (tokens.length != network.getConfig().getHiddenSize()) {
                            throw new IOException("Invalid number of weights in WeightsInputHidden row " + i);
                        }
                        for (int j = 0; j < tokens.length; j++) {
                            wIH[i][j] = Double.parseDouble(tokens[j]);
                        }
                    }
                    network.setWeightsInputHidden(wIH);
                } else if (line.startsWith("[WeightsHiddenOutput]")) {
                    // Read Weights Hidden to Output
                    wHO = new double[network.getConfig().getHiddenSize()][network.getConfig().getOutputSize()];
                    for (int i = 0; i < network.getConfig().getHiddenSize(); i++) {
                        line = reader.readLine();
                        if (line == null) throw new IOException("Unexpected end of file in WeightsHiddenOutput.");
                        String[] tokens = line.trim().split(" ");
                        if (tokens.length != network.getConfig().getOutputSize()) {
                            throw new IOException("Invalid number of weights in WeightsHiddenOutput row " + i);
                        }
                        for (int j = 0; j < tokens.length; j++) {
                            wHO[i][j] = Double.parseDouble(tokens[j]);
                        }
                    }
                    network.setWeightsHiddenOutput(wHO);
                } else if (line.startsWith("[BiasHidden]")) {
                    // Read Biases Hidden
                    line = reader.readLine();
                    if (line == null) throw new IOException("Unexpected end of file in BiasHidden.");
                    String[] tokens = line.trim().split(" ");
                    if (tokens.length != network.getConfig().getHiddenSize()) {
                        throw new IOException("Invalid number of biases in BiasHidden.");
                    }
                    biasHidden = new double[network.getConfig().getHiddenSize()];
                    for (int i = 0; i < tokens.length; i++) {
                        biasHidden[i] = Double.parseDouble(tokens[i]);
                    }
                    network.setBiasHidden(biasHidden);
                } else if (line.startsWith("[BiasOutput]")) {
                    // Read Biases Output
                    line = reader.readLine();
                    if (line == null) throw new IOException("Unexpected end of file in BiasOutput.");
                    String[] tokens = line.trim().split(" ");
                    if (tokens.length != network.getConfig().getOutputSize()) {
                        throw new IOException("Invalid number of biases in BiasOutput.");
                    }
                    biasOutput = new double[network.getConfig().getOutputSize()];
                    for (int i = 0; i < tokens.length; i++) {
                        biasOutput[i] = Double.parseDouble(tokens[i]);
                    }
                    network.setBiasOutput(biasOutput);
                } else {
                    // Unknown section
                    throw new IOException("Unknown section header: " + line);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Helper method to join a double array into a single string with a given delimiter.
     *
     * @param array     The array to join.
     * @param delimiter The delimiter between elements.
     * @return A string representing the joined array.
     */
    private String joinArray(double[] array, String delimiter) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            sb.append(array[i]);
            if (i < array.length - 1) sb.append(delimiter);
        }
        return sb.toString();
    }


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
