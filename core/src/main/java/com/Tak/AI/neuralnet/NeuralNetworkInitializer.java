package com.Tak.AI.neuralnet;

/**
 * Creates a NeuralNetworkTrainer with a given architecture.
 */
public class NeuralNetworkInitializer {

    /**
     * Explicit version: user-provided hidden layers + activation
     */
    public static NeuralNetworkTrainer initializeTrainer(int[] hiddenLayerSizes, ActivationFunction activationFunction) {
        // We assume the input is 130 (5x5 board with 5 features/cell + 5 global = 130) 
        // and the output is 2. Hidden layer sizes come from the user.
        NeuralNetworkConfig config = NeuralNetworkSetup.createConfig(hiddenLayerSizes, activationFunction);
        NeuralNetwork network = new NeuralNetwork(config);

        // Lower default LR
        double learningRate = 0.001;

        NeuralNetworkTrainer trainer = new NeuralNetworkTrainer(network, learningRate);
        // optionally set other hyperparams
        trainer.setShuffleData(true);
        trainer.setMiniBatchSize(32);
        trainer.setRegularizationRate(1e-4);
        return trainer;
    }

    /**
     * Convenience version: Single hidden layer of size 64, defaulting to TANH.
     */
    public static NeuralNetworkTrainer initializeTrainer() {
        // Single layer of 64 (change to 2 layers if you want)
        return initializeTrainer(new int[]{64}, ActivationFunction.TANH);
    }
}
