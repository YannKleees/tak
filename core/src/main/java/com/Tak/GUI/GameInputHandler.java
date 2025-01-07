// File: core/src/main/java/com/Tak/GUI/GameInputHandler.java
package com.Tak.GUI;

import com.Tak.Logic.exceptions.GameOverException;
import com.Tak.Logic.exceptions.InvalidMoveException;
import com.Tak.Logic.models.Direction;
import com.Tak.Logic.models.Piece;
import com.Tak.Logic.models.Player;
import com.Tak.Logic.models.TakGame;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.Gdx;
import com.Tak.Logic.models.Board;
import java.util.List;

/**
 * The GameInputHandler class handles user input for placing and moving pieces on the board.
 */
public class GameInputHandler extends InputAdapter {
    private PerspectiveCamera camera;
    private TakGame takGame;
    private GameRenderer renderer;
    private UIManager uiManager;
    private UICallback uiCallback;

    private enum SelectionState {
        SELECTING_SOURCE,
        SELECTING_DIRECTION
    }

    private SelectionState selectionState = SelectionState.SELECTING_SOURCE;
    private int sourceX, sourceY;

    /**
     * Interface to handle UI callbacks.
     */
    public interface UICallback {
        void showErrorDialog(String message);
        void showGameOverDialog(Player winner);
        void addMoveToList(String moveDescription);
        void updatePlayerScores();
        void updateCurrentPlayerLabel();
        void updateHotbarColors();
        void showDropCountsPrompt(int sourceX, int sourceY, Direction direction, UIManager.DropCountsCallback callback);
        Piece.PieceType getSelectedPieceType();
        void deselectPiece();
        boolean isPieceSelected();
    }

    /**
     * Constructor to initialize the GameInputHandler.
     */
    public GameInputHandler(PerspectiveCamera camera, TakGame takGame, GameRenderer renderer, UIManager uiManager, UICallback uiCallback) {
        this.camera = camera;
        this.takGame = takGame;
        this.renderer = renderer;
        this.uiManager = uiManager;
        this.uiCallback = uiCallback;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (button != Input.Buttons.LEFT) return false;

        // Convert screen coordinates to world coordinates
        Ray pickRay = camera.getPickRay(screenX, screenY);
        Vector3 intersection = new Vector3();

        // Plane of the board
        Plane boardPlane = new Plane(new Vector3(0, 1, 0), new Vector3(0, 0, 0));

        if (Intersector.intersectRayPlane(pickRay, boardPlane, intersection)) {
            // Board's position
            Vector3 boardPos = new Vector3();
            renderer.getBoardInstance().transform.getTranslation(boardPos);

            float localX = intersection.x - (boardPos.x - (takGame.getBoardSize() * renderer.getTileSize()) / 2f);
            float localY = intersection.z - (boardPos.z - (takGame.getBoardSize() * renderer.getTileSize()) / 2f);

            int x = (int) Math.floor(localX);
            int y = (int) Math.floor(localY);

            Gdx.app.log("GameInputHandler", "Clicked position: (" + x + ", " + y + ")");

            if (x >= 0 && x < takGame.getBoardSize() && y >= 0 && y < takGame.getBoardSize()) {
                if (uiCallback.isPieceSelected()) {
                    // =======================
                    //   Placing a NEW piece
                    // =======================

                    // 1) Capture the current player/color BEFORE placePiece().
                    Player oldPlayer = takGame.getCurrentPlayer();
                    Piece.PieceType selectedPiece = uiCallback.getSelectedPieceType();

                    try {
                        takGame.placePiece(x, y, selectedPiece);

                        renderer.updatePieceInstances();

                        // 2) Build moveDescription using the oldPlayer
                        String moveDescription = "Player " + oldPlayer.getColor()
                            + " placed " + selectedPiece + " at (" + x + ", " + y + ")";
                        uiCallback.addMoveToList(moveDescription);

                        uiCallback.updatePlayerScores();
                        uiCallback.deselectPiece();
                        renderer.removeHoverOutline();

                        uiCallback.updateCurrentPlayerLabel();
                        uiCallback.updateHotbarColors();

                        if (takGame.isGameEnded()) {
                            uiCallback.showGameOverDialog(takGame.getWinner());
                        }

                    } catch (InvalidMoveException e) {
                        uiCallback.showErrorDialog("Invalid Move: " + e.getMessage());
                    } catch (GameOverException e) {
                        uiCallback.showGameOverDialog(takGame.getWinner());
                    }

                } else {
                    // =================================
                    //   Attempting to MOVE an existing stack
                    // =================================

                    if (selectionState == SelectionState.SELECTING_SOURCE) {
                        List<Piece> stack = takGame.getBoard().getBoardPosition(x, y);
                        if (!stack.isEmpty() && stack.get(stack.size() - 1).getOwner() == takGame.getCurrentPlayer()) {
                            sourceX = x;
                            sourceY = y;
                            selectionState = SelectionState.SELECTING_DIRECTION;
                            renderer.highlightSquare(x, y);
                        } else {
                            uiCallback.showErrorDialog("Select a stack with your piece on top.");
                        }
                    } else if (selectionState == SelectionState.SELECTING_DIRECTION) {
                        int dx = x - sourceX;
                        int dy = y - sourceY;
                        final Direction direction;

                        if (dx == 0 && dy > 0) {
                            direction = Direction.UP;
                        } else if (dx == 0 && dy < 0) {
                            direction = Direction.DOWN;
                        } else if (dy == 0 && dx > 0) {
                            direction = Direction.RIGHT;
                        } else if (dy == 0 && dx < 0) {
                            direction = Direction.LEFT;
                        } else {
                            uiCallback.showErrorDialog("Invalid direction. Moves must be in a straight line.");
                            return true;
                        }

                        List<Piece> stackPieces = takGame.getBoard().getBoardPosition(sourceX, sourceY);
                        if (stackPieces.size() == 1) {
                            // Move directly if only one piece

                            // Capture current player BEFORE moveStack().
                            Player oldPlayer = takGame.getCurrentPlayer();

                            try {
                                takGame.moveStack(sourceX, sourceY, direction, new int[]{1});
                                renderer.updatePieceInstances();

                                String moveDescription = "Player " + oldPlayer.getColor()
                                    + " moved 1 piece from (" + sourceX + ", " + sourceY + ") " + direction;
                                uiCallback.addMoveToList(moveDescription);

                                uiCallback.updateCurrentPlayerLabel();
                                uiCallback.updateHotbarColors();

                                if (takGame.isGameEnded()) {
                                    uiCallback.showGameOverDialog(takGame.getWinner());
                                }
                            } catch (InvalidMoveException e) {
                                uiCallback.showErrorDialog("Invalid Move: " + e.getMessage());
                            } catch (GameOverException e) {
                                uiCallback.showGameOverDialog(takGame.getWinner());
                            }

                            selectionState = SelectionState.SELECTING_SOURCE;
                            renderer.removeHighlight();

                        } else {
                            // Prompt user for drop counts
                            uiCallback.showDropCountsPrompt(sourceX, sourceY, direction, (dropCounts) -> {

                                // Capture current player in the callback
                                Player oldPlayer = takGame.getCurrentPlayer();

                                try {
                                    takGame.moveStack(sourceX, sourceY, direction, dropCounts);
                                    renderer.updatePieceInstances();

                                    String moveDescription = "Player " + oldPlayer.getColor()
                                        + " moved stack from (" + sourceX + ", " + sourceY + ") " + direction;
                                    uiCallback.addMoveToList(moveDescription);

                                    uiCallback.updateCurrentPlayerLabel();
                                    uiCallback.updateHotbarColors();

                                    if (takGame.isGameEnded()) {
                                        uiCallback.showGameOverDialog(takGame.getWinner());
                                    }
                                } catch (InvalidMoveException e) {
                                    uiCallback.showErrorDialog("Invalid Move: " + e.getMessage());
                                } catch (GameOverException e) {
                                    uiCallback.showGameOverDialog(takGame.getWinner());
                                } finally {
                                    selectionState = SelectionState.SELECTING_SOURCE;
                                    renderer.removeHighlight();
                                }
                            });
                        }
                    }
                }
            } else {
                Gdx.app.log("GameInputHandler", "Clicked out of bounds: (" + x + ", " + y + ")");
            }
        } else {
            Gdx.app.log("GameInputHandler", "No intersection with board plane.");
        }

        return true;
    }



    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        if (!uiCallback.isPieceSelected()) {
            renderer.removeHoverOutline();
            return false;
        }

        Ray pickRay = camera.getPickRay(screenX, screenY);
        Vector3 intersection = new Vector3();
        Plane boardPlane = new Plane(new Vector3(0, 1, 0), new Vector3(0, 0, 0));

        if (Intersector.intersectRayPlane(pickRay, boardPlane, intersection)) {
            Vector3 boardPos = new Vector3();
            renderer.getBoardInstance().transform.getTranslation(boardPos);

            float localX = intersection.x - (boardPos.x - (takGame.getBoardSize() * renderer.getTileSize()) / 2f);
            float localY = intersection.z - (boardPos.z - (takGame.getBoardSize() * renderer.getTileSize()) / 2f);

            int x = (int) Math.floor(localX);
            int y = (int) Math.floor(localY);

            if (x >= 0 && x < takGame.getBoardSize() && y >= 0 && y < takGame.getBoardSize()) {
                renderer.removeHoverOutline();
                renderer.createHoverOutline(x, y);
            } else {
                renderer.removeHoverOutline();
            }
        }

        return false;
    }
}
