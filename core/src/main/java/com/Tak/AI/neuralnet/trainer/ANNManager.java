package com.Tak.AI.neuralnet.trainer;

import com.Tak.AI.neuralnet.experiments.ExperimentRunner;
import com.Tak.AI.neuralnet.net.ActivationFunction;
import com.Tak.AI.neuralnet.net.NeuralNetworkInitializer;
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

    // Example metrics
    private double averageGameLength;
    private double netWinRate;
    private double averageNodesEvaluated;
    private int gamesPlayed;

    // ADDED: OpponentType enum for user selection
    public enum OpponentType {
        NET_VS_NET,
        NET_VS_HEURISTIC,
        NET_VS_RANDOM,
        NET_VS_OLD_NET
    }

    public static void main(String[] args) {
        ANNManager manager = new ANNManager();
        manager.run();
    }

    public void run() {
        ensureNetworksDirectory();

        System.out.println("\n=== Welcome to the central ANN Manager ===");
        while (running) {
            displayMainMenu();
            int choice = getUserChoice(1, 8);
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
                    ExperimentRunner.runAllExperiments();
                    break;
                case 7:
                    // ADDED: Let user do some specialized “opponent-type” self-play
                    selfPlayWithOpponentTypeMenu();
                    break;
                case 8:
                    exitManager();
                    break;
            }
        }
    }

    private void displayMainMenu() {
        System.out.println("\n--- Main Menu ---");
        System.out.println("1. Create a New Neural Network");
        System.out.println("2. Iterative Training vs. Heuristic");
        System.out.println("3. Self-Play Training");
        System.out.println("4. Delete an Existing Neural Network");
        System.out.println("5. Show Training Metrics");
        System.out.println("6. Run Experiments");
        System.out.println("7. Self-Play with Different Opponent Types (NEW)");
        System.out.println("8. Exit");
        System.out.print("Enter your choice (1-8): ");
    }

    private void exitManager() {
        System.out.println("Exiting ANNManager. Goodbye!");
        scanner.close();
        running = false;
    }

    private void createNewNetwork() {
        System.out.println("\n--- Create a New Neural Network ---");
        String networkName;
        while (true) {
            System.out.print("Enter a unique name: ");
            networkName = scanner.nextLine().trim();
            if (networkName.isEmpty()) {
                System.out.println("Name cannot be empty.");
                continue;
            }
            if (networkExists(networkName)) {
                System.out.println("Network exists. Pick another name.");
            } else {
                break;
            }
        }

        int numHiddenLayers = promptForNumber("Number of hidden layers: ", 1, 10);
        int[] hiddenLayerSizes = new int[numHiddenLayers];
        for (int i = 0; i < numHiddenLayers; i++) {
            hiddenLayerSizes[i] = promptForNumber(
                "Neurons in hidden layer " + (i + 1) + ": ", 1, 1000
            );
        }

        ActivationFunction activationFunction = promptForActivationFunction();
        NeuralNetworkTrainer trainer =
            NeuralNetworkInitializer.initializeTrainer(hiddenLayerSizes, activationFunction);

        String networkFilePath = getNetworkFilePath(networkName);
        trainer.saveNetwork(networkFilePath);
        System.out.println("New net '" + networkName + "' created and saved to '" + networkFilePath + "'.");
    }

    private void iterativeTrainingMenu() {
        System.out.println("\n--- Iterative Training vs. Heuristic ---");
        List<String> networks = listNetworks();
        if (networks.isEmpty()) {
            System.out.println("No existing nets found.");
            return;
        }

        System.out.println("\nSelect the network (BLUE) to iteratively train vs. Heuristic (GREEN):");
        String selectedNetwork = selectNetwork(networks);
        if (selectedNetwork == null) {
            return;
        }

        String networkFilePath = getNetworkFilePath(selectedNetwork);
        NeuralNetworkTrainer trainer = NeuralNetworkInitializer.initializeTrainer();
        trainer.loadNetwork(networkFilePath);

        int totalRounds = promptForNumber("Total rounds: ", 1, 1000);
        int gamesPerRound = promptForNumber("Games per round: ", 1, 10000);
        boolean printBoard = promptForYesNo("Print final board each game? (yes/no): ");
        int boardSize = promptForNumber("Board size (e.g. 5): ", 3, 10);

        IterativeNetVsHeuristicTrainer it =
            new IterativeNetVsHeuristicTrainer(
                trainer, boardSize, gamesPerRound, totalRounds, printBoard
            );

        long startTime = System.currentTimeMillis();
        it.runIterativeTraining();
        long endTime = System.currentTimeMillis();
        System.out.printf("Finished in %.2f sec.\n", (endTime - startTime)/1000.0);

        trainer.saveNetwork(networkFilePath);
        System.out.println("Network updated and saved.");

        // -------------------------------------------------------------
        // ADDED for Over/Under-fitting checks:
        // We now retrieve final train/validation loss from the trainer
        double finalTrainLoss = it.getFinalTrainLoss(); // or whatever your trainer calls it
        double finalValLoss   = it.getFinalValLoss();   // if you do have a validation step

        // Heuristic approach to detect overfitting vs underfitting:
        if (finalTrainLoss < finalValLoss * 0.8) {
            System.out.println("WARNING: Potential overfitting (training loss << validation loss).");
        } else if (finalTrainLoss > finalValLoss * 1.2) {
            System.out.println("WARNING: Potential underfitting (training loss >> validation loss).");
        } else {
            System.out.println("Looks okay (training vs validation loss are relatively close).");
        }
        // -------------------------------------------------------------
    }


    private void selfPlayTrainingMenu() {
        System.out.println("\n--- Self-Play Training ---");
        List<String> networks = listNetworks();
        if (networks.isEmpty()) {
            System.out.println("No existing nets found.");
            return;
        }

        System.out.println("\nSelect net for self-play training:");
        String selectedNetwork = selectNetwork(networks);
        if (selectedNetwork == null) {
            return;
        }

        String networkFilePath = getNetworkFilePath(selectedNetwork);
        NeuralNetworkTrainer trainer = NeuralNetworkInitializer.initializeTrainer();
        trainer.loadNetwork(networkFilePath);

        int numberOfGames = promptForNumber("How many self-play games?: ", 1, 50000);
        int valFrequency = promptForNumber("Validation frequency (#games): ", 1, numberOfGames);
        int patience = promptForNumber("Patience: ", 1, 100);

        boolean netVsNet = promptForYesNo("Use Net-vs-Net? (yes = net vs net, no = net vs heuristic)");
        int randomOpening = promptForNumber("How many random opening moves per side? (0=no random): ", 0, 10);
        boolean doCsv = promptForYesNo("Save CSV logs of training? (yes/no)");
        String csvName = null;
        if (doCsv) {
            System.out.print("Enter CSV filename (e.g. MyTraining.csv): ");
            csvName = scanner.nextLine().trim();
        }

        // Create the trainer
        SelfPlayMinimaxTrainer sp =
            new SelfPlayMinimaxTrainer(trainer, numberOfGames, valFrequency, patience);

        //sp.setSelfPlayNetVsNet(netVsNet);
        //sp.setRandomOpeningPlies(randomOpening);

        if (doCsv && !csvName.isEmpty()) {
            //sp.setCsvLogging(csvName);
        }

        long startTime = System.currentTimeMillis();
        sp.runTraining();
        long endTime = System.currentTimeMillis();
        System.out.printf("Self-play took %.2f sec.\n", (endTime - startTime)/1000.0);

        trainer.saveNetwork(networkFilePath);
        System.out.println("Self-play done. Net saved.");
    }

    // ADDED: new menu to ask user for OpponentType
    private void selfPlayWithOpponentTypeMenu() {
        List<String> networks = listNetworks();
        if (networks.isEmpty()) {
            System.out.println("No existing nets found.");
            return;
        }

        System.out.println("\nSelect net for self-play training:");
        String selectedNetwork = selectNetwork(networks);
        if (selectedNetwork == null) {
            return;
        }

        // Let the user pick an OpponentType
        OpponentType chosenType = promptForOpponentType();

        String networkFilePath = getNetworkFilePath(selectedNetwork);
        NeuralNetworkTrainer trainer = NeuralNetworkInitializer.initializeTrainer();
        trainer.loadNetwork(networkFilePath);

        int numberOfGames = promptForNumber("How many self-play games?: ", 1, 1000);

        // Create a new custom self-play trainer
        SelfPlayMinimaxTrainer sp = new SelfPlayMinimaxTrainer(trainer, numberOfGames, 0, 0);
        //sp.setCsvLogging(null); // skip CSV for brevity
        //sp.setRandomOpeningPlies(3);

        // ADDED: set the opponent type
        //sp.setOpponentType(chosenType);

        System.out.println("\nStarting self-play with " + chosenType + "...");

        sp.runTraining();
        trainer.saveNetwork(networkFilePath);

        System.out.println("Done self-play with " + chosenType + ". Network updated/saved.");
    }

    // ADDED: let user choose among the enum
    private OpponentType promptForOpponentType() {
        System.out.println("\nChoose Opponent Type:");
        OpponentType[] values = OpponentType.values();
        for (int i = 0; i < values.length; i++) {
            System.out.println((i + 1) + ". " + values[i]);
        }
        System.out.print("Pick: ");
        while (true) {
            try {
                int c = Integer.parseInt(scanner.nextLine().trim());
                if (c >= 1 && c <= values.length) {
                    return values[c - 1];
                }
                System.out.print("Invalid. ");
            } catch (Exception e) {
                System.out.print("Invalid. ");
            }
        }
    }

    private void deleteNetwork() {
        System.out.println("\n--- Delete a Network ---");
        List<String> networks = listNetworks();
        if (networks.isEmpty()) {
            System.out.println("None found.");
            return;
        }

        String selectedNetwork = selectNetwork(networks);
        if (selectedNetwork == null) {
            return;
        }

        System.out.print("Sure to delete '" + selectedNetwork + "'? (yes/no): ");
        String conf = scanner.nextLine().trim().toLowerCase();
        if (!conf.equals("yes") && !conf.equals("y")) {
            System.out.println("Canceled.");
            return;
        }

        String filePath = getNetworkFilePath(selectedNetwork);
        File f = new File(filePath);
        if (f.delete()) {
            System.out.println("Deleted " + selectedNetwork);
        } else {
            System.out.println("Failed to delete. Try again.");
        }
    }

    private void showMetrics() {
        System.out.println("\n--- Current Metrics ---");
        System.out.printf("Games played: %d\n", gamesPlayed);
        System.out.printf("Avg game length: %.2f\n", averageGameLength);
        System.out.printf("Net win rate: %.2f\n", netWinRate);
        System.out.printf("Avg nodes evaluated: %.2f\n", averageNodesEvaluated);
    }

    // Utilities

    private void ensureNetworksDirectory() {
        File dir = new File(NETWORKS_DIR);
        if (!dir.exists()) {
            boolean created = dir.mkdir();
            if (!created) {
                System.out.println("Failed to create networks folder.");
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
        System.out.println("\nChoose activation function:");
        ActivationFunction[] funcs = ActivationFunction.values();
        for (int i = 0; i < funcs.length; i++) {
            System.out.println((i + 1) + ". " + funcs[i]);
        }
        System.out.print("Pick: ");
        while (true) {
            try {
                int c = Integer.parseInt(scanner.nextLine().trim());
                if (c >= 1 && c <= funcs.length) {
                    return funcs[c - 1];
                }
                System.out.print("Invalid. ");
            } catch (Exception e) {
                System.out.print("Invalid. ");
            }
        }
    }

    private List<String> listNetworks() {
        File dir = new File(NETWORKS_DIR);
        File[] files = dir.listFiles((d,n)->n.endsWith(NETWORK_FILE_EXTENSION));

        List<String> nets = new ArrayList<>();
        if (files != null) {
            for (File f : files) {
                String nm = f.getName();
                nm = nm.substring(0, nm.length() - NETWORK_FILE_EXTENSION.length());
                nets.add(nm);
            }
        }
        if (nets.isEmpty()) {
            System.out.println("No networks found in " + NETWORKS_DIR);
            return nets;
        }

        System.out.println("\nAvailable:");
        for (int i = 0; i < nets.size(); i++) {
            System.out.println((i+1) + ". " + nets.get(i));
        }
        return nets;
    }

    private String selectNetwork(List<String> nets) {
        if (nets.isEmpty()) return null;
        System.out.println("Enter # (0=cancel): ");
        while (true) {
            String in = scanner.nextLine().trim();
            int ch;
            try {
                ch = Integer.parseInt(in);
            } catch(Exception e) {
                continue;
            }
            if (ch == 0) {
                return null;
            }
            if (ch >= 1 && ch <= nets.size()) {
                return nets.get(ch-1);
            }
            System.out.print("Invalid. ");
        }
    }

    private int getUserChoice(int min, int max) {
        while (true) {
            try {
                int c = Integer.parseInt(scanner.nextLine().trim());
                if (c >= min && c <= max) return c;
            } catch(Exception e) { }
            System.out.printf("Invalid. Enter %d..%d: ", min, max);
        }
    }

    private int promptForNumber(String prompt, int min, int max) {
        System.out.print(prompt);
        while (true) {
            try {
                int val = Integer.parseInt(scanner.nextLine().trim());
                if (val >= min && val <= max) return val;
            } catch(Exception e){}
            System.out.print("Invalid. " + prompt);
        }
    }

    private boolean promptForYesNo(String prompt) {
        System.out.print(prompt + " ");
        while(true) {
            String ans = scanner.nextLine().trim().toLowerCase();
            if (ans.equals("y") || ans.equals("yes")) return true;
            if (ans.equals("n") || ans.equals("no")) return false;
            System.out.print("Please enter yes or no: ");
        }
    }

    /**
     * Example for recording post-game stats:
     */
    public void recordGameResult(int moves, Player winner, int nodesEvaluated) {
        gamesPlayed++;
        double oldAvg = averageGameLength;
        averageGameLength = ((oldAvg * (gamesPlayed -1)) + moves) / gamesPlayed;

        double oldNodes = averageNodesEvaluated;
        averageNodesEvaluated = ((oldNodes * (gamesPlayed -1)) + nodesEvaluated) / gamesPlayed;

        if (winner != null && winner.getColor() == Color.BLUE) {
            double oldWin = netWinRate;
            netWinRate = ((oldWin * (gamesPlayed -1)) + 1) / gamesPlayed;
        } else {
            double oldWin = netWinRate;
            netWinRate = ((oldWin * (gamesPlayed -1)) + 0) / gamesPlayed;
        }
    }
}
