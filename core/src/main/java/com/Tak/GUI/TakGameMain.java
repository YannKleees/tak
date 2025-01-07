// File: gui/src/main/java/com/Tak/GUI/TakGameMain.java
package com.Tak.GUI;

import com.Tak.Logic.utils.Logger;
import com.badlogic.gdx.Game;

/**
 * The TakGameMain class serves as the entry point of the application.
 * It initializes the logger and sets the main menu screen.
 */
public class TakGameMain extends Game {
    @Override
    public void create() {
        Logger.initialize();
        setScreen(new MainMenuScreen(this));
    }

    @Override
    public void dispose() {
        super.dispose();
        getScreen().dispose();
    }
}
