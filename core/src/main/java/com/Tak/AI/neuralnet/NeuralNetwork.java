package com.Tak.AI.neuralnet;

import java.io.Serializable;

/**
 * <p>Represents a simple feedforward neural network with one hidden layer.</p>
 *
 * <p><strong>SRP:</strong> Focuses solely on the structure and operations of the neural network,
 * including forward passes and backpropagation.</p>
 */
public class NeuralNetwork implements Serializable {
    private static final long serialVersionUID = 1L;

    private NeuralNetworkConfig config;
    private double[][] weightsInputHidden;
    private double[][] weightsHiddenOutput;
    private double[] biasHidden;
    private double[] biasOutput;

    /**
     * <p>Constructs a NeuralNetwork with the specified config,
     * and initializes the weights and biases randomly by default.</p>
     *
     * @param config A NeuralNetworkConfig object containing structure & activation details.
     */
    public NeuralNetwork(NeuralNetworkConfig config) {
        this.config = config;
        initializeWeights();
    }

    /**
     * <p>Initializes the weight matrices and biases using {@link WeightInitializer}.</p>
     */
    private void initializeWeights() {
        this.weightsInputHidden = WeightInitializer.initializeRandom(
                config.getInputSize(), config.getHiddenSize()
        );
        this.weightsHiddenOutput = WeightInitializer.initializeRandom(
                config.getHiddenSize(), config.getOutputSize()
        );
        this.biasHidden = WeightInitializer.initializeBias(config.getHiddenSize());
        this.biasOutput = WeightInitializer.initializeBias(config.getOutputSize());
    }

    /**
     * <p>Performs a forward pass through the neural network.</p>
     *
     * @param inputs An array containing input values.
     * @return The output array from the network (length = outputSize).
     */
    public double[] forward(double[] inputs) {
        return ForwardPropagator.forward(
                inputs,
                weightsInputHidden,
                weightsHiddenOutput,
                biasHidden,
                biasOutput,
                config.getActivationFunction()
        );
    }

    /**
     * <p>Performs backpropagation and updates the network weights and biases.</p>
     *
     * @param inputs       The input array.
     * @param targets      The target output array (desired).
     * @param learningRate The learning rate for weight updates.
     */
    public void backpropagate(double[] inputs, double[] targets, double learningRate) {
        // 1) Get output by forward pass
        double[] outputs = forward(inputs);

        // 2) Compute hidden activations again for backprop (or we could do a forwardWithIntermediate)
        double[] hiddenActivations = new double[config.getHiddenSize()];
        for (int h = 0; h < config.getHiddenSize(); h++) {
            double sum = 0.0;
            for (int i = 0; i < config.getInputSize(); i++) {
                sum += inputs[i] * weightsInputHidden[i][h];
            }
            sum += biasHidden[h];
            hiddenActivations[h] = ForwardPropagator.applyActivation(sum, config.getActivationFunction());
        }

        // 3) Call BackPropagator with biases
        BackPropagator.backpropagate(
            inputs,
            targets,
            learningRate,
            weightsInputHidden,
            weightsHiddenOutput,
            biasHidden,
            biasOutput,
            hiddenActivations,
            outputs,
            config.getActivationFunction()
        );
    }

    /**
     * <p>Returns both hidden and output activations for debugging or a specialized backprop routine.</p>
     *
     * @param inputs The input array to feed forward.
     * @return A {@link ForwardPropagator.ForwardResults} containing arrays for hidden & output.
     */
    public ForwardPropagator.ForwardResults forwardWithIntermediate(double[] inputs) {
        // Manually compute hidden & output
        int hiddenSize = config.getHiddenSize();
        double[] hiddenActivations = new double[hiddenSize];
        for (int h = 0; h < hiddenSize; h++) {
            double sum = 0.0;
            for (int i = 0; i < config.getInputSize(); i++) {
                sum += inputs[i] * weightsInputHidden[i][h];
            }
            sum += biasHidden[h];
            hiddenActivations[h] = ForwardPropagator.applyActivation(sum, config.getActivationFunction());
        }

        int outputSize = config.getOutputSize();
        double[] outputActivations = new double[outputSize];
        for (int o = 0; o < outputSize; o++) {
            double sum = 0.0;
            for (int h2 = 0; h2 < hiddenSize; h2++) {
                sum += hiddenActivations[h2] * weightsHiddenOutput[h2][o];
            }
            sum += biasOutput[o];
            outputActivations[o] = ForwardPropagator.applyActivation(sum, config.getActivationFunction());
        }

        return new ForwardPropagator.ForwardResults(hiddenActivations, outputActivations);
    }

    // --------------------- Getters / Setters ---------------------

    public NeuralNetworkConfig getConfig() {
        return config;
    }

    public double[][] getWeightsInputHidden() {
        return weightsInputHidden;
    }

    public double[][] getWeightsHiddenOutput() {
        return weightsHiddenOutput;
    }

    public double[] getBiasHidden() {
        return biasHidden;
    }

    public double[] getBiasOutput() {
        return biasOutput;
    }

    public void setWeightsInputHidden(double[][] newWeights) {
        if (newWeights.length != config.getInputSize() || newWeights[0].length != config.getHiddenSize()) {
            throw new IllegalArgumentException("Invalid shape for weightsInputHidden.");
        }
        this.weightsInputHidden = newWeights;
    }

    public void setWeightsHiddenOutput(double[][] newWeights) {
        if (newWeights.length != config.getHiddenSize() || newWeights[0].length != config.getOutputSize()) {
            throw new IllegalArgumentException("Invalid shape for weightsHiddenOutput.");
        }
        this.weightsHiddenOutput = newWeights;
    }

    public void setBiasHidden(double[] newBias) {
        if (newBias.length != config.getHiddenSize()) {
            throw new IllegalArgumentException("Invalid biasHidden dimension.");
        }
        this.biasHidden = newBias;
    }

    public void setBiasOutput(double[] newBias) {
        if (newBias.length != config.getOutputSize()) {
            throw new IllegalArgumentException("Invalid biasOutput dimension.");
        }
        this.biasOutput = newBias;
    }
}
