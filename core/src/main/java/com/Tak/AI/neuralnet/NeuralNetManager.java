package com.Tak.AI.neuralnet;

import com.Tak.AI.experiments.NeuralNetVsHeuristicExperiment;
import com.Tak.AI.neuralnet.NeuralNetworkInitializer;
import com.Tak.AI.neuralnet.NeuralNetworkTrainer;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

/**
 * A simplified NeuralNetManager that works with a single network file ("trained_network.txt"),
 * allowing you to continue training or run experiments without creating multiple .txt files.
 */
public class NeuralNetManager {

    private static final String SINGLE_NETWORK_FILE = "trained_network.txt";
    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        NeuralNetManager manager = new NeuralNetManager();
        manager.run();
    }

    public void run() {
        System.out.println("\n=== Welcome to the Single-File Neural Network Manager ===");

        // If the file doesn't exist yet, create a new network and save it once.
        File networkFile = new File(SINGLE_NETWORK_FILE);
        if (!networkFile.exists()) {
            System.out.println("No existing network file found. Creating a new one...");
            createNewNetwork();
        } else {
            System.out.println("Existing network file found. You can continue training or run an experiment.");
        }

        while (true) {
            displayMainMenu();
            int choice = getUserChoice(1, 4);
            switch (choice) {
                case 1:
                    continueTraining();
                    break;
                case 2:
                    runExperiment();
                    break;
                case 3:
                    System.out.println("Exiting Single-File Neural Network Manager. Goodbye!");
                    scanner.close();
                    System.exit(0);
                    break;
                case 4: // This won't happen due to the range (1-3), but left in for clarity
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    /**
     * Displays a simple main menu with only three options:
     * 1) Continue training the single-file network
     * 2) Run an experiment vs. the heuristic approach
     * 3) Exit
     */
    private void displayMainMenu() {
        System.out.println("\n--- Main Menu ---");
        System.out.println("1. Continue Training");
        System.out.println("2. Run NeuralNet vs. Heuristic Experiment");
        System.out.println("3. Exit");
        System.out.print("Enter your choice (1-3): ");
    }

    private int getUserChoice(int min, int max) {
        while (true) {
            try {
                int choice = Integer.parseInt(scanner.nextLine().trim());
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
     * Creates a fresh network file if none exists. 
     * Called once at startup if "trained_network.txt" doesn't exist.
     */
    private void createNewNetwork() {
        NeuralNetworkTrainer trainer = NeuralNetworkInitializer.initializeTrainer();
        trainer.saveNetwork(SINGLE_NETWORK_FILE);
        System.out.println("New network created and saved to '" + SINGLE_NETWORK_FILE + "'.");
    }

    /**
     * Loads the single-file network from "trained_network.txt" and asks the user 
     * for how many self-play (or any other approach) training games they'd like to run.
     * Then saves back to the same file.
     */
    private void continueTraining() {
        // Load existing network
        NeuralNetworkTrainer trainer = NeuralNetworkInitializer.initializeTrainer();
        trainer.loadNetwork(SINGLE_NETWORK_FILE);

        // Let user choose how many games to train
        int numberOfGames = promptForNumber("Enter the number of self-play training games: ", 1, Integer.MAX_VALUE);
        
        // Run SelfPlayMinimaxTrainer or any custom training logic
        SelfPlayMinimaxTrainer selfPlayTrainer = new SelfPlayMinimaxTrainer(trainer, numberOfGames);
        selfPlayTrainer.runTraining();

        // Save back to the same file
        trainer.saveNetwork(SINGLE_NETWORK_FILE);
        System.out.println("Training completed. Network updated in '" + SINGLE_NETWORK_FILE + "'.");
    }

    /**
     * Runs the NeuralNet vs. Heuristic experiment with the single-file network. 
     * Shows metrics to indicate if/when the ANN outperforms the heuristic approach.
     */
    private void runExperiment() {
        // Load the network
        NeuralNetworkTrainer trainer = NeuralNetworkInitializer.initializeTrainer();
        trainer.loadNetwork(SINGLE_NETWORK_FILE);

        // Create a NeuralNetworkEvaluator from the trainer's network
        NeuralNetworkEvaluator nnEvaluator = new NeuralNetworkEvaluator(trainer.getNetwork());

        // We pass the nnEvaluator AND the trainer to the experiment so it can
        // retrain or do post-processing if we want. (You can omit if not needed.)
        NeuralNetVsHeuristicExperiment experiment =
                new NeuralNetVsHeuristicExperiment(nnEvaluator, trainer);

        // Run the experiment (the experiment class will handle the details)
        try {
            experiment.run();
        } catch (IOException e) {
            System.out.println("Error during experiment: " + e.getMessage());
        }

        // Optionally re-save if the experiment triggers any training
        trainer.saveNetwork(SINGLE_NETWORK_FILE);
        System.out.println("Experiment finished. Network saved to '" + SINGLE_NETWORK_FILE + "'.");
    }

    private int promptForNumber(String prompt, int min, int max) {
        System.out.print(prompt);
        while (true) {
            try {
                int number = Integer.parseInt(scanner.nextLine().trim());
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
}
