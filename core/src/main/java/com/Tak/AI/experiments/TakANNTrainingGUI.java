package com.Tak.AI.experiments;

import com.Tak.AI.neuralnet.*;
import com.Tak.AI.neuralnet.SelfPlayMinimaxTrainer.TrainingExample;
import com.Tak.AI.players.MinimaxAgent;
import com.Tak.Logic.models.Board;
import com.Tak.Logic.models.Piece;
import com.Tak.Logic.models.PieceStack;
import com.Tak.Logic.models.Player;
import com.Tak.Logic.models.Player.Color;
import com.Tak.Logic.models.TakGame;
import com.Tak.AI.neuralnet.ForwardPropagator; // We assume you have a ForwardCache class in here.

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.geom.Line2D;
import java.util.*;
import java.util.List;

/**
 * Updated GUI that checks if the net is improving over time
 * and now supports switching between different NeuralNetwork configurations
 * (e.g. single hidden layer vs. multiple layers).
 *
 * Key change:
 *  - Removed references to ForwardPropagator.ForwardResults.
 *  - Now uses ForwardPropagator.ForwardCache (layerOutputs).
 */
public class TakANNTrainingGUI extends JFrame {

    // -------------- CORE OBJECTS --------------
    private NeuralNetworkTrainer trainer;
    private NeuralNetwork network;

    private TakGame currentGame;
    private final int boardSize = 5;

    // We track partial states
    private final List<GameStateRecord> gameRecords = new ArrayList<>();

    // Basic iteration info
    private int currentIteration = 0; 
    private double currentLoss = 0.0;
    private final List<Double> lossHistory = new ArrayList<>();

    // Automated improvement checks
    private double lowestLossSoFar = Double.MAX_VALUE;
    private final int ROLLING_WINDOW = 5;
    private final LinkedList<Double> recentLosses = new LinkedList<>();
    private String improvementStatus = "No data yet";

    // Minimax metrics
    private long p1NodesEvaluated = 0L;
    private int p1Prunes = 0;
    private long p1TimeMillis = 0L;

    private long p2NodesEvaluated = 0L;
    private int p2Prunes = 0;
    private long p2TimeMillis = 0L;

    // For net visualization
    private double[] sampleInputs;
    private boolean[][] highlightHiddenLayers;  
    private boolean[] highlightOutput;

    // -------------- MINIMAX SETTINGS UI --------------
    private JComboBox<Integer> depthPlayer1;
    private JComboBox<Integer> depthPlayer2;
    private JCheckBox orderingPlayer1;
    private JCheckBox orderingPlayer2;

    // -------------- GUI PANELS --------------
    private final MetricsPanel metricsPanel;
    private final ControlPanel controlPanel;
    private final OptionsPanel optionsPanel;
    private final NetworkSetupPanel networkSetupPanel;

    private final JTabbedPane tabbedPane;
    private final NetworkPanel networkPanel;
    private final ChartPanel chartPanel;
    private final BoardPanel boardPanel;
    private final DashboardPanel dashboardPanel;

    // -------------- RECORD FOR TRAINING --------------
    private static class GameStateRecord {
        double[] boardFeatures;
        Player mover;

        public GameStateRecord(double[] boardFeatures, Player mover) {
            this.boardFeatures = boardFeatures;
            this.mover = mover;
        }
    }

    // =========================================
    // CONSTRUCTOR
    // =========================================
    public TakANNTrainingGUI() {
        super("Tak - ANN Training & Visualization");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1600, 900);
        setLayout(new BorderLayout());

        // 1) Build a default network (single hidden layer)
        createNetwork(new int[]{20}, ActivationFunction.TANH);

        // 2) Basic UI layout
        metricsPanel = new MetricsPanel();
        controlPanel = new ControlPanel();
        optionsPanel = new OptionsPanel();
        networkSetupPanel = new NetworkSetupPanel();

        tabbedPane = new JTabbedPane();
        networkPanel = new NetworkPanel();
        chartPanel = new ChartPanel();
        boardPanel = new BoardPanel();
        dashboardPanel = new DashboardPanel();

        tabbedPane.addTab("Network", networkPanel);
        tabbedPane.addTab("Loss Chart", chartPanel);
        tabbedPane.addTab("Board", boardPanel);
        tabbedPane.addTab("Dashboard", dashboardPanel);

        add(metricsPanel, BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        JPanel subBottom = new JPanel(new GridLayout(1, 2));
        subBottom.add(controlPanel);
        subBottom.add(optionsPanel);
        bottomPanel.add(subBottom, BorderLayout.CENTER);

        bottomPanel.add(networkSetupPanel, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);

        // 3) Reset the game
        resetGame();

        setVisible(true);
    }

    /**
     * Creates (or recreates) a new network + trainer based on hidden layer sizes, activation, etc.
     * We also reallocate "sampleInputs" and highlight arrays accordingly.
     */
    private void createNetwork(int[] hiddenLayers, ActivationFunction activation) {
        // Example: For a 5x5 board => 3*(5*5) + 2 = 75 + 2 = 77
        int inputSize = 3 * boardSize * boardSize + 2;
        int outputSize = 1;

        NeuralNetworkConfig config = new NeuralNetworkConfig(
                inputSize,
                hiddenLayers,
                outputSize,
                activation
        );

        this.network = new NeuralNetwork(config);
        this.trainer = new NeuralNetworkTrainer(network, 0.01);

        sampleInputs = new double[inputSize];
        Arrays.fill(sampleInputs, 0.0);

        highlightHiddenLayers = new boolean[hiddenLayers.length][];
        for (int i = 0; i < hiddenLayers.length; i++) {
            highlightHiddenLayers[i] = new boolean[hiddenLayers[i]];
        }
        highlightOutput = new boolean[outputSize];
    }

    // =========================================
    // GAME RESET
    // =========================================
    private void resetGame() {
        gameRecords.clear();

        p1NodesEvaluated = 0L;
        p1Prunes = 0;
        p1TimeMillis = 0L;
        p2NodesEvaluated = 0L;
        p2Prunes = 0;
        p2TimeMillis = 0L;

        int p1Depth = (Integer) depthPlayer1.getSelectedItem();
        int p2Depth = (Integer) depthPlayer2.getSelectedItem();
        boolean p1Ordering = orderingPlayer1.isSelected();
        boolean p2Ordering = orderingPlayer2.isSelected();

        Player p1 = createNetMinimaxAgent(Color.BLUE, p1Depth, p1Ordering);
        Player p2 = createNetMinimaxAgent(Color.GREEN, p2Depth, p2Ordering);

        currentGame = new TakGame(boardSize, Arrays.asList(p1, p2));

        boardPanel.repaint();
        dashboardPanel.repaint();
        updateDashboard("New game initialized with chosen Minimax settings.");
    }

    private Player createNetMinimaxAgent(Player.Color c, int depth, boolean useOrdering) {
        return new MinimaxAgent(
                c,
                15,
                6,
                1,
                depth,
                useOrdering,
                new NeuralNetworkEvaluator(network)
        );
    }

    // =========================================
    // PLAY ONE MOVE
    // =========================================
    private void playOneMove() {
        if (currentGame == null || currentGame.isGameEnded()) {
            updateDashboard("Game ended or not available.");
            return;
        }

        Player current = currentGame.getCurrentPlayer();
        double[] feats = BoardToInputsConverter.convert(currentGame.getBoard(), current);
        gameRecords.add(new GameStateRecord(feats, current));

        try {
            current.makeMove(currentGame);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // partial train, label=0
        trainer.train(feats, new double[]{0.0});

        if (current instanceof MinimaxAgent) {
            MinimaxAgent ma = (MinimaxAgent) current;
            if (current.getColor() == Color.BLUE) {
                p1NodesEvaluated = ma.getNodesEvaluated();
                p1Prunes = ma.getPruneCount();
                p1TimeMillis = ma.getTimeTakenMillis();
            } else {
                p2NodesEvaluated = ma.getNodesEvaluated();
                p2Prunes = ma.getPruneCount();
                p2TimeMillis = ma.getTimeTakenMillis();
            }
        }

        if (currentGame.isGameEnded()) {
            finalizeGameTraining();
        }

        highlightTopNeurons();
        boardPanel.repaint();
        dashboardPanel.repaint();
        updateDashboard("One Move done by " + current.getColor());
    }

    // =========================================
    // PLAY N GAMES
    // =========================================
    private void playNGames(int numberOfGames) {
        for (int i = 1; i <= numberOfGames; i++) {
            resetGame();
            while (!currentGame.isGameEnded()) {
                playOneMove();
            }
            updateDashboard("Finished training game " + i + "/" + numberOfGames);
        }
    }

    // =========================================
    // FINALIZE GAME TRAINING
    // =========================================
    private void finalizeGameTraining() {
        Player winner = currentGame.getWinner();
        List<TrainingExample> examples = new ArrayList<>();
        double[] label = new double[1];

        for (GameStateRecord rec : gameRecords) {
            if (winner == null) {
                label[0] = 0.0;
            } else if (rec.mover.equals(winner)) {
                label[0] = 1.0;
            } else {
                label[0] = -1.0;
            }
            double[] inCopy = Arrays.copyOf(rec.boardFeatures, rec.boardFeatures.length);
            double[] outCopy = Arrays.copyOf(label, label.length);
            examples.add(new TrainingExample(inCopy, outCopy));
        }

        // final training pass
        trainer.trainAll(examples);
        double mse = computeMSE(examples);
        currentLoss = mse;
        lossHistory.add(mse);
        currentIteration++;

        updateImprovement(mse);

        if (winner instanceof MinimaxAgent) {
            MinimaxAgent wma = (MinimaxAgent) winner;
            if (winner.getColor() == Color.BLUE) {
                p1NodesEvaluated = wma.getNodesEvaluated();
                p1Prunes = wma.getPruneCount();
                p1TimeMillis = wma.getTimeTakenMillis();
            } else {
                p2NodesEvaluated = wma.getNodesEvaluated();
                p2Prunes = wma.getPruneCount();
                p2TimeMillis = wma.getTimeTakenMillis();
            }
        }

        if (winner == null) {
            updateDashboard("Tie. MSE=" + String.format("%.4f", mse));
        } else {
            updateDashboard("Winner=" + winner.getColor() + ", MSE=" + String.format("%.4f", mse));
        }

        networkPanel.repaint();
        chartPanel.repaint();
        dashboardPanel.repaint();
    }

    private void updateImprovement(double mse) {
        if (mse < lowestLossSoFar) {
            lowestLossSoFar = mse;
            improvementStatus = "New best MSE (" + String.format("%.4f", mse) + ")!";
        } else {
            improvementStatus = "No new best. Current MSE=" + String.format("%.4f", mse);
        }

        if (recentLosses.size() == ROLLING_WINDOW) {
            recentLosses.removeFirst();
        }
        recentLosses.add(mse);

        double sum = 0.0;
        for (double val : recentLosses) {
            sum += val;
        }
        double avgRecent = sum / recentLosses.size();

        if (mse < avgRecent) {
            improvementStatus += " (Better than rolling avg " + String.format("%.4f", avgRecent) + ")";
        } else {
            improvementStatus += " (Worse/equal vs rolling avg " + String.format("%.4f", avgRecent) + ")";
        }
    }

    private double computeMSE(List<TrainingExample> data) {
        if (data.isEmpty()) return 0.0;
        double sum = 0.0;
        for (TrainingExample ex : data) {
            double[] pred = network.forward(ex.getInput());
            double diff = pred[0] - ex.getTarget()[0];
            sum += diff * diff;
        }
        return sum / data.size();
    }

    // =========================================
    // HIGHLIGHT NEURONS
    // =========================================
    private void highlightTopNeurons() {
        // Instead of ForwardResults, we use ForwardCache
        ForwardPropagator.ForwardCache fwd = ForwardPropagator.forwardWithIntermediate(sampleInputs, network.getWeights(), network.getBiases(), network.getConfig().getActivationFunction());

        // layerOutputs[i] => outputs of layer i, with i=0..(L-1)
        // final layer is output layer
        double[][] layerOutputs = fwd.layerOutputs;
        if (layerOutputs == null || layerOutputs.length == 0) {
            return;
        }

        // last index = output layer
        int lastLayerIndex = layerOutputs.length - 1;
        double[] outputActivations = layerOutputs[lastLayerIndex];

        // hidden layers are everything except the last
        // e.g. layerOutputs[0] => first hidden, layerOutputs[1] => second hidden, etc.
        double[][] hiddenLayersOut = new double[lastLayerIndex][];
        for (int i = 0; i < lastLayerIndex; i++) {
            hiddenLayersOut[i] = layerOutputs[i];
        }

        // Clear highlights
        for (boolean[] layerHL : highlightHiddenLayers) {
            Arrays.fill(layerHL, false);
        }
        Arrays.fill(highlightOutput, false);

        // For each hidden layer, highlight top 1 or 2
        for (int l = 0; l < hiddenLayersOut.length; l++) {
            double[] layerAct = hiddenLayersOut[l];
            if (layerAct == null || layerAct.length == 0) continue;

            // find top two
            Integer[] idx = new Integer[layerAct.length];
            for (int i = 0; i < layerAct.length; i++) {
                idx[i] = i;
            }
            Arrays.sort(idx, (a, b) -> Double.compare(layerAct[b], layerAct[a]));

            highlightHiddenLayers[l][idx[0]] = true;
            if (layerAct.length > 1) {
                highlightHiddenLayers[l][idx[1]] = true;
            }
        }

        // Output layer => highlight the best neuron
        if (outputActivations != null && outputActivations.length > 0) {
            int bestIdx = 0;
            double bestVal = outputActivations[0];
            for (int i = 1; i < outputActivations.length; i++) {
                if (outputActivations[i] > bestVal) {
                    bestVal = outputActivations[i];
                    bestIdx = i;
                }
            }
            highlightOutput[bestIdx] = true;
        }

        networkPanel.repaint();
    }

    private void updateDashboard(String status) {
        metricsPanel.updateMetrics(currentIteration, currentLoss, status, improvementStatus);
    }

    // =========================================
    // METRICS PANEL
    // =========================================
    private class MetricsPanel extends JPanel {
        private final JLabel iterationLabel;
        private final JLabel lossLabel;
        private final JLabel statusLabel;
        private final JLabel improvementLabel;

        public MetricsPanel() {
            setLayout(new FlowLayout(FlowLayout.LEFT));
            iterationLabel = new JLabel("Games: 0");
            lossLabel = new JLabel("Loss: 0.0000");
            statusLabel = new JLabel("Status: Ready");
            improvementLabel = new JLabel("Improvement: N/A");

            add(iterationLabel);
            add(Box.createHorizontalStrut(20));
            add(lossLabel);
            add(Box.createHorizontalStrut(20));
            add(statusLabel);
            add(Box.createHorizontalStrut(20));
            add(improvementLabel);
        }

        public void updateMetrics(int iteration, double loss, String status, String improveText) {
            iterationLabel.setText("Games: " + iteration);
            lossLabel.setText(String.format("Loss: %.4f", loss));
            statusLabel.setText("Status: " + status);
            improvementLabel.setText("Improvement: " + improveText);
        }
    }

    // =========================================
    // CONTROL PANEL
    // =========================================
    private class ControlPanel extends JPanel {
        public ControlPanel() {
            setLayout(new FlowLayout(FlowLayout.CENTER));

            JButton stepBtn = new JButton("Play 1 Move");
            stepBtn.addActionListener((ActionEvent e) -> playOneMove());

            JButton fullGameBtn = new JButton("Play 1 Full Game");
            fullGameBtn.addActionListener(e -> {
                resetGame();
                while (!currentGame.isGameEnded()) {
                    playOneMove();
                }
            });

            JButton multiBtn = new JButton("Play 3 Games");
            multiBtn.addActionListener(e -> playNGames(3));

            JButton resetBtn = new JButton("Reset Game");
            resetBtn.addActionListener(e -> {
                resetGame();
                updateDashboard("Game reset.");
            });

            add(stepBtn);
            add(fullGameBtn);
            add(multiBtn);
            add(resetBtn);
        }
    }

    // =========================================
    // OPTIONS PANEL
    // =========================================
    private class OptionsPanel extends JPanel {
        public OptionsPanel() {
            setBorder(new LineBorder(java.awt.Color.BLACK, 1));
            setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(4,4,4,4);

            JLabel labelP1 = new JLabel("Player1(Blue) Depth:");
            add(labelP1, gbc);
            gbc.gridx++;
            depthPlayer1 = new JComboBox<>(new Integer[]{1,2,3,4,5});
            depthPlayer1.setSelectedIndex(1);
            add(depthPlayer1, gbc);

            gbc.gridx=0;
            gbc.gridy++;
            JLabel labelP2 = new JLabel("Player2(Green) Depth:");
            add(labelP2, gbc);
            gbc.gridx++;
            depthPlayer2 = new JComboBox<>(new Integer[]{1,2,3,4,5});
            depthPlayer2.setSelectedIndex(2);
            add(depthPlayer2, gbc);

            gbc.gridx=0;
            gbc.gridy++;
            orderingPlayer1 = new JCheckBox("MoveOrdering P1", false);
            add(orderingPlayer1, gbc);
            gbc.gridx++;
            orderingPlayer2 = new JCheckBox("MoveOrdering P2", true);
            add(orderingPlayer2, gbc);
        }
    }

    // =========================================
    // NETWORK SETUP PANEL
    // =========================================
    private class NetworkSetupPanel extends JPanel {
        private final JTextField hiddenSizesField;
        private final JComboBox<ActivationFunction> activationBox;
        private final JButton createBtn;

        public NetworkSetupPanel() {
            setBorder(new LineBorder(java.awt.Color.BLACK, 1));
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

            add(new JLabel("Network Setup:"));
            add(new JLabel("Hidden Layers (comma-separated)"));

            hiddenSizesField = new JTextField("20");
            add(hiddenSizesField);

            add(new JLabel("Activation:"));
            activationBox = new JComboBox<>(ActivationFunction.values());
            activationBox.setSelectedItem(ActivationFunction.TANH);
            add(activationBox);

            createBtn = new JButton("Create Network");
            createBtn.addActionListener(e -> onCreateNetwork());
            add(createBtn);
        }

        private void onCreateNetwork() {
            try {
                String text = hiddenSizesField.getText().trim();
                String[] parts = text.split(",");
                int[] hiddenLayers = Arrays.stream(parts)
                        .map(String::trim)
                        .mapToInt(Integer::parseInt)
                        .toArray();

                ActivationFunction func = (ActivationFunction) activationBox.getSelectedItem();
                createNetwork(hiddenLayers, func);

                // reset iteration + MSE tracking
                currentIteration = 0;
                currentLoss = 0.0;
                lossHistory.clear();
                recentLosses.clear();
                lowestLossSoFar = Double.MAX_VALUE;
                improvementStatus = "New network created. No data yet.";

                resetGame();
                updateDashboard("Created new network with hidden layers=" + Arrays.toString(hiddenLayers));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid input for hidden layers. " + ex.getMessage());
            }
        }
    }

    // =========================================
    // NETWORK PANEL - MULTI-LAYER VIS
    // =========================================
    private class NetworkPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (network == null) return;

            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            NeuralNetworkConfig cfg = network.getConfig();
            int inputSize = cfg.getInputSize();
            int[] hiddenSizes = cfg.getHiddenLayerSizes();
            int outputSize = cfg.getOutputSize();

            // build layer sizes: input -> hidden1 -> hidden2 -> ... -> output
            List<Integer> layerSizes = new ArrayList<>();
            layerSizes.add(inputSize);
            for (int hs : hiddenSizes) {
                layerSizes.add(hs);
            }
            layerSizes.add(outputSize);

            double[][][] allWeights = network.getWeights();
            // double[][] allBiases = network.getBiases(); // not drawn, but could be

            // We'll do a forwardWithIntermediate -> ForwardCache
            ForwardPropagator.ForwardCache fwd = ForwardPropagator.forwardWithIntermediate(sampleInputs, network.getWeights(), network.getBiases(), network.getConfig().getActivationFunction());
            double[][] layerOutputs = fwd.layerOutputs;
            if (layerOutputs == null || layerOutputs.length == 0) return;

            int lastLayerIndex = layerOutputs.length - 1;
            double[] outActs = layerOutputs[lastLayerIndex];

            // hiddenActs => everything but the last
            double[][] hiddenActs = new double[lastLayerIndex][];
            for (int i = 0; i < lastLayerIndex; i++) {
                hiddenActs[i] = layerOutputs[i];
            }

            int w = getWidth();
            int h = getHeight();
            int layerCount = layerSizes.size();
            int spacing = w / (layerCount + 1);

            List<Point[]> nodeLocations = new ArrayList<>();

            // compute node positions
            for (int layerIndex = 0; layerIndex < layerCount; layerIndex++) {
                int size = layerSizes.get(layerIndex);
                int[] yPositions = computeVerticalPositions(size, h - 60, 30);
                Point[] coords = new Point[size];
                int xPos = (layerIndex + 1) * spacing;
                for (int i = 0; i < size; i++) {
                    coords[i] = new Point(xPos, yPositions[i]);
                }
                nodeLocations.add(coords);
            }

            // draw connections
            for (int layerIndex = 0; layerIndex < layerCount - 1; layerIndex++) {
                double[][] wLayer = allWeights[layerIndex];
                Point[] fromCoords = nodeLocations.get(layerIndex);
                Point[] toCoords = nodeLocations.get(layerIndex + 1);

                for (int i = 0; i < wLayer.length; i++) {
                    for (int j = 0; j < wLayer[i].length; j++) {
                        drawConnection(g2d, fromCoords[i].x, fromCoords[i].y,
                                       toCoords[j].x, toCoords[j].y,
                                       wLayer[i][j]);
                    }
                }
            }

            // draw input layer
            Point[] inputCoords = nodeLocations.get(0);
            for (int i = 0; i < inputSize; i++) {
                double val = sampleInputs[i];
                drawNode(g2d, inputCoords[i].x, inputCoords[i].y, val, "I" + i, false);
            }
            // draw hidden layers
            for (int hl = 0; hl < hiddenSizes.length; hl++) {
                Point[] coords = nodeLocations.get(hl + 1);
                double[] act = hiddenActs[hl];
                boolean[] highlight = highlightHiddenLayers[hl];
                for (int i = 0; i < hiddenSizes[hl]; i++) {
                    double val = act[i];
                    drawNode(g2d, coords[i].x, coords[i].y, val,
                             "H" + hl + ":" + i, highlight[i]);
                }
            }
            // draw output layer
            Point[] outputCoords = nodeLocations.get(layerCount - 1);
            for (int i = 0; i < outputSize; i++) {
                double val = outActs[i];
                boolean hl = highlightOutput[i];
                drawNode(g2d, outputCoords[i].x, outputCoords[i].y, val, "O" + i, hl);

                String strVal = String.format("%.3f", val);
                g2d.setColor(java.awt.Color.BLACK);
                g2d.drawString(strVal, outputCoords[i].x + 15, outputCoords[i].y);
            }

            g2d.setColor(java.awt.Color.DARK_GRAY);
            g2d.drawString("Multi-layer Network Visualization (GUI-only).", 10, 20);
        }

        private int[] computeVerticalPositions(int count, int availableHeight, int topMargin) {
            int[] positions = new int[count];
            if (count <= 1) {
                if (count == 1) {
                    positions[0] = topMargin + availableHeight / 2;
                }
                return positions;
            }
            for (int i = 0; i < count; i++) {
                positions[i] = topMargin + (i * availableHeight) / (count - 1);
            }
            return positions;
        }

        private void drawConnection(Graphics2D g2d, int x1, int y1, int x2, int y2, double weight) {
            float alpha = (float) Math.min(1.0, Math.abs(weight));
            if (weight >= 0) {
                g2d.setColor(new java.awt.Color(0, 180, 0, (int) (alpha * 255)));
            } else {
                g2d.setColor(new java.awt.Color(200, 0, 0, (int) (alpha * 255)));
            }
            float thickness = 1f + (float) (1.5 * Math.abs(weight));
            Stroke old = g2d.getStroke();
            g2d.setStroke(new BasicStroke(thickness));
            g2d.draw(new Line2D.Float(x1 + 10, y1, x2 - 10, y2));
            g2d.setStroke(old);
        }

        private void drawNode(Graphics2D g2d, int x, int y, double activation, String label, boolean highlight) {
            int radius = 10;
            float val = (float) activation;
            if (val > 1f) val = 1f;
            if (val < -1f) val = -1f;

            float green = 0.6f * Math.max(0, val);
            float red = 0.6f * Math.max(0, -val);
            float base = 0.8f - 0.4f * Math.abs(val);
            if (base < 0.4f) base = 0.4f;

            if (highlight) {
                g2d.setColor(new java.awt.Color(255, 215, 0, 150));
                g2d.fillOval(x - radius - 4, y - radius - 4, (2 * radius) + 8, (2 * radius) + 8);
            }

            java.awt.Color fill = new java.awt.Color(base + red, base + green, base);
            g2d.setColor(fill);
            g2d.fillOval(x - radius, y - radius, 2 * radius, 2 * radius);

            g2d.setColor(java.awt.Color.BLACK);
            g2d.drawOval(x - radius, y - radius, 2 * radius, 2 * radius);
            g2d.drawString(label, x - 25, y + 4);
        }
    }

    // =========================================
    // CHART PANEL
    // =========================================
    private class ChartPanel extends JPanel {
        public ChartPanel() {
            setPreferredSize(new Dimension(400, 400));
            setBorder(new LineBorder(java.awt.Color.BLACK, 1));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (lossHistory.isEmpty()) {
                g.drawString("No data yet.", 30, 30);
                return;
            }

            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            g2d.setColor(java.awt.Color.LIGHT_GRAY);
            g2d.fillRect(0, 0, w, h);

            // Axes
            g2d.setColor(java.awt.Color.BLACK);
            g2d.drawLine(50, h - 50, w - 20, h - 50);
            g2d.drawLine(50, h - 50, 50, 20);

            double maxLoss = 0.0001;
            for (double val : lossHistory) {
                if (val > maxLoss) maxLoss = val;
            }
            int chartWidth = w - 70;
            int chartHeight = h - 70;
            int dataCount = lossHistory.size();
            double xStep = (double) chartWidth / (Math.max(1, dataCount - 1));

            g2d.setColor(java.awt.Color.RED);
            int prevX = 50;
            int prevY = h - 50;
            for (int i = 0; i < dataCount; i++) {
                double val = lossHistory.get(i);
                int xPos = (int) (50 + i * xStep);
                double frac = val / maxLoss;
                int yPos = (int) ((h - 50) - (chartHeight * frac));

                if (i > 0) {
                    g2d.drawLine(prevX, prevY, xPos, yPos);
                }
                prevX = xPos;
                prevY = yPos;
            }

            double lastVal = lossHistory.get(dataCount - 1);
            String text = String.format("Last Loss=%.4f", lastVal);
            g2d.setColor(java.awt.Color.BLACK);
            g2d.drawString(text, 60, 30);
            g2d.drawString("MSE after each completed game", 60, 50);
        }
    }

    // =========================================
    // BOARD PANEL
    // =========================================
    private class BoardPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (currentGame == null) {
                g.drawString("No game loaded.", 20, 20);
                return;
            }
            Board board = currentGame.getBoard();
            if (board == null) {
                g.drawString("No board found.", 20, 40);
                return;
            }

            int size = boardSize;
            int cellSize = Math.min(getWidth() / size, getHeight() / size);

            Graphics2D g2d = (Graphics2D) g;
            g2d.setColor(java.awt.Color.WHITE);
            g2d.fillRect(0, 0, getWidth(), getHeight());

            for (int row = 0; row < size; row++) {
                for (int col = 0; col < size; col++) {
                    int sx = col * cellSize;
                    int sy = row * cellSize;

                    g2d.setColor(java.awt.Color.LIGHT_GRAY);
                    g2d.fillRect(sx, sy, cellSize, cellSize);

                    g2d.setColor(java.awt.Color.BLACK);
                    g2d.drawRect(sx, sy, cellSize, cellSize);

                    PieceStack stack = board.getBoardStack(col, row);
                    if (!stack.isEmpty()) {
                        Piece topPiece = stack.getTopPiece();
                        int stackSize = stack.size();
                        if (topPiece.getOwner().getColor() == Color.BLUE) {
                            g2d.setColor(java.awt.Color.BLUE);
                        } else {
                            g2d.setColor(java.awt.Color.GREEN);
                        }
                        String pieceType = topPiece.getPieceType().toString();
                        String label = pieceType + ":" + stackSize;

                        FontMetrics fm = g2d.getFontMetrics();
                        int textWidth = fm.stringWidth(label);
                        int textHeight = fm.getAscent();
                        int tx = sx + (cellSize - textWidth) / 2;
                        int ty = sy + (cellSize + textHeight) / 2;

                        g2d.drawString(label, tx, ty);
                    }
                }
            }

            g2d.setColor(java.awt.Color.RED);
            if (!currentGame.isGameEnded()) {
                g2d.drawString("Current Player: " + currentGame.getCurrentPlayer().getColor(), 10, 15);
            } else {
                Player w = currentGame.getWinner();
                String txt = (w == null) ? "Tie!" : "Winner: " + w.getColor();
                g2d.drawString(txt, 10, 15);
            }

            g2d.setColor(java.awt.Color.DARK_GRAY);
            g2d.drawString("Board Panel: Real top-piece display. Color=Owner, Label=PieceType:StackSize",
                    10, getHeight() - 10);
        }
    }

    // =========================================
    // DASHBOARD PANEL
    // =========================================
    private class DashboardPanel extends JPanel {
        private final JTextArea textArea;

        public DashboardPanel() {
            setLayout(new BorderLayout());
            textArea = new JTextArea();
            textArea.setEditable(false);
            textArea.setBorder(new LineBorder(java.awt.Color.BLACK, 1));
            add(new JScrollPane(textArea), BorderLayout.CENTER);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            refreshText();
        }

        private void refreshText() {
            StringBuilder sb = new StringBuilder();
            sb.append("=== Dashboard ===\n");
            sb.append("Player1 (Blue) Last Move Stats:\n");
            sb.append("  Nodes Evaluated: ").append(p1NodesEvaluated).append("\n");
            sb.append("  Prunes: ").append(p1Prunes).append("\n");
            sb.append("  Time (ms): ").append(p1TimeMillis).append("\n\n");

            sb.append("Player2 (Green) Last Move Stats:\n");
            sb.append("  Nodes Evaluated: ").append(p2NodesEvaluated).append("\n");
            sb.append("  Prunes: ").append(p2Prunes).append("\n");
            sb.append("  Time (ms): ").append(p2TimeMillis).append("\n\n");

            sb.append("Learning Rate: ").append(String.format("%.4f", trainer.getLearningRate())).append("\n");
            sb.append("Games Completed: ").append(currentIteration).append("\n");
            sb.append(String.format("Current MSE: %.4f\n", currentLoss));
            sb.append("Lowest MSE so far: ").append(String.format("%.4f", lowestLossSoFar)).append("\n");

            sb.append("\n(Automated Improvement Tracking)\n");
            sb.append("Improvements are checked after each game.\n");
            sb.append("Rolling window size: ").append(ROLLING_WINDOW).append("\n");
            sb.append("Current Improvement Status: ").append(improvementStatus).append("\n");

            textArea.setText(sb.toString());
        }
    }

    // =========================================
    // MAIN
    // =========================================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TakANNTrainingGUI());
    }
}
