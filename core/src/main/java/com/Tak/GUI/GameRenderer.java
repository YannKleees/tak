// File: core/src/main/java/com/Tak/GUI/GameRenderer.java
package com.Tak.GUI;

import com.Tak.Logic.models.Board;
import com.Tak.Logic.models.Piece;
import com.Tak.Logic.models.Player;
import com.Tak.Logic.models.TakGame;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.*;
import com.badlogic.gdx.graphics.g3d.environment.*;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;

import java.util.List;

/**
 * The GameRenderer class is responsible for rendering the 3D board and pieces.
 */
public class GameRenderer {
    private PerspectiveCamera camera;
    private ModelBatch modelBatch;
    private Environment environment;

    public ModelInstance boardInstance;
    private Model boardModel;

    private Model flatStoneModel, standingStoneModel, capstoneModel;
    private Array<ModelInstance> pieceInstances;

    private int boardSize;
    private TakGame takGame;

    public ModelInstance hoverOutlineInstance;
    public ModelInstance selectionHighlightInstance;

    // Define tile size for consistent scaling (assuming 1 unit per tile)
    final float TILE_SIZE = 1.0f; // Adjust based on your game's scaling

    /**
     * Constructor to initialize the GameRenderer.
     *
     * @param camera    The PerspectiveCamera used for rendering.
     * @param boardSize The size of the game board.
     * @param takGame   The current TakGame instance.
     */
    public GameRenderer(PerspectiveCamera camera, int boardSize, TakGame takGame) {
        this.camera = camera;
        this.boardSize = boardSize;
        this.takGame = takGame;
        initializeRendering();
    }

    /**
     * Initializes rendering components, including models and environment.
     */
    private void initializeRendering() {
        modelBatch = new ModelBatch();
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.8f, 0.8f, 0.8f, 1f));
        environment.add(new DirectionalLight().set(1f, 1f, 1f, -1f, -0.8f, -0.2f));

        ModelBuilder modelBuilder = new ModelBuilder();

        // Load the board texture with grid
        Texture boardTexture;
        try {
            boardTexture = new Texture(Gdx.files.internal("board_texture.png")); // Ensure board_texture.png is in assets
            boardTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        } catch (Exception e) {
            Gdx.app.error("GameRenderer", "Failed to load board_texture.png", e);
            throw new GdxRuntimeException("Missing asset: board_texture.png");
        }
        Material boardMaterial = new Material(TextureAttribute.createDiffuse(boardTexture));

        // Create the game board model with texture
        boardModel = modelBuilder.createBox(boardSize * TILE_SIZE, 0.2f, boardSize * TILE_SIZE,
            boardMaterial,
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates);
        boardInstance = new ModelInstance(boardModel);
        // Position the board at the center, slightly below y=0 to serve as the base
        boardInstance.transform.setToTranslation((boardSize * TILE_SIZE) / 2f, -0.1f, (boardSize * TILE_SIZE) / 2f);

        // Flat Stone: Cylinder with increased radius and same height
        flatStoneModel = modelBuilder.createBox(0.5f, 0.1f, 0.5f,
            new Material(ColorAttribute.createDiffuse(Color.GRAY)),
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);

        // Standing Stone: Box with reduced width and depth, same height
        standingStoneModel = modelBuilder.createBox(0.5f, 0.5f, 0.1f,
            new Material(ColorAttribute.createDiffuse(Color.DARK_GRAY)),
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);

        // Capstone: Cone with increased base diameter and increased height
        capstoneModel = modelBuilder.createCylinder(
            0.5f, // radius (increased for a larger base)
            0.3f, // height (taller for a more imposing structure)
            0.5f, // depth (same as radius for a symmetrical cylinder)
            32,   // number of divisions around the circumference
            new Material(ColorAttribute.createDiffuse(Color.DARK_GRAY)), // darker color for realism
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal
        );

        pieceInstances = new Array<>();

        // Initialize hover and selection outlines as null
        hoverOutlineInstance = null;
        selectionHighlightInstance = null;
    }

    /**
     * Renders the board and pieces.
     */
    public void render() {
        modelBatch.begin(camera);
        modelBatch.render(boardInstance, environment);
        for (ModelInstance instance : pieceInstances) {
            modelBatch.render(instance, environment);
        }
        modelBatch.end();
    }

    /**
     * Updates the rendering instances of all pieces on the board.
     * Ensures that pieces are appropriately sized and positioned.
     */
    public void updatePieceInstances() {
        pieceInstances.clear();
        Board board = takGame.getBoard();
        for (int x = 0; x < boardSize; x++) {
            for (int y = 0; y < boardSize; y++) {
                List<Piece> stack = board.getBoardPosition(x, y);
                if (!stack.isEmpty()) {
                    float heightOffset = 0f;
                    for (int i = 0; i < stack.size(); i++) {
                        Piece piece = stack.get(i);
                        Model model = getModelForPiece(piece);
                        ModelInstance pieceInstance = new ModelInstance(model);

                        // Calculate position based on tile size and height offset
                        float posX = x * TILE_SIZE + TILE_SIZE / 2f;
                        float posZ = y * TILE_SIZE + TILE_SIZE / 2f;

                        // Determine the height at which to place the piece
                        // Start just above the board's top surface
                        float boardTopY = 0.1f; // Board height is 0.2f, centered at -0.1f, so top is at y=0.1f
                        float pieceHeight = getHeightForPiece(piece);
                        float pieceBaseY = boardTopY + heightOffset + pieceHeight / 2f;

                        pieceInstance.transform.setToTranslation(posX, pieceBaseY, posZ);

                        // Apply color based on the owner
                        Color pieceColor = piece.getOwner().getColor() == Player.Color.GREEN ? Color.GREEN : Color.BLUE;
                        ((ColorAttribute) pieceInstance.materials.get(0).get(ColorAttribute.Diffuse)).color.set(pieceColor);

                        pieceInstances.add(pieceInstance);

                        // **Adjust Height Offset Based on Piece Type**
                        heightOffset += pieceHeight;
                    }
                }
            }
        }

        // Re-add hover outline if it exists
        if (hoverOutlineInstance != null) {
            pieceInstances.add(hoverOutlineInstance);
        }

        // Re-add selection highlight if it exists
        if (selectionHighlightInstance != null) {
            pieceInstances.add(selectionHighlightInstance);
        }
    }
    public ModelInstance getBoardInstance() {
        return boardInstance;
    }
    /**
     * Returns the appropriate model for a given piece type.
     *
     * @param piece The piece whose model is needed.
     * @return The corresponding model.
     */
    private Model getModelForPiece(Piece piece) {
        switch (piece.getPieceType()) {
            case FLAT_STONE:
                return flatStoneModel;
            case STANDING_STONE:
                return standingStoneModel;
            case CAPSTONE:
                return capstoneModel;
            default:
                return flatStoneModel;
        }
    }
    public float getTileSize() {
        return TILE_SIZE;
    }

    /**
     * Returns the height for a given piece type.
     *
     * @param piece The piece whose height is needed.
     * @return The height of the piece.
     */
    private float getHeightForPiece(Piece piece) {
        switch (piece.getPieceType()) {
            case FLAT_STONE:
                return 0.1f; // Height matches the model's height
            case STANDING_STONE:
                return 0.3f; // Height matches the model's height
            case CAPSTONE:
                return 0.4f; // Height matches the model's height
            default:
                return 0.1f;
        }
    }

    /**
     * Creates a hover outline at the specified board position.
     *
     * @param x The x-coordinate.
     * @param y The y-coordinate.
     */
    public void createHoverOutline(int x, int y) {
        removeHoverOutline();
        ModelBuilder modelBuilder = new ModelBuilder();
        Material outlineMaterial = new Material(ColorAttribute.createDiffuse(new Color(0, 0, 1, 0.3f)));
        outlineMaterial.set(new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA));
        Model outlineModel = modelBuilder.createBox(TILE_SIZE, 0.05f, TILE_SIZE,
            outlineMaterial,
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
        hoverOutlineInstance = new ModelInstance(outlineModel);

        // Position the hover outline slightly above the board to prevent z-fighting
        float posX = x * TILE_SIZE + TILE_SIZE / 2f;
        float posZ = y * TILE_SIZE + TILE_SIZE / 2f;
        float posY = 0.15f; // Slightly above the board's top

        hoverOutlineInstance.transform.setToTranslation(posX, posY, posZ);
        pieceInstances.add(hoverOutlineInstance);
    }

    /**
     * Removes the hover outline.
     */
    public void removeHoverOutline() {
        if (hoverOutlineInstance != null) {
            pieceInstances.removeValue(hoverOutlineInstance, true);
            if (hoverOutlineInstance.model != null) {
                hoverOutlineInstance.model.dispose();
            }
            hoverOutlineInstance = null;
        }
    }

    /**
     * Highlights the selected square.
     *
     * @param x The x-coordinate.
     * @param y The y-coordinate.
     */
    public void highlightSquare(int x, int y) {
        removeHighlight();
        ModelBuilder modelBuilder = new ModelBuilder();
        Material highlightMaterial = new Material(ColorAttribute.createDiffuse(new Color(1, 1, 0, 0.3f)));
        highlightMaterial.set(new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA));
        Model highlightModel = modelBuilder.createBox(TILE_SIZE, 0.05f, TILE_SIZE,
            highlightMaterial,
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
        selectionHighlightInstance = new ModelInstance(highlightModel);

        // Position the highlight slightly above the board to prevent z-fighting
        float posX = x * TILE_SIZE + TILE_SIZE / 2f;
        float posZ = y * TILE_SIZE + TILE_SIZE / 2f;
        float posY = 0.15f; // Slightly above the board's top

        selectionHighlightInstance.transform.setToTranslation(posX, posY, posZ);
        pieceInstances.add(selectionHighlightInstance);
    }

    /**
     * Removes the selection highlight.
     */
    public void removeHighlight() {
        if (selectionHighlightInstance != null) {
            pieceInstances.removeValue(selectionHighlightInstance, true);
            if (selectionHighlightInstance.model != null) {
                selectionHighlightInstance.model.dispose();
            }
            selectionHighlightInstance = null;
        }
    }

    /**
     * Disposes of rendering resources.
     */
    public void dispose() {
        modelBatch.dispose();
        boardModel.dispose();
        flatStoneModel.dispose();
        standingStoneModel.dispose();
        capstoneModel.dispose();

        // Dispose hover outline model
        if (hoverOutlineInstance != null && hoverOutlineInstance.model != null) {
            hoverOutlineInstance.model.dispose();
        }

        // Dispose selection highlight model
        if (selectionHighlightInstance != null && selectionHighlightInstance.model != null) {
            selectionHighlightInstance.model.dispose();
        }
    }
}
