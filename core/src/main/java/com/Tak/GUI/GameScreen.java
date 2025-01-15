package com.Tak.GUI;

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
import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.util.ArrayList;
import java.util.List;

/**
 * The GameScreen class handles the main game screen, coordinating between the renderer, UI, and input handler.
 * Supports Pausing, Move History (Back/Forward), branching when new moves are made after going back, etc.
 */
public class GameScreen implements Screen, GameInputHandler.UICallback {

    // Reference to main application
    private final TakGameMain game;

    // The actual game logic
    public TakGame takGame;
    public int boardSize;

    // Rendering & Input
    private PerspectiveCamera camera;
    private CameraInputController camController;
    private GameRenderer renderer;
    private UIManager uiManager;
    private GameInputHandler inputHandler;
    private ShapeRenderer shapeRenderer;

    // Flags
    private boolean isAIMoving = false;
    private boolean paused = false;  // If paused, no new moves by humans

    // Move History & Index
    private List<com.Tak.Logic.models.Board> boardHistory;
    private int currentHistoryIndex;

    // Player Interaction
    public Piece.PieceType selectedPieceType = null;

    // Buttons & UI
    private TextButton rulesButton;
    private boolean useAI; // If human vs AI
    private GameMode gameMode;

    // Possibly store a reference to the AI player if needed
    private Player aiPlayer;

    /**
     * Constructor #1: Build a new TakGame internally based on GameMode.
     */
    public GameScreen(TakGameMain game, GameMode gameMode) {
        this.game = game;
        this.gameMode = gameMode;
        this.useAI = (gameMode == GameMode.HUMAN_VS_AI);
        createNewTakGame(); // Builds takGame internally
    }

    /**
     * Constructor #2: Use an existing TakGame (e.g., loaded from file).
     */
    public GameScreen(TakGameMain game, TakGame existingGame) {
        this.game = game;
        this.takGame = existingGame;
        this.boardSize = existingGame.getBoardSize();
        this.useAI = false; // Or detect if there's an AI in takGame
        initCommonParts();  // Skip building players, just init cameras, UI, etc.
    }

    /**
     * Creates a new TakGame internally based on the specified gameMode.
     */
    private void createNewTakGame() {
        List<Player> players = new ArrayList<>();

        switch (gameMode) {
            case HUMAN_VS_HUMAN:
                // Two human players
            {
                Player p1 = new com.Tak.Logic.players.HumanPlayer(Player.Color.BLUE, 21, 1, 1);
                Player p2 = new com.Tak.Logic.players.HumanPlayer(Player.Color.GREEN, 21, 1, 1);
                p1.setOpponent(p2);
                p2.setOpponent(p1);
                players.add(p1);
                players.add(p2);
                boardSize = 5;
                Logger.log("GameScreen", "Initialized Human vs Human");
            }
            break;
            case HUMAN_VS_AI:
                // One Human, one AI
            {
                Player humanPlayer = new com.Tak.Logic.players.HumanPlayer(Player.Color.BLUE, 21, 1, 1);
                Player minimax = new MinimaxAgent(Player.Color.GREEN, 21, 1, 1, 3);
                humanPlayer.setOpponent(minimax);
                minimax.setOpponent(humanPlayer);
                players.add(humanPlayer);
                players.add(minimax);
                aiPlayer = minimax;
                boardSize = 5;
                Logger.log("GameScreen", "Initialized Human vs AI");
            }
            break;
            case AI_VS_AI:
                // Two AI players
            {
                Player ai1 = new MinimaxAgent(Player.Color.BLUE, 21, 1, 1, 3);
                Player ai2 = new MinimaxAgent(Player.Color.GREEN, 21, 1, 1, 3);
                ai1.setOpponent(ai2);
                ai2.setOpponent(ai1);
                players.add(ai1);
                players.add(ai2);
                aiPlayer = ai1; // Just store one reference if needed
                boardSize = 5;
                Logger.log("GameScreen", "Initialized AI vs AI");
            }
            break;
            default:
                // fallback
                Logger.log("GameScreen", "Unknown gameMode. Defaulting to Human vs Human.");
                boardSize = 5;
                Player p1 = new com.Tak.Logic.players.HumanPlayer(Player.Color.BLUE, 21, 1, 1);
                Player p2 = new com.Tak.Logic.players.HumanPlayer(Player.Color.GREEN, 21, 1, 1);
                p1.setOpponent(p2);
                p2.setOpponent(p1);
                players.add(p1);
                players.add(p2);
                break;
        }

        // Build the TakGame
        this.takGame = new TakGame(boardSize, players);

        // Then init camera, UI, inputs, etc.
        initCommonParts();
    }

    /**
     * Sets up camera, UI, input, and the board history system.
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

        // UI Manager & Stage
        uiManager = new UIManager(takGame, this);
        Viewport viewport = new ScreenViewport();
        uiManager.getStage().setViewport(viewport);

        // Input Handler
        inputHandler = new GameInputHandler(camera, takGame, renderer, uiManager, this);

        // Combine Stage + GameInput + Camera
        Gdx.input.setInputProcessor(new InputMultiplexer(uiManager.getStage(), inputHandler, camController));

        // Add UI elements
        addRulesButton();
        addBackButton();
        addPauseAndMoveButtons();
        if (detectAnyAI()) {
            addAISelectionDropdown();
        }
        uiManager.updateHotbarColors();

        // Initialize Move History
        boardHistory = new ArrayList<>();
        currentHistoryIndex = 0;
        // Save the initial board state
        boardHistory.add(takGame.getBoard().copy());
    }

    /**
     * If there's any AI player (Minimax/Random).
     */
    private boolean detectAnyAI() {
        for (Player p : takGame.getPlayers()) {
            if (p instanceof MinimaxAgent || p instanceof RandomAIPlayer) {
                return true;
            }
        }
        return false;
    }

    // --------------------------------------------------
    // UI Buttons: Pause, Back, Forward, Rules, BackToMenu
    // --------------------------------------------------

    private void addPauseAndMoveButtons() {
        Table table = new Table();
        table.top().right();
        table.setFillParent(true);

        // Pause/Resume
        TextButton pauseButton = new TextButton("Pause", uiManager.getSkin());
        pauseButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                paused = !paused;
                pauseButton.setText(paused ? "Resume" : "Pause");
            }
        });

        // Back
        TextButton backButton = new TextButton("Back", uiManager.getSkin());
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float px, float py) {
                if (currentHistoryIndex > 0) {
                    currentHistoryIndex--;
                    // Retrieve old board state
                    takGame.setBoard(boardHistory.get(currentHistoryIndex).copy());
                    // Refresh rendering & UI
                    renderer.updatePieceInstances();
                    uiManager.updatePlayerScores();
                    updateCurrentPlayerLabel();
                    updateHotbarColors();
                }
            }
        });

        // Forward
        TextButton forwardButton = new TextButton("Forward", uiManager.getSkin());
        forwardButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float px, float py) {
                if (currentHistoryIndex < boardHistory.size() - 1) {
                    currentHistoryIndex++;
                    takGame.setBoard(boardHistory.get(currentHistoryIndex).copy());
                    renderer.updatePieceInstances();
                    uiManager.updatePlayerScores();
                    updateCurrentPlayerLabel();
                    updateHotbarColors();
                }
            }
        });

        table.add(pauseButton).pad(5).width(80).height(30);
        table.row();
        table.add(backButton).pad(5).width(80).height(30);
        table.row();
        table.add(forwardButton).pad(5).width(80).height(30);

        uiManager.getStage().addActor(table);
    }

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

        // Switch AI type
        aiSelectBox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                String selectedAI = aiSelectBox.getSelected();
                depthSelectBox.setVisible("MinimaxAgent".equals(selectedAI));
                changeAIBehavior(selectedAI, depthSelectBox.getSelected());
            }
        });

        // Switch depth
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

    private void changeAIBehavior(String aiType, int depth) {
        if (aiPlayer == null) {
            // find the existing AI among players
            for (Player p : takGame.getPlayers()) {
                if (p instanceof MinimaxAgent || p instanceof RandomAIPlayer) {
                    aiPlayer = p;
                    break;
                }
            }
        }
        if (!(aiPlayer instanceof MinimaxAgent || aiPlayer instanceof RandomAIPlayer)) {
            Logger.log("GameScreen", "No AI found to replace, or the current AI isn't recognized.");
            return;
        }

        Player newAI;
        if ("RandomAI".equals(aiType)) {
            newAI = new RandomAIPlayer(
                Player.Color.GREEN,
                aiPlayer.getRemainingPieces(PieceType.FLAT_STONE),
                aiPlayer.getRemainingPieces(PieceType.STANDING_STONE),
                aiPlayer.getRemainingPieces(PieceType.CAPSTONE)
            );
            Logger.log("GameScreen", "Switching AI to RandomAI");
        } else {
            newAI = new MinimaxAgent(
                Player.Color.GREEN,
                aiPlayer.getRemainingPieces(PieceType.FLAT_STONE),
                aiPlayer.getRemainingPieces(PieceType.STANDING_STONE),
                aiPlayer.getRemainingPieces(PieceType.CAPSTONE),
                depth
            );
            Logger.log("GameScreen", "Switching AI to MinimaxAgent with depth=" + depth);
        }

        takGame.replacePlayer(aiPlayer, newAI);
        aiPlayer = newAI;
    }

    private String getRulesText() {
        return "1. Players take turns placing or moving a stack.\n"
            + "2. Only flat stones are allowed in the first two moves.\n"
            + "3. Movement is orthogonal. Road or flat victory.\n"
            + "4. Etc.\n";
    }

    // ------------------------------------
    // RENDER & UPDATE
    // ------------------------------------

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

        // If paused or game ended, skip AI
        if (paused || takGame.isGameEnded()) {
            return;
        }

        // If current player is AI, schedule a move
        Player current = takGame.getCurrentPlayer();
        if (current instanceof MinimaxAgent || current instanceof RandomAIPlayer) {
            handleAIMove();
        }
    }

    private void handleAIMove() {
        if (isAIMoving) return;
        isAIMoving = true;

        // Temporarily disable input so the human doesn’t disrupt the board while we start the AI
        Gdx.input.setInputProcessor(null);

        // ADDED/CHANGED FOR THREADING: Move the AI search into a separate Thread
        new Thread(() -> {
            try {
                // Sleep a bit so user can see the board state
                Thread.sleep(500); // optional small delay

                Player currentAI = takGame.getCurrentPlayer();
                if (currentAI instanceof MinimaxAgent || currentAI instanceof RandomAIPlayer) {
                    currentAI.makeMove(takGame);
                }

                // Once AI move is done, schedule the UI updates back on the LibGDX render thread
                Gdx.app.postRunnable(() -> {
                    renderer.updatePieceInstances();
                    uiManager.addMoveToList("AI moved");
                    uiManager.updatePlayerScores();
                    updateCurrentPlayerLabel();
                    updateHotbarColors();

                    if (takGame.isGameEnded()) {
                        showGameOverDialog(takGame.getWinner());
                    }

                    // Re-enable input for the user in the main thread
                    Gdx.input.setInputProcessor(new InputMultiplexer(uiManager.getStage(), inputHandler, camController));

                    isAIMoving = false;
                });

            } catch (InvalidMoveException | GameOverException e) {
                Logger.log("GameScreen", "AI encountered error: " + e.getMessage());
                // Also re-enable input on error
                Gdx.app.postRunnable(() -> {
                    Gdx.input.setInputProcessor(new InputMultiplexer(uiManager.getStage(), inputHandler, camController));
                    isAIMoving = false;
                });
            } catch (InterruptedException e) {
                // If the thread is interrupted
                Logger.log("GameScreen", "AI thread was interrupted: " + e.getMessage());
            }
        }).start();
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
    public void show() {}
    @Override
    public void hide() {}
    @Override
    public void pause() {}
    @Override
    public void resume() {}

    // ------------------------------------
    // UICallback methods & move logic
    // ------------------------------------

    /**
     * Toggling selection of a piece type to place.
     * If paused => block. If user goes back => typical branching logic handled in addMoveToList
     */
    public void toggleSelection(PieceType pieceType) {
        if (paused) {
            showErrorDialog("Cannot place or select pieces while paused!");
            return;
        }

        // Example: block standing/cap for first 2 moves
        if (takGame.getMoveCount() < 2 && pieceType != PieceType.FLAT_STONE) {
            showErrorDialog("Only flat stones can be placed in the first two moves.");
            return;
        }

        int remaining = takGame.getCurrentPlayer().getRemainingPieces(pieceType);
        if (remaining <= 0) {
            showErrorDialog("No remaining " + pieceType + "s left.");
            return;
        }

        if (selectedPieceType == pieceType) {
            selectedPieceType = null;
            renderer.removeHoverOutline();
        } else {
            selectedPieceType = pieceType;
            renderer.removeHoverOutline();
        }
    }

    /**
     * After resetting the game, we clear move history, UI, etc.
     */
    public void updateAfterGameReset() {
        renderer.updatePieceInstances();
        uiManager.clearMovesList();
        uiManager.updatePlayerScores();
        updateCurrentPlayerLabel();
        updateHotbarColors();
        selectedPieceType = null;
        renderer.removeHoverOutline();

        boardHistory.clear();
        boardHistory.add(takGame.getBoard().copy());
        currentHistoryIndex = 0;
    }

    // UICallback interface:

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

    /**
     * Called each time a move is completed (human or AI).
     * Adds the new board state to history, removing "future" states if we branched.
     * Also updates moveCount for clarity.
     */
    @Override
    public void addMoveToList(String moveDescription) {
        // ADDED CODE: ensure TakGame increments the move count if it isn't done inside placePiece/moveStack
        // takGame.incrementMoveCount(); // Only if needed; many TakGame methods do it themselves.

        uiManager.addMoveToList(moveDescription);

        // If we branched by going back, remove all future states
        if (currentHistoryIndex < boardHistory.size() - 1) {
            while (boardHistory.size() - 1 > currentHistoryIndex) {
                boardHistory.remove(boardHistory.size() - 1);
            }
        }

        // Add the new board state
        boardHistory.add(takGame.getBoard().copy());
        currentHistoryIndex = boardHistory.size() - 1;

        // Also update UI so new move is reflected
        renderer.updatePieceInstances();
        uiManager.updatePlayerScores();
        updateCurrentPlayerLabel();
        updateHotbarColors();
    }

    @Override
    public void updatePlayerScores() {
        uiManager.updatePlayerScores();
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
    }

    @Override
    public boolean isPieceSelected() {
        return (selectedPieceType != null);
    }

    private void showRulesDialog() {
        Dialog dialog = new Dialog("Rules", uiManager.getSkin(), "dialog");
        dialog.text(getRulesText());
        dialog.button("OK");
        dialog.show(uiManager.getStage());
    }
}
