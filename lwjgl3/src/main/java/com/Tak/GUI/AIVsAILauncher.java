package com.Tak.GUI;

import com.Tak.Logic.models.TakGame;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

public class AIVsAILauncher {

    public static void main(String[] args) {
        // If your project relies on StartupHelper (like your Lwjgl3Launcher does), call it here:
        if (StartupHelper.startNewJvmIfRequired()) return;

        // Create the config
        Lwjgl3ApplicationConfiguration config = getAIVsAIConfiguration();

        // Create the Lwjgl3Application, passing in an *anonymous* TakGameMain
        // that overrides create() to load a 5x5 TakGame with 2 AI players.
        new Lwjgl3Application(new TakGameMain() {
            @Override
            public void create() {
                // 1) Call super.create() in case TakGameMain has any logging or initial setup
                super.create();

                // 2) Create a TakGame that has a 5x5 board and 2 AI players
                TakGame takGame = new TakGame(5, true, 2);

                // 3) Switch the screen to a GameScreen that uses the new takGame
                //    The assumption: you have a constructor like
                //      GameScreen(TakGameMain, TakGame)
                //    which doesn't create its own TakGame but uses ours.
                //    If your existing code only has GameScreen(TakGameMain, boolean),
                //    then adapt accordingly so it *doesn't* recreate a takGame.
                setScreen(new GameScreen(this, takGame));

                // Now both players are MinimaxAgent. They will take turns,
                // place pieces, and the match will unfold automatically.
            }
        }, config);
    }

    private static Lwjgl3ApplicationConfiguration getAIVsAIConfiguration() {
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("Tak: AI vs. AI (Minimax)");
        configuration.useVsync(true);
        // E.g., match your typical config logic
        configuration.setForegroundFPS(Lwjgl3ApplicationConfiguration.getDisplayMode().refreshRate + 1);
        configuration.setWindowedMode(800, 600);
        configuration.setWindowIcon("libgdx128.png", "libgdx64.png", "libgdx32.png", "libgdx16.png");
        return configuration;
    }
}
