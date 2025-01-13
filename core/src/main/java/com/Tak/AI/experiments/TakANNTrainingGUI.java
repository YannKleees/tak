package com.Tak.AI.experiments;

import com.Tak.AI.neuralnet.*;
import com.Tak.AI.neuralnet.SelfPlayMinimaxTrainer.TrainingExample;
import com.Tak.AI.players.MinimaxAgent;
import com.Tak.Logic.exceptions.GameOverException;
import com.Tak.Logic.exceptions.InvalidMoveException;
import com.Tak.Logic.models.Board;
import com.Tak.Logic.models.Piece;
import com.Tak.Logic.models.PieceStack;
import com.Tak.Logic.models.Player;
import com.Tak.Logic.models.Player.Color;
import com.Tak.Logic.models.TakGame;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.geom.Line2D;
import java.util.*;
import java.util.List;

/**
 * Updated GUI that checks if the net is improving over time.
 * 
 * Key changes from previous version:
 *   - Tracks the "lowestLossSoFar" to see if we have a new best MSE.
 *   - Maintains a rolling MSE array (last N games) to compute an average for trend checking.
 *   - Adds a small UI note for improvement status in the metrics panel.
 */
public class TakANNTrainingGUI extends JFrame {

    // -------------- CORE OBJECTS --------------
    private final NeuralNetworkTrainer trainer; 
    private final NeuralNetwork network;

    private TakGame currentGame;
    private final int boardSize = 5;

    // We track partial states
    private final List<GameStateRecord> gameRecords = new ArrayList<>();

    // Basic iteration info
    private int currentIteration = 0;      // how many full games completed
    private double currentLoss = 0.0;      // MSE on the last completed game
    private final List<Double> lossHistory = new ArrayList<>();

    // Automated improvement checks
    private double lowestLossSoFar = Double.MAX_VALUE; // track best (lowest) MSE
    private final int ROLLING_WINDOW = 5;              // how many recent games to average
    private final LinkedList<Double> recentLosses = new LinkedList<>();
    private String improvementStatus = "No data yet";  // e.g. "Better!", "Worse...", etc.

    // Minimax metrics
    private long p1NodesEvaluated = 0L;
    private int p1Prunes = 0;
    private long p1TimeMillis = 0L;

    private long p2NodesEvaluated = 0L;
    private int p2Prunes = 0;
    private long p2TimeMillis = 0L;

    // For net visualization (sample input, highlight neurons)
    private double[] sampleInputs;
    private boolean[] highlightHidden;
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

        // 1) Build or load the network
        NeuralNetworkConfig config = new NeuralNetworkConfig(
                3 * 5 * 5 + 2,  // Example input size for 5x5
                20,             // hidden size
                1,              // output size
                ActivationFunction.TANH
        );
        this.network = new NeuralNetwork(config);
        double learningRate = 0.01;
        this.trainer = new NeuralNetworkTrainer(network, learningRate);

        // Prepare sample input for net visualization
        int inputSize = config.getInputSize();
        sampleInputs = new double[inputSize];
        Arrays.fill(sampleInputs, 0.5);
        highlightHidden = new boolean[config.getHiddenSize()];
        highlightOutput = new boolean[config.getOutputSize()];

        // 2) Basic UI layout
        metricsPanel = new MetricsPanel();
        controlPanel = new ControlPanel();
        optionsPanel = new OptionsPanel();

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

        // Bottom row: control + minimax options
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(controlPanel, BorderLayout.CENTER);
        bottomPanel.add(optionsPanel, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);

        // 3) Reset the game
        resetGame();

        setVisible(true);
    }

    // =========================================
    // GAME RESET
    // =========================================
    private void resetGame() {
        gameRecords.clear();

        // Clear per-player metrics
        p1NodesEvaluated = 0L;
        p1Prunes = 0;
        p1TimeMillis = 0L;
        p2NodesEvaluated = 0L;
        p2Prunes = 0;
        p2TimeMillis = 0L;

        // Read UI
        int p1Depth = (Integer) depthPlayer1.getSelectedItem();
        int p2Depth = (Integer) depthPlayer2.getSelectedItem();
        boolean p1Ordering = orderingPlayer1.isSelected();
        boolean p2Ordering = orderingPlayer2.isSelected();

        // Create 2 MinimaxAgents
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

        // Actual move
        try {
            current.makeMove(currentGame);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // partial train, label=0
        trainer.train(feats, new double[]{0.0});

        // gather metrics
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

        // Track improvement
        updateImprovement(mse);

        // gather final metrics if winner is MinimaxAgent
        if (winner != null && (winner instanceof MinimaxAgent)) {
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

        // final status
        if (winner == null) {
            updateDashboard("Tie. MSE=" + String.format("%.4f", mse));
        } else {
            updateDashboard("Winner=" + winner.getColor() + ", MSE=" + String.format("%.4f", mse));
        }

        // repaint
        networkPanel.repaint();
        chartPanel.repaint();
        dashboardPanel.repaint();
    }

    /**
     * Updates improvement metrics by:
     *  - Checking if the new MSE is lower than the best so far.
     *  - Maintaining a rolling list of MSEs (recentLosses) for short-term average.
     */
    private void updateImprovement(double mse) {
        // 1) Check for new best
        if (mse < lowestLossSoFar) {
            lowestLossSoFar = mse;
            improvementStatus = "New best MSE (" + String.format("%.4f", mse) + ")!";
        } else {
            improvementStatus = "No new best. Current MSE=" + String.format("%.4f", mse);
        }

        // 2) Rolling window average
        if (recentLosses.size() == ROLLING_WINDOW) {
            recentLosses.removeFirst();
        }
        recentLosses.add(mse);

        double sum = 0.0;
        for (double val : recentLosses) {
            sum += val;
        }
        double avgRecent = sum / recentLosses.size();

        // As an example, if MSE is below that average, we say "Better," otherwise "Worse," etc.
        if (mse < avgRecent) {
            improvementStatus += " (Better than rolling avg " + String.format("%.4f", avgRecent) + ")";
        } else {
            improvementStatus += " (Worse or equal vs rolling avg " + String.format("%.4f", avgRecent) + ")";
        }
    }

    // =========================================
    // MSE
    // =========================================
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
        ForwardPropagator.ForwardResults fwd = network.forwardWithIntermediate(sampleInputs);
        double[] h = fwd.getHiddenActivations();
        double[] o = fwd.getOutputActivations();

        Arrays.fill(highlightHidden, false);
        Arrays.fill(highlightOutput, false);

        if (h.length > 0) {
            Integer[] idxH = new Integer[h.length];
            for (int i = 0; i < h.length; i++) {
                idxH[i] = i;
            }
            Arrays.sort(idxH, (a, b) -> Double.compare(h[b], h[a]));
            highlightHidden[idxH[0]] = true;
            if (h.length > 1) highlightHidden[idxH[1]] = true;
        }
        if (o.length > 0) {
            int best = 0;
            double bestVal = o[0];
            for (int i = 1; i < o.length; i++) {
                if (o[i] > bestVal) {
                    bestVal = o[i];
                    best = i;
                }
            }
            highlightOutput[best] = true;
        }

        networkPanel.repaint();
    }

    // =========================================
    // UPDATE DASHBOARD TEXT
    // =========================================
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
    // NETWORK PANEL
    // =========================================
    private class NetworkPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (network == null) return;

            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            NeuralNetworkConfig cfg = network.getConfig();
            int inSize = cfg.getInputSize();
            int hidSize = cfg.getHiddenSize();
            int outSize = cfg.getOutputSize();

            int w = getWidth();
            int h = getHeight();

            int xIn = w / 6;
            int xHid = w / 2;
            int xOut = (5 * w) / 6;

            int topMargin = 60;
            int availableHeight = h - 2 * topMargin;

            int[] yIn = computeVerticalPositions(inSize, availableHeight, topMargin);
            int[] yHid = computeVerticalPositions(hidSize, availableHeight, topMargin);
            int[] yOut = computeVerticalPositions(outSize, availableHeight, topMargin);

            double[][] wIH = network.getWeightsInputHidden();
            double[][] wHO = network.getWeightsHiddenOutput();

            ForwardPropagator.ForwardResults fwd = network.forwardWithIntermediate(sampleInputs);
            double[] hidAct = fwd.getHiddenActivations();
            double[] outAct = fwd.getOutputActivations();

            // draw connections input->hidden
            for (int i = 0; i < inSize; i++) {
                for (int hh = 0; hh < hidSize; hh++) {
                    double wVal = wIH[i][hh];
                    drawConnection(g2d, xIn, yIn[i], xHid, yHid[hh], wVal);
                }
            }
            // hidden->output
            for (int hh = 0; hh < hidSize; hh++) {
                for (int oo = 0; oo < outSize; oo++) {
                    double wVal = wHO[hh][oo];
                    drawConnection(g2d, xHid, yHid[hh], xOut, yOut[oo], wVal);
                }
            }

            // draw input nodes
            for (int i = 0; i < inSize; i++) {
                double val = sampleInputs[i];
                drawNode(g2d, xIn, yIn[i], val, "I" + i, false);
            }
            // hidden nodes
            for (int hh = 0; hh < hidSize; hh++) {
                double val = hidAct[hh];
                boolean highlight = highlightHidden[hh];
                drawNode(g2d, xHid, yHid[hh], val, "H" + hh, highlight);
            }
            // output nodes
            for (int oo = 0; oo < outSize; oo++) {
                double val = outAct[oo];
                boolean highlight = highlightOutput[oo];
                drawNode(g2d, xOut, yOut[oo], val, "O" + oo, highlight);

                String strVal = String.format("%.3f", val);
                g2d.setColor(java.awt.Color.BLACK);
                g2d.drawString(strVal, xOut + 15, yOut[oo] + 5);
            }

            g2d.setColor(java.awt.Color.DARK_GRAY);
            g2d.drawString("Network Visualization Panel (Weights color-coded by sign/magnitude).", 10, 20);
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
            g2d.drawString("Board Panel: Real top-piece display. Color=Owner, Label=PieceType:StackSize", 10, getHeight() - 10);
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

