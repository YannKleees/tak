package com.Tak.AI.evaluation;

import com.Tak.Logic.models.Board;
import com.Tak.Logic.models.Player;
import java.io.Serializable;

/**
 * IEvaluationFunction defines the contract for evaluating the current board state.
 * Any implementation (heuristic, ANN, etc.) must implement this interface.
 */
public interface IEvaluationFunction extends Serializable {

    /**
     * Evaluates the given board state for the specified player.
     *
     * @param board  The current game board.
     * @param player The player for whom the evaluation is being performed.
     * @return A numerical score for the board state, from the perspective of 'player'.
     */
    double evaluate(Board board, Player player);

    int depthFromBoardSize(int size);
}
