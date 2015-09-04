package ca.josephroque.swip.screen;

import ca.josephroque.swip.SwipGame;
import com.badlogic.gdx.Screen;

/**
 * Provides default methods for creating a {@link com.badlogic.gdx.Screen}.
 */
public abstract class SwipScreen
        implements Screen {

    /** The current instance of the game. */
    private SwipGame mSwipGame;

    /** Indicates if the screen has been disposed. */
    private boolean mWasDisposed;

    /**
     * Stores a reference to the parameter provided.
     *
     * @param game instance of game
     */
    public SwipScreen(SwipGame game) {
        this.mSwipGame = game;
    }

    @Override
    public void resize(int width, int height) {
        // does nothing
    }

    @Override
    public void show() {
        // does nothing
    }

    @Override
    public void hide() {
        // does nothing
    }

    @Override
    public void pause() {
        // does nothing
    }

    @Override
    public void resume() {
        // does nothing
    }

    @Override
    public void dispose() {
        mWasDisposed = true;
    }

    /**
     * Updates the game logic.
     *
     * @param delta delta time
     */
    abstract void tick(float delta);

    /**
     * Draws the game to the screen.
     */
    abstract void draw();

    /**
     * Returns true if the screen was disposed.
     *
     * @return {@code mWasDisposed}
     */
    public boolean wasDisposed() {
        return mWasDisposed;
    }

    /**
     * Gets the current {@code SwipGame}.
     *
     * @return {@code mSwipGame}
     */
    SwipGame getSwipGame() {
        return mSwipGame;
    }
}
