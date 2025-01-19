package com.Tak.AI.neuralnet;

import com.Tak.AI.experiments.IterativeNetVsHeuristicTrainer;
import com.Tak.AI.neuralnet.SelfPlayMinimaxTrainer;
import com.Tak.AI.players.MinimaxAgent;
import com.Tak.Logic.models.Player;
import com.Tak.Logic.models.Player.Color;
import com.Tak.Logic.models.TakGame;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Central manager for Neural Networks and training/usage tasks.
 * Provides a single entry point for:
 *  - Creating/loading/deleting networks
 *  - Training (iterative or self-play)
 *  - Gathering extended training metrics
 *  - Saving results
 */
public class ANNManager {

    private static final String NETWORKS_DIR = "networks";
    private static final String NETWORK_FILE_EXTENSION = ".txt";
    private static final Scanner scanner = new Scanner(System.in);

    private boolean running = true;

    // Example metrics to track (you can expand or reorganize as needed).
    private double averageGameLength;
    private double netWinRate;
    private double averageNodesEvaluated;
    private int gamesPlayed;

    public static void main(String[] args) {
        ANNManager manager = new ANNManager();
        manager.run();
    }

    /**
     * Main menu loop for user interaction.
     */
    public void run() {
        ensureNetworksDirectory();

        System.out.println("\n=== Welcome to the central ANN Manager ===");
        while (running) {
            displayMainMenu();
            // We have 7 menu items: 1..6, plus 7=Exit
            int choice = getUserChoice(1, 7);
            switch (choice) {
                case 1:
                    createNewNetwork();
                    break;
                case 2:
                    iterativeTrainingMenu();
                    break;
                case 3:
                    selfPlayTrainingMenu();
                    break;
                case 4:
                    deleteNetwork();
                    break;
                case 5:
                    showMetrics();
                    break;
                case 6:
                    // Delegate to our separate ExperimentRunner
                    ExperimentRunner.runAllExperiments();
                    break;
                case 7:
                    exitManager();
                    break;
            }
        }
    }

    /**
     * Displays the main menu options, now with a 7th item for experiments.
     */
    private void displayMainMenu() {
        System.out.println("\n--- Main Menu ---");
        System.out.println("1. Create a New Neural Network");
        System.out.println("2. Iterative Training vs. Heuristic");
        System.out.println("3. Self-Play Training");
        System.out.println("4. Delete an Existing Neural Network");
        System.out.println("5. Show Training Metrics");
        System.out.println("6. Run Experiments");
        System.out.println("7. Exit");
        System.out.print("Enter your choice (1-7): ");
    }

    private void exitManager() {
        System.out.println("Exiting ANNManager. Goodbye!");
        scanner.close();
        running = false;
    }

    // ------------------------------------------------------------------------
    // 1) Create a New Network
    // ------------------------------------------------------------------------
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

        // Prompt user for # of hidden layers
        int numHiddenLayers = promptForNumber("Enter the number of hidden layers: ", 1, 10);

        // Prompt for size of each hidden layer
        int[] hiddenLayerSizes = new int[numHiddenLayers];
        for (int i = 0; i < numHiddenLayers; i++) {
            hiddenLayerSizes[i] = promptForNumber(
                "Enter the number of neurons in hidden layer " + (i + 1) + ": ",
                1, 1000
            );
        }

        // Prompt for activation function
        ActivationFunction activationFunction = promptForActivationFunction();

        // Build the trainer
        NeuralNetworkTrainer trainer =
            NeuralNetworkInitializer.initializeTrainer(hiddenLayerSizes, activationFunction);

        // Save the new network
        String networkFilePath = getNetworkFilePath(networkName);
        trainer.saveNetwork(networkFilePath);
        System.out.println("New neural network '" + networkName + "' created and saved to '" + networkFilePath + "'.");
    }

    // ------------------------------------------------------------------------
    // 2) Iterative Training vs. Heuristic
    // ------------------------------------------------------------------------
    private void iterativeTrainingMenu() {
        System.out.println("\n--- Iterative Training vs. Heuristic ---");
        // List existing networks
        List<String> networks = listNetworks();
        if (networks.isEmpty()) {
            System.out.println("No existing neural networks found. Please create a new one first.");
            return;
        }

        // Pick a network
        System.out.println("\nSelect the network (BLUE) to iteratively train vs. Heuristic (GREEN):");
        String selectedNetwork = selectNetwork(networks);
        if (selectedNetwork == null) {
            return;
        }

        // Load it
        String networkFilePath = getNetworkFilePath(selectedNetwork);
        NeuralNetworkTrainer trainer = NeuralNetworkInitializer.initializeTrainer();  // default constructor
        trainer.loadNetwork(networkFilePath);

        // Prompt for iterative training parameters
        int totalRounds = promptForNumber("Enter total training rounds: ", 1, 1000);
        int gamesPerRound = promptForNumber("Enter # training games per round: ", 1, 10000);
        boolean printBoard = promptForYesNo("Print final board after each game? (yes/no): ");
        int boardSize = promptForNumber("Enter board size (e.g., 5 for 5x5): ", 3, 10);

        // Run the iterative trainer
        IterativeNetVsHeuristicTrainer iterativeTrainer =
            new IterativeNetVsHeuristicTrainer(
                trainer,
                boardSize,
                gamesPerRound,
                totalRounds,
                printBoard
            );

        long startTime = System.currentTimeMillis();
        iterativeTrainer.runIterativeTraining();
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        double seconds = duration / 1000.0;

        // Example: update local metrics if you like
        System.out.printf("Finished iterative training in %.2f seconds.\n", seconds);

        // Save updated net
        trainer.saveNetwork(networkFilePath);
        System.out.println("Network '" + selectedNetwork + "' updated and saved.");
    }

    // ------------------------------------------------------------------------
    // 3) Self-Play Training
    // ------------------------------------------------------------------------
    private void selfPlayTrainingMenu() {
        System.out.println("\n--- Self-Play Training ---");
        List<String> networks = listNetworks();
        if (networks.isEmpty()) {
            System.out.println("No existing neural networks found. Please create a new one first.");
            return;
        }

        System.out.println("\nSelect the network to use for self-play training:");
        String selectedNetwork = selectNetwork(networks);
        if (selectedNetwork == null) {
            return;
        }

        // Load it
        String networkFilePath = getNetworkFilePath(selectedNetwork);
        NeuralNetworkTrainer trainer = NeuralNetworkInitializer.initializeTrainer();
        trainer.loadNetwork(networkFilePath);

        // Ask how many self-play games to run
        int numberOfGames = promptForNumber("How many self-play training games to run: ", 1, 50000);
        int valFrequency = promptForNumber("Validation frequency (# games between validations): ", 1, numberOfGames);
        int patience = promptForNumber("Patience (stop if no improvement for X validations): ", 1, 100);

        // Build SelfPlayMinimaxTrainer
        SelfPlayMinimaxTrainer selfPlay = new SelfPlayMinimaxTrainer(trainer, numberOfGames, valFrequency, patience);

        long startTime = System.currentTimeMillis();
        selfPlay.runTraining();
        long endTime = System.currentTimeMillis();
        double seconds = (endTime - startTime) / 1000.0;
        System.out.printf("Self-play training took %.2f seconds.\n", seconds);

        // Optionally gather some additional stats here
        // e.g. averageGameLength = ...
        // netWinRate = ...

        // Save updated net
        trainer.saveNetwork(networkFilePath);
        System.out.println("\nSelf-play training done. Network '" + selectedNetwork + "' saved.");
    }

    // ------------------------------------------------------------------------
    // 4) Delete Network
    // ------------------------------------------------------------------------
    private void deleteNetwork() {
        System.out.println("\n--- Delete a Network ---");
        List<String> networks = listNetworks();
        if (networks.isEmpty()) {
            System.out.println("No existing neural networks found. Nothing to delete.");
            return;
        }

        String selectedNetwork = selectNetwork(networks);
        if (selectedNetwork == null) {
            return;
        }

        // Confirm
        System.out.print("Are you sure you want to delete '" + selectedNetwork + "'? (yes/no): ");
        String confirmation = scanner.nextLine().trim().toLowerCase();
        if (!confirmation.equals("yes") && !confirmation.equals("y")) {
            System.out.println("Deletion canceled.");
            return;
        }

        // Delete the file
        String networkFilePath = getNetworkFilePath(selectedNetwork);
        File f = new File(networkFilePath);
        if (f.delete()) {
            System.out.println("Network '" + selectedNetwork + "' deleted.");
        } else {
            System.out.println("Failed to delete. Try again.");
        }
    }

    // ------------------------------------------------------------------------
    // 5) Show Training Metrics
    // ------------------------------------------------------------------------
    private void showMetrics() {
        System.out.println("\n--- Current Metrics ---");
        System.out.printf("Games played: %d\n", gamesPlayed);
        System.out.printf("Average game length: %.2f\n", averageGameLength);
        System.out.printf("Net win rate: %.2f\n", netWinRate);
        System.out.printf("Average nodes evaluated: %.2f\n", averageNodesEvaluated);
        // Add any other relevant info you track
    }

    // ------------------------------------------------------------------------
    // 6) Run Experiments
    // ------------------------------------------------------------------------
    private void runExperiments() {
        // We simply delegate to the separate class
        ExperimentRunner.runAllExperiments();
    }

    // ------------------------------------------------------------------------
    // Internal Utility Methods
    // ------------------------------------------------------------------------

    private void ensureNetworksDirectory() {
        File networksDirectory = new File(NETWORKS_DIR);
        if (!networksDirectory.exists()) {
            boolean created = networksDirectory.mkdir();
            if (created) {
                System.out.println("Created 'networks' directory to store neural networks.");
            } else {
                System.out.println("Failed to create 'networks' directory. Exiting.");
                System.exit(0);
            }
        }
    }

    private String getNetworkFilePath(String netName) {
        return NETWORKS_DIR + File.separator + netName + NETWORK_FILE_EXTENSION;
    }

    private boolean networkExists(String netName) {
        File f = new File(getNetworkFilePath(netName));
        return f.exists();
    }

    private ActivationFunction promptForActivationFunction() {
        System.out.println("\nSelect an Activation Function (recommended: TANH or RELU):");
        ActivationFunction[] funcs = ActivationFunction.values();
        for (int i = 0; i < funcs.length; i++) {
            System.out.println((i + 1) + ". " + funcs[i]);
        }
        System.out.print("Enter your choice (1-" + funcs.length + "): ");
        while (true) {
            try {
                int choice = Integer.parseInt(scanner.nextLine().trim());
                if (choice >= 1 && choice <= funcs.length) {
                    return funcs[choice - 1];
                }
                System.out.print("Invalid. Enter a number between 1 and " + funcs.length + ": ");
            } catch (NumberFormatException e) {
                System.out.print("Invalid. Enter a number between 1 and " + funcs.length + ": ");
            }
        }
    }

    private List<String> listNetworks() {
        File dir = new File(NETWORKS_DIR);
        File[] files = dir.listFiles((d, name) -> name.endsWith(NETWORK_FILE_EXTENSION));

        List<String> networks = new ArrayList<>();
        if (files != null) {
            for (File f : files) {
                String fileName = f.getName();
                String netName = fileName.substring(0, fileName.length() - NETWORK_FILE_EXTENSION.length());
                networks.add(netName);
            }
        }

        if (networks.isEmpty()) {
            System.out.println("No networks found in '" + NETWORKS_DIR + "'.");
            return networks;
        }

        System.out.println("\nAvailable Networks:");
        for (int i = 0; i < networks.size(); i++) {
            String nm = networks.get(i);
            String path = getNetworkFilePath(nm);
            // Attempt to load quickly for metadata
            NeuralNetworkTrainer tmp = NeuralNetworkInitializer.initializeTrainer();
            tmp.loadNetwork(path);
            System.out.println((i + 1) + ". " + nm
                + " | StatesTrained=" + tmp.getNumberOfStatesTrained()
                + " | LastOutput=" + tmp.getLastOutput());
        }

        return networks;
    }

    private String selectNetwork(List<String> nets) {
        if (nets.isEmpty()) return null;
        System.out.println("Enter the number of the network (0 to cancel): ");
        while (true) {
            String in = scanner.nextLine().trim();
            int choice;
            try {
                choice = Integer.parseInt(in);
            } catch (NumberFormatException e) {
                System.out.print("Invalid. Enter a number between 0 and " + nets.size() + ": ");
                continue;
            }
            if (choice == 0) {
                System.out.println("Canceled.");
                return null;
            } else if (choice >= 1 && choice <= nets.size()) {
                return nets.get(choice - 1);
            } else {
                System.out.print("Invalid. Enter a number between 0 and " + nets.size() + ": ");
            }
        }
    }

    private int getUserChoice(int min, int max) {
        while (true) {
            try {
                int ch = Integer.parseInt(scanner.nextLine().trim());
                if (ch >= min && ch <= max) return ch;
                System.out.print("Invalid. Enter a number " + min + ".." + max + ": ");
            } catch (NumberFormatException e) {
                System.out.print("Invalid. Enter a number " + min + ".." + max + ": ");
            }
        }
    }

    private int promptForNumber(String prompt, int min, int max) {
        System.out.print(prompt);
        while (true) {
            try {
                int val = Integer.parseInt(scanner.nextLine().trim());
                if (val >= min && val <= max) return val;
            } catch (NumberFormatException ignored) {}
            System.out.print("Invalid. " + prompt);
        }
    }

    private boolean promptForYesNo(String prompt) {
        System.out.print(prompt + " ");
        while (true) {
            String resp = scanner.nextLine().trim().toLowerCase();
            if (resp.equals("yes") || resp.equals("y")) return true;
            if (resp.equals("no") || resp.equals("n")) return false;
            System.out.print("Please enter yes or no: ");
        }
    }

    // ------------------------------------------------------------------------
    // (Metrics example) If you want to attach a new game result:
    //   recordGameResult(gameLength, winner, totalNodesEvaluated)
    // ------------------------------------------------------------------------
    public void recordGameResult(int moves, Player winner, int nodesEvaluated) {
        gamesPlayed++;
        double oldAvgLen = averageGameLength;
        averageGameLength = (oldAvgLen * (gamesPlayed - 1) + moves) / gamesPlayed;

        double oldAvgNodes = averageNodesEvaluated;
        averageNodesEvaluated = (oldAvgNodes * (gamesPlayed - 1) + nodesEvaluated) / gamesPlayed;

        // E.g. if we always consider "BLUE" as the net's color:
        if (winner != null && winner.getColor() == Color.BLUE) {
            // net won
            double oldWinRate = netWinRate;
            netWinRate = (oldWinRate * (gamesPlayed - 1) + 1.0) / gamesPlayed;
        } else {
            // net lost or draw
            double oldWinRate = netWinRate;
            netWinRate = (oldWinRate * (gamesPlayed - 1) + 0.0) / gamesPlayed;
        }
    }
}
