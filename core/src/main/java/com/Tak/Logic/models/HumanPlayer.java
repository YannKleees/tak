package com.Tak.Logic.models;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.Objects;

import com.Tak.Logic.utils.GameOverException;
import com.Tak.Logic.utils.InvalidMoveException;
import com.Tak.Logic.utils.Logger;

/**
 * The HumanPlayer class represents a human player in the game.
 */
public class HumanPlayer extends Player implements Serializable {
    private static final long serialVersionUID = 1L;

    private Scanner scanner;

    /**
     * Constructs a HumanPlayer with the specified color and piece counts.
     *
     * @param color           The player's color.
     * @param flatStones      The number of flat stones.
     * @param standingStones  The number of standing stones.
     * @param capstones       The number of capstones.
     */
    public HumanPlayer(Color color, int flatStones, int standingStones, int capstones) {
        super(color, flatStones, capstones);
        this.scanner = new Scanner(System.in); // Initialize scanner for user input
    }

    /**
     * Creates a copy of this HumanPlayer.
     *
     * @return A new HumanPlayer instance with the same properties.
     */
    @Override
    public Player copy() {
        HumanPlayer copy = new HumanPlayer(this.getColor(),
                this.getRemainingPieces(Piece.PieceType.FLAT_STONE),
                this.getRemainingPieces(Piece.PieceType.STANDING_STONE),
                this.getRemainingPieces(Piece.PieceType.CAPSTONE));
        copy.setScore(this.getScore());
        return copy;
    }

    /**
     * Executes a human player's move.
     * Collects input from the console to perform the move.
     *
     * @param game The current TakGame instance.
     * @throws InvalidMoveException If an invalid move is attempted.
     * @throws GameOverException    If the game has already ended.
     */
    @Override
    public void makeMove(TakGame game) throws InvalidMoveException, GameOverException {
        //Logger.log("HumanPlayer", "It's your turn: " + this.getColor());

        try {
            // Collect starting position
            System.out.print("Enter starting X coordinate (0-" + (game.getBoard().getSize() - 1) + "): ");
            int startX = Integer.parseInt(scanner.nextLine());

            System.out.print("Enter starting Y coordinate (0-" + (game.getBoard().getSize() - 1) + "): ");
            int startY = Integer.parseInt(scanner.nextLine());

            // Collect direction
            System.out.print("Enter direction (UP, DOWN, LEFT, RIGHT): ");
            String directionInput = scanner.nextLine().toUpperCase();
            Direction direction;
            try {
                direction = Direction.valueOf(directionInput);
            } catch (IllegalArgumentException e) {
                System.out.println("Invalid direction. Try again.");
                return;
            }

            // Collect number of pieces to move
            System.out.print("Enter number of pieces to move: ");
            int numberOfPieces = Integer.parseInt(scanner.nextLine());

            // Collect drop counts
            System.out.print("Enter drop counts separated by commas (e.g., 2,1): ");
            String dropCountsInput = scanner.nextLine();
            String[] dropCountsStr = dropCountsInput.split(",");
            List<Integer> dropCounts = new java.util.ArrayList<>();
            for (String countStr : dropCountsStr) {
                int count = Integer.parseInt(countStr.trim());
                if (count <= 0) {
                    System.out.println("Drop counts must be positive integers. Try again.");
                    return;
                }
                dropCounts.add(count);
            }

            // Validate that the sum of drop counts equals the number of pieces
            int totalDrops = dropCounts.stream().mapToInt(Integer::intValue).sum();
            if (totalDrops != numberOfPieces) {
                System.out.println("Sum of drop counts does not equal the number of pieces. Try again.");
                return;
            }

            // Create and execute the move
            Move move = new Move(startX, startY, direction, numberOfPieces, dropCounts);
            game.moveStack(startX, startY, direction, dropCounts.stream().mapToInt(Integer::intValue).toArray());

            System.out.println("Move executed successfully.");

        } catch (NumberFormatException e) {
            System.out.println("Invalid input format. Please enter integers where required.");
        } catch (InvalidMoveException | GameOverException e) {
            System.out.println("Error: " + e.getMessage());
            throw e; // Re-throw to handle in TakGame if necessary
        }
    }

    /**
     * Overrides equals method to compare HumanPlayers based on Player properties.
     *
     * @param obj The object to compare.
     * @return true if equal, false otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        return super.equals(obj) && obj instanceof HumanPlayer;
    }

    /**
     * Overrides hashCode method consistent with equals.
     *
     * @return The hash code.
     */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), "HumanPlayer");
    }

    /**
     * Returns the total number of pieces left for the player.
     * This includes flat stones, standing stones, and capstones.
     */
    @Override
    public int getTotalPiecesLeft() {
        int totalPieces = 0;
        for (Piece.PieceType pieceType : Piece.PieceType.values()) {
            totalPieces += getRemainingPieces(pieceType);
        }
        return totalPieces;
    }
}
