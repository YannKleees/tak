// File: core/src/main/java/com/Tak/GUI/GameScreen.java
package com.Tak.GUI;

import com.Tak.AI.evaluation.HeuristicEvaluator;
import com.Tak.AI.evaluation.IEvaluationFunction;
import com.Tak.AI.neuralnet.net.NeuralNetworkEvaluator;
import com.Tak.AI.neuralnet.net.NeuralNetworkInitializer;
import com.Tak.AI.neuralnet.trainer.IterativeNetVsHeuristicTrainer;
import com.Tak.AI.neuralnet.trainer.NeuralNetworkTrainer;
import com.Tak.AI.neuralnet.trainer.SelfPlayMinimaxTrainer;
import com.Tak.AI.players.MinimaxAgent;
import com.Tak.AI.players.RandomAIPlayer;
import com.Tak.GUI.UIManager.DropCountsCallback;
import com.Tak.Logic.models.Direction;
import com.Tak.Logic.models.GameMode;
import com.Tak.Logic.models.Piece;
import com.Tak.Logic.models.Piece.PieceType;
import com.Tak.Logic.models.Player;
import com.Tak.Logic.models.TakGame;
import com.Tak.Logic.utils.GameOverException;
import com.Tak.Logic.utils.InvalidMoveException;
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
 * the renderer, UI, input handler, and includes a "Training" tab on the right side.
 *
 * Key points:
 *  - "Back to Main Menu", "Rules", "Training Tab" are placed at top-right
 *  - AI selection (if any AI is present) is placed at bottom-right
 *  - The training panel is also on the right side, toggled by the "Training Tab" button
 */
public class GameScreen implements Screen, GameInputHandler.UICallback {

    private final TakGameMain game;
    public int boardSize = 5;  
    public TakGame takGame;    

    // Camera / rendering
    public PerspectiveCamera camera;
    public CameraInputController camController;
    private GameRenderer renderer;
    private ShapeRenderer shapeRenderer;

    // UI
    private UIManager uiManager;
    private GameInputHandler inputHandler;
    private Table mainGameTable; // normal in-game UI container

    private boolean useAI;
    private GameMode gameMode;
    private Player aiPlayer;  // if Human vs AI

    // Piece selection
    public Piece.PieceType selectedPieceType;
    private boolean isAIMoving = false;

    // Buttons at top-right
    private TextButton backButton;
    private TextButton rulesButton;
    private TextButton trainingTabButton; // toggles the training tab

    // Training Tab UI
    private Table trainingTable;
    private TextField gamesPerRoundField;
    private TextField totalRoundsField;
    private TextField selfPlayGamesField;
    private TextField valFreqField;
    private TextField patienceField;
    private TextField boardSizeField;
    private TextArea trainingLogArea;
    private TextButton startTrainingButton;
    private CheckBox continueCheckBox;
    private SelectBox<String> existingNetSelect;

    /**
     * Constructor: Builds a new TakGame internally for the chosen GameMode.
     */
    public GameScreen(TakGameMain game, GameMode gameMode) {
        this.game = game;
        this.gameMode = gameMode;
        this.useAI = (gameMode == GameMode.HUMAN_VS_AI);
        create();
    }

    /**
     * Constructor: Accepts an existing TakGame (e.g., for AI vs AI).
     */
    public GameScreen(TakGameMain game, TakGame existingGame) {
        this.game = game;
        this.takGame = existingGame;
        this.boardSize = existingGame.getBoardSize();
        this.useAI = false;
        createFromExistingGame();
    }

    private void create() {
        // Build TakGame with either Human vs AI or Human vs Human
        List<Player> players = new ArrayList<>();

        if (useAI) {
            // Human vs AI
            Player humanPlayer = new com.Tak.Logic.models.HumanPlayer(Player.Color.BLUE, 21, 1, 1);
            aiPlayer = new MinimaxAgent(Player.Color.GREEN, 21, 1, 1, 3);

            humanPlayer.setOpponent(aiPlayer);
            aiPlayer.setOpponent(humanPlayer);
            players.add(humanPlayer);
            players.add(aiPlayer);
            Logger.log("GameScreen", "Initialized Human vs AI: BLUE vs GREEN");
        } else {
            // Human vs Human
            Player p1 = new com.Tak.Logic.models.HumanPlayer(Player.Color.BLUE, 21, 1, 1);
            Player p2 = new com.Tak.Logic.models.HumanPlayer(Player.Color.GREEN, 21, 1, 1);
            p1.setOpponent(p2);
            p2.setOpponent(p1);
            players.add(p1);
            players.add(p2);
            Logger.log("GameScreen", "Initialized Human vs Human: BLUE vs GREEN");
        }

        takGame = new TakGame(boardSize, players);
        initCommonParts();
    }

    private void createFromExistingGame() {
        // We already have takGame from the constructor
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

        // Main in-game table
        mainGameTable = new Table();
        mainGameTable.setFillParent(true);
        uiManager.getStage().addActor(mainGameTable);

        // Add top-right buttons: Back, Rules, Training Tab
        addTopRightButtons();

        // If there's an AI in the game, place the AI selection in bottom-right
        if (detectAnyAI()) {
            addAISelectionDropdown();
        }

        // Add the "Training Tab" UI, also on the right
        addTrainingTabUI();

        // Update hotbar colors
        uiManager.updateHotbarColors();
    }

    /**
     * Checks if there's an AI in the players
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
     * Creates a vertical column at the top-right containing:
     * 1) "Back to Main Menu"
     * 2) "Rules"
     * 3) "Training Tab"
     */
    private void addTopRightButtons() {
        // "Back to Main Menu"
        backButton = new TextButton("Back", uiManager.getSkin());
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new MainMenuScreen(game));
            }
        });

        // "Rules"
        rulesButton = new TextButton("Rules", uiManager.getSkin());
        rulesButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                showRulesDialog();
            }
        });

        // "Training Tab" (toggles training UI)
        trainingTabButton = new TextButton("Training Tab", uiManager.getSkin());

        // We'll just toggle visibility
        trainingTabButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                boolean isVisible = trainingTable.isVisible();
                trainingTable.setVisible(!isVisible);

                // Optionally hide the mainGameTable so the board is visible behind training panel
                mainGameTable.setVisible(isVisible);
            }
        });

        // Add them in a vertical table at top-right
        Table topRightButtons = new Table();
        topRightButtons.top().right();
        topRightButtons.setFillParent(true);

        topRightButtons.add(backButton).pad(5).width(110).height(35).row();
        topRightButtons.add(rulesButton).pad(5).width(110).height(35).row();
        topRightButtons.add(trainingTabButton).pad(5).width(110).height(35).row();

        uiManager.getStage().addActor(topRightButtons);
    }

    /**
     * Places the AI selection in the bottom-right corner.
     * Allows picking AI type, depth, evaluator, and network.
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
        networkSelectBox.setVisible(false);

        Table dropdownTable = new Table();
        dropdownTable.bottom().right();
        dropdownTable.setFillParent(true);

        dropdownTable.add(new Label("AI Type:", uiManager.getSkin())).pad(3);
        dropdownTable.add(aiSelectBox).width(120).height(30).pad(3).row();

        dropdownTable.add(new Label("Minimax Depth:", uiManager.getSkin())).pad(3);
        dropdownTable.add(depthSelectBox).width(120).height(30).pad(3).row();

        dropdownTable.add(new Label("Evaluator:", uiManager.getSkin())).pad(3);
        dropdownTable.add(evalSelectBox).width(120).height(30).pad(3).row();

        dropdownTable.add(new Label("Network File:", uiManager.getSkin())).pad(3);
        dropdownTable.add(networkSelectBox).width(120).height(30).pad(3).row();

        uiManager.getStage().addActor(dropdownTable);

        // Listeners
        aiSelectBox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                String selectedAI = aiSelectBox.getSelected();
                depthSelectBox.setVisible("MinimaxAgent".equals(selectedAI));
                changeAIBehavior(selectedAI,
                                 evalSelectBox.getSelected(),
                                 networkSelectBox.getSelected(),
                                 depthSelectBox.getSelected());
            }
        });

        evalSelectBox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                String evalChoice = evalSelectBox.getSelected();
                networkSelectBox.setVisible("NeuralNetwork".equals(evalChoice));
                changeAIBehavior(aiSelectBox.getSelected(),
                                 evalChoice,
                                 networkSelectBox.getSelected(),
                                 depthSelectBox.getSelected());
            }
        });

        networkSelectBox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                changeAIBehavior(aiSelectBox.getSelected(),
                                 evalSelectBox.getSelected(),
                                 networkSelectBox.getSelected(),
                                 depthSelectBox.getSelected());
            }
        });

        depthSelectBox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                changeAIBehavior(aiSelectBox.getSelected(),
                                 evalSelectBox.getSelected(),
                                 networkSelectBox.getSelected(),
                                 depthSelectBox.getSelected());
            }
        });
    }

    /**
     * Returns a list of .txt networks from /networks folder
     */
    private Array<String> listNetworkFiles() {
        Array<String> fileNames = new Array<>();
        FileHandle dirHandle = Gdx.files.local("networks");
        if (!dirHandle.exists()) {
            fileNames.add("TrainedModel.txt");
            return fileNames;
        }
        FileHandle[] files = dirHandle.list();
        if (files != null) {
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
     * Switch AI type, depth, evaluator, etc.
     */
    private void changeAIBehavior(String aiType,
                                  String evalType,
                                  String networkFile,
                                  int depth) {
        // Identify AI player
        if (aiPlayer == null) {
            for (Player p : takGame.getPlayers()) {
                if (p instanceof MinimaxAgent || p instanceof RandomAIPlayer) {
                    aiPlayer = p;
                    break;
                }
            }
        }
        if (aiPlayer == null) {
            Logger.log("GameScreen", "No AI found in players.");
            return;
        }

        // 1) If user picked "RandomAI"
        if ("RandomAI".equals(aiType)) {
            Player newAI = new RandomAIPlayer(
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

        // 2) Minimax
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
                Logger.log("GameScreen", "Loaded NeuralNet from " + filePath);
            } catch (Exception e) {
                Logger.log("GameScreen", "Failed to load network: " + e.getMessage());
                evaluator = new HeuristicEvaluator(); // fallback
            }
        }

        Player newAI = new MinimaxAgent(
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
             + "3. You can stack pieces and move the stacks\n"
             + "4. Road victory or flat victory.\n"
             + "5.for more information read the ReadMe file";
    }
    /**
     * Training Tab UI on the right side, toggled by the "Training Tab" button.
     */
    private void addTrainingTabUI() {
        // The training table
        trainingTable = new Table(uiManager.getSkin());
        trainingTable.setBackground(uiManager.getSkin().newDrawable("button-up", new Color(0,0,0,0.7f)));
        trainingTable.pad(15);
        trainingTable.setVisible(false);  // hidden by default

        // Title
        Label trainingTitle = new Label("Neural Net Training", uiManager.getSkin());
        trainingTitle.setColor(Color.YELLOW);
        trainingTable.add(trainingTitle).colspan(2).padBottom(10).row();

        // Mode
        Label modeLabel = new Label("Training Mode:", uiManager.getSkin());
        final SelectBox<String> modeSelectBox = new SelectBox<>(uiManager.getSkin());
        modeSelectBox.setItems("Iterative vs Heuristic", "Self-Play");
        modeSelectBox.setSelected("Iterative vs Heuristic");
        trainingTable.add(modeLabel).pad(5).right();
        trainingTable.add(modeSelectBox).pad(5).row();

        // Iterative fields
        Label gamesLabel = new Label("Games Per Round:", uiManager.getSkin());
        gamesPerRoundField = new TextField("10", uiManager.getSkin());
        Label roundsLabel = new Label("Total Rounds:", uiManager.getSkin());
        totalRoundsField = new TextField("3", uiManager.getSkin());
        trainingTable.add(gamesLabel).pad(5).right();
        trainingTable.add(gamesPerRoundField).width(60).row();
        trainingTable.add(roundsLabel).pad(5).right();
        trainingTable.add(totalRoundsField).width(60).row();

        // Self-play sub-table
        final Table selfPlayTable = new Table(uiManager.getSkin());
        Label selfPlayGamesLabel = new Label("Number of Games:", uiManager.getSkin());
        selfPlayGamesField = new TextField("20", uiManager.getSkin());
        Label valFreqLabel = new Label("Validation Freq:", uiManager.getSkin());
        valFreqField = new TextField("5", uiManager.getSkin());
        Label patienceLabel = new Label("Patience:", uiManager.getSkin());
        patienceField = new TextField("3", uiManager.getSkin());

        selfPlayTable.add(selfPlayGamesLabel).pad(5).right();
        selfPlayTable.add(selfPlayGamesField).width(60).row();
        selfPlayTable.add(valFreqLabel).pad(5).right();
        selfPlayTable.add(valFreqField).width(60).row();
        selfPlayTable.add(patienceLabel).pad(5).right();
        selfPlayTable.add(patienceField).width(60).row();
        selfPlayTable.setVisible(false);
        trainingTable.add(selfPlayTable).colspan(2).row();

        // Board size
        Label bszLabel = new Label("Board Size:", uiManager.getSkin());
        boardSizeField = new TextField("5", uiManager.getSkin());
        trainingTable.add(bszLabel).pad(5).right();
        trainingTable.add(boardSizeField).width(60).row();

        // "Continue from existing net?"
        continueCheckBox = new CheckBox(" Continue from Existing Net?", uiManager.getSkin());
        trainingTable.add(continueCheckBox).colspan(2).pad(5).left().row();

        final Label existingLabel = new Label("Existing Net:", uiManager.getSkin());
        existingLabel.setVisible(false);
        existingNetSelect = new SelectBox<>(uiManager.getSkin());
        existingNetSelect.setVisible(false);

        // Fill from /networks
        existingNetSelect.setItems(listExistingNetworks());

        final Table continueTable = new Table(uiManager.getSkin());
        continueTable.add(existingLabel).pad(5).right();
        continueTable.add(existingNetSelect).width(150).pad(5);
        continueTable.setVisible(false);
        trainingTable.add(continueTable).colspan(2).row();

        continueCheckBox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                boolean checked = continueCheckBox.isChecked();
                existingLabel.setVisible(checked);
                existingNetSelect.setVisible(checked);
                continueTable.setVisible(checked);
            }
        });

        // "Save network as"
        Label saveNameLabel = new Label("Save Net As:", uiManager.getSkin());
        final TextField saveNameField = new TextField("MyNewNetwork", uiManager.getSkin());
        trainingTable.add(saveNameLabel).pad(5).right();
        trainingTable.add(saveNameField).width(100).row();

        // Start/Stop
        startTrainingButton = new TextButton("Start Training", uiManager.getSkin());
        final TextButton stopTrainingButton = new TextButton("Stop", uiManager.getSkin());
        stopTrainingButton.setVisible(false);

        Table btnTable = new Table(uiManager.getSkin());
        btnTable.add(startTrainingButton).padRight(10);
        btnTable.add(stopTrainingButton);
        trainingTable.add(btnTable).colspan(2).padTop(10).row();

        // Log area
        Label logLabel = new Label("Training Log:", uiManager.getSkin());
        trainingTable.add(logLabel).colspan(2).padTop(10).row();

        trainingLogArea = new TextArea("", uiManager.getSkin());
        trainingLogArea.setDisabled(true);
        trainingLogArea.setPrefRows(6);

        ScrollPane scrollPane = new ScrollPane(trainingLogArea, uiManager.getSkin());
        scrollPane.setFlickScroll(true);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);
        scrollPane.setOverscroll(false, false);

        trainingTable.add(scrollPane).colspan(2).minHeight(150).maxHeight(220).width(300).pad(5);

        // Instead of top-right, place the training panel near center:
        Table trainingPanelContainer = new Table();
        trainingPanelContainer.center();    // <-- Align center horizontally/vertically
        trainingPanelContainer.setFillParent(true);

        // If you just want it *below the top row of buttons* but still center horizontally,
        // you could do .top() + padTop(130). E.g.:
        //
        // trainingPanelContainer.top().center();
        // trainingPanelContainer.padTop(130);

        trainingPanelContainer.add(trainingTable).width(700).height(800);
        trainingTable.setBackground(uiManager.getSkin().newDrawable("button-up", new Color(0,0,0,0.8f)));

        uiManager.getStage().addActor(trainingPanelContainer);

        // Mode selection toggles iterative vs self-play
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

        startTrainingButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                String mode = modeSelectBox.getSelected();
                boolean isIterative = mode.equals("Iterative vs Heuristic");

                int bsz = Integer.parseInt(boardSizeField.getText().trim());
                String saveName = saveNameField.getText().trim();
                boolean continuing = continueCheckBox.isChecked();
                String existingFile = existingNetSelect.getSelected();

                stopTrainingButton.setVisible(false);

                // run in background
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
                    Gdx.app.postRunnable(() -> stopTrainingButton.setVisible(false));
                }).start();
            }
        });

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
    private void runIterativeTraining(int gamesPerRound, int totalRounds, int bsz,
                                      String networkName, boolean continuing, String existingFile) {
        try {
            logToTrainingArea("Starting ITERATIVE training... (vs Heuristic)");
            NeuralNetworkTrainer nnTrainer;
            if (continuing) {
                String path = "networks/" + existingFile;
                nnTrainer = NeuralNetworkInitializer.initializeTrainer();
                nnTrainer.loadNetwork(path);
                logToTrainingArea("Continuing from: " + path);
            } else {
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

    private void runSelfPlayTraining(int numberOfGames, int valFrequency, int patience,
                                     int bsz, String networkName,
                                     boolean continuing, String existingFile) {
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
    // Screen overrides
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

        // If not ended, maybe handle AI
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
        // disable input
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
                    // re-enable if next is human
                    Player next = takGame.getCurrentPlayer();
                    if (!(next instanceof MinimaxAgent || next instanceof RandomAIPlayer)) {
                        Gdx.input.setInputProcessor(
                            new InputMultiplexer(uiManager.getStage(), inputHandler, camController)
                        );
                    }
                    isAIMoving = false;
                }
            }
        }, 0.5f);
    }

    @Override
    public void resize(int width, int height) {
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();
        uiManager.getStage().getViewport().update(width, height, true);
    }

    @Override
    public void show() { }
    @Override
    public void hide() { }
    @Override
    public void pause() { }
    @Override
    public void resume() { }

    @Override
    public void dispose() {
        renderer.dispose();
        uiManager.dispose();
        shapeRenderer.dispose();
    }

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
        Label rulesLabel = new Label(getRulesText(), uiManager.getSkin());
        rulesLabel.setWrap(true);
        rulesLabel.setFontScale(1.1f);

        ScrollPane scrollPane = new ScrollPane(rulesLabel, uiManager.getSkin());
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);

        dialog.getContentTable().add(scrollPane).width(500).height(300).pad(10).row();
        dialog.button("Close");
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
        // First two moves => only FLAT_STONE
        if (takGame.getMoveCount() < 2 && pieceType != Piece.PieceType.FLAT_STONE) {
            showErrorDialog("Only flat stones in the first two moves.");
            return;
        }
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
     * Refreshes UI states after new game or continue
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
