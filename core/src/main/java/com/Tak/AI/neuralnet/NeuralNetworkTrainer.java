package com.Tak.AI.neuralnet;

import java.io.IOException;
import com.Tak.AI.neuralnet.NeuralNetworkTxtManager;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * A trainer for a multi-layer NeuralNetwork,
 * providing single/batch training, plus optional mini-batching, shuffling,
 * learning rate decay, and save/load methods.
 */
public class NeuralNetworkTrainer {

    private NeuralNetwork network;
    private double learningRate;

    // Metadata
    private int numberOfStatesTrained;
    private double lastOutput;

    // (New) Options for advanced features
    private boolean shuffleData = false;    // If true, shuffle data in trainAll
    private int miniBatchSize = 0;         // If > 0, do mini-batches
    private double lrDecayFactor = 1.0;    // e.g. 0.99 each epoch, or 1.0 to disable
    private Random rnd = new Random();

    // (New) L2 regularization parameter
    private double regularizationRate = 0.0;

    public NeuralNetworkTrainer(NeuralNetwork network, double learningRate) {
        this.network = network;
        this.learningRate = learningRate;
        this.numberOfStatesTrained = 0;
        this.lastOutput = 0.0;
    }

    /**
     * Trains the network on a single example and returns the loss.
     */
    public double train(double[] inputs, double[] targets) {
        if (network == null) {
            throw new IllegalStateException("NeuralNetwork is not initialized.");
        }

        // Perform backprop with given regularization
        network.backpropagate(inputs, targets, learningRate, regularizationRate);

        // Calculate MSE loss
        double[] outputs = network.forward(inputs);
        double loss = 0.0;
        for (int i = 0; i < targets.length; i++) {
            loss += Math.pow(targets[i] - outputs[i], 2);
        }
        loss /= targets.length;

        // Update metadata
        numberOfStatesTrained++;
        if (targets.length > 0) {
            lastOutput = targets[0];
        }

        return loss;
    }

    /**
     * (New) Shuffle and optionally do mini-batches. Return average loss.
     */
    public double trainAll(List<SelfPlayMinimaxTrainer.TrainingExample> dataset) {
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

    /**
     * (New) Perform mini-batch training on the dataset.
     */
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

    public void trainBatch(List<double[]> inputBatch, List<double[]> targetBatch) {
        for (int i = 0; i < inputBatch.size(); i++) {
            train(inputBatch.get(i), targetBatch.get(i));
        }
    }

    /**
     * (New) Apply learning rate decay after each epoch (call once per epoch).
     */
    public void applyLearningRateDecay() {
        if (lrDecayFactor < 1.0 && lrDecayFactor > 0.0) {
            this.learningRate *= lrDecayFactor;
        }
    }

    /**
     * Saves the network and metadata to a .txt file.
     */
    public void saveNetwork(String filePath) {
        try {
            NeuralNetworkTxtManager.saveTrainer(this, filePath, numberOfStatesTrained, lastOutput);
            System.out.println("Network and metadata successfully saved to " + filePath);
        } catch (IOException e) {
            System.out.println("Failed to save network: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Loads the network and metadata from a .txt file.
     */
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

    // -------------------- Getters / Setters / Misc. --------------------

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
