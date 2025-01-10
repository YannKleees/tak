package com.Tak.GUI;

import com.Tak.AI.players.MinimaxAgent;
import com.Tak.AI.players.RandomAIPlayer;
import com.Tak.GUI.UIManager.DropCountsCallback;
import com.Tak.Logic.exceptions.GameOverException;
import com.Tak.Logic.exceptions.InvalidMoveException;
import com.Tak.Logic.models.Direction;
import com.Tak.Logic.models.Piece;
import com.Tak.Logic.models.Piece.PieceType;
import com.Tak.Logic.models.Player;
import com.Tak.Logic.models.TakGame;
import com.Tak.Logic.models.GameMode;
import com.Tak.Logic.utils.Logger;
import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

import java.util.ArrayList;
import java.util.List;

/**
 * The GameScreen class handles the main game screen, coordinating between the renderer, UI, and input handler.
 */
public class GameScreen implements Screen, GameInputHandler.UICallback {

    private TakGameMain game;
    public int boardSize = 5;        // Default if we build a new TakGame
    public TakGame takGame;          // The actual game logic

    public PerspectiveCamera camera;
    public CameraInputController camController;
    private GameRenderer renderer;
    private UIManager uiManager;
    private GameInputHandler inputHandler;

    public Piece.PieceType selectedPieceType;
    private boolean isAIMoving = false;
    private TextButton rulesButton;
    private ShapeRenderer shapeRenderer;
    public int moveCount = 0;        // Not used much, but preserved
    private boolean useAI;
    private GameMode gameMode;       // Only if we used the old constructor with a GameMode

    private Player aiPlayer;         // Reference if we do a Human vs AI scenario

    /**
     * Constructor #1:
     * Creates the GameScreen based on a chosen GameMode (e.g., Human vs AI, Human vs Human).
     * This internally builds a new TakGame (with default boardSize=5).
     */
    public GameScreen(TakGameMain game, GameMode gameMode) {
        this.game = game;
        this.gameMode = gameMode;
        this.useAI = (gameMode == GameMode.HUMAN_VS_AI);
        create();  // builds takGame internally
    }

    /**
     * Constructor #2 (NEW):
     * Accepts an existing TakGame, so you can skip building another one internally.
     * (e.g., for AI vs AI, or a custom board size, piece counts, etc.)
     */
    public GameScreen(TakGameMain game, TakGame existingGame) {
        this.game = game;
        this.takGame = existingGame;
        this.boardSize = existingGame.getBoardSize(); // override local boardSize
        this.useAI = false; // or detect if there's an AI in takGame
        createFromExistingGame();    // sets up camera, UI, input, etc.
    }

    /**
     * If we are building our own TakGame internally, we call this method from the first constructor.
     */
    public void create() {
        // Initialize the game logic based on gameMode
        List<Player> players = new ArrayList<>();

        if (useAI) {
            // Human vs. AI Mode
            Player humanPlayer = new com.Tak.Logic.players.HumanPlayer(Player.Color.BLUE, 21, 1, 1);
            aiPlayer = new MinimaxAgent(Player.Color.GREEN, 21, 1, 1, 3); // Example AI

            humanPlayer.setOpponent(aiPlayer);
            aiPlayer.setOpponent(humanPlayer);

            players.add(humanPlayer);
            players.add(aiPlayer);
            Logger.log("GameScreen", "Initialized Human vs AI: BLUE (Human) vs GREEN (AI)");
        } else {
            // Human vs. Human Mode
            Player player1 = new com.Tak.Logic.players.HumanPlayer(Player.Color.BLUE, 21, 1, 1);
            Player player2 = new com.Tak.Logic.players.HumanPlayer(Player.Color.GREEN, 21, 1, 1);

            player1.setOpponent(player2);
            player2.setOpponent(player1);

            players.add(player1);
            players.add(player2);
            Logger.log("GameScreen", "Initialized Human vs Human: BLUE vs GREEN");
        }

        // Build a new TakGame with local boardSize (default 5)
        takGame = new TakGame(boardSize, players);

        // Finish setup
        initCommonParts();
    }

    /**
     * If a TakGame is externally provided (the second constructor),
     * we skip building players and just init camera, UI, input, etc.
     */
    private void createFromExistingGame() {
        initCommonParts();
    }

    /**
     * Common code that sets up the camera, renderer, UI, input, etc.
     */
    private void initCommonParts() {
        // Set up the camera
        camera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(boardSize * 2f, boardSize * 2f, boardSize * 2f);
        camera.lookAt(boardSize / 2f, 0, boardSize / 2f);
        camera.near = 0.1f;
        camera.far = 100f;
        camera.update();

        // Camera controller
        camController = new CameraInputController(camera);
        camController.rotateButton = Input.Buttons.RIGHT;
        camController.translateButton = Input.Buttons.MIDDLE;
        camController.scrollFactor = -0.1f;

        // GameRenderer
        renderer = new GameRenderer(camera, boardSize, takGame);
        renderer.updatePieceInstances();

        shapeRenderer = new ShapeRenderer();

        // UI Manager
        uiManager = new UIManager(takGame, this);
        uiManager.getStage().setViewport(new ScreenViewport());

        // Input Handler
        inputHandler = new GameInputHandler(camera, takGame, renderer, uiManager, this);

        // Combine inputs
        Gdx.input.setInputProcessor(new InputMultiplexer(uiManager.getStage(), inputHandler, camController));

        addRulesButton();
        addBackButton();

        // Optionally add AI selection if takGame has an AI (or always, your choice)
        if (detectAnyAI()) {
            addAISelectionDropdown();
        }

        uiManager.updateHotbarColors();
    }

    /**
     * Utility: check if any player in takGame is Minimax or Random AI.
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
     * Adds the Rules button to the UI.
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
     * Adds a Back button to return to the main menu.
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
                game.setScreen(new MainMenuScreen(game)); // Return to main menu
            }
        });
    }

    /**
     * Adds a dropdown for AI selection to dynamically change the AI type,
     * plus a dropdown for Minimax depth.
     */
    private void addAISelectionDropdown() {
        SelectBox<String> aiSelectBox = new SelectBox<>(uiManager.getSkin());
        aiSelectBox.setItems("RandomAI", "MinimaxAgent");
        aiSelectBox.setSelected("MinimaxAgent");

        SelectBox<Integer> depthSelectBox = new SelectBox<>(uiManager.getSkin());
        depthSelectBox.setItems(1, 2, 3, 4);
        depthSelectBox.setSelected(3);

        Table dropdownTable = new Table();
        dropdownTable.bottom().right();
        dropdownTable.setFillParent(true);

        dropdownTable.add(new Label("AI Type: ", uiManager.getSkin())).pad(10);
        dropdownTable.add(aiSelectBox).width(150).height(40).pad(10);
        dropdownTable.row();
        dropdownTable.add(new Label("Minimax Depth: ", uiManager.getSkin())).pad(10);
        dropdownTable.add(depthSelectBox).width(150).height(40).pad(10);

        uiManager.getStage().addActor(dropdownTable);

        aiSelectBox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                String selectedAI = aiSelectBox.getSelected();
                depthSelectBox.setVisible("MinimaxAgent".equals(selectedAI));
                changeAIBehavior(selectedAI, depthSelectBox.getSelected());
            }
        });

        depthSelectBox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                String selectedAI = aiSelectBox.getSelected();
                if ("MinimaxAgent".equals(selectedAI)) {
                    int selectedDepth = depthSelectBox.getSelected();
                    changeAIBehavior(selectedAI, selectedDepth);
                }
            }
        });
    }

    /**
     * Dynamically replace the AI (if any) with a new AI type.
     */
    private void changeAIBehavior(String aiType, int depth) {
        // If we haven't stored aiPlayer yet, let's attempt to find it
        if (aiPlayer == null) {
            for (Player p : takGame.getPlayers()) {
                if (p instanceof MinimaxAgent || p instanceof RandomAIPlayer) {
                    aiPlayer = p;
                    break;
                }
            }
        }
        if (!(aiPlayer instanceof MinimaxAgent || aiPlayer instanceof RandomAIPlayer)) {
            Logger.log("GameScreen", "No AI found to replace or the current AI isn't recognized.");
            return;
        }

        Player newAI;
        if ("RandomAI".equals(aiType)) {
            newAI = new RandomAIPlayer(Player.Color.GREEN,
                aiPlayer.getRemainingPieces(PieceType.FLAT_STONE),
                aiPlayer.getRemainingPieces(PieceType.STANDING_STONE),
                aiPlayer.getRemainingPieces(PieceType.CAPSTONE));
            Logger.log("GameScreen", "Switching AI to RandomAI");
        } else {
            newAI = new MinimaxAgent(Player.Color.GREEN,
                aiPlayer.getRemainingPieces(PieceType.FLAT_STONE),
                aiPlayer.getRemainingPieces(PieceType.STANDING_STONE),
                aiPlayer.getRemainingPieces(PieceType.CAPSTONE),
                depth);
            Logger.log("GameScreen", "Switching AI to MinimaxAgent with depth = " + depth);
        }

        takGame.replacePlayer(aiPlayer, newAI);
        aiPlayer = newAI;
    }

    /**
     * A short rules text for the "Rules" dialog.
     */
    private String getRulesText() {
        return "1. Players take turns placing tiles on the board. "
            + "No standing or cap tiles can be placed in the first round.\n"
            + "2. Movement is orthogonal. Only the top tile of a stack can be moved.\n"
            + "3. Road victory or flat victory.\n"
            + "4. Etc...\n";
    }

    /**
     * The core render method. Updates camera, clears screen, renders board & UI,
     * and triggers AI moves if needed.
     */
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

        // If the game isn't ended, check if it's an AI's turn
        if (!takGame.isGameEnded()) {
            Player current = takGame.getCurrentPlayer();
            if (current instanceof MinimaxAgent || current instanceof RandomAIPlayer) {
                handleAIMove();
            }
        }
    }

    /**
     * If it's an AI player's turn, schedule a short Task to let them move automatically.
     */
    private void handleAIMove() {
        if (isAIMoving) return;
        isAIMoving = true;

        // Disable input so we don't interfere while the AI chooses
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
                    Logger.log("GameScreen", "AI encountered an error: " + e.getMessage());
                } finally {
                    // If next player isn't an AI, restore input
                    Player nextPlayer = takGame.getCurrentPlayer();
                    if (!(nextPlayer instanceof MinimaxAgent || nextPlayer instanceof RandomAIPlayer)) {
                        Gdx.input.setInputProcessor(new InputMultiplexer(
                            uiManager.getStage(), inputHandler, camController));
                    }
                    isAIMoving = false;
                }
            }
        }, 0.5f); // 0.5-second delay
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
    public void show() {
        // Called when this screen becomes the current screen
    }

    @Override
    public void hide() {
        // Called when this screen is no longer the current screen
    }

    @Override
    public void pause() {
        // Called when the application is paused
    }

    @Override
    public void resume() {
        // Called when the application is resumed
    }

    /**
     * Toggles the selection of a piece type.
     * If it's already selected, deselect it; otherwise select it.
     */
    public void toggleSelection(Piece.PieceType pieceType) {
        // Restrict standing/cap in the first two moves
        if (takGame.getMoveCount() < 2 && pieceType != Piece.PieceType.FLAT_STONE) {
            showErrorDialog("Only flat stones can be placed in the first two moves.");
            return;
        }

        // Check if the current player has any of that piece left
        int remainingPieces = takGame.getCurrentPlayer().getRemainingPieces(pieceType);
        if (remainingPieces <= 0) {
            showErrorDialog("No remaining " + pieceType.toString().replace("_", " ").toLowerCase() + "s left.");
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
     * Updates the UI elements after resetting the game (like clearing the move list).
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

    // --------------------------
    // UICallback implementations
    // --------------------------

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
            message = winner.getColor() + " wins!\n"
                + "Final Scores:\n"
                + "BLUE: " + takGame.getPlayer1().getScore() + "\n"
                + "GREEN: " + takGame.getPlayer2().getScore();
        } else {
            message = "It's a tie!\n"
                + "Final Scores:\n"
                + "BLUE: " + takGame.getPlayer1().getScore() + "\n"
                + "GREEN: " + takGame.getPlayer2().getScore();
        }

        Dialog dialog = new Dialog("Game Over", uiManager.getSkin(), "dialog") {
            @Override
            protected void result(Object object) {
                if (object.equals("newGame")) {
                    takGame.resetGame(true); // Reset both board & scores
                    updateAfterGameReset();
                } else if (object.equals("continue")) {
                    takGame.resetGame(false); // Keep scores
                    updateAfterGameReset();
                } else if (object.equals("exit")) {
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
        return selectedPieceType != null;
    }
}
