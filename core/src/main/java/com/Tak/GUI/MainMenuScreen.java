// File: core/src/main/java/com/Tak/GUI/MainMenuScreen.java
package com.Tak.GUI;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

/**
 * The MainMenuScreen class represents the main menu of the game.
 * It allows the player to start a new game, view game rules, or exit the application.
 */
public class MainMenuScreen implements Screen {
    private TakGameMain game; // Reference to the main game class
    private Stage stage;
    private Skin skin;
    private TextButton startGameButton;
    private TextButton rulesButton;
    private TextButton exitButton;
    private Label titleLabel;

    /**
     * Constructor to initialize the MainMenuScreen.
     *
     * @param game The main game instance.
     */
    public MainMenuScreen(TakGameMain game) {
        this.game = game;
        create();
    }

    /**
     * Initializes the main menu screen, including UI elements.
     */
    public void create() {
        // Set up the stage and skin
        stage = new Stage(new ScreenViewport());
        skin = createBasicSkin();

        // Create the title label
        titleLabel = new Label("TAK", skin, "title");
        titleLabel.setAlignment(Align.center);

        // Create buttons
        startGameButton = new TextButton("Start Game", skin);
        rulesButton = new TextButton("Game Rules", skin);
        exitButton = new TextButton("Exit", skin);

        // Set up the table layout
        Table table = new Table();
        table.setFillParent(true);
        table.center();

        // Add the title label to the table
        table.add(titleLabel).padBottom(30);
        table.row();

        // Add buttons to the table
        table.add(startGameButton).width(200).height(60).pad(10);
        table.row();
        table.add(rulesButton).width(200).height(60).pad(10);
        table.row();
        table.add(exitButton).width(200).height(60).pad(10);

        // Add the table to the stage
        stage.addActor(table);

        // Set input processor
        Gdx.input.setInputProcessor(stage);

        // Add listeners to buttons
        addButtonListeners();
    }

    /**
     * Creates a basic skin programmatically for UI elements.
     * For scalability, consider using external skin files with texture atlases.
     *
     * @return The created skin.
     */
    private Skin createBasicSkin() {
        Skin skin = new Skin();

        // Create a default font
        BitmapFont font = new BitmapFont();
        skin.add("default-font", font);

        // Create a larger font for the title
        BitmapFont titleFont = new BitmapFont();
        titleFont.getData().setScale(3);
        skin.add("title-font", titleFont);

        // Create button textures
        Pixmap pixmapUp = new Pixmap(200, 60, Pixmap.Format.RGBA8888);
        pixmapUp.setColor(Color.GRAY);
        pixmapUp.fill();
        skin.add("button-up", new Texture(pixmapUp));

        Pixmap pixmapDown = new Pixmap(200, 60, Pixmap.Format.RGBA8888);
        pixmapDown.setColor(Color.DARK_GRAY);
        pixmapDown.fill();
        skin.add("button-down", new Texture(pixmapDown));

        pixmapUp.dispose();
        pixmapDown.dispose();

        // Create a TextButton style
        TextButton.TextButtonStyle textButtonStyle = new TextButton.TextButtonStyle();
        textButtonStyle.up = skin.newDrawable("button-up");
        textButtonStyle.down = skin.newDrawable("button-down", Color.DARK_GRAY);
        textButtonStyle.font = skin.getFont("default-font");
        skin.add("default", textButtonStyle);

        // Create a Label style
        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = skin.getFont("default-font");
        skin.add("default", labelStyle);

        // Create a Label style for the title
        Label.LabelStyle titleStyle = new Label.LabelStyle();
        titleStyle.font = skin.getFont("title-font");
        skin.add("title", titleStyle);

        // Window style (for Dialog)
        Window.WindowStyle windowStyle = new Window.WindowStyle();
        windowStyle.background = skin.newDrawable("button-up", Color.DARK_GRAY);
        windowStyle.titleFont = skin.getFont("default-font");
        skin.add("dialog", windowStyle);

        return skin;
    }

    /**
     * Adds listeners to the Start Game, Rules, and Exit buttons.
     */
    private void addButtonListeners() {
        // Listener for Start Game button
        startGameButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                showGameModeSelection();
            }
        });

        // Listener for Rules button
        rulesButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                showGameRules();
            }
        });

        // Listener for Exit button
        exitButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.exit();
            }
        });
    }

    /**
     * Displays a dialog for selecting the game mode (Human vs. Human or Human vs. AI).
     */
    private void showGameModeSelection() {
        Dialog dialog = new Dialog("Select Game Mode", skin, "dialog") {
            @Override
            protected void result(Object object) {
                if (object.equals("human")) {
                    game.setScreen(new GameScreen(game, false));
                } else if (object.equals("ai")) {
                    game.setScreen(new GameScreen(game, true));
                }
            }
        };
        dialog.text("Choose an option:");
        dialog.button("Play against Human", "human");
        dialog.button("Play against AI", "ai");
        dialog.show(stage);
    }

    /**
     * Displays a dialog with the game rules.
     */
    private void showGameRules() {
        Dialog dialog = new Dialog("Game Rules", skin, "dialog");
        dialog.text(getRulesText());
        dialog.button("OK");
        dialog.show(stage);
    }

    /**
     * Retrieves the game rules text.
     *
     * @return The game rules.
     */
    private String getRulesText() {
        return "1. Players take turns placing tiles on the board. No standing tiles or cap (hat) tiles can be placed during the first round.\n"
            + "2. Players can stack flat tiles or standing tiles on top of flat tiles. Cap tiles cannot be stacked on top of other tiles.\n"
            + "3. Cap tiles can flatten standing tiles, turning them into flat tiles.\n"
            + "4. Movement is orthogonal (along rows or columns). Only the top tile of a stack can be moved.\n"
            + "5. Victory Conditions:\n"
            + "   - Road Victory: Create a continuous orthogonal line of flat or cap tiles connecting one side of the board to the opposite side.\n"
            + "   - Flat Victory: If the board is full and no road victory is achieved, the player with the most visible flat tiles wins.\n"
            + "6. Each turn, players must either place a tile or move a tile/stack. Only cap tiles can flatten standing tiles.\n"
            + "7. The game ends when one of the victory conditions is met.";
    }

    @Override
    public void render(float delta) {
        // Clear the screen
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Update and draw the UI
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        // Update the stage viewport
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
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
}
