package com.Tak.AI.neuralnet.net;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import com.Tak.AI.neuralnet.trainer.NeuralNetworkTrainer;

/**
 * Handles saving/loading a NeuralNetworkTrainer to/from a .txt file.
 * Plain text approach, no JSON library needed.
 */
public class NeuralNetworkTxtManager {

    public static void saveTrainer(NeuralNetworkTrainer trainer,
                                   String filePath,
                                   int numberOfGames,
                                   double lastOutput) throws IOException {
        try(BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            NeuralNetworkConfig cfg = trainer.getNetwork().getConfig();
            writer.write("[Configuration]");
            writer.newLine();
            writer.write("inputSize="+cfg.getInputSize());
            writer.newLine();
            writer.write("numHiddenLayers="+cfg.getHiddenLayerSizes().length);
            writer.newLine();
            for(int i=0; i<cfg.getHiddenLayerSizes().length; i++){
                writer.write("hiddenSize"+i+"="+cfg.getHiddenLayerSizes()[i]);
                writer.newLine();
            }
            writer.write("outputSize="+cfg.getOutputSize());
            writer.newLine();
            writer.write("activationFunction="+cfg.getActivationFunction());
            writer.newLine();
            writer.newLine();

            // Weights + Bias
            double[][][] weights = trainer.getNetwork().getWeights();
            double[][] biases = trainer.getNetwork().getBiases();

            for(int layerIndex=0; layerIndex<weights.length; layerIndex++){
                writer.write("[WeightsLayer"+layerIndex+"]");
                writer.newLine();
                for(double[] row : weights[layerIndex]){
                    writer.write(joinArray(row, " "));
                    writer.newLine();
                }
                writer.newLine();

                writer.write("[BiasLayer"+layerIndex+"]");
                writer.newLine();
                writer.write(joinArray(biases[layerIndex], " "));
                writer.newLine();
                writer.newLine();
            }

            // metadata
            writer.write("[Metadata]");
            writer.newLine();
            writer.write("numberOfGamesTrained="+numberOfGames);
            writer.newLine();
            writer.write("lastOutput="+lastOutput);
            writer.newLine();
        }
    }

    private static String joinArray(double[] arr, String delim){
        StringBuilder sb = new StringBuilder();
        for(int i=0; i<arr.length; i++){
            sb.append(arr[i]);
            if(i<arr.length-1) sb.append(delim);
        }
        return sb.toString();
    }

    public static NeuralNetworkLoadData loadTrainer(String filePath) throws IOException {
        double[][][] weights=null;
        double[][] biases=null;
        int numGames=0; 
        double lastOut=0.0;

        int inputSize=0, numHidden=0, outputSize=0;
        ActivationFunction af = ActivationFunction.TANH;
        List<Integer> hiddenSizes = new ArrayList<>();

        int currentLayerIndex=-1;
        List<double[]> currentLayerWeights = new ArrayList<>();
        List<Double> currentBiases = new ArrayList<>();

        try(BufferedReader reader = new BufferedReader(new FileReader(filePath))){
            String line;
            while((line=reader.readLine())!=null){
                line=line.trim();
                if(line.isEmpty()) continue;

                if(line.startsWith("[Configuration]")){
                    while((line=reader.readLine())!=null && !line.startsWith("[")){
                        String[] parts = line.split("=");
                        if(parts.length==2){
                            String k=parts[0], v=parts[1];
                            if(k.equals("inputSize")) inputSize=Integer.parseInt(v);
                            else if(k.equals("numHiddenLayers")) numHidden=Integer.parseInt(v);
                            else if(k.startsWith("hiddenSize")){
                                hiddenSizes.add(Integer.parseInt(v));
                            }
                            else if(k.equals("outputSize")) outputSize=Integer.parseInt(v);
                            else if(k.equals("activationFunction")) af=ActivationFunction.valueOf(v);
                        }
                    }
                    int totalLayers = numHidden+1;
                    weights = new double[totalLayers][][];
                    biases  = new double[totalLayers][];
                    if(line==null) break;
                    // line now is next tag
                    if(line.startsWith("[WeightsLayer")){
                        currentLayerIndex = Integer.parseInt(line.substring("[WeightsLayer".length(), line.indexOf("]")));
                    } else if(line.startsWith("[Metadata]")){
                        // We'll handle it below
                    } else if(line.startsWith("[BiasLayer")){
                        currentLayerIndex = Integer.parseInt(line.substring("[BiasLayer".length(), line.indexOf("]")));
                    } else {
                        // might do nothing
                    }
                }
                else if(line.startsWith("[WeightsLayer")){
                    // store previous if needed
                    if(currentLayerIndex!=-1 && !currentLayerWeights.isEmpty()){
                        weights[currentLayerIndex] = currentLayerWeights.toArray(new double[0][]);
                        currentLayerWeights.clear();
                    }
                    currentLayerIndex = Integer.parseInt(line.substring("[WeightsLayer".length(), line.indexOf("]")));
                }
                else if(line.startsWith("[BiasLayer")){
                    if(currentLayerIndex!=-1 && !currentLayerWeights.isEmpty()){
                        weights[currentLayerIndex] = currentLayerWeights.toArray(new double[0][]);
                        currentLayerWeights.clear();
                    }
                    currentLayerIndex = Integer.parseInt(line.substring("[BiasLayer".length(), line.indexOf("]")));
                    // read next line for biases
                    line = reader.readLine();
                    if(line!=null){
                        line=line.trim();
                        String[] tokens=line.split(" ");
                        currentBiases.clear();
                        for(String tok : tokens){
                            currentBiases.add(Double.parseDouble(tok));
                        }
                        biases[currentLayerIndex] = currentBiases.stream().mapToDouble(Double::doubleValue).toArray();
                    }
                }
                else if(line.startsWith("[Metadata]")){
                    if(currentLayerIndex!=-1 && !currentLayerWeights.isEmpty()){
                        weights[currentLayerIndex] = currentLayerWeights.toArray(new double[0][]);
                        currentLayerWeights.clear();
                    }
                    // read metadata lines
                    while((line=reader.readLine())!=null && !line.startsWith("[")){
                        String[] parts = line.split("=");
                        if(parts.length==2){
                            if(parts[0].equals("numberOfGamesTrained")){
                                numGames=Integer.parseInt(parts[1]);
                            }
                            else if(parts[0].equals("lastOutput")){
                                lastOut=Double.parseDouble(parts[1]);
                            }
                        }
                    }
                    break;
                }
                else {
                    // parse row of weights
                    String[] tokens=line.split(" ");
                    double[] row = new double[tokens.length];
                    for(int i=0; i<tokens.length; i++){
                        row[i] = Double.parseDouble(tokens[i]);
                    }
                    currentLayerWeights.add(row);
                }
            }
        }

        // last assignment
        if(currentLayerIndex!=-1 && !currentLayerWeights.isEmpty()){
            weights[currentLayerIndex] = currentLayerWeights.toArray(new double[0][]);
        }

        NeuralNetworkConfig cfg = new NeuralNetworkConfig(
            inputSize,
            hiddenSizes.stream().mapToInt(i->i).toArray(),
            outputSize,
            af
        );
        NeuralNetwork net = new NeuralNetwork(cfg);
        net.setWeights(weights);
        net.setBiases(biases);

        return new NeuralNetworkLoadData(net, numGames, lastOut);
    }

    public static class NeuralNetworkLoadData {
        public NeuralNetwork network;
        public int numberOfGamesTrained;
        public double lastOutput;

        public NeuralNetworkLoadData(NeuralNetwork net, int n, double lo){
            this.network=net;
            this.numberOfGamesTrained=n;
            this.lastOutput=lo;
        }
    }
}
