package com.Tak.AI.neuralnet;

import com.Tak.Logic.utils.Logger;

import java.io.IOException;
import java.util.List;

/**
 * <p>A trainer for a multi-layer NeuralNetwork,
 * providing single-sample or batch training, plus save/load methods.</p>
 */
public class NeuralNetworkTrainer {

    private NeuralNetwork network;
    private double learningRate;

    // Metadata
    private int numberOfStatesTrained;
    private double lastOutput;

    public NeuralNetworkTrainer(NeuralNetwork network, double learningRate) {
        this.network = network;
        this.learningRate = learningRate;
        this.numberOfStatesTrained = 0;
        this.lastOutput = 0.0;
    }

    /**
     * Trains the network on a single example.
     */
    public void train(double[] inputs, double[] targets) {
        network.backpropagate(inputs, targets, learningRate);
        numberOfStatesTrained++;
        if (targets.length > 0) {
            lastOutput = targets[0];
        }
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
     * <p>Handles a list of SelfPlayMinimaxTrainer.TrainingExample objects.</p>
     */
    public void trainAll(List<SelfPlayMinimaxTrainer.TrainingExample> gameData) {
        for (SelfPlayMinimaxTrainer.TrainingExample ex : gameData) {
            train(ex.getInput(), ex.getTarget());
        }
    }

    public void saveNetwork(String filePath) {
        try {
            NeuralNetworkTxtManager.saveTrainer(this, filePath, numberOfStatesTrained, lastOutput);
            Logger.log("NeuralNetworkTrainer", "Network and metadata successfully saved to " + filePath);
        } catch (IOException e) {
            Logger.log("NeuralNetworkTrainer", "Failed to save network: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void loadNetwork(String filePath) {
        try {
            NeuralNetworkTxtManager.NeuralNetworkLoadData loadData = NeuralNetworkTxtManager.loadTrainer(filePath);
            this.network = loadData.network;
            this.numberOfStatesTrained = loadData.numberOfGamesTrained;
            this.lastOutput = loadData.lastOutput;
            Logger.log("NeuralNetworkTrainer", "Network and metadata successfully loaded from " + filePath);
        } catch (IOException e) {
            Logger.log("NeuralNetworkTrainer", "Failed to load network: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Getters and Setters
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

    public int getNumberOfStatesTrained() {
        return numberOfStatesTrained;
    }

    public double getLastOutput() {
        return lastOutput;
    }
}
