// File: core/src/main/java/com/Tak/GUI/GameScreen.java
package com.Tak.GUI;

import com.Tak.AI.evaluation.HeuristicEvaluator;
import com.Tak.AI.evaluation.IEvaluationFunction;
import com.Tak.AI.neuralnet.NeuralNetworkEvaluator;
import com.Tak.AI.neuralnet.NeuralNetworkInitializer;
import com.Tak.AI.neuralnet.NeuralNetworkTrainer;
import com.Tak.AI.neuralnet.SelfPlayMinimaxTrainer;
import com.Tak.AI.experiments.IterativeNetVsHeuristicTrainer;
import com.Tak.AI.players.MinimaxAgent;
import com.Tak.AI.players.RandomAIPlayer;
import com.Tak.GUI.UIManager.DropCountsCallback;
import com.Tak.Logic.exceptions.GameOverException;
import com.Tak.Logic.exceptions.InvalidMoveException;
import com.Tak.Logic.models.Direction;
import com.Tak.Logic.models.GameMode;
import com.Tak.Logic.models.Piece;
import com.Tak.Logic.models.Piece.PieceType;
import com.Tak.Logic.models.Player;
import com.Tak.Logic.models.TakGame;
import com.Tak.Logic.utils.Logger;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * The GameScreen class handles the main game screen, coordinating between
 * the renderer, UI, input handler, and includes a "Training" tab on the top-right.
 *
 * This version includes:
 *  - AI selection (Random / Minimax)
 *  - Evaluator selection (Heuristic / NeuralNet)
 *  - Training tab with mode selection (Iterative vs. Self-Play),
 *    continuing from existing nets, smaller scroll area, etc.
 */
public class GameScreen implements Screen, GameInputHandler.UICallback {

    private TakGameMain game;
    public int boardSize = 5;  // default board size if we build a new TakGame
    public TakGame takGame;    // the actual game logic

    public PerspectiveCamera camera;
    public CameraInputController camController;
    private GameRenderer renderer;
    private UIManager uiManager;
    private GameInputHandler inputHandler;

    public Piece.PieceType selectedPieceType;
    private boolean isAIMoving = false;
    private TextButton rulesButton;
    private ShapeRenderer shapeRenderer;
    private boolean useAI;
    private GameMode gameMode;
    private Player aiPlayer;   // if Human vs AI

    // The main table for normal in-game UI
    private Table mainGameTable;

    // ----------------------------------------------------------------------
    //  TRAINING TAB UI
    // ----------------------------------------------------------------------
    private Table trainingTable;
    private TextButton trainingTabButton;

    // Fields for iterative vs. heuristic
    private TextField gamesPerRoundField;
    private TextField totalRoundsField;

    // Fields for self-play
    private TextField selfPlayGamesField;
    private TextField valFreqField;
    private TextField patienceField;

    private TextField boardSizeField;
    private TextArea trainingLogArea;
    private TextButton startTrainingButton;

    // For "Continue from existing net?"
    private CheckBox continueCheckBox;
    private SelectBox<String> existingNetSelect;

    /**
     * Constructor #1: Builds a new TakGame internally for a chosen GameMode (e.g., Human vs AI).
     */
    public GameScreen(TakGameMain game, GameMode gameMode) {
        this.game = game;
        this.gameMode = gameMode;
        this.useAI = (gameMode == GameMode.HUMAN_VS_AI);
        create();
    }

    /**
     * Constructor #2: Accepts an existing TakGame (e.g. for AI vs AI).
     */
    public GameScreen(TakGameMain game, TakGame existingGame) {
        this.game = game;
        this.takGame = existingGame;
        this.boardSize = existingGame.getBoardSize();
        this.useAI = false;
        createFromExistingGame();
    }

    /**
     * Build a new TakGame internally (Human vs AI or Human vs Human).
     */
    public void create() {
        List<Player> players = new ArrayList<>();

        if (useAI) {
            // Human vs AI
            Player humanPlayer = new com.Tak.Logic.players.HumanPlayer(Player.Color.BLUE, 21, 1, 1);
            aiPlayer = new MinimaxAgent(Player.Color.GREEN, 21, 1, 1, 3);

            humanPlayer.setOpponent(aiPlayer);
            aiPlayer.setOpponent(humanPlayer);
            players.add(humanPlayer);
            players.add(aiPlayer);

            Logger.log("GameScreen", "Initialized Human vs AI: BLUE vs GREEN");
        } else {
            // Human vs Human
            Player p1 = new com.Tak.Logic.players.HumanPlayer(Player.Color.BLUE, 21, 1, 1);
            Player p2 = new com.Tak.Logic.players.HumanPlayer(Player.Color.GREEN, 21, 1, 1);

            p1.setOpponent(p2);
            p2.setOpponent(p1);
            players.add(p1);
            players.add(p2);

            Logger.log("GameScreen", "Initialized Human vs Human: BLUE vs GREEN");
        }

        takGame = new TakGame(boardSize, players);
        initCommonParts();
    }

    /**
     * If an existing TakGame is provided, skip building players.
     */
    private void createFromExistingGame() {
        initCommonParts();
    }

    /**
     * Sets up camera, renderer, UI, input, etc.
     */
    private void initCommonParts() {
        // Camera
        camera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(boardSize * 2f, boardSize * 2f, boardSize * 2f);
        camera.lookAt(boardSize / 2f, 0, boardSize / 2f);
        camera.near = 0.1f;
        camera.far = 100f;
        camera.update();

        camController = new CameraInputController(camera);
        camController.rotateButton = Input.Buttons.RIGHT;
        camController.translateButton = Input.Buttons.MIDDLE;
        camController.scrollFactor = -0.1f;

        // Renderer
        renderer = new GameRenderer(camera, boardSize, takGame);
        renderer.updatePieceInstances();

        shapeRenderer = new ShapeRenderer();

        // UI manager
        uiManager = new UIManager(takGame, this);
        uiManager.getStage().setViewport(new ScreenViewport());

        // Input
        inputHandler = new GameInputHandler(camera, takGame, renderer, uiManager, this);
        Gdx.input.setInputProcessor(new InputMultiplexer(uiManager.getStage(), inputHandler, camController));

        // Main table for game UI
        mainGameTable = new Table();
        mainGameTable.setFillParent(true);
        uiManager.getStage().addActor(mainGameTable);

        // Common UI
        addRulesButton();
        addBackButton();

        // If there's an AI in the game, let's show the AI selection
        if (detectAnyAI()) {
            addAISelectionDropdown();
        }

        // Add the "Training Tab"
        addTrainingTabUI();

        // Make sure hotbar colors reflect current turn
        uiManager.updateHotbarColors();
    }

    /**
     * Check if any of the players is an AI.
     */
    private boolean detectAnyAI() {
        for (Player p : takGame.getPlayers()) {
            if (p instanceof MinimaxAgent || p instanceof RandomAIPlayer) {
                return true;
            }
        }
        return false;
    }

    /**
     * Places a "Rules" button in the top-left corner.
     */
    private void addRulesButton() {
        rulesButton = new TextButton("Rules", uiManager.getSkin());
        Table table = new Table();
        table.top().left();
        table.setFillParent(true);

        table.add(rulesButton).pad(10);
        uiManager.getStage().addActor(table);

        rulesButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                showRulesDialog();
            }
        });
    }

    /**
     * "Back to Main Menu" in the bottom-left.
     */
    private void addBackButton() {
        TextButton backButton = new TextButton("Back to Main Menu", uiManager.getSkin());
        Table backButtonTable = new Table();
        backButtonTable.bottom().left();
        backButtonTable.setFillParent(true);

        backButtonTable.add(backButton).pad(10).width(150).height(40);
        uiManager.getStage().addActor(backButtonTable);

        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new MainMenuScreen(game));
            }
        });
    }

    /**
     * Lets the user pick AI type (RandomAI or Minimax), Minimax Depth,
     * Evaluator (Heuristic or NeuralNetwork), and network file if NeuralNet.
     */
    private void addAISelectionDropdown() {
        final SelectBox<String> aiSelectBox = new SelectBox<>(uiManager.getSkin());
        aiSelectBox.setItems("RandomAI", "MinimaxAgent");
        aiSelectBox.setSelected("MinimaxAgent");

        final SelectBox<Integer> depthSelectBox = new SelectBox<>(uiManager.getSkin());
        depthSelectBox.setItems(1, 2, 3, 4);
        depthSelectBox.setSelected(3);

        final SelectBox<String> evalSelectBox = new SelectBox<>(uiManager.getSkin());
        evalSelectBox.setItems("Heuristic", "NeuralNetwork");
        evalSelectBox.setSelected("Heuristic");

        final SelectBox<String> networkSelectBox = new SelectBox<>(uiManager.getSkin());
        Array<String> netFiles = listNetworkFiles();
        networkSelectBox.setItems(netFiles);
        networkSelectBox.setVisible(false); // hidden unless "NeuralNetwork" is chosen

        Table dropdownTable = new Table();
        dropdownTable.bottom().right();
        dropdownTable.setFillParent(true);

        // Layout
        dropdownTable.add(new Label("AI Type:", uiManager.getSkin())).pad(5);
        dropdownTable.add(aiSelectBox).width(150).height(30).pad(5).row();

        dropdownTable.add(new Label("Minimax Depth:", uiManager.getSkin())).pad(5);
        dropdownTable.add(depthSelectBox).width(150).height(30).pad(5).row();

        dropdownTable.add(new Label("Evaluator:", uiManager.getSkin())).pad(5);
        dropdownTable.add(evalSelectBox).width(150).height(30).pad(5).row();

        dropdownTable.add(new Label("Network File:", uiManager.getSkin())).pad(5);
        dropdownTable.add(networkSelectBox).width(150).height(30).pad(5).row();

        uiManager.getStage().addActor(dropdownTable);

        // Listeners
        aiSelectBox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                String selectedAI = aiSelectBox.getSelected();
                depthSelectBox.setVisible("MinimaxAgent".equals(selectedAI));

                // Re-invoke logic
                changeAIBehavior(
                    selectedAI,
                    evalSelectBox.getSelected(),
                    networkSelectBox.getSelected(),
                    depthSelectBox.getSelected()
                );
            }
        });

        evalSelectBox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                String evalChoice = evalSelectBox.getSelected();
                networkSelectBox.setVisible("NeuralNetwork".equals(evalChoice));

                changeAIBehavior(
                    aiSelectBox.getSelected(),
                    evalChoice,
                    networkSelectBox.getSelected(),
                    depthSelectBox.getSelected()
                );
            }
        });

        networkSelectBox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                changeAIBehavior(
                    aiSelectBox.getSelected(),
                    evalSelectBox.getSelected(),
                    networkSelectBox.getSelected(),
                    depthSelectBox.getSelected()
                );
            }
        });

        depthSelectBox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                changeAIBehavior(
                    aiSelectBox.getSelected(),
                    evalSelectBox.getSelected(),
                    networkSelectBox.getSelected(),
                    depthSelectBox.getSelected()
                );
            }
        });
    }

    /**
     * Gets a list of .txt network files in networks/ folder.
     */
    private Array<String> listNetworkFiles() {
        Array<String> fileNames = new Array<>();
        FileHandle dirHandle = Gdx.files.local("networks");
        if (!dirHandle.exists()) {
            // fallback
            fileNames.add("TrainedModel.txt");
            return fileNames;
        }

        FileHandle[] files = dirHandle.list();
        if (files != null && files.length > 0) {
            for (FileHandle fh : files) {
                if (fh.extension().equalsIgnoreCase("txt")) {
                    fileNames.add(fh.name());
                }
            }
        }

        if (fileNames.size == 0) {
            fileNames.add("TrainedModel.txt");
        }
        return fileNames;
    }

    /**
     * Switch AI type (Random vs Minimax),
     * evaluator (Heuristic vs NeuralNetwork),
     * depth, and network file for Minimax.
     */
    private void changeAIBehavior(String aiType, String evalType, String networkFile, int depth) {
        if (aiPlayer == null) {
            for (Player p : takGame.getPlayers()) {
                if (p instanceof MinimaxAgent || p instanceof RandomAIPlayer) {
                    aiPlayer = p;
                    break;
                }
            }
        }

        if (aiPlayer == null) {
            Logger.log("GameScreen", "No AI found or current AI not recognized.");
            return;
        }

        Player newAI;
        // 1) If user picked "RandomAI"
        if ("RandomAI".equals(aiType)) {
            newAI = new RandomAIPlayer(
                aiPlayer.getColor(),
                aiPlayer.getRemainingPieces(PieceType.FLAT_STONE),
                aiPlayer.getRemainingPieces(PieceType.STANDING_STONE),
                aiPlayer.getRemainingPieces(PieceType.CAPSTONE)
            );
            Logger.log("GameScreen", "Switched AI to RandomAI");
            takGame.replacePlayer(aiPlayer, newAI);
            aiPlayer = newAI;
            return;
        }

        // 2) If Minimax
        IEvaluationFunction evaluator;
        if ("Heuristic".equals(evalType)) {
            evaluator = new HeuristicEvaluator();
        } else {
            // "NeuralNetwork"
            try {
                String filePath = "networks/" + networkFile;
                NeuralNetworkTrainer trainer = NeuralNetworkInitializer.initializeTrainer();
                trainer.loadNetwork(filePath);
                evaluator = new NeuralNetworkEvaluator(trainer.getNetwork());
                Logger.log("GameScreen", "Loaded NeuralNetwork from " + filePath);
            } catch (Exception e) {
                Logger.log("GameScreen", "Failed to load network: " + e.getMessage());
                evaluator = new HeuristicEvaluator(); // fallback
            }
        }

        newAI = new MinimaxAgent(
            aiPlayer.getColor(),
            aiPlayer.getRemainingPieces(PieceType.FLAT_STONE),
            aiPlayer.getRemainingPieces(PieceType.STANDING_STONE),
            aiPlayer.getRemainingPieces(PieceType.CAPSTONE),
            depth,
            true,
            evaluator
        );

        Logger.log("GameScreen",
            "Switched AI -> Minimax, depth=" + depth
                + ", eval=" + evalType
                + (evalType.equals("NeuralNetwork") ? " file=" + networkFile : ""));

        takGame.replacePlayer(aiPlayer, newAI);
        aiPlayer = newAI;
    }

    /**
     * Basic text explaining the game rules.
     */
    private String getRulesText() {
        return "1. Players take turns placing tiles on the board.\n"
             + "2. Movement is orthogonal. Only the top tile can move.\n"
             + "3. Road victory or flat victory.\n"
             + "4. Etc...";
    }

    // ------------------------------------------------------------------------
    // ADVANCED TRAINING TAB
    // ------------------------------------------------------------------------
    /**
     * Adds an advanced training tab UI to the top-right corner.
     * Now includes:
     *  - "Training Mode": (Iterative vs. Self-Play)
     *  - "Continue from existing net?" checkbox + dropdown
     *  - SaveAs text field
     *  - Start/Stop
     *  - Smaller scroll area
     */
    private void addTrainingTabUI() {
        // 1) Create the button that toggles the training panel
        trainingTabButton = new TextButton("Training Tab", uiManager.getSkin());

        // Make a top-right table for alignment
        Table topRightTable = new Table();
        topRightTable.top().right();
        topRightTable.defaults().pad(5).align(Align.right);
        topRightTable.setFillParent(true);

        // Place the "Training Tab" button
        topRightTable.add(trainingTabButton).width(120).height(40);
        uiManager.getStage().addActor(topRightTable);

        // 2) The training table itself
        trainingTable = new Table(uiManager.getSkin());
        // Make a slightly translucent dark background
        trainingTable.setBackground(uiManager.getSkin().newDrawable("button-up", new Color(0,0,0,0.7f)));
        trainingTable.pad(15);
        trainingTable.setVisible(false);
        trainingTable.center();  // <--- center alignment inside the table

        // Title
        Label trainingTitle = new Label("Neural Net Training", uiManager.getSkin());
        trainingTitle.setColor(Color.YELLOW);
        trainingTable.add(trainingTitle).colspan(2).padBottom(10).row();

        // (A) Training Mode
        Label modeLabel = new Label("Training Mode:", uiManager.getSkin());
        final SelectBox<String> modeSelectBox = new SelectBox<>(uiManager.getSkin());
        modeSelectBox.setItems("Iterative vs Heuristic", "Self-Play");
        modeSelectBox.setSelected("Iterative vs Heuristic");
        trainingTable.add(modeLabel).pad(5).right();
        trainingTable.add(modeSelectBox).pad(5).row();

        // (B) Iterative-specific fields
        Label gamesLabel = new Label("Games Per Round:", uiManager.getSkin());
        gamesPerRoundField = new TextField("10", uiManager.getSkin());
        Label roundsLabel = new Label("Total Rounds:", uiManager.getSkin());
        totalRoundsField = new TextField("3", uiManager.getSkin());

        trainingTable.add(gamesLabel).pad(5).right();
        trainingTable.add(gamesPerRoundField).width(60).row();
        trainingTable.add(roundsLabel).pad(5).right();
        trainingTable.add(totalRoundsField).width(60).row();

        // (C) Self-play table (hidden if iterative)
        Label selfPlayGamesLabel = new Label("Number of Games:", uiManager.getSkin());
        selfPlayGamesField = new TextField("20", uiManager.getSkin());

        Label valFreqLabel = new Label("Validation Freq:", uiManager.getSkin());
        valFreqField = new TextField("5", uiManager.getSkin());

        Label patienceLabel = new Label("Patience:", uiManager.getSkin());
        patienceField = new TextField("3", uiManager.getSkin());

        final Table selfPlayTable = new Table(uiManager.getSkin());
        selfPlayTable.add(selfPlayGamesLabel).pad(5).right();
        selfPlayTable.add(selfPlayGamesField).width(60).row();
        selfPlayTable.add(valFreqLabel).pad(5).right();
        selfPlayTable.add(valFreqField).width(60).row();
        selfPlayTable.add(patienceLabel).pad(5).right();
        selfPlayTable.add(patienceField).width(60).row();

        selfPlayTable.setVisible(false);
        // Add the selfPlay table to main
        trainingTable.add(selfPlayTable).colspan(2).row();

        // (D) Board Size
        Label bszLabel = new Label("Board Size:", uiManager.getSkin());
        boardSizeField = new TextField("5", uiManager.getSkin());
        trainingTable.add(bszLabel).pad(5).right();
        trainingTable.add(boardSizeField).width(60).row();

        // (E) "Continue from existing net?"
        continueCheckBox = new CheckBox(" Continue from Existing Net?", uiManager.getSkin());
        trainingTable.add(continueCheckBox).colspan(2).pad(5).left().row();

        // A sub-table to pick an existing net
        final Label existingLabel = new Label("Existing Net:", uiManager.getSkin());
        existingLabel.setVisible(false);
        existingNetSelect = new SelectBox<>(uiManager.getSkin());
        existingNetSelect.setVisible(false);

        final Table continueTable = new Table(uiManager.getSkin());
        continueTable.add(existingLabel).pad(5).right();
        continueTable.add(existingNetSelect).width(150).pad(5);
        continueTable.setVisible(false);
        trainingTable.add(continueTable).colspan(2).row();

        // Checkbox toggles the table
        continueCheckBox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                boolean checked = continueCheckBox.isChecked();
                existingLabel.setVisible(checked);
                existingNetSelect.setVisible(checked);
                continueTable.setVisible(checked);
            }
        });

        // (F) "Save network as"
        Label saveNameLabel = new Label("Save Net As:", uiManager.getSkin());
        final TextField saveNameField = new TextField("MyNewNetwork", uiManager.getSkin());
        trainingTable.add(saveNameLabel).pad(5).right();
        trainingTable.add(saveNameField).width(100).row();

        // (G) Start/Stop
        startTrainingButton = new TextButton("Start Training", uiManager.getSkin());
        final TextButton stopTrainingButton = new TextButton("Stop", uiManager.getSkin());
        stopTrainingButton.setVisible(false);

        Table btnTable = new Table(uiManager.getSkin());
        btnTable.add(startTrainingButton).padRight(10);
        btnTable.add(stopTrainingButton);
        trainingTable.add(btnTable).colspan(2).padTop(10).row();

        // (H) Log area, smaller + no visible scroll bar
        Label logLabel = new Label("Training Log:", uiManager.getSkin());
        trainingTable.add(logLabel).colspan(2).padTop(10).row();

        trainingLogArea = new TextArea("", uiManager.getSkin());
        trainingLogArea.setDisabled(true);
        trainingLogArea.setPrefRows(6);

        // Create the scroll pane
        ScrollPane scrollPane = new ScrollPane(trainingLogArea, uiManager.getSkin());
        scrollPane.setFlickScroll(true);
        scrollPane.setFadeScrollBars(false);

        // Hide scrollbars, only mouse-wheel
        ScrollPane.ScrollPaneStyle spStyle = uiManager.getSkin().get(ScrollPane.ScrollPaneStyle.class);
        spStyle.vScrollKnob = null;
        spStyle.vScroll = null;
        spStyle.hScrollKnob = null;
        spStyle.hScroll = null;

        scrollPane.setScrollingDisabled(true, false); // allow vertical only
        scrollPane.setOverscroll(false, false);

        trainingTable.add(scrollPane).colspan(2).minHeight(150).maxHeight(220).width(300).pad(5);

        // 3) Put the trainingTable in its own container on top-right
        Table trainingPanelContainer = new Table();
        trainingPanelContainer.top().right();
        trainingPanelContainer.setFillParent(true);
        // slight offset so it doesn't overlap the top-right button
        trainingPanelContainer.add(trainingTable).padTop(60).padRight(10);
        uiManager.getStage().addActor(trainingPanelContainer);

        // 4) Listeners
        // Toggle the entire training panel
        trainingTabButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                boolean isVisible = trainingTable.isVisible();
                trainingTable.setVisible(!isVisible);
                // Optionally hide the mainGameTable
                mainGameTable.setVisible(isVisible);
            }
        });

        // Switch mode => hide iterative or self-play
        modeSelectBox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                String mode = modeSelectBox.getSelected();
                boolean isIterative = mode.equals("Iterative vs Heuristic");

                gamesLabel.setVisible(isIterative);
                gamesPerRoundField.setVisible(isIterative);
                roundsLabel.setVisible(isIterative);
                totalRoundsField.setVisible(isIterative);

                selfPlayTable.setVisible(!isIterative);
            }
        });

        // Start button
        startTrainingButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                String mode = modeSelectBox.getSelected();
                boolean isIterative = mode.equals("Iterative vs Heuristic");

                int bsz = Integer.parseInt(boardSizeField.getText().trim());
                String saveName = saveNameField.getText().trim();

                boolean continuing = continueCheckBox.isChecked();
                String existingFile = existingNetSelect.getSelected();

                stopTrainingButton.setVisible(true);

                // Run in background thread
                new Thread(() -> {
                    if (isIterative) {
                        int gpr = Integer.parseInt(gamesPerRoundField.getText().trim());
                        int tr = Integer.parseInt(totalRoundsField.getText().trim());
                        runIterativeTraining(gpr, tr, bsz, saveName, continuing, existingFile);
                    } else {
                        int spGames = Integer.parseInt(selfPlayGamesField.getText().trim());
                        int valFreq = Integer.parseInt(valFreqField.getText().trim());
                        int pat = Integer.parseInt(patienceField.getText().trim());
                        runSelfPlayTraining(spGames, valFreq, pat, bsz, saveName, continuing, existingFile);
                    }

                    // Hide stop button after done
                    Gdx.app.postRunnable(() -> stopTrainingButton.setVisible(false));
                }).start();
            }
        });

        // Stop button (demo only)
        stopTrainingButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                logToTrainingArea("Stop training requested (demo).");
            }
        });
    }

    /**
     * Lists the .txt networks in your /networks folder
     * for the "Continue from existing?" dropdown.
     */
    private Array<String> listExistingNetworks() {
        Array<String> arr = new Array<>();
        FileHandle dirHandle = Gdx.files.local("networks");
        if (!dirHandle.exists()) {
            return arr; // empty
        }
        for (FileHandle fh : dirHandle.list()) {
            if (fh.extension().equalsIgnoreCase("txt")) {
                arr.add(fh.name());
            }
        }
        return arr;
    }

    private void runIterativeTraining(int gamesPerRound,
                                      int totalRounds,
                                      int bsz,
                                      String networkName,
                                      boolean continuing,
                                      String existingFile) {
        try {
            logToTrainingArea("Starting ITERATIVE training... (vs Heuristic)");
            NeuralNetworkTrainer nnTrainer;
            if (continuing) {
                // Load existing
                String path = "networks/" + existingFile;
                nnTrainer = NeuralNetworkInitializer.initializeTrainer();
                nnTrainer.loadNetwork(path);
                logToTrainingArea("Continuing from: " + path);
            } else {
                // Fresh
                nnTrainer = NeuralNetworkInitializer.initializeTrainer();
            }

            IterativeNetVsHeuristicTrainer trainer =
                new IterativeNetVsHeuristicTrainer(nnTrainer, bsz, gamesPerRound, totalRounds, false);

            trainer.runIterativeTraining();

            String path = "networks/" + networkName + ".txt";
            nnTrainer.saveNetwork(path);
            logToTrainingArea("Saved trained network to: " + path);
        } catch (Exception e) {
            logToTrainingArea("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void runSelfPlayTraining(int numberOfGames,
                                     int valFrequency,
                                     int patience,
                                     int bsz,
                                     String networkName,
                                     boolean continuing,
                                     String existingFile) {
        try {
            logToTrainingArea("Starting SELF-PLAY training...");
            NeuralNetworkTrainer nnTrainer;
            if (continuing) {
                String path = "networks/" + existingFile;
                nnTrainer = NeuralNetworkInitializer.initializeTrainer();
                nnTrainer.loadNetwork(path);
                logToTrainingArea("Continuing from: " + path);
            } else {
                nnTrainer = NeuralNetworkInitializer.initializeTrainer();
            }

            SelfPlayMinimaxTrainer selfPlay =
                new SelfPlayMinimaxTrainer(nnTrainer, numberOfGames, valFrequency, patience);
            selfPlay.runTraining();

            String path = "networks/" + networkName + ".txt";
            nnTrainer.saveNetwork(path);
            logToTrainingArea("Saved trained network to: " + path);
        } catch (Exception e) {
            logToTrainingArea("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void logToTrainingArea(String msg) {
        Gdx.app.postRunnable(() -> {
            trainingLogArea.appendText(msg + "\n");
        });
    }

    // ------------------------------------------------------------------------
    // Standard LibGDX Screen methods
    // ------------------------------------------------------------------------
    @Override
    public void render(float delta) {
        camController.update();

        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        renderer.updatePieceInstances();
        renderer.render();

        uiManager.getStage().act(delta);
        uiManager.getStage().draw();

        if (!takGame.isGameEnded()) {
            Player current = takGame.getCurrentPlayer();
            if (current instanceof MinimaxAgent || current instanceof RandomAIPlayer) {
                handleAIMove();
            }
        }
    }

    private void handleAIMove() {
        if (isAIMoving) return;
        isAIMoving = true;

        // Temporarily disable user input
        Gdx.input.setInputProcessor(null);

        Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                try {
                    Player currentAI = takGame.getCurrentPlayer();
                    if (currentAI instanceof MinimaxAgent) {
                        ((MinimaxAgent) currentAI).makeMove(takGame);
                    } else if (currentAI instanceof RandomAIPlayer) {
                        ((RandomAIPlayer) currentAI).makeMove(takGame);
                    }

                    renderer.updatePieceInstances();
                    uiManager.addMoveToList("AI moved");
                    uiManager.updatePlayerScores();
                    updateCurrentPlayerLabel();
                    updateHotbarColors();

                    if (takGame.isGameEnded()) {
                        showGameOverDialog(takGame.getWinner());
                    }
                } catch (InvalidMoveException | GameOverException e) {
                    Logger.log("GameScreen", "AI error: " + e.getMessage());
                } finally {
                    Player next = takGame.getCurrentPlayer();
                    // Re-enable user input if next player is human
                    if (!(next instanceof MinimaxAgent || next instanceof RandomAIPlayer)) {
                        Gdx.input.setInputProcessor(new InputMultiplexer(
                            uiManager.getStage(), inputHandler, camController));
                    }
                    isAIMoving = false;
                }
            }
        }, 0.5f); // slight delay
    }

    @Override
    public void resize(int width, int height) {
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();
        uiManager.getStage().getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        renderer.dispose();
        uiManager.dispose();
        shapeRenderer.dispose();
    }

    @Override
    public void show() { }
    @Override
    public void hide() { }
    @Override
    public void pause() { }
    @Override
    public void resume() { }

    // -----------------------------------------------------------------------
    // UICallback from GameInputHandler
    // -----------------------------------------------------------------------
    @Override
    public void updateCurrentPlayerLabel() {
        uiManager.currentPlayerLabel.setText("Current Player: " + takGame.getCurrentPlayer().getColor());
    }

    @Override
    public void updateHotbarColors() {
        uiManager.updateHotbarColors();
    }

    @Override
    public void showGameOverDialog(Player winner) {
        String message;
        if (winner != null) {
            message = winner.getColor() + " wins!\nFinal Scores:\n"
                + "BLUE: " + takGame.getPlayer1().getScore() + "\n"
                + "GREEN: " + takGame.getPlayer2().getScore();
        } else {
            message = "It's a tie!\n"
                + "BLUE: " + takGame.getPlayer1().getScore() + "\n"
                + "GREEN: " + takGame.getPlayer2().getScore();
        }

        Dialog dialog = new Dialog("Game Over", uiManager.getSkin(), "dialog") {
            @Override
            protected void result(Object object) {
                if ("newGame".equals(object)) {
                    takGame.resetGame(true);
                    updateAfterGameReset();
                } else if ("continue".equals(object)) {
                    takGame.resetGame(false);
                    updateAfterGameReset();
                } else if ("exit".equals(object)) {
                    Gdx.app.exit();
                }
            }
        };
        dialog.text(message);
        dialog.button("New Game", "newGame");
        dialog.button("Continue", "continue");
        dialog.button("Exit", "exit");
        dialog.show(uiManager.getStage());
    }

    @Override
    public void showErrorDialog(String errorMessage) {
        Dialog dialog = new Dialog("Error", uiManager.getSkin(), "dialog");
        dialog.text(errorMessage);
        dialog.button("OK");
        dialog.show(uiManager.getStage());
    }

    @Override
    public void addMoveToList(String moveDescription) {
        uiManager.addMoveToList(moveDescription);
    }

    @Override
    public void updatePlayerScores() {
        uiManager.updatePlayerScores();
    }

    private void showRulesDialog() {
        Dialog dialog = new Dialog("Rules", uiManager.getSkin(), "dialog");
        dialog.text(getRulesText());
        dialog.button("OK");
        dialog.show(uiManager.getStage());
    }

    @Override
    public void showDropCountsPrompt(int sourceX, int sourceY, Direction direction, DropCountsCallback callback) {
        uiManager.promptForDropCounts(sourceX, sourceY, direction, callback);
    }

    @Override
    public PieceType getSelectedPieceType() {
        return selectedPieceType;
    }

    @Override
    public void deselectPiece() {
        selectedPieceType = null;
        renderer.removeHoverOutline();
        Gdx.app.log("GameScreen", "Piece deselected");
    }

    @Override
    public boolean isPieceSelected() {
        return (selectedPieceType != null);
    }

    /**
     * Toggles the selection of a piece type from the hotbar.
     */
    public void toggleSelection(Piece.PieceType pieceType) {
        // Restrict first two moves to FLAT_STONE only
        if (takGame.getMoveCount() < 2 && pieceType != Piece.PieceType.FLAT_STONE) {
            showErrorDialog("Only flat stones in the first two moves.");
            return;
        }
        // Check if we have any left
        int remain = takGame.getCurrentPlayer().getRemainingPieces(pieceType);
        if (remain <= 0) {
            showErrorDialog("No remaining " + pieceType + " left!");
            return;
        }

        if (selectedPieceType == pieceType) {
            // Deselect
            selectedPieceType = null;
            renderer.removeHoverOutline();
            Gdx.app.log("GameScreen", "Deselected " + pieceType);
        } else {
            // Select
            selectedPieceType = pieceType;
            renderer.removeHoverOutline();
            Gdx.app.log("GameScreen", "Selected " + pieceType);
        }
    }

    /**
     * Refreshes UI states after new game or continue.
     */
    public void updateAfterGameReset() {
        renderer.updatePieceInstances();
        uiManager.clearMovesList();
        uiManager.updatePlayerScores();
        updateCurrentPlayerLabel();
        updateHotbarColors();
        selectedPieceType = null;
        renderer.removeHoverOutline();
    }
}
