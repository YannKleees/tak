// File: com/Tak/AI/experiments/NeuralNetMinimaxAgent.java
package com.Tak.AI.experiments;

import com.Tak.AI.neuralnet.BoardToInputsConverter;
import com.Tak.AI.players.MinimaxAgent;
import com.Tak.Logic.exceptions.GameOverException;
import com.Tak.Logic.exceptions.InvalidMoveException;
import com.Tak.AI.neuralnet.NeuralNetworkEvaluator;
import com.Tak.Logic.models.Board;
import com.Tak.Logic.models.Piece.PieceType;
import com.Tak.Logic.models.Player;
import com.Tak.Logic.models.Player.Color;
import com.Tak.Logic.models.TakGame;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * NeuralNetMinimaxAgent extends MinimaxAgent and logs its moves for training purposes.
 */
public class NeuralNetMinimaxAgent extends MinimaxAgent {
    private static final long serialVersionUID = 1L;

    // List to store board states before each move
    private List<double[]> moveBoardStates;

    public NeuralNetMinimaxAgent(Color color, int flatStones, int standingStones, int capstones, int maxDepth, NeuralNetworkEvaluator evaluator) {
        super(color, flatStones, standingStones, capstones, maxDepth, true, evaluator);
        this.moveBoardStates = new ArrayList<>();
    }

    public NeuralNetworkEvaluator getEvaluationFunction() {
        return (NeuralNetworkEvaluator) super.getEvaluationFunction();
    }

    @Override
    public void makeMove(TakGame game) throws InvalidMoveException, GameOverException {
        // Log the current board state before making a move
        double[] boardInput = BoardToInputsConverter.convert(game.getBoard(), this);
        moveBoardStates.add(boardInput);

        // Make the move using the superclass method
        super.makeMove(game);
    }

    /**
     * Retrieves the list of board states before each move.
     *
     * @return List of board state arrays.
     */
    public List<double[]> getMoveBoardStates() {
        return moveBoardStates;
    }

    @Override
    public Player copy() {
        NeuralNetMinimaxAgent copy = new NeuralNetMinimaxAgent(
                this.getColor(),
                this.getRemainingPieces(PieceType.FLAT_STONE),
                this.getRemainingPieces(PieceType.STANDING_STONE),
                this.getRemainingPieces(PieceType.CAPSTONE),
                this.getMaxDepth(),
                (NeuralNetworkEvaluator) this.getEvaluationFunction()
        );
        copy.moveBoardStates = new ArrayList<>(this.moveBoardStates);
        return copy;
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj) && obj instanceof NeuralNetMinimaxAgent;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), "NeuralNetMinimaxAgent");
    }
}
