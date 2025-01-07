// File: core/src/main/java/com/Tak/GUI/GameScreen.java
package com.Tak.GUI;

import com.Tak.AI.players.MinimaxAgent;
import com.Tak.GUI.UIManager.DropCountsCallback;
import com.Tak.Logic.exceptions.GameOverException;
import com.Tak.Logic.exceptions.InvalidMoveException;
import com.Tak.Logic.models.Direction;
import com.Tak.Logic.models.Piece;
import com.Tak.Logic.models.Piece.PieceType;
import com.Tak.Logic.models.Player;
import com.Tak.Logic.models.TakGame;
import com.Tak.Logic.players.HumanPlayer;
import com.Tak.Logic.utils.Logger;
import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import java.util.ArrayList;
import java.util.List;

/**
 * The GameScreen class handles the main game screen, coordinating between the renderer, UI, and input handler.
 */
public class GameScreen implements Screen, GameInputHandler.UICallback {
    private TakGameMain game;
    private boolean useAI;
    private TakGame takGame;

    public int boardSize = 5;
    public PerspectiveCamera camera;
    public CameraInputController camController;
    private GameRenderer renderer;
    private UIManager uiManager;
    private GameInputHandler inputHandler;

    public Piece.PieceType selectedPieceType;
    private boolean isAIMoving = false;
    private TextButton rulesButton;

    /**
     * Constructor to initialize the GameScreen.
     *
     * @param game   The main game instance.
     * @param useAI  Flag indicating whether AI is enabled.
     */
    public GameScreen(TakGameMain game, boolean useAI) {
        this.game = game;
        this.useAI = useAI;
        create();
    }

    /**
     * Initializes the game components, camera, renderer, UI, and input handler.
     */
    public void create() {
        // If AI is enabled, one Human (Blue) and one AI (Green).
        // This ordering ensures the first player is BLUE (matching the TakGame's "first move = Blue places green's piece").
        Player player1;
        Player player2;

        if (useAI) {
            player1 = new HumanPlayer(Player.Color.BLUE, 21, 1, 1);
            player2 = new MinimaxAgent(Player.Color.GREEN, 21, 1, 1, 3);
        } else {
            // Two human players: Blue first, Green second
            player1 = new HumanPlayer(Player.Color.BLUE, 21, 1, 1);
            player2 = new HumanPlayer(Player.Color.GREEN, 21, 1, 1);
        }

        player1.setOpponent(player2);
        player2.setOpponent(player1);

        List<Player> players = new ArrayList<>();
        players.add(player1);
        players.add(player2);

        // Create the actual TakGame
        takGame = new TakGame(boardSize, players);

        // Set up the camera
        camera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(boardSize * 2f, boardSize * 2f, boardSize * 2f);
        camera.lookAt(boardSize / 2f, 0, boardSize / 2f);
        camera.near = 0.1f;
        camera.far = 100f;
        camera.update();

        // Initialize the camera controller
        camController = new CameraInputController(camera);
        camController.rotateButton = Input.Buttons.RIGHT;
        camController.translateButton = Input.Buttons.MIDDLE;
        camController.scrollFactor = -0.1f;

        // Initialize Renderer
        renderer = new GameRenderer(camera, boardSize, takGame);
        renderer.updatePieceInstances();

        // Initialize UI Manager
        uiManager = new UIManager(takGame, this);
        uiManager.getStage().setViewport(new ScreenViewport());

        // Initialize Input Handler
        inputHandler = new GameInputHandler(camera, takGame, renderer, uiManager, this);

        // Combine stage + game input + camera controls
        Gdx.input.setInputProcessor(new InputMultiplexer(uiManager.getStage(), inputHandler, camController));

        addRulesButton();
        uiManager.updateHotbarColors();
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
     * Displays the game rules in a dialog.
     */
    private void showRulesDialog() {
        Dialog dialog = new Dialog("Rules", uiManager.getSkin(), "dialog");
        dialog.text(getRulesText());
        dialog.button("OK");
        dialog.show(uiManager.getStage());
    }

    /**
     * Retrieves the game rules text.
     *
     * @return The game rules.
     */
    private String getRulesText() {
        return "1. Players take turns placing tiles on the board. No standing or cap tiles can be placed during the first two moves.\n"
            + "2. Players can stack flat or standing tiles on top of flat tiles. Cap tiles cannot be stacked on top of other tiles.\n"
            + "3. Cap tiles can flatten standing tiles, turning them into flat tiles.\n"
            + "4. Movement is orthogonal (along rows or columns). Only the top tile of a stack can be moved. Multiple tiles can be moved, but at least one must be placed on each square along the path.\n"
            + "5. Victory Conditions:\n"
            + "   - Road Victory: Create a continuous orthogonal line of flat/cap tiles connecting one side of the board to the opposite side.\n"
            + "   - Flat Victory: If the board is full and no road victory is achieved, the player with the most visible flat tiles wins.\n"
            + "6. Each turn, players must either place a tile or move a tile/stack. Only cap tiles can flatten standing tiles.\n"
            + "7. The game ends when one of the victory conditions is met.";
    }

    @Override
    public void render(float delta) {
        // Update camera controller
        camController.update();

        // Clear the screen
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        // Update piece instances in case AI made a move
        renderer.updatePieceInstances();
        renderer.render();

        // UI
        uiManager.getStage().act(delta);
        uiManager.getStage().draw();

        // Handle AI move if it's AI's turn
        if (useAI && takGame.getCurrentPlayer() instanceof MinimaxAgent && !takGame.isGameEnded()) {
            handleAIMove();
        }
    }

    private void handleAIMove() {
        if (isAIMoving) return; // Avoid multiple AI calls

        isAIMoving = true;
        // Temporarily disable input
        Gdx.input.setInputProcessor(null);

        // Execute AI move on a short delay so user sees changes
        Timer.schedule(new Timer.Task(){
            @Override
            public void run() {
                try {
                    MinimaxAgent ai = (MinimaxAgent) takGame.getCurrentPlayer();
                    ai.makeMove(takGame);

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
                    if (!(takGame.getCurrentPlayer() instanceof MinimaxAgent)) {
                        Gdx.input.setInputProcessor(new InputMultiplexer(uiManager.getStage(), inputHandler, camController));
                    }
                    isAIMoving = false;
                }
            }
        }, 0.5f);
    }

    @Override
    public void resize(int width, int height) {
        // Update camera viewport
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();

        // Update UI viewport
        uiManager.getStage().getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        renderer.dispose();
        uiManager.dispose();
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
     * Toggles the selection of a piece type. If already selected, deselect it.
     */
    public void toggleSelection(Piece.PieceType pieceType) {
        // During first two moves, only flat stones can be selected
        if (takGame.getMoveCount() < 2 && pieceType != Piece.PieceType.FLAT_STONE) {
            showErrorDialog("Only flat stones can be placed in the first two moves.");
            return;
        }

        if (selectedPieceType == pieceType) {
            selectedPieceType = null;
            renderer.removeHoverOutline();
            Gdx.app.log("GameScreen", "Deselected " + pieceType);
        } else {
            selectedPieceType = pieceType;
            renderer.removeHoverOutline();
            Gdx.app.log("GameScreen", "Selected " + pieceType);
        }
    }

    /**
     * Updates the UI elements after resetting the game.
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

    /**
     * UICallback methods
     */
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
                    takGame.resetGame(true);
                    updateAfterGameReset();
                } else if (object.equals("continue")) {
                    takGame.resetGame(false);
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
