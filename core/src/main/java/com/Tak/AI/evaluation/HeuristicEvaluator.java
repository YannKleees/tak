package com.Tak.AI.evaluation;

import com.Tak.AI.utils.RoadConnectivity;
import com.Tak.Logic.models.Board;
import com.Tak.Logic.models.Direction;
import com.Tak.Logic.models.Piece;
import com.Tak.Logic.models.PieceStack;
import com.Tak.Logic.models.Player;
import com.Tak.Logic.utils.Logger;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * The EvaluationFunction class evaluates the game board from the AI's perspective,
 * without any color-specific orientation logic (no forced left-right or top-down).
 * It assigns scores based on strategic factors like road potential, stacking, blocking, etc.
 */
public class HeuristicEvaluator implements IEvaluationFunction, Serializable {

    private static final long serialVersionUID = 1L;

    private RoadConnectivity roadChecker;
    private transient Map<String, Double> evaluationCache;

    /**
     * Default constructor. Initializes necessary components.
     */
    public HeuristicEvaluator() {
        this.roadChecker = new RoadConnectivity();
        this.evaluationCache = new HashMap<>();
    }

    /**
     * Evaluates the current board state and returns a score from the perspective of 'player'.
     *
     * @param board  The current game board.
     * @param player The player for whom the evaluation is being done.
     * @return A numerical score representing the desirability of the board state for 'player'.
     */
    @Override
    public double evaluate(Board board, Player player) {
        String stateHash = generateStateHash(board, player);

        if (evaluationCache.containsKey(stateHash)) {
            return evaluationCache.get(stateHash);
        }

        // 1. Check for road wins
        if (roadChecker.checkForRoadWin(player, board)) {
            evaluationCache.put(stateHash, Double.POSITIVE_INFINITY);
            return Double.POSITIVE_INFINITY;
        }
        else if (roadChecker.checkForRoadWin(player.getOpponent(), board)) {
            evaluationCache.put(stateHash, Double.NEGATIVE_INFINITY);
            return Double.NEGATIVE_INFINITY;
        }

        // 2. Evaluate using a heuristic
        double heuristicScore = heuristicEvaluation(board, player);

        evaluationCache.put(stateHash, heuristicScore);
        return heuristicScore;
    }

    /**
     * Heuristic evaluation function that adjusts weights dynamically based on whether
     * the AI is ahead or behind.
     *
     * @param board  The current game board.
     * @param player The player (AI) for whom the evaluation is being done.
     * @return A numerical score representing the heuristic evaluation from that player's POV.
     */
    private double heuristicEvaluation(Board board, Player player) {
        double score = 0.0;

        // 1. Immediate Threat Detection
        boolean opponentThreat = roadChecker.isPlayerCloseToWinning(board, player.getOpponent());
        if (opponentThreat) {
            score -= 1000.0; // Large penalty if opponent is close to winning
        }

        // 2. Immediate Winning Opportunity
        boolean canWinNext = roadChecker.isPlayerCloseToWinning(board, player);
        if (canWinNext) {
            score += 1000.0; // Large reward if we can possibly win soon
        }

        // Base evaluation at this point
        double baseScore = score;

        // 3. Road Potential Difference
        int playerRoadPotential = roadChecker.calculateRoadPotential(board, player);
        int opponentRoadPotential = roadChecker.calculateRoadPotential(board, player.getOpponent());
        double roadPotentialScore = (playerRoadPotential - opponentRoadPotential) * 100.0;

        // 4. Stacking Mechanics
        double stackingScore = evaluateStacking(board, player) * 50.0;

        // 5. Blocking Potential
        double blockingScore = evaluateBlockingPotential(board, player) * 50.0;

        // 6. Capstone Utilization
        double capstoneScore = evaluateCapstoneUtilization(board, player) * 30.0;

        // 7. Flat Stones Difference
        int flatStoneDifference = countFlatStones(board, player)
            - countFlatStones(board, player.getOpponent());
        double flatStoneScore = flatStoneDifference * 10.0;

        // Summation before dynamic weighting
        double currentEvaluation = baseScore
            + roadPotentialScore
            + stackingScore
            + blockingScore
            + capstoneScore
            + flatStoneScore;

        // 8. Dynamic weight adjustments based on whether the AI is behind or ahead
        if (currentEvaluation < 0) {
            // AI is behind => more defensive
            blockingScore *= 1.5;
            roadPotentialScore *= 0.8;
            // stackingScore stays the same
        } else {
            // AI is ahead => more offensive
            roadPotentialScore *= 1.2;
            blockingScore *= 0.8;
            // stackingScore stays the same
        }

        // Final total
        score = baseScore
            + roadPotentialScore
            + stackingScore
            + blockingScore
            + capstoneScore
            + flatStoneScore;

        // Method 1: Using Math.max and Math.min
        double clampedScore = Math.max(-1, Math.min(1, score));


        return clampedScore;
    }

    /**
     * Evaluates stacking mechanics to assess control over stacks and mobility.
     * (No color-coded direction logic is used here).
     *
     * @param board  The game board.
     * @param player The player.
     * @return A score representing stacking benefits for 'player'.
     */
    private double evaluateStacking(Board board, Player player) {
        double stackingScore = 0.0;
        int size = board.getSize();

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                PieceStack stack = board.getBoardStack(x, y);
                if (!stack.isEmpty()) {
                    Player stackOwner = stack.getTopPiece().getOwner();
                    if (stackOwner.equals(player)) {
                        // Add some points for controlling taller stacks
                        stackingScore += stack.size() * 2;
                    } else if (stackOwner.equals(player.getOpponent())) {
                        // Slight penalty if opponent controls a tall stack
                        stackingScore -= stack.size();
                    }
                }
            }
        }
        return stackingScore;
    }

    /**
     * Evaluates the potential to block the opponent's road-building efforts.
     *
     * @param board  The game board.
     * @param player The player for whom we are calculating blocking potential.
     * @return A score representing how well 'player' can block the opponent.
     */
    private double evaluateBlockingPotential(Board board, Player player) {
        double blockingScore = 0.0;
        int size = board.getSize();
        Player opponent = player.getOpponent();

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                // If adjacent to an opponent's potential road piece, occupying it or controlling it is beneficial
                if (isAdjacentToOpponentRoad(board, x, y, opponent)) {
                    PieceStack stack = board.getBoardStack(x, y);
                    if (stack.isEmpty()) {
                        blockingScore += 1.0;
                    } else {
                        Piece topPiece = stack.getTopPiece();
                        if (topPiece.getOwner().equals(player)) {
                            blockingScore += 1.5; // Already blocking
                        }
                    }
                }
            }
        }
        return blockingScore;
    }

    /**
     * Checks if the (x,y) position is adjacent to the opponent's road pieces.
     */
    private boolean isAdjacentToOpponentRoad(Board board, int x, int y, Player opponent) {
        for (Direction dir : Direction.values()) {
            int adjX = x + dir.getDeltaX();
            int adjY = y + dir.getDeltaY();
            if (board.isWithinBounds(adjX, adjY)) {
                PieceStack adjStack = board.getBoardStack(adjX, adjY);
                if (!adjStack.isEmpty()) {
                    Piece topPiece = adjStack.getTopPiece();
                    if (topPiece.getOwner().equals(opponent) && topPiece.canBePartOfRoad()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Evaluates the utilization of capstones on the board.
     */
    private double evaluateCapstoneUtilization(Board board, Player player) {
        double capstoneScore = 0.0;
        int size = board.getSize();

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                PieceStack stack = board.getBoardStack(x, y);
                if (!stack.isEmpty()) {
                    Piece topPiece = stack.getTopPiece();
                    if (topPiece.getPieceType() == Piece.PieceType.CAPSTONE
                        && topPiece.getOwner().equals(player)) {
                        capstoneScore += evaluateCapstonePosition(board, x, y, player);
                    }
                }
            }
        }
        return capstoneScore;
    }

    /**
     * Evaluates the impact of a capstone at a given position (like flattening standing stones, etc.).
     */
    private double evaluateCapstonePosition(Board board, int x, int y, Player player) {
        double impactScore = 0.0;
        Player opponent = player.getOpponent();

        for (Direction dir : Direction.values()) {
            int adjX = x + dir.getDeltaX();
            int adjY = y + dir.getDeltaY();
            if (board.isWithinBounds(adjX, adjY)) {
                PieceStack adjStack = board.getBoardStack(adjX, adjY);
                if (!adjStack.isEmpty()) {
                    Piece topPiece = adjStack.getTopPiece();
                    // Example: reward if adjacent to an opponent's standing stone (cap can flatten it).
                    if (topPiece.getOwner().equals(opponent)
                        && topPiece.getPieceType() == Piece.PieceType.STANDING_STONE) {
                        impactScore += 2.0;
                    }
                }
            }
        }
        return impactScore;
    }

    /**
     * Counts how many flat stones 'player' has on top of stacks across the board.
     */
    private int countFlatStones(Board board, Player player) {
        int count = 0;
        int size = board.getSize();
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                PieceStack stack = board.getBoardStack(x, y);
                if (!stack.isEmpty()) {
                    Piece topPiece = stack.getTopPiece();
                    if (topPiece.getOwner().equals(player)
                        && topPiece.getPieceType() == Piece.PieceType.FLAT_STONE) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    /**
     * Generates a unique hash for the board state including the player's color for caching.
     */
    private String generateStateHash(Board board, Player player) {
        StringBuilder sb = new StringBuilder();
        sb.append(player.getColor() == Player.Color.BLUE ? "B_" : "G_");

        for (int y = 0; y < board.getSize(); y++) {
            for (int x = 0; x < board.getSize(); x++) {
                PieceStack stack = board.getBoardStack(x, y);
                if (stack.isEmpty()) {
                    sb.append("E");
                } else {
                    Piece topPiece = stack.getTopPiece();
                    sb.append(topPiece.getPieceType().toString().charAt(0));
                    sb.append(topPiece.getOwner().getColor().toString().charAt(0));
                    sb.append(stack.size());
                }
                sb.append(",");
            }
            sb.append(";");
        }
        return sb.toString();
    }
}
