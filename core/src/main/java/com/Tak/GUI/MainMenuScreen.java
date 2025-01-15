// File: core/src/main/java/com/Tak/GUI/MainMenuScreen.java
package com.Tak.GUI;

import com.Tak.Logic.models.GameMode;
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

    public MainMenuScreen(TakGameMain game) {
        this.game = game;
        create();
    }

    public void create() {
        stage = new Stage(new ScreenViewport());
        skin = createBasicSkin();

        // Title label
        titleLabel = new Label("TAK", skin, "title");
        titleLabel.setAlignment(Align.center);

        // Buttons
        startGameButton = new TextButton("Start Game", skin);
        rulesButton = new TextButton("Game Rules", skin);
        exitButton = new TextButton("Exit", skin);

        // Layout table
        Table table = new Table();
        table.setFillParent(true);
        table.center();

        table.add(titleLabel).padBottom(30);
        table.row();

        table.add(startGameButton).width(200).height(60).pad(10);
        table.row();
        table.add(rulesButton).width(200).height(60).pad(10);
        table.row();
        table.add(exitButton).width(200).height(60).pad(10);

        stage.addActor(table);

        Gdx.input.setInputProcessor(stage);

        addButtonListeners();
    }

    private Skin createBasicSkin() {
        Skin skin = new Skin();

        // Default font
        BitmapFont font = new BitmapFont();
        skin.add("default-font", font);

        // Larger title font
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

        // TextButton style
        TextButton.TextButtonStyle textButtonStyle = new TextButton.TextButtonStyle();
        textButtonStyle.up = skin.newDrawable("button-up");
        textButtonStyle.down = skin.newDrawable("button-down", Color.DARK_GRAY);
        textButtonStyle.font = skin.getFont("default-font");
        skin.add("default", textButtonStyle);

        // Label style
        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = skin.getFont("default-font");
        skin.add("default", labelStyle);

        // Title label style
        Label.LabelStyle titleStyle = new Label.LabelStyle();
        titleStyle.font = skin.getFont("title-font");
        skin.add("title", titleStyle);

        // Dialog style
        Window.WindowStyle windowStyle = new Window.WindowStyle();
        windowStyle.background = skin.newDrawable("button-up", Color.DARK_GRAY);
        windowStyle.titleFont = skin.getFont("default-font");
        skin.add("dialog", windowStyle);

        return skin;
    }

    private void addButtonListeners() {
        // Start Game -> show a dialog to pick the mode
        startGameButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                showGameModeSelection();
            }
        });

        // Game Rules
        rulesButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                showGameRules();
            }
        });

        // Exit
        exitButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.exit();
            }
        });
    }

    /**
     * Displays a dialog for selecting the game mode (Human vs. Human, Human vs. AI, or AI vs. AI).
     */
    private void showGameModeSelection() {
        Dialog dialog = new Dialog("Select Game Mode", skin, "dialog") {
            @Override
            protected void result(Object object) {
                if (object.equals("human")) {
                    // Human vs Human
                    game.setScreen(new GameScreen(game, GameMode.HUMAN_VS_HUMAN));
                } else if (object.equals("ai")) {
                    // Human vs AI
                    game.setScreen(new GameScreen(game, GameMode.HUMAN_VS_AI));
                } else if (object.equals("aiVsAi")) {
                    // AI vs AI
                    game.setScreen(new GameScreen(game, GameMode.AI_VS_AI));
                }
            }
        };
        dialog.text("Choose an option:");
        dialog.button("Human vs Human", "human");
        dialog.button("Human vs AI", "ai");
        dialog.button("AI vs AI", "aiVsAi");
        dialog.show(stage);
    }

    private void showGameRules() {
        Dialog dialog = new Dialog("Game Rules", skin, "dialog");
        dialog.text(getRulesText());
        dialog.button("OK");
        dialog.show(stage);
    }

    private String getRulesText() {
        return "1. Players take turns placing tiles on the board. "
            + "No standing tiles or cap tiles in the first round.\n"
            + "2. Movement is orthogonal, only top tile can be moved.\n"
            + "3. Victory: either Road (connect sides) or most flats if board is full.\n"
            + "4. Etc.\n";
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
    }

    @Override
    public void show() {}
    @Override
    public void hide() {}
    @Override
    public void pause() {}
    @Override
    public void resume() {}
}
