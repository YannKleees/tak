package com.Tak.AI.neuralnet.trainer;

import com.Tak.AI.evaluation.HeuristicEvaluator;
import com.Tak.AI.neuralnet.net.BoardToInputsConverter;
import com.Tak.AI.neuralnet.net.NeuralNetworkEvaluator;
import com.Tak.AI.players.MinimaxAgent;
import com.Tak.AI.players.RandomAIPlayer;
import com.Tak.Logic.models.Player;
import com.Tak.Logic.models.TakGame;
import com.Tak.Logic.utils.GameOverException;
import com.Tak.Logic.utils.InvalidMoveException;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Self-play or AI vs AI training using Minimax + NeuralNetwork.
 *
 * Now with partial updates for BOTH sides, etc.
 */
public class SelfPlayMinimaxTrainer {

    private NeuralNetworkTrainer nnTrainer;

    private int numberOfGames;
    private int validationFrequency;
    private int patience;

    private ANNManager.OpponentType opponentType = ANNManager.OpponentType.NET_VS_NET;

    private String csvFilename = null;
    private BufferedWriter csvWriter = null;

    private double bestValidationLoss;
    private int patienceCounter;

    private int blueWinsCount = 0;
    private int lastGameMoves = -1;
    private String lastGameWinnerColor = "None";

    // UPDATED: discount changed to e.g. 0.90
    private static final double DISCOUNT = 0.90;

    private int randomOpeningPlies = 3;
    private int truncationLimit = -1; 

    private int totalStatesCollected = 0;
    private int lastGameStateCount = 0;

    public SelfPlayMinimaxTrainer(NeuralNetworkTrainer nnTrainer,
                                  int numberOfGames,
                                  int valFrequency,
                                  int patience) {
        this.nnTrainer = nnTrainer;
        this.numberOfGames = numberOfGames;
        this.validationFrequency = valFrequency;
        this.patience = patience;
        this.bestValidationLoss = Double.MAX_VALUE;
        this.patienceCounter = 0;
    }

    // same getters/setters as before ...

    public void runTraining() {
        initCsvWriter();
        for (int i = 0; i < numberOfGames; i++) {
            long start = System.currentTimeMillis();

            System.out.println("=== Self-Play Game " + (i+1) + "/" + numberOfGames + " ===");
            List<TrainingExample> gameData = playOneGame(i);

            // final big correction
            double oldLR = nnTrainer.getLearningRate();
            nnTrainer.setLearningRate(nnTrainer.getAfterGameRate());
            double gameLoss = nnTrainer.trainAll(gameData);
            nnTrainer.setLearningRate(oldLR);

            double valLoss = Double.NaN;
            if (validationFrequency > 0 && (i+1) % validationFrequency == 0) {
                valLoss = validateNetwork();
                if (valLoss < bestValidationLoss) {
                    bestValidationLoss = valLoss;
                    patienceCounter = 0;
                } else {
                    patienceCounter++;
                    if (patienceCounter >= patience) {
                        System.out.println("Early stopping at game " + (i+1));
                        logCsvRow(i+1, gameLoss, valLoss, bestValidationLoss, -1, 
                            "None", blueWinsCount, lastGameStateCount, totalStatesCollected,
                            getHeuristicSide(), "Unknown");
                        break;
                    }
                }
            }

            long end = System.currentTimeMillis();
            long gameTime = end - start;
            String phase = determineGamePhase(lastGameMoves);

            // logging row
            logCsvRow(
                i+1,
                gameLoss,
                valLoss,
                bestValidationLoss,
                lastGameMoves,
                lastGameWinnerColor,
                blueWinsCount,
                lastGameStateCount,
                totalStatesCollected,
                getHeuristicSide(),
                phase
            );

            System.out.println("Game " + (i+1) 
                + " ended in " + (gameTime/1000.0) + "s, moves=" + lastGameMoves 
                + ", statesThisGame=" + lastGameStateCount);
        }
        closeCsvWriter();
        System.out.println("\nSelf-play done. Total states: " + totalStatesCollected);
    }

    private List<TrainingExample> playOneGame(int idx) {
        TakGame g = createTrainingTakGame(5);
        List<StateRecord> rec = new ArrayList<>();

        while (!g.isGameEnded()) {
            int mc = g.getMoveCount();
            if (truncationLimit > 0 && mc >= truncationLimit) break;

            Player current = g.getCurrentPlayer();
            try {
                if (mc < randomOpeningPlies*2) {
                    doRandomMove(g, current, mc);
                } else {
                    double[] oldState = BoardToInputsConverter.convert(g.getBoard(), current);
                    current.makeMove(g);

                    // UPDATED: partial update for whichever side (both!)
                    if (!g.isGameEnded()) {
                        double[] nextState = BoardToInputsConverter.convert(g.getBoard(), current);
                        double[] nextVal = nnTrainer.getNetwork().forward(nextState);
                        for (int k=0;k<nextVal.length;k++){
                            nextVal[k]*=DISCOUNT;
                        }
                        nnTrainer.trainSingleSampleWithInGameRate(oldState, nextVal);
                    }

                    rec.add(new StateRecord(oldState, current));
                }
            } catch(Exception e) {
                break;
            }
        }

        Player w = g.getWinner();
        lastGameMoves = g.getMoveCount();
        if (w==null) {
            lastGameWinnerColor="None";
        } else {
            lastGameWinnerColor=w.getColor().toString();
            if (w.getColor() == Player.Color.BLUE) {
                blueWinsCount++;
            }
        }

        List<TrainingExample> finalEx = finalizeLabels(rec, w);
        lastGameStateCount = finalEx.size();
        totalStatesCollected+= lastGameStateCount;
        return finalEx;
    }

    private void doRandomMove(TakGame g, Player c, int mc) throws InvalidMoveException, GameOverException {
        List<String> moves= com.Tak.AI.utils.ActionGenerator.generatePossibleActions(g.getBoard(), c, mc);
        if (!moves.isEmpty()){
            java.util.Random r=new java.util.Random();
            String pick= moves.get(r.nextInt(moves.size()));
            com.Tak.AI.actions.Action a= com.Tak.AI.actions.Action.fromString(pick, c.getColor());
            a.execute(g.getBoard());
            g.incrementMoveCount();
            g.checkWinConditions();
            g.switchPlayer();
        }
    }

    private TakGame createTrainingTakGame(int sz){
        Player b,g;
        switch(opponentType){
            case NET_VS_HEURISTIC:
                b=new MinimaxAgent(Player.Color.BLUE,21,21,1,2,false,
                    new NeuralNetworkEvaluator(nnTrainer.getNetwork()));
                g=new MinimaxAgent(Player.Color.GREEN,21,21,1,2,false,
                    new HeuristicEvaluator());
                break;
            case NET_VS_RANDOM:
                b=new MinimaxAgent(Player.Color.BLUE,21,21,1,2,false,
                    new NeuralNetworkEvaluator(nnTrainer.getNetwork()));
                g=new RandomAIPlayer(Player.Color.GREEN,21,21,1);
                break;
            case NET_VS_OLD_NET:
                // old net not fully implemented
                System.out.println("NET_VS_OLD_NET => fallback net vs net");
                // fall-thru
            case NET_VS_NET:
            default:
                b=new MinimaxAgent(Player.Color.BLUE,21,21,1,2,false,
                    new NeuralNetworkEvaluator(nnTrainer.getNetwork()));
                g=new MinimaxAgent(Player.Color.GREEN,21,21,1,2,false,
                    new NeuralNetworkEvaluator(nnTrainer.getNetwork()));
                break;
        }
        return new TakGame(sz, Arrays.asList(b,g));
    }

    private List<TrainingExample> finalizeLabels(List<StateRecord> list, Player winner){
        List<TrainingExample> out=new ArrayList<>();
        for (StateRecord sr : list){
            double[] tar= new double[2];
            if (winner==null){
                tar[0]=0.0; tar[1]=0.0;
            } else {
                boolean isWin = (sr.mover==winner);
                if (sr.mover.getColor()==Player.Color.BLUE){
                    tar[0]= isWin? 1.0 : -1.0;
                    tar[1]= isWin? -1.0: 1.0;
                } else {
                    tar[0]= isWin? -1.0: 1.0;
                    tar[1]= isWin? 1.0: -1.0;
                }
            }
            out.add(new TrainingExample(sr.state, tar));
        }
        return out;
    }

    private double validateNetwork() {
        // small demonstration
        int valGames=3;
        double total=0.0;
        for(int i=0;i<valGames;i++){
            TakGame tg = createTrainingTakGame(5);
            List<TrainingExample> st= new ArrayList<>();
            while (!tg.isGameEnded()){
                try {
                    Player c= tg.getCurrentPlayer();
                    double[] s= BoardToInputsConverter.convert(tg.getBoard(), c);
                    c.makeMove(tg);
                    if(c.getColor()== Player.Color.BLUE){
                        st.add(new TrainingExample(s,new double[]{0,0}));
                    }
                } catch(Exception e){
                    break;
                }
            }
            Player w= tg.getWinner();
            for(TrainingExample ex: st){
                double[] netOut=nnTrainer.getNetwork().forward(ex.getInput());
                double[] label=new double[2];
                if(w==null){
                    label[0]=0; label[1]=0;
                } else if(w.getColor()== Player.Color.BLUE){
                    label[0]=1; label[1]=-1;
                } else {
                    label[0]=-1; label[1]=1;
                }
                double tmp=0.0;
                for (int k=0;k<2;k++){
                    tmp+= Math.pow(label[k]-netOut[k],2);
                }
                tmp/=2.0;
                total+= tmp;
            }
        }
        return total/valGames;
    }

    // CSV stuff remains the same, just with added columns

    private void initCsvWriter(){
        if (csvFilename==null)return;
        try{
            csvWriter = new BufferedWriter(new FileWriter(csvFilename,false));
            csvWriter.write("GameIndex,GameLoss,ValLoss,BestValLoss,"
                +"GameTimeMillis,GameLength,WinnerColor,BlueWinsSoFar,"
                +"StatesThisGame,TotalStatesSoFar,HeuristicSide,GamePhase");
            csvWriter.newLine();
            csvWriter.flush();
        }catch(IOException e){
            e.printStackTrace();
            csvWriter=null;
        }
    }

    private void logCsvRow(int gameIndex,
                           double gameLoss,
                           double valLoss,
                           double bestValLoss,
                           int gameLength,
                           String winnerColor,
                           int blueWins,
                           int statesThisGame,
                           int totalStates,
                           String heuristicSide,
                           String phase){
        if(csvWriter==null)return;
        try{
            String vs= Double.isNaN(valLoss)? "":String.format("%.6f", valLoss);
            String row= String.format("%d,%.6f,%s,%.6f,%d,%s,%d,%d,%d,%s,%s",
                gameIndex,gameLoss,vs,bestValLoss,gameLength,winnerColor,blueWins,
                statesThisGame,totalStates,heuristicSide,phase);
            csvWriter.write(row);
            csvWriter.newLine();
            csvWriter.flush();
        }catch(IOException e){
            System.err.println("CSV error: "+e.getMessage());
        }
    }

    private void closeCsvWriter(){
        if(csvWriter!=null){
            try{csvWriter.close();}catch(IOException e){e.printStackTrace();}
        }
    }

    private String getHeuristicSide(){
        if(opponentType==ANNManager.OpponentType.NET_VS_HEURISTIC){
            return "Green";
        }
        return "None";
    }

    private String determineGamePhase(int mc){
        if(mc<0)return "Unknown";
        if(mc<10)return "Early";
        if(mc<30)return "Mid";
        return "Late";
    }

    private static class StateRecord {
        double[] state;
        Player mover;
        StateRecord(double[] s, Player m){
            this.state=s;this.mover=m;
        }
    }

    public static class TrainingExample {
        private double[] input;
        private double[] target;
        public TrainingExample(double[] i, double[] t){
            this.input=i; this.target=t;
        }
        public double[] getInput(){return input;}
        public double[] getTarget(){return target;}
    }
}
