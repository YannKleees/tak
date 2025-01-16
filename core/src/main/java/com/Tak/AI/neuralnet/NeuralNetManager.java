package com.Tak.AI.neuralnet;

import com.Tak.AI.evaluation.HeuristicEvaluator;
import com.Tak.AI.evaluation.IEvaluationFunction;
import com.Tak.AI.experiments.NeuralNetVsHeuristicExperiment;
import com.Tak.AI.neuralnet.SelfPlayMinimaxTrainer.TrainingExample;
import com.Tak.AI.players.MinimaxAgent;
import com.Tak.Logic.exceptions.GameOverException;
import com.Tak.Logic.exceptions.InvalidMoveException;
import com.Tak.Logic.models.Board;
import com.Tak.Logic.models.Move;
import com.Tak.Logic.models.Player;
import com.Tak.Logic.models.Player.Color;
import com.Tak.Logic.models.TakGame;
import com.Tak.Logic.utils.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * A NeuralNetManager that manages multiple neural networks stored in the "networks" directory.
 * It allows creating new networks, selecting existing ones for training or experiments,
 * training two networks against each other or against a heuristic agent,
 * deleting networks, and ensures that all metrics are calculated and displayed appropriately.
 */
public class NeuralNetManager {

    private static final String NETWORKS_DIR = "networks";
    private static final String NETWORK_FILE_EXTENSION = ".txt";
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        NeuralNetManager manager = new NeuralNetManager();
        manager.run();
    }

    public void run() {
        System.out.println("\n=== Welcome to the Neural Network Manager ===");

        // Ensure the networks directory exists
        File networksDirectory = new File(NETWORKS_DIR);
        if (!networksDirectory.exists()) {
            boolean created = networksDirectory.mkdir();
            if (created) {
                System.out.println("Created 'networks' directory to store neural networks.");
            } else {
                System.out.println("Failed to create 'networks' directory. Exiting.");
                return;
            }
        }

        // Main loop
        while (true) {
            displayMainMenu();
            int choice = getUserChoice(1, 7);
            switch (choice) {
                case 1:
                    createNewNetwork();
                    break;
                case 2:
                    continueTraining();
                    break;
                case 3:
                    runExperiment();
                    break;
                case 4:
                    trainTwoNetworks();
                    break;
                case 5:
                    trainNetworkAgainstHeuristic();
                    break;
                case 6:
                    deleteNetwork();
                    break;
                case 7:
                    System.out.println("Exiting Neural Network Manager. Goodbye!");
                    scanner.close();
                    System.exit(0);
                    break;
                default:
                    // This should never happen due to input validation
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    /**
     * Displays the main menu with options to create, train, run experiments,
     * train two networks against each other or a heuristic agent,
     * delete networks, or exit.
     */
    private void displayMainMenu() {
        System.out.println("\n--- Main Menu ---");
        System.out.println("1. Create a New Neural Network");
        System.out.println("2. Continue Training an Existing Neural Network");
        System.out.println("3. Run NeuralNet vs. Heuristic Experiment");
        System.out.println("4. Train Two Networks Against Each Other");
        System.out.println("5. Train a Network Against a Heuristic Agent");
        System.out.println("6. Delete an Existing Neural Network");
        System.out.println("7. Exit");
        System.out.print("Enter your choice (1-7): ");
    }

    /**
     * Gets a validated user choice within the specified range.
     *
     * @param min Minimum valid choice.
     * @param max Maximum valid choice.
     * @return The user's choice as an integer.
     */
    private int getUserChoice(int min, int max) {
        while (true) {
            try {
                String input = scanner.nextLine().trim();
                int choice = Integer.parseInt(input);
                if (choice >= min && choice <= max) {
                    return choice;
                } else {
                    System.out.print("Please enter a valid number between " + min + " and " + max + ": ");
                }
            } catch (NumberFormatException e) {
                System.out.print("Invalid input. Enter a number between " + min + " and " + max + ": ");
            }
        }
    }

    /**
     * Creates a new neural network by prompting the user for a unique name,
     * activation function, and hidden layer configuration.
     * The network is initialized and saved to the "networks" directory.
     */
    private void createNewNetwork() {
        System.out.println("\n--- Create a New Neural Network ---");
        String networkName;
        while (true) {
            System.out.print("Enter a unique name for the new neural network: ");
            networkName = scanner.nextLine().trim();
            if (networkName.isEmpty()) {
                System.out.println("Network name cannot be empty. Please try again.");
                continue;
            }
            if (networkExists(networkName)) {
                System.out.println("A network with this name already exists. Please choose a different name.");
            } else {
                break;
            }
        }

        // Prompt for activation function
        ActivationFunction activationFunction = promptForActivationFunction();

        // Prompt for number of hidden layers
        int numHiddenLayers = promptForNumber("Enter the number of hidden layers: ", 1, 10); // Arbitrary max limit

        // Prompt for size of each hidden layer
        int[] hiddenLayerSizes = new int[numHiddenLayers];
        for (int i = 0; i < numHiddenLayers; i++) {
            hiddenLayerSizes[i] = promptForNumber("Enter the number of neurons in hidden layer " + (i + 1) + ": ", 1, 1000);
        }

        // Initialize the trainer with the specified configuration
        NeuralNetworkTrainer trainer = NeuralNetworkInitializer.initializeTrainer(hiddenLayerSizes, activationFunction);
        String networkFilePath = getNetworkFilePath(networkName);

        // Save the new network
        trainer.saveNetwork(networkFilePath);
        System.out.println("New neural network '" + networkName + "' created and saved to '" + networkFilePath + "'.");
    }

    /**
     * Prompts the user to select an activation function.
     *
     * @return The selected ActivationFunction.
     */
    private ActivationFunction promptForActivationFunction() {
        System.out.println("\nSelect an Activation Function:");
        ActivationFunction[] functions = ActivationFunction.values();
        for (int i = 0; i < functions.length; i++) {
            System.out.println((i + 1) + ". " + functions[i]);
        }
        System.out.print("Enter your choice (1-" + functions.length + "): ");
        while (true) {
            try {
                String input = scanner.nextLine().trim();
                int choice = Integer.parseInt(input);
                if (choice >= 1 && choice <= functions.length) {
                    return functions[choice - 1];
                } else {
                    System.out.print("Please enter a valid number between 1 and " + functions.length + ": ");
                }
            } catch (NumberFormatException e) {
                System.out.print("Invalid input. Enter a number between 1 and " + functions.length + ": ");
            }
        }
    }

    /**
     * Continues training an existing neural network selected by the user.
     */
    private void continueTraining() {
        System.out.println("\n--- Continue Training ---");
        List<String> networks = listNetworks();
        if (networks.isEmpty()) {
            System.out.println("No existing neural networks found. Please create a new one first.");
            return;
        }

        String selectedNetwork = selectNetwork(networks);
        if (selectedNetwork == null) {
            // User chose to cancel
            return;
        }

        String networkFilePath = getNetworkFilePath(selectedNetwork);
        NeuralNetworkTrainer trainer = NeuralNetworkInitializer.initializeTrainer();
        trainer.loadNetwork(networkFilePath);

        // Prompt for number of training games
        int numberOfGames = promptForNumber("Enter the number of self-play training games: ", 1, Integer.MAX_VALUE);

        // Initialize SelfPlayMinimaxTrainer
        SelfPlayMinimaxTrainer selfPlayTrainer = new SelfPlayMinimaxTrainer(trainer, numberOfGames);
        selfPlayTrainer.runTraining();

        // Save the updated network
        trainer.saveNetwork(networkFilePath);
        System.out.println("Training completed. Network '" + selectedNetwork + "' updated and saved.");
    }

    /**
     * Runs an experiment comparing the neural network against a heuristic-based AI.
     */
    private void runExperiment() {
        System.out.println("\n--- Run NeuralNet vs. Heuristic Experiment ---");
        List<String> networks = listNetworks();
        if (networks.isEmpty()) {
            System.out.println("No existing neural networks found. Please create a new one first.");
            return;
        }

        String selectedNetwork = selectNetwork(networks);
        if (selectedNetwork == null) {
            // User chose to cancel
            return;
        }

        String networkFilePath = getNetworkFilePath(selectedNetwork);
        NeuralNetworkTrainer trainer = NeuralNetworkInitializer.initializeTrainer();
        trainer.loadNetwork(networkFilePath);

        // Create NeuralNetworkEvaluator
        NeuralNetworkEvaluator nnEvaluator = new NeuralNetworkEvaluator(trainer.getNetwork());

        // Initialize HeuristicEvaluator
        IEvaluationFunction heuristicEvaluator = new HeuristicEvaluator();

        // Create and run the experiment
        NeuralNetVsHeuristicExperiment experiment = new NeuralNetVsHeuristicExperiment(nnEvaluator, heuristicEvaluator, trainer);
        try {
            experiment.run();
        } catch (IOException e) {
            System.out.println("Error during experiment: " + e.getMessage());
            Logger.log("NeuralNetManager", "Experiment failed: " + e.getMessage());
            return;
        }

        // Optionally save the network if the experiment involves training
        trainer.saveNetwork(networkFilePath);
        System.out.println("Experiment finished. Network '" + selectedNetwork + "' saved.");
    }

    /**
     * Trains two different neural networks by having them play against each other.
     */
    private void trainTwoNetworks() {
        System.out.println("\n--- Train Two Networks Against Each Other ---");
        List<String> networks = listNetworks();
        if (networks.size() < 2) {
            System.out.println("At least two neural networks are required for this training mode.");
            return;
        }

        // Select Network A
        System.out.println("\nSelect the first neural network (Player A):");
        String networkA = selectNetwork(networks);
        if (networkA == null) {
            // User chose to cancel
            return;
        }

        // Remove selected network from the list to prevent selecting it again
        List<String> remainingNetworks = new ArrayList<>(networks);
        remainingNetworks.remove(networkA);

        // Select Network B
        System.out.println("\nSelect the second neural network (Player B):");
        String networkB = selectNetwork(remainingNetworks);
        if (networkB == null) {
            // User chose to cancel
            return;
        }

        // Load both trainers
        String networkFilePathA = getNetworkFilePath(networkA);
        NeuralNetworkTrainer trainerA = NeuralNetworkInitializer.initializeTrainer();
        trainerA.loadNetwork(networkFilePathA);

        String networkFilePathB = getNetworkFilePath(networkB);
        NeuralNetworkTrainer trainerB = NeuralNetworkInitializer.initializeTrainer();
        trainerB.loadNetwork(networkFilePathB);

        // Prompt for number of training games
        int numberOfGames = promptForNumber("Enter the number of training games: ", 1, Integer.MAX_VALUE);

        // Initialize AI Evaluators
        NeuralNetworkEvaluator evaluatorA = new NeuralNetworkEvaluator(trainerA.getNetwork());
        NeuralNetworkEvaluator evaluatorB = new NeuralNetworkEvaluator(trainerB.getNetwork());

        // Define board size (modify if your game supports different sizes)
        int boardSize = 5;

        // Start training
        System.out.println("\nStarting training between '" + networkA + "' and '" + networkB + "'...");
        for (int i = 1; i <= numberOfGames; i++) {
            System.out.println("\n--- Game " + i + " ---");

            // Create a new TakGame with fresh Player instances
            TakGame game = createDualTrainingTakGame(boardSize, evaluatorA, evaluatorB);

            // Initialize training data
            List<TrainingExample> trainingDataA = new ArrayList<>();
            List<TrainingExample> trainingDataB = new ArrayList<>();

            while (!game.isGameEnded()) {
                try {
                    Player currentPlayer = game.getCurrentPlayer();
                    double[] state = BoardToInputsConverter.convert(game.getBoard(), currentPlayer);
                    System.out.println("Current player: " + currentPlayer.getColor());

                    // Let the current player make a move
                    currentPlayer.makeMove(game);

                    System.out.println("Move made successfully. Switching player.");
                    game.getBoard().printBoard();

                    // Record the state before the move
                    if (currentPlayer.getColor() == Color.BLUE) {
                        trainingDataA.add(new TrainingExample(state, new double[]{0.0})); // Temporary target
                    } else {
                        trainingDataB.add(new TrainingExample(state, new double[]{0.0})); // Temporary target
                    }

                } catch (InvalidMoveException | GameOverException e) {
                    // If the move is invalid or the game is over for some reason,
                    // we break out of the loop
                    System.out.println("An error occurred during the game: " + e.getMessage());
                    break;
                }
            }

            // Handle game outcome
            Player winner = game.getWinner();
            if (winner != null) {
                System.out.println("Winner: " + winner.getColor());
            } else {
                System.out.println("The game ended in a tie.");
            }

            // Update training targets based on outcome
            for (TrainingExample ex : trainingDataA) {
                if (winner != null) {
                    if (winner.getColor() == Color.BLUE) {
                        ex.setTarget(new double[]{1.0}); // Player A won
                    } else {
                        ex.setTarget(new double[]{-1.0}); // Player A lost
                    }
                } else {
                    ex.setTarget(new double[]{0.0}); // Tie
                }
            }

            for (TrainingExample ex : trainingDataB) {
                if (winner != null) {
                    if (winner.getColor() == Color.GREEN) {
                        ex.setTarget(new double[]{1.0}); // Player B won
                    } else {
                        ex.setTarget(new double[]{-1.0}); // Player B lost
                    }
                } else {
                    ex.setTarget(new double[]{0.0}); // Tie
                }
            }

            // Train both networks
            trainerA.trainAll(trainingDataA);
            trainerB.trainAll(trainingDataB);

            if (i % 100 == 0) {
                System.out.println("Completed " + i + " training games.");
            }
        }

        // Save both networks
        trainerA.saveNetwork(networkFilePathA);
        trainerB.saveNetwork(networkFilePathB);

        System.out.println("\nTraining between '" + networkA + "' and '" + networkB + "' completed and saved.");
    }

    /**
     * Trains a neural network against a heuristic-based Minimax agent.
     */
    private void trainNetworkAgainstHeuristic() {
        System.out.println("\n--- Train a Network Against a Heuristic Agent ---");
        List<String> networks = listNetworks();
        if (networks.isEmpty()) {
            System.out.println("No existing neural networks found. Please create a new one first.");
            return;
        }

        // Select Neural Network
        System.out.println("\nSelect the neural network to train against the Heuristic Agent:");
        String selectedNetwork = selectNetwork(networks);
        if (selectedNetwork == null) {
            // User chose to cancel
            return;
        }

        String networkFilePath = getNetworkFilePath(selectedNetwork);
        NeuralNetworkTrainer trainer = NeuralNetworkInitializer.initializeTrainer();
        trainer.loadNetwork(networkFilePath);

        // Prompt for number of training games
        int numberOfGames = promptForNumber("Enter the number of training games: ", 1, Integer.MAX_VALUE);

        // Initialize AI Evaluators
        NeuralNetworkEvaluator nnEvaluator = new NeuralNetworkEvaluator(trainer.getNetwork());
        IEvaluationFunction heuristicEvaluator = new HeuristicEvaluator();

        // Define board size (modify if your game supports different sizes)
        int boardSize = 5;

        // Start training
        System.out.println("\nStarting training between '" + selectedNetwork + "' and Heuristic Agent...");
        for (int i = 1; i <= numberOfGames; i++) {
            System.out.println("\n--- Game " + i + " ---");

            // Create a new TakGame with fresh Player instances
            TakGame game = createTrainingTakGame(boardSize, nnEvaluator, heuristicEvaluator);

            // Initialize training data
            List<TrainingExample> trainingDataNN = new ArrayList<>();

            while (!game.isGameEnded()) {
                try {
                    Player currentPlayer = game.getCurrentPlayer();
                    double[] state = BoardToInputsConverter.convert(game.getBoard(), currentPlayer);
                    System.out.println("Current player: " + currentPlayer.getColor());

                    // Let the current player make a move
                    currentPlayer.makeMove(game);

                    System.out.println("Move made successfully. Switching player.");
                    game.getBoard().printBoard();

                    // Record the state before the move
                    trainingDataNN.add(new TrainingExample(state, new double[]{0.0})); // Temporary target

                } catch (InvalidMoveException | GameOverException e) {
                    // If the move is invalid or the game is over for some reason,
                    // we break out of the loop
                    System.out.println("An error occurred during the game: " + e.getMessage());
                    break;
                }
            }

            // Handle game outcome
            Player winner = game.getWinner();
            if (winner != null) {
                System.out.println("Winner: " + winner.getColor());
            } else {
                System.out.println("The game ended in a tie.");
            }

            // Update training targets based on outcome
            for (TrainingExample ex : trainingDataNN) {
                if (winner != null) {
                    if (winner.getColor() == Color.BLUE) {
                        ex.setTarget(new double[]{1.0}); // Neural Network won
                    } else {
                        ex.setTarget(new double[]{-1.0}); // Neural Network lost
                    }
                } else {
                    ex.setTarget(new double[]{0.0}); // Tie
                }
            }

            // Train the neural network
            trainer.trainAll(trainingDataNN);

            if (i % 100 == 0) {
                System.out.println("Completed " + i + " training games.");
            }
        }

        // Save the trained network
        trainer.saveNetwork(networkFilePath);

        System.out.println("\nTraining between '" + selectedNetwork + "' and Heuristic Agent completed and saved.");
    }

    /**
     * Deletes an existing neural network selected by the user.
     */
    private void deleteNetwork() {
        System.out.println("\n--- Delete an Existing Neural Network ---");
        List<String> networks = listNetworks();
        if (networks.isEmpty()) {
            System.out.println("No existing neural networks found. Nothing to delete.");
            return;
        }

        String selectedNetwork = selectNetwork(networks);
        if (selectedNetwork == null) {
            // User chose to cancel
            return;
        }

        // Confirm deletion
        System.out.print("Are you sure you want to delete the neural network '" + selectedNetwork + "'? (yes/no): ");
        String confirmation = scanner.nextLine().trim().toLowerCase();
        if (!confirmation.equals("yes") && !confirmation.equals("y")) {
            System.out.println("Deletion cancelled.");
            return;
        }

        // Delete the network file
        String networkFilePath = getNetworkFilePath(selectedNetwork);
        File networkFile = new File(networkFilePath);
        if (networkFile.delete()) {
            System.out.println("Neural network '" + selectedNetwork + "' has been deleted successfully.");
        } else {
            System.out.println("Failed to delete neural network '" + selectedNetwork + "'. Please try again.");
        }
    }

    /**
     * Lists all neural networks stored in the "networks" directory with their metadata.
     *
     * @return A list of network names without file extensions.
     */
    private List<String> listNetworks() {
        File networksDirectory = new File(NETWORKS_DIR);
        File[] files = networksDirectory.listFiles((dir, name) -> name.endsWith(NETWORK_FILE_EXTENSION));

        List<String> networks = new ArrayList<>();
        if (files != null) {
            for (File file : files) {
                String fileName = file.getName();
                String networkName = fileName.substring(0, fileName.length() - NETWORK_FILE_EXTENSION.length());
                networks.add(networkName);
            }
        }

        if (networks.isEmpty()) {
            System.out.println("No neural networks found in the '" + NETWORKS_DIR + "' directory.");
        } else {
            System.out.println("\nAvailable Neural Networks:");
            for (int i = 0; i < networks.size(); i++) {
                String networkName = networks.get(i);
                String networkFilePath = getNetworkFilePath(networkName);
                NeuralNetworkTrainer trainer = NeuralNetworkInitializer.initializeTrainer();
                trainer.loadNetwork(networkFilePath);
                System.out.println((i + 1) + ". " + networkName +
                        " | States Trained: " + trainer.getNumberOfStatesTrained() +
                        " | Last Output: " + trainer.getLastOutput());
            }
        }

        return networks;
    }

    /**
     * Prompts the user to select a neural network from the list.
     *
     * @param networks A list of available network names.
     * @return The selected network name, or null if the user cancels.
     */
    private String selectNetwork(List<String> networks) {
        if (networks.isEmpty()) {
            return null;
        }

        System.out.println("\nSelect a neural network by entering its number:");
        System.out.println("0. Cancel");
        for (int i = 0; i < networks.size(); i++) {
            System.out.println((i + 1) + ". " + networks.get(i));
        }
        System.out.print("Your choice: ");

        while (true) {
            try {
                String input = scanner.nextLine().trim();
                int choice = Integer.parseInt(input);
                if (choice == 0) {
                    System.out.println("Operation cancelled.");
                    return null;
                }
                if (choice >= 1 && choice <= networks.size()) {
                    return networks.get(choice - 1);
                } else {
                    System.out.print("Please enter a valid number between 0 and " + networks.size() + ": ");
                }
            } catch (NumberFormatException e) {
                System.out.print("Invalid input. Enter a number between 0 and " + networks.size() + ": ");
            }
        }
    }

    /**
     * Checks if a network with the given name already exists.
     *
     * @param networkName The name of the network.
     * @return True if the network exists, false otherwise.
     */
    private boolean networkExists(String networkName) {
        File networkFile = new File(getNetworkFilePath(networkName));
        return networkFile.exists();
    }

    /**
     * Constructs the full file path for a given network name.
     *
     * @param networkName The name of the network.
     * @return The full file path as a string.
     */
    private String getNetworkFilePath(String networkName) {
        return NETWORKS_DIR + File.separator + networkName + NETWORK_FILE_EXTENSION;
    }

    /**
     * Prompts the user for a number within a specified range.
     *
     * @param prompt The prompt message.
     * @param min    The minimum acceptable number.
     * @param max    The maximum acceptable number.
     * @return The user's input as an integer.
     */
    private int promptForNumber(String prompt, int min, int max) {
        System.out.print(prompt);
        while (true) {
            try {
                String input = scanner.nextLine().trim();
                int number = Integer.parseInt(input);
                if (number >= min && number <= max) {
                    return number;
                } else {
                    System.out.print("Please enter a number between " + min + " and " + max + ": ");
                }
            } catch (NumberFormatException e) {
                System.out.print("Invalid input. " + prompt);
            }
        }
    }

    /**
     * Creates a TakGame instance for dual-network training.
     *
     * @param boardSize   The size of the Tak board.
     * @param evaluatorA  The NeuralNetworkEvaluator for Player A.
     * @param evaluatorB  The NeuralNetworkEvaluator for Player B.
     * @return A TakGame instance configured for dual-network training.
     */
    private TakGame createDualTrainingTakGame(int boardSize, NeuralNetworkEvaluator evaluatorA, NeuralNetworkEvaluator evaluatorB) {
        // Always create fresh Player instances for each game
        Player playerA = new MinimaxAgent(Color.BLUE, 21, 21, 1, 3, false, evaluatorA);
        Player playerB = new MinimaxAgent(Color.GREEN, 21, 21, 1, 3, false, evaluatorB);

        List<Player> players = Arrays.asList(playerA, playerB);
        TakGame game = new TakGame(boardSize, players);
        return game;
    }

    /**
     * Creates a TakGame instance for training against a heuristic agent.
     *
     * @param boardSize          The size of the Tak board.
     * @param nnEvaluator        The NeuralNetworkEvaluator for the neural network player.
     * @param heuristicEvaluator The heuristic-based evaluation function for the opponent.
     * @return A TakGame instance configured for training against a heuristic agent.
     */
    private TakGame createTrainingTakGame(int boardSize, NeuralNetworkEvaluator nnEvaluator, IEvaluationFunction heuristicEvaluator) {
        // Always create fresh Player instances for each game
        Player nnPlayer = new MinimaxAgent(Color.BLUE, 21, 21, 1, 3, false, nnEvaluator);
        Player heuristicPlayer = new MinimaxAgent(Color.GREEN, 21, 21, 1, 3, false, heuristicEvaluator);

        List<Player> players = Arrays.asList(nnPlayer, heuristicPlayer);
        TakGame game = new TakGame(boardSize, players);
        return game;
    }

    /**
     * Handles the outcome of a game by announcing the winner.
     *
     * @param game The final TakGame instance.
     */
    private void handleGameOutcome(TakGame game) {
        Player winner = game.getWinner();
        if (winner != null) {
            System.out.println("Winner: " + winner.getColor());
            // Optionally, provide additional rewards or handle post-game logic
        } else {
            System.out.println("The game ended in a tie.");
            // Optionally, handle draw scenarios
        }
    }
}
