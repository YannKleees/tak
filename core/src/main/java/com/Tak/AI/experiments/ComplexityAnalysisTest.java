// File: com/Tak/AI/experiments/ComplexityAnalysisTest.java
package com.Tak.AI.experiments;

import com.Tak.AI.players.MinimaxAgent;
import com.Tak.AI.players.RandomAIPlayer;
import com.Tak.Logic.models.Player;
import com.Tak.Logic.models.TakGame;
import com.Tak.Logic.models.Player.Color;
import com.Tak.Logic.utils.GameOverException;
import com.Tak.Logic.utils.InvalidMoveException;

import java.util.List;
import java.util.ArrayList;

/**
 * ComplexityAnalysisTest runs multiple games between MinimaxAgents and RandomAIPlayers
 * across different search depths and board sizes, collecting complexity metrics and win rates.
 */
public class ComplexityAnalysisTest {

    /**
     * Represents a single experiment configuration and its results.
     */
    static class ExperimentResult {
        int boardSize;
        int minimaxDepth1; // Depth for MinimaxAgent1
        int minimaxDepth2; // Depth for MinimaxAgent2 (only for Minimax vs Minimax)
        boolean moveOrdering;
        String opponentType; // e.g., "Minimax vs RandomAI" or "Minimax vs Minimax"

        int minimaxWins;
        int randomAIWins;
        int minimax2Wins;
        int draws;

        long totalNodesEvaluated;
        long totalTimeTakenMillis;
        int totalPruningEvents;

        int gamesRun;

        public ExperimentResult(int boardSize, int minimaxDepth1, int minimaxDepth2, boolean moveOrdering, String opponentType) {
            this.boardSize = boardSize;
            this.minimaxDepth1 = minimaxDepth1;
            this.minimaxDepth2 = minimaxDepth2;
            this.moveOrdering = moveOrdering;
            this.opponentType = opponentType;

            this.minimaxWins = 0;
            this.randomAIWins = 0;
            this.minimax2Wins = 0;
            this.draws = 0;

            this.totalNodesEvaluated = 0;
            this.totalTimeTakenMillis = 0;
            this.totalPruningEvents = 0;

            this.gamesRun = 0;
        }

        /**
         * Updates the result based on the game outcome and metrics.
         */
        public void updateResult(Player winner, long nodesEvaluated, long timeTaken, int pruningEvents) {
            if (winner == null) {
                this.draws++;
            } else if (opponentType.equals("Minimax vs RandomAI") && winner instanceof MinimaxAgent) {
                this.minimaxWins++;
            } else if (opponentType.equals("Minimax vs RandomAI") && winner instanceof RandomAIPlayer) {
                this.randomAIWins++;
            } else if (opponentType.equals("Minimax vs Minimax") && winner instanceof MinimaxAgent) {
                // Determine which MinimaxAgent won based on color
                MinimaxAgent winnerAgent = (MinimaxAgent) winner;
                if (winnerAgent.getColor() == Color.GREEN) {
                    this.minimaxWins++;
                } else if (winnerAgent.getColor() == Color.BLUE) {
                    this.minimax2Wins++;
                }
            }

            this.totalNodesEvaluated += nodesEvaluated;
            this.totalTimeTakenMillis += timeTaken;
            this.totalPruningEvents += pruningEvents;
            this.gamesRun++;
        }

        /**
         * Formats the experiment result for display.
         */
        @Override
        public String toString() {
            double avgNodes = gamesRun > 0 ? (double) totalNodesEvaluated / gamesRun : 0.0;
            double avgTime = gamesRun > 0 ? (double) totalTimeTakenMillis / gamesRun : 0.0;
            double avgPrunes = gamesRun > 0 ? (double) totalPruningEvents / gamesRun : 0.0;

            String results;
            if (opponentType.equals("Minimax vs RandomAI")) {
                results = String.format(
                    "Results -> Minimax Wins: %d | RandomAI Wins: %d | Draws: %d\n",
                    minimaxWins, randomAIWins, draws
                );
            } else if (opponentType.equals("Minimax vs Minimax")) {
                results = String.format(
                    "Results -> Minimax1 (Depth %d) Wins: %d | Minimax2 (Depth %d) Wins: %d | Draws: %d\n",
                    minimaxDepth1, minimaxWins, minimaxDepth2, minimax2Wins, draws
                );
            } else {
                results = "Results -> Undefined Opponent Type\n";
            }

            return String.format(
                "Board Size: %dx%d | Minimax Depth1: %d | Minimax Depth2: %d | Move Ordering: %b | Opponent Type: %s\n" +
                "%s" +
                "Avg Nodes Evaluated: %.2f | Avg Time Taken: %.2fms | Avg Pruning Events: %.2f\n",
                boardSize, boardSize,
                minimaxDepth1,
                minimaxDepth2,
                moveOrdering,
                opponentType,
                results,
                avgNodes,
                avgTime,
                avgPrunes
            );
        }
    }

    /**
     * Runs multiple games between MinimaxAgents and RandomAIPlayers or between two MinimaxAgents
     * across different configurations, collecting complexity metrics and win rates.
     */
    public void runExperiments() {
        // Define experiment parameters
        int[] boardSizes = {4,5,6}; // Add more sizes as needed (e.g., 3, 5, 7)
        int[] minimaxDepths = {1, 2, 3}; // Different search depths
        boolean moveOrdering = false; // Move Ordering: Disabled as per instruction
        int gamesPerConfigurationRandomAI = 10; // Number of games per configuration for Minimax vs RandomAI
        int gamesPerConfigurationMinimax = 1; // Number of games per configuration for Minimax vs Minimax

        // List to hold all experiment results
        List<ExperimentResult> results = new ArrayList<>();

        // --- Experiments: MinimaxAgent vs RandomAIPlayer ---
        String opponentTypeRandomAI = "Minimax vs RandomAI";
        for (int boardSize : boardSizes) {
            for (int depth : minimaxDepths) {
                ExperimentResult expResultRandomAI = new ExperimentResult(boardSize, depth, 0, moveOrdering, opponentTypeRandomAI);

                for (int gameNum = 1; gameNum <= gamesPerConfigurationRandomAI; gameNum++) {
                    System.out.println("Running Game " + gameNum + " | Board Size: " + boardSize + "x" + boardSize +
                                       " | Minimax Depth: " + depth + " | Move Ordering: " + moveOrdering +
                                       " | Opponent Type: " + opponentTypeRandomAI);

                    // Initialize players
                    Player minimaxPlayer = new MinimaxAgent(Color.GREEN, 21, 1, 1, depth, moveOrdering);
                    Player randomAIPlayer = new RandomAIPlayer(Color.BLUE, 21, 1, 1);

                    // Set opponents
                    minimaxPlayer.setOpponent(randomAIPlayer);
                    randomAIPlayer.setOpponent(minimaxPlayer);

                    // Initialize TakGame with the players (MinimaxAgent starts first)
                    TakGame game = new TakGame(boardSize, List.of(minimaxPlayer, randomAIPlayer));

                    // Log starting player
                    System.out.println("Starting Player: " + game.getCurrentPlayer().getColor());

                    // Run the game loop
                    try {
                        while (!game.isGameEnded()) {
                            Player currentPlayer = game.getCurrentPlayer();
                            currentPlayer.makeMove(game);
                        }

                        // Determine the winner
                        Player winner = game.getWinner();

                        // Collect metrics from MinimaxAgent
                        long nodesEvaluated = (minimaxPlayer instanceof MinimaxAgent) ? 
                                               ((MinimaxAgent) minimaxPlayer).getNodesEvaluated() : 0;
                        long timeTaken = (minimaxPlayer instanceof MinimaxAgent) ? 
                                         ((MinimaxAgent) minimaxPlayer).getTimeTakenMillis() : 0;
                        int pruningEvents = (minimaxPlayer instanceof MinimaxAgent) ? 
                                             ((MinimaxAgent) minimaxPlayer).getPruneCount() : 0;

                        // Update experiment results
                        expResultRandomAI.updateResult(winner, nodesEvaluated, timeTaken, pruningEvents);

                        // Log game result
                        if (winner == null) {
                            System.out.println("Result: Draw\n");
                        } else if (winner.equals(minimaxPlayer)) {
                            System.out.println("Result: MinimaxAgent (GREEN) Wins\n");
                        } else if (winner.equals(randomAIPlayer)) {
                            System.out.println("Result: RandomAIPlayer (BLUE) Wins\n");
                        }

                    } catch (InvalidMoveException e) {
                        System.err.println("InvalidMoveException: " + e.getMessage() + "\n");
                    } catch (GameOverException e) {
                        System.out.println("GameOverException: " + e.getMessage() + "\n");
                    } catch (Exception e) {
                        System.err.println("Unexpected Exception: " + e.getMessage() + "\n");
                    }
                }

                // Add the experiment result to the list
                results.add(expResultRandomAI);
            }
        }

        // --- Experiments: MinimaxAgent vs MinimaxAgent ---
        String opponentTypeMinimax = "Minimax vs Minimax";
        for (int boardSize : boardSizes) {
            for (int depth1 : minimaxDepths) {
                for (int depth2 : minimaxDepths) {
                    // Avoid redundant experiments where both depths are the same if desired
                    // Uncomment the following line to skip identical depth pairings
                    // if (depth1 == depth2) continue;

                    ExperimentResult expResultMinimax = new ExperimentResult(boardSize, depth1, depth2, moveOrdering, opponentTypeMinimax);

                    for (int gameNum = 1; gameNum <= gamesPerConfigurationMinimax; gameNum++) {
                        System.out.println("Running Game " + gameNum + " | Board Size: " + boardSize + "x" + boardSize +
                                           " | Minimax Depth1: " + depth1 + " | Minimax Depth2: " + depth2 +
                                           " | Move Ordering: " + moveOrdering +
                                           " | Opponent Type: " + opponentTypeMinimax);

                        // Initialize players with different depths
                        Player minimaxPlayer1 = new MinimaxAgent(Color.GREEN, 21, 1, 1, depth1, moveOrdering);
                        Player minimaxPlayer2 = new MinimaxAgent(Color.BLUE, 21, 1, 1, depth2, moveOrdering);

                        // Set opponents
                        minimaxPlayer1.setOpponent(minimaxPlayer2);
                        minimaxPlayer2.setOpponent(minimaxPlayer1);

                        // Initialize TakGame with the players (MinimaxAgent1 starts first)
                        TakGame game = new TakGame(boardSize, List.of(minimaxPlayer1, minimaxPlayer2));

                        // Log starting player
                        System.out.println("Starting Player: " + game.getCurrentPlayer().getColor());

                        // Run the game loop
                        try {
                            while (!game.isGameEnded()) {
                                Player currentPlayer = game.getCurrentPlayer();
                                currentPlayer.makeMove(game);
                            }

                            // Determine the winner
                            Player winner = game.getWinner();

                            // Collect metrics from MinimaxAgent1
                            long nodesEvaluated = (minimaxPlayer1 instanceof MinimaxAgent) ? 
                                                   ((MinimaxAgent) minimaxPlayer1).getNodesEvaluated() : 0;
                            long timeTaken = (minimaxPlayer1 instanceof MinimaxAgent) ? 
                                             ((MinimaxAgent) minimaxPlayer1).getTimeTakenMillis() : 0;
                            int pruningEvents = (minimaxPlayer1 instanceof MinimaxAgent) ? 
                                                 ((MinimaxAgent) minimaxPlayer1).getPruneCount() : 0;

                            // Update experiment results
                            expResultMinimax.updateResult(winner, nodesEvaluated, timeTaken, pruningEvents);

                            // Log game result
                            if (winner == null) {
                                System.out.println("Result: Draw\n");
                            } else if (winner.equals(minimaxPlayer1)) {
                                System.out.println("Result: MinimaxAgent1 (GREEN) Wins\n");
                            } else if (winner.equals(minimaxPlayer2)) {
                                System.out.println("Result: MinimaxAgent2 (BLUE) Wins\n");
                            }

                        } catch (InvalidMoveException e) {
                            System.err.println("InvalidMoveException: " + e.getMessage() + "\n");
                        } catch (GameOverException e) {
                            System.out.println("GameOverException: " + e.getMessage() + "\n");
                        } catch (Exception e) {
                            System.err.println("Unexpected Exception: " + e.getMessage() + "\n");
                        }
                    }

                    // Add the experiment result to the list
                    results.add(expResultMinimax);
                }
            }
        }

        // Display all experiment results
        System.out.println("=============== Complexity Analysis Results ===============");
        for (ExperimentResult res : results) {
            System.out.println(res.toString());
        }
        System.out.println("===========================================================");
    }

    /**
     * The main method to execute the complexity analysis test.
     *
     * @param args Command-line arguments (not used).
     */
    public static void main(String[] args) {
        ComplexityAnalysisTest test = new ComplexityAnalysisTest();
        test.runExperiments();
    }
}
