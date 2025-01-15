package com.Tak.AI.neuralnet;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class NeuralNetManager {

    private static final String NETWORK_DIR = "neuralnet";
    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        NeuralNetManager manager = new NeuralNetManager();
        manager.run();
    }

    public void run() {
        while (true) {
            displayMainMenu();
            int choice = getUserChoice(1, 3);
            switch (choice) {
                case 1:
                    continueTraining();
                    break;
                case 2:
                    createNewNetwork();
                    break;
                case 3:
                    System.out.println("Exiting Neural Network Manager. Goodbye!");
                    scanner.close();
                    System.exit(0);
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    private void displayMainMenu() {
        System.out.println("\n=== Neural Network Manager ===");
        System.out.println("1. Continue Training an Existing Network");
        System.out.println("2. Create a New Neural Network");
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

    private void continueTraining() {
        List<String> networks = listNetworks();
        if (networks.isEmpty()) {
            System.out.println("No existing networks found. Please create a new network first.");
            return;
        }

        System.out.println("\n=== Select a Neural Network to Continue Training ===");
        for (int i = 0; i < networks.size(); i++) {
            System.out.println((i + 1) + ". " + networks.get(i));
        }
        System.out.println((networks.size() + 1) + ". Cancel");
        System.out.print("Enter your choice (1-" + (networks.size() + 1) + "): ");

        int choice = getUserChoice(1, networks.size() + 1);
        if (choice == networks.size() + 1) {
            System.out.println("Cancelled selection.");
            return;
        }

        String selectedNetwork = networks.get(choice - 1);
        System.out.println("Selected Network: " + selectedNetwork);

        // Load the selected network
        NeuralNetworkTrainer trainer = NeuralNetworkInitializer.initializeTrainer();
        trainer.loadNetwork(NETWORK_DIR + File.separator + selectedNetwork);

        // Proceed with training
        int numberOfGames = promptForNumber("Enter the number of training games to run: ", 1, Integer.MAX_VALUE);
        SelfPlayMinimaxTrainer selfPlayTrainer = new SelfPlayMinimaxTrainer(trainer, numberOfGames);
        selfPlayTrainer.runTraining();

        // Save the trained network
        trainer.saveNetwork(NETWORK_DIR + File.separator + selectedNetwork);
        System.out.println("Training completed and network saved to " + selectedNetwork);
    }

    private void createNewNetwork() {
        System.out.println("\n=== Create a New Neural Network ===");

        int inputSize = promptForNumber("Enter the input size: ", 1, Integer.MAX_VALUE);
        int numHiddenLayers = promptForNumber("Enter the number of hidden layers: ", 1, 10);

        int[] hiddenLayerSizes = new int[numHiddenLayers];
        for (int i = 0; i < numHiddenLayers; i++) {
            hiddenLayerSizes[i] = promptForNumber("Enter the size of hidden layer " + (i + 1) + ": ", 1, 1000);
        }

        int outputSize = promptForNumber("Enter the output size: ", 1, Integer.MAX_VALUE);
        ActivationFunction activationFunction = promptForActivationFunction();

        // Create the configuration
        NeuralNetworkConfig config = new NeuralNetworkConfig(inputSize, hiddenLayerSizes, outputSize, activationFunction);
        NeuralNetwork network = new NeuralNetwork(config);
        double learningRate = 0.01;
        NeuralNetworkTrainer trainer = new NeuralNetworkTrainer(network, learningRate);

        // Save the new network
        String networkName = promptForNetworkName();
        String savePath = NETWORK_DIR + File.separator + networkName + ".txt";
        trainer.saveNetwork(savePath);
        System.out.println("New neural network created and saved as " + networkName + ".txt");
    }

    private List<String> listNetworks() {
        List<String> networks = new ArrayList<>();
        File dir = new File(NETWORK_DIR);
        if (!dir.exists()) {
            dir.mkdir();
            return networks;
        }

        File[] files = dir.listFiles((d, name) -> name.endsWith(".txt"));
        if (files != null) {
            for (File file : files) {
                networks.add(file.getName());
            }
        }
        return networks;
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

    private ActivationFunction promptForActivationFunction() {
        System.out.println("Select Activation Function:");
        ActivationFunction[] functions = ActivationFunction.values();
        for (int i = 0; i < functions.length; i++) {
            System.out.println((i + 1) + ". " + functions[i]);
        }
        System.out.print("Enter your choice (1-" + functions.length + "): ");

        int choice = getUserChoice(1, functions.length);
        return functions[choice - 1];
    }

    private String promptForNetworkName() {
        System.out.print("Enter a name for the new neural network: ");
        while (true) {
            String name = scanner.nextLine().trim();
            if (name.isEmpty()) {
                System.out.print("Name cannot be empty. Enter a valid name: ");
            } else if (name.contains(" ") || name.contains("/") || name.contains("\\")) {
                System.out.print("Name contains invalid characters. Enter a different name: ");
            } else {
                File file = new File(NETWORK_DIR + File.separator + name + ".txt");
                if (file.exists()) {
                    System.out.print("A network with this name already exists. Enter a different name: ");
                } else {
                    return name;
                }
            }
        }
    }
}
