package com.Tak.AI.neuralnet.experiments;

import com.Tak.AI.neuralnet.net.ActivationFunction;
import com.Tak.AI.neuralnet.net.NeuralNetworkInitializer;
import com.Tak.AI.neuralnet.trainer.IterativeNetVsHeuristicTrainer;
import com.Tak.AI.neuralnet.trainer.NeuralNetworkTrainer;
import com.Tak.AI.neuralnet.trainer.SelfPlayMinimaxTrainer;

/**
 * A central place to store, organize, and run your test experiments
 * (activation functions, hidden sizes, self-play, continuing from existing net, etc.).
 */
public class ExperimentRunner {

    public static void runAllExperiments() {
        System.out.println("\n--- Running Automatic Experiments ---");
        runActivationFunctionExperiment();
        runHiddenLayerExperiment();
        runSelfPlayExperiment();
        runContinueFromExistingExperiment();
        System.out.println("\nAll experiments done!");
    }

    private static void runActivationFunctionExperiment() {
        System.out.println("Experiment A: Activation");
        ActivationFunction[] afs = {ActivationFunction.SIGMOID, ActivationFunction.TANH, ActivationFunction.RELU};
        for (ActivationFunction af : afs) {
            System.out.println("\n--- Testing AF = " + af + " ---");
            NeuralNetworkTrainer trainer = NeuralNetworkInitializer.initializeTrainer(new int[]{64}, af);
            IterativeNetVsHeuristicTrainer it = new IterativeNetVsHeuristicTrainer(trainer, 5, 5, 2, false);
            it.runIterativeTraining();
            System.out.println("Done with AF=" + af);
        }
    }

    private static void runHiddenLayerExperiment() {
        System.out.println("\nExperiment B: Hidden layers");
        int[][] layerConfigs = {{64},{128},{64,64}};
        for (int[] config : layerConfigs) {
            System.out.println("\n--- Testing hidden config=" + java.util.Arrays.toString(config) + " ---");
            NeuralNetworkTrainer trainer = NeuralNetworkInitializer.initializeTrainer(config, ActivationFunction.TANH);
            IterativeNetVsHeuristicTrainer it = new IterativeNetVsHeuristicTrainer(trainer,5,5,2,false);
            it.runIterativeTraining();
            System.out.println("Done config " + java.util.Arrays.toString(config));
        }
    }

    private static void runSelfPlayExperiment() {
        System.out.println("\nExperiment C: Self-play demonstration");
        NeuralNetworkTrainer nn = NeuralNetworkInitializer.initializeTrainer();
        SelfPlayMinimaxTrainer sp = new SelfPlayMinimaxTrainer(nn,10,5,2);
        sp.runTraining();
        System.out.println("Self-play done.\n");
    }

    private static void runContinueFromExistingExperiment() {
        System.out.println("\nExperiment D: Continue from existing net (if available).");
        String existing = "networks/MyNet.txt";
        java.io.File f = new java.io.File(existing);
        if(!f.exists()) {
            System.out.println("   - 'MyNet.txt' not found. Skipping.\n");
            return;
        }
        NeuralNetworkTrainer trainer = NeuralNetworkInitializer.initializeTrainer();
        trainer.loadNetwork(existing);

        IterativeNetVsHeuristicTrainer it = new IterativeNetVsHeuristicTrainer(trainer,5,5,1,false);
        it.runIterativeTraining();

        trainer.saveNetwork(existing);
        System.out.println("Updated MyNet.txt stored.\n");
    }
}
