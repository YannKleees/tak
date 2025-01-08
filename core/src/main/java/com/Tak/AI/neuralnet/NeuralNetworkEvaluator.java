package com.Tak.AI.neuralnet;

import com.Tak.AI.evaluation.IEvaluationFunction;
import com.Tak.Logic.models.Board;
import com.Tak.Logic.models.Player;

import java.io.Serializable;

/**
 * An evaluator that uses a NeuralNetwork to score the board state.
 * It implements IEvaluationFunction, so it can be plugged into Minimax or other search algorithms.
 * 
 * <p><strong>SRP:</strong> This class only bridges the Tak board with the neural net's forward pass.
 */
public class NeuralNetworkEvaluator implements IEvaluationFunction, Serializable {
    private static final long serialVersionUID = 1L;

    private NeuralNetwork network;

    /**
     * Constructs a NeuralNetworkEvaluator with a given neural network.
     *
     * @param network The NeuralNetwork instance to use for evaluation.
     */
    public NeuralNetworkEvaluator(NeuralNetwork network) {
        this.network = network;
    }

    /**
     * Evaluates the given board state for the specified player using the neural network.
     *
     * @param board  The current Tak game board.
     * @param player The player for whom the evaluation is being performed.
     * @return A numerical score from the perspective of 'player'.
     */
    @Override
    public double evaluate(Board board, Player player) {
        // Convert the board state to an input array
        double[] inputs = BoardToInputsConverter.convert(board, player);

        // Run the network's forward pass
        double[] outputs = network.forward(inputs);

        // If multiple outputs, interpret them. Here we just assume output[0] is the final score
        return (outputs.length > 0) ? outputs[0] : 0.0;
    }

    /**
     * (Optional) For IEvaluationFunction usage:
     * Determine how deep the AI search should go based on board size or complexity.
     *
     * @param size The size of the board (e.g., 5 for a 5x5).
     * @return The recommended search depth for Minimax or other search algorithms.
     */
    public int depthFromBoardSize(int size) {
        // TODO: Possibly base it on size (larger board => smaller depth or some heuristic).
        return 3; // placeholder
    }
}
