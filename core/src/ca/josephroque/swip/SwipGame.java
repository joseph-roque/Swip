package ca.josephroque.swip;

import ca.josephroque.swip.screen.GameScreen;
import com.badlogic.gdx.Application;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;

/**
 * Initial starting point of the game. Delegates functionality to relevant {@link ca.josephroque.swip.screen.GameScreen}
 * instances.
 */
public class SwipGame
        extends Game {

    @Override
    public void create() {
        Gdx.app.setLogLevel(Application.LOG_DEBUG);

        // Opens the main menu when the application begins
        setScreen(new GameScreen());
    }
}
