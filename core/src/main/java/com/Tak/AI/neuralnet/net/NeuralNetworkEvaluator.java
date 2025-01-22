package com.Tak.AI.neuralnet.net;

import com.Tak.AI.evaluation.IEvaluationFunction;
import com.Tak.Logic.models.Board;
import com.Tak.Logic.models.Player;
import com.Tak.Logic.models.Player.Color;

import java.io.Serializable;

/**
 * Uses a NeuralNetwork to score the board state with 2 outputs: [scoreBLUE, scoreGREEN].
 * Returns the relevant index for whichever player asked for evaluation.
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

        if (outputs.length < 2) {
            // fallback
            return (outputs.length > 0) ? outputs[0] : 0.0;
        }
        if (player.getColor() == Color.BLUE) {
            return outputs[0];
        } else {
            return outputs[1];
        }
    }
}
