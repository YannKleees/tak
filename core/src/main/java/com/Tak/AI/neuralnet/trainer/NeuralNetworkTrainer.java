package com.Tak.AI.neuralnet.trainer;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.Tak.AI.neuralnet.net.NeuralNetwork;
import com.Tak.AI.neuralnet.net.NeuralNetworkTxtManager;
import com.Tak.AI.neuralnet.net.NeuralNetworkTxtManager.NeuralNetworkLoadData;
import com.Tak.AI.neuralnet.trainer.SelfPlayMinimaxTrainer.TrainingExample;

/**
 * Trainer for a multi-layer NeuralNetwork.
 *
 * Features:
 *  - momentum
 *  - two-phase learning rates: inGameRate, afterGameRate
 *  - partial "trainSingleSampleWithInGameRate(...)"
 */
public class NeuralNetworkTrainer {

    private NeuralNetwork network;
    // "standard" global learningRate if you do normal trainAll calls
    private double learningRate;

    // UPDATED: default momentum set to 0.9 (you can tweak)
    private double momentum = 0.9;

    // separate "in-game" vs. "after-game" rates
    private double inGameRate = 0.001;
    private double afterGameRate = 0.01;

    private int numberOfStatesTrained;
    private double lastOutput;

    private boolean shuffleData = true;
    private int miniBatchSize = 32;
    private double lrDecayFactor = 1.0;
    private Random rnd = new Random();
    private double regularizationRate = 1e-4;

    public NeuralNetworkTrainer(NeuralNetwork net, double baseLearningRate) {
        this.network = net;
        this.learningRate = baseLearningRate;
        this.numberOfStatesTrained = 0;
        this.lastOutput = 0.0;
        // If you prefer to *not* do momentum by default, set momentum=0.0 here.
        // But typically 0.9 is a good starting point.
    }

    /**
     * Train on a single input -> target. Return MSE loss.
     */
    public double train(double[] inputs, double[] targets) {
        double[] outputs = network.forward(inputs);
        if (outputs.length != targets.length) {
            throw new IllegalArgumentException("Mismatch output/target length");
        }

        network.backpropagate(
            inputs, 
            targets,
            learningRate, 
            regularizationRate, 
            momentum
        );

        // MSE
        double loss = 0;
        for (int i = 0; i < targets.length; i++) {
            loss += Math.pow(targets[i] - outputs[i], 2);
        }
        loss /= targets.length;

        numberOfStatesTrained++;
        if (targets.length > 0) {
            lastOutput = targets[0];
        }
        return loss;
    }

    /**
     * A convenience for partial updates mid-game using inGameRate.
     * This forcibly sets learningRate = inGameRate, calls train(...),
     * then reverts to the old learningRate.
     */
    public double trainSingleSampleWithInGameRate(double[] inputs, double[] targets) {
        double oldLR = this.learningRate;
        this.learningRate = inGameRate;
        double loss = train(inputs, targets);
        this.learningRate = oldLR;
        return loss;
    }

    // For final big correction or normal training on a batch
    public double trainAll(List<SelfPlayMinimaxTrainer.TrainingExample> data) {
        if (data.isEmpty()) return 0.0;
        if (shuffleData) {
            Collections.shuffle(data, rnd);
        }

        if (miniBatchSize > 0 && miniBatchSize < data.size()) {
            return trainMiniBatches(data);
        } else {
            double total = 0;
            for (SelfPlayMinimaxTrainer.TrainingExample ex : data) {
                total += train(ex.getInput(), ex.getTarget());
            }
            return total / data.size();
        }
    }

    private double trainMiniBatches(List<SelfPlayMinimaxTrainer.TrainingExample> data) {
        double totalLoss = 0.0;
        int count = 0;

        for (int start = 0; start < data.size(); start += miniBatchSize) {
            int end = Math.min(start + miniBatchSize, data.size());
            double batchLoss = 0.0;
            for (int i = start; i < end; i++) {
                batchLoss += train(data.get(i).getInput(), data.get(i).getTarget());
            }
            batchLoss /= (end - start);
            totalLoss += batchLoss;
            count++;
        }
        return totalLoss / count;
    }

    // Basic save/load
    public void saveNetwork(String filePath) {
        try {
            NeuralNetworkTxtManager.saveTrainer(this, filePath, numberOfStatesTrained, lastOutput);
            System.out.println("Network + metadata saved to " + filePath);
        } catch (IOException e) {
            System.err.println("Failed to save network: " + e.getMessage());
        }
    }

    public void loadNetwork(String filePath) {
        try {
            NeuralNetworkTxtManager.NeuralNetworkLoadData d 
                = NeuralNetworkTxtManager.loadTrainer(filePath);
            this.network = d.network;
            this.numberOfStatesTrained = d.numberOfGamesTrained;
            this.lastOutput = d.lastOutput;
            System.out.println("Loaded network from " + filePath);
        } catch (IOException e) {
            System.err.println("Failed to load: " + e.getMessage());
        }
    }

    // getters / setters

    public NeuralNetwork getNetwork() { return network; }

    public double getLearningRate() { return learningRate; }
    public void setLearningRate(double lr) { this.learningRate = lr; }

    public double getMomentum() { return momentum; }
    public void setMomentum(double m) { this.momentum = m; }

    public double getInGameRate() { return inGameRate; }
    public void setInGameRate(double igr) { this.inGameRate = igr; }

    public double getAfterGameRate() { return afterGameRate; }
    public void setAfterGameRate(double agr) { this.afterGameRate = agr; }

    public int getNumberOfStatesTrained() { return numberOfStatesTrained; }
    public double getLastOutput() { return lastOutput; }

    public void setShuffleData(boolean s) { this.shuffleData = s; }
    public void setMiniBatchSize(int b) { this.miniBatchSize = b; }
    public void setLearningRateDecay(double f) { this.lrDecayFactor = f; }
    public void setRegularizationRate(double r) { this.regularizationRate = r; }

    public void applyLearningRateDecay() {
        if (lrDecayFactor < 1.0 && lrDecayFactor > 0.0) {
            this.learningRate *= lrDecayFactor;
        }
    }
}
