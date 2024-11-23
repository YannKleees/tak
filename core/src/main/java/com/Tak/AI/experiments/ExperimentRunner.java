package com.Tak.AI.experiments;

import java.util.List;
import java.util.ArrayList;

public class ExperimentRunner {
    public static void main(String[] args) {
        MinimaxExperiment experiment = new MinimaxExperiment();

        List<Results.DepthExperimentResult> depthResults = experiment.runDepthAnalysis(1, 5, 10);
        System.out.println("Depth results: " + depthResults);

        List<Results.OpeningExperimentResult> positionResults = experiment.runPositionAnalysis(3);
        System.out.println("Position results: " + positionResults);

        List<Results.BranchingExperimentResult> branchingResults = experiment.runBranchingAnalysis(3);
        System.out.println("Branching results: " + branchingResults);
    }
}
