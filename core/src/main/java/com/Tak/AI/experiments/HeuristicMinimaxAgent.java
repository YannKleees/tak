// File: com/Tak/AI/experiments/HeuristicMinimaxAgent.java
package com.Tak.AI.experiments;

import com.Tak.AI.evaluation.HeuristicEvaluator;
import com.Tak.AI.neuralnet.BoardToInputsConverter;
import com.Tak.AI.players.MinimaxAgent;
import com.Tak.Logic.exceptions.GameOverException;
import com.Tak.Logic.exceptions.InvalidMoveException;
import com.Tak.Logic.models.Board;
import com.Tak.Logic.models.Piece.PieceType;
import com.Tak.Logic.models.Player;
import com.Tak.Logic.models.Player.Color;
import com.Tak.Logic.models.TakGame;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * HeuristicMinimaxAgent extends MinimaxAgent and logs its moves for training purposes.
 */
public class HeuristicMinimaxAgent extends MinimaxAgent {
    private static final long serialVersionUID = 1L;

    // List to store board states before each move
    private List<double[]> moveBoardStates;

    public HeuristicMinimaxAgent(Color color, int flatStones, int standingStones, int capstones, int maxDepth) {
        super(color, flatStones, standingStones, capstones, maxDepth, true, new HeuristicEvaluator());
        this.moveBoardStates = new ArrayList<>();
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
        HeuristicMinimaxAgent copy = new HeuristicMinimaxAgent(
                this.getColor(),
                this.getRemainingPieces(PieceType.FLAT_STONE),
                this.getRemainingPieces(PieceType.STANDING_STONE),
                this.getRemainingPieces(PieceType.CAPSTONE),
                this.getMaxDepth()
        );
        copy.moveBoardStates = new ArrayList<>(this.moveBoardStates);
        return copy;
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj) && obj instanceof HeuristicMinimaxAgent;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), "HeuristicMinimaxAgent");
    }
}
