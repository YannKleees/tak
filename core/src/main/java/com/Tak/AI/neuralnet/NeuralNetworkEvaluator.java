package com.Tak.AI.neuralnet;

import com.Tak.AI.evaluation.IEvaluationFunction;
import com.Tak.Logic.models.Board;
import com.Tak.Logic.models.Player;

import java.io.Serializable;

/**
 * An evaluator that uses a NeuralNetwork to score the board state.
 * Implements IEvaluationFunction so it can be used in Minimax or other search algorithms.
 */
public class NeuralNetworkEvaluator implements IEvaluationFunction, Serializable {
    private static final long serialVersionUID = 1L;

    private NeuralNetwork network;

    public NeuralNetworkEvaluator(NeuralNetwork network) {
        this.network = network;
    }

    @Override
    public double evaluate(Board board, Player player) {
        double[] inputs = BoardToInputsConverter.convert(board, player);
        double[] outputs = network.forward(inputs);
        return (outputs.length > 0) ? outputs[0] : 0.0;
    }

    @Override
    public int depthFromBoardSize(int size) {
        // Decide search depth based on board size if needed
        return 3;
    }
}
