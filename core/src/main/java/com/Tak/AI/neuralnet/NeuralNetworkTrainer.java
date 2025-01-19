package com.Tak.AI.neuralnet;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Trainer for a multi-layer NeuralNetwork.
 *
 * Key changes:
 *  - Lower default learning rate from 0.01 to 0.001.
 *  - Encourage multiple epochs on each data batch (controlled externally).
 *  - Shuffle data + mini-batch if desired.
 */
public class NeuralNetworkTrainer {

    private NeuralNetwork network;
    private double learningRate;

    private int numberOfStatesTrained;
    private double lastOutput;

    private boolean shuffleData = true;       // By default, shuffle
    private int miniBatchSize = 32;          // Default 32
    private double lrDecayFactor = 1.0;
    private Random rnd = new Random();

    private double regularizationRate = 1e-4; // L2

    public NeuralNetworkTrainer(NeuralNetwork network, double learningRate) {
        this.network = network;
        this.learningRate = learningRate;
        this.numberOfStatesTrained = 0;
        this.lastOutput = 0.0;
    }

    /**
     * Train the network on a single (input, target) pair, returns MSE loss
     */
    public double train(double[] inputs, double[] targets) {
        if (network == null) {
            throw new IllegalStateException("NeuralNetwork is not initialized.");
        }

        double[] outputs = network.forward(inputs);

        // Check dimension
        if (outputs.length != targets.length) {
            System.err.println("Error: Output length (" + outputs.length + ") != target length (" + targets.length + ").");
            throw new IllegalArgumentException("Mismatch between network outputs and targets.");
        }

        // Backprop
        network.backpropagate(inputs, targets, learningRate, regularizationRate);

        // Compute MSE
        double loss = 0.0;
        for (int i = 0; i < targets.length; i++) {
            loss += Math.pow(targets[i] - outputs[i], 2);
        }
        loss /= targets.length;

        numberOfStatesTrained++;
        if (targets.length > 0) {
            // Just record the first dimension for reference
            lastOutput = targets[0];
        }

        return loss;
    }

    /**
     * Train the network on an entire dataset, returning the average loss.
     */
    public double trainAll(List<SelfPlayMinimaxTrainer.TrainingExample> dataset) {
        if (dataset.isEmpty()) return 0.0;

        if (shuffleData) {
            Collections.shuffle(dataset, rnd);
        }

        if (miniBatchSize > 0 && miniBatchSize < dataset.size()) {
            return trainMiniBatches(dataset);
        } else {
            double totalLoss = 0.0;
            for (SelfPlayMinimaxTrainer.TrainingExample ex : dataset) {
                totalLoss += train(ex.getInput(), ex.getTarget());
            }
            return totalLoss / dataset.size();
        }
    }

    private double trainMiniBatches(List<SelfPlayMinimaxTrainer.TrainingExample> dataset) {
        double totalLoss = 0.0;
        int count = 0;

        for (int start = 0; start < dataset.size(); start += miniBatchSize) {
            int end = Math.min(start + miniBatchSize, dataset.size());

            double batchLoss = 0.0;
            for (int i = start; i < end; i++) {
                SelfPlayMinimaxTrainer.TrainingExample ex = dataset.get(i);
                batchLoss += train(ex.getInput(), ex.getTarget());
            }

            batchLoss /= (end - start);
            totalLoss += batchLoss;
            count++;
        }
        return totalLoss / count;
    }

    public void applyLearningRateDecay() {
        if (lrDecayFactor < 1.0 && lrDecayFactor > 0.0) {
            this.learningRate *= lrDecayFactor;
        }
    }

    public void saveNetwork(String filePath) {
        try {
            NeuralNetworkTxtManager.saveTrainer(this, filePath, numberOfStatesTrained, lastOutput);
            System.out.println("Network and metadata successfully saved to " + filePath);
        } catch (IOException e) {
            System.out.println("Failed to save network: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void loadNetwork(String filePath) {
        try {
            NeuralNetworkTxtManager.NeuralNetworkLoadData loadData = NeuralNetworkTxtManager.loadTrainer(filePath);
            this.network = loadData.network;
            this.numberOfStatesTrained = loadData.numberOfGamesTrained;
            this.lastOutput = loadData.lastOutput;
            System.out.println("Network '" + filePath + "' loaded successfully.");
        } catch (IOException e) {
            System.out.println("Failed to load network: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ------------------------------------------------------------------------
    // Getters / Setters
    // ------------------------------------------------------------------------

    public NeuralNetwork getNetwork() {
        return network;
    }

    public double getLearningRate() {
        return learningRate;
    }

    public void setLearningRate(double lr) {
        this.learningRate = lr;
    }

    public int getNumberOfStatesTrained() {
        return numberOfStatesTrained;
    }

    public double getLastOutput() {
        return lastOutput;
    }

    public void setShuffleData(boolean shuffle) {
        this.shuffleData = shuffle;
    }

    public void setMiniBatchSize(int batchSize) {
        this.miniBatchSize = batchSize;
    }

    public void setLearningRateDecay(double factor) {
        this.lrDecayFactor = factor;
    }

    public void setRegularizationRate(double regRate) {
        this.regularizationRate = regRate;
    }
}
