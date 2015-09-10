package ca.josephroque.swip.manager;

import ca.josephroque.swip.entity.BasicBall;
import ca.josephroque.swip.entity.Button;
import ca.josephroque.swip.entity.GameBall;
import ca.josephroque.swip.entity.Wall;
import ca.josephroque.swip.input.GameInputProcessor;
import ca.josephroque.swip.screen.GameScreen;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.TimeUtils;

import java.util.Random;

/**
 * Manages game objects and rendering them to the screen.
 */
public class GameManager {

    /** Identifies output from this class in the logcat. */
    @SuppressWarnings("unused")
    private static final String TAG = "GameManager";

    /** Number of milliseconds that turns initially last. */
    private static final int INITIAL_TURN_LENGTH = 10000; // TODO: change to actual start value, 1200
    /** Number of milliseconds to subtract from the length of a turn at a time. */
    private static final int TURN_LENGTH_DECREMENT = 50;
    /** Number of turns that must pass before the turn length is decremented. */
    private static final int TURNS_BEFORE_DECREMENT = 10;
    /** Shortest number of milliseconds that a turn can last. */
    private static final int MINIMUM_TURN_LENGTH = 300;
    /** Number of milliseconds until a game starts. */
    private static final int TIME_UNTIL_GAME_STARTS = 1000;

    /** Size of the pause button relative to the screen. */
    private static final float PAUSE_BUTTON_SCALE = 0.15f;

    /** Width of the screen. */
    private int mScreenWidth;
    /** Height of the screen. */
    private int mScreenHeight;

    /** Generates random numbers for the game. */
    private final Random mRandomNumberGenerator = new Random();

    /** Manages textures for game objects. */
    private AssetManager mAssetManager;
    /** Instance of callback interface. */
    private GameCallback mGameCallback;

    /** Time at which the game began, in milliseconds. */
    private long mGameStartTime;

    /** The ball being used by the game. */
    private GameBall mCurrentGameBall;
    /** Button to pause the game. */
    private Button mPauseButton;
    /** The four walls in the game. */
    private final Wall[] mWalls;
    /** Colors of the four walls. */
    private final AssetManager.GameColor[] mWallColors;

    /** Time that the current turn started at. */
    private long mStartOfTurn;
    /** Length of a single turn. */
    private int mTurnLength;
    /** Number of milliseconds that have passed since this turn began. */
    private int mCurrentTurnDuration;
    /** Total number of turns that have passed since the game began (i.e. the player's score). */
    private int mTotalTurns;

    /**
     * Sets up a new game manager.
     *
     * @param callback instance of callback interface
     * @param assetManager textures for game objects
     * @param screenWidth width of the screen
     * @param screenHeight height of the screen
     */
    public GameManager(GameCallback callback, AssetManager assetManager, int screenWidth, int screenHeight) {
        mScreenWidth = screenWidth;
        mScreenHeight = screenHeight;
        mAssetManager = assetManager;
        mGameCallback = callback;

        Wall.initialize(screenWidth, screenHeight);
        mWallColors = new AssetManager.GameColor[Wall.NUMBER_OF_WALLS];
        Wall.getRandomWallColors(mRandomNumberGenerator, mWallColors, false);
        mWalls = new Wall[Wall.NUMBER_OF_WALLS];
        for (int i = 0; i < mWalls.length; i++) {
            mWalls[i] = new Wall(i, mWallColors[i], mScreenWidth, mScreenHeight);
        }

        final float pauseButtonSize = Math.min(mScreenWidth, mScreenHeight) * PAUSE_BUTTON_SCALE;
        mPauseButton = new Button(assetManager.getSystemIconTexture(AssetManager.SystemIcon.Pause),
                mScreenWidth - pauseButtonSize,
                mScreenHeight - pauseButtonSize,
                pauseButtonSize,
                pauseButtonSize);
    }

    /**
     * Updates the logic of the game based on the current state.
     *
     * @param gameState state of the game
     * @param gameInput player's input events
     * @param delta number of seconds the last rendering took
     */
    public void tick(GameScreen.GameState gameState, GameInputProcessor gameInput, float delta) {
        switch (gameState) {
            case GameStarting:
                tickGameStarting(gameInput, delta);
                break;
            case GamePlaying:
                tickGamePlaying(gameInput, delta);
                break;
            case GamePaused:
                tickGamePaused(gameInput, delta);
                break;
            default:
                throw new IllegalArgumentException("invalid game state.");
        }
    }

    /**
     * Updates a game which is starting.
     *
     * @param gameInput player's input events
     * @param delta number of seconds the last rendering took
     */
    private void tickGameStarting(GameInputProcessor gameInput, float delta) {
        // Counts down timer to start of game
        if (TimeUtils.timeSinceMillis(mGameStartTime) >= TIME_UNTIL_GAME_STARTS && mGameCallback != null) {
            startGame();
            if (mGameCallback != null)
                mGameCallback.startGame();
        } else if (mPauseButton.wasClicked(gameInput)) {
            if (mGameCallback != null)
                mGameCallback.pauseGame();
        }
    }

    /**
     * Updates a game which is being played.
     *
     * @param gameInput player's input events
     * @param delta number of seconds the last rendering took
     */
    private void tickGamePlaying(GameInputProcessor gameInput, float delta) {
        if (mStartOfTurn == 0)
            mStartOfTurn = TimeUtils.millis();
        mCurrentTurnDuration = (int) TimeUtils.timeSinceMillis(mStartOfTurn);

        if (mCurrentTurnDuration >= mTurnLength) {
            endGame();
        } else {
            mCurrentGameBall.drag(gameInput);
            mCurrentGameBall.tryToReleaseBall(gameInput);
            mCurrentGameBall.tick(delta, mWalls);

            if (mCurrentGameBall.hasPassedThroughWall())
                turnSucceeded();
            else if (mCurrentGameBall.hasHitInvalidWall())
                endGame();
        }

        if (mPauseButton.wasClicked(gameInput) && mGameCallback != null)
            mGameCallback.pauseGame();
    }

    /**
     * Updates a game which is paused.
     *
     * @param gameInput player's input events
     * @param delta number of seconds the last rendering took
     */
    private void tickGamePaused(GameInputProcessor gameInput, float delta) {

    }

    /**
     * Draws the game to the screen.
     *
     * @param gameState the current state of the application
     * @param spriteBatch graphics context to draw to
     */
    public void draw(GameScreen.GameState gameState, SpriteBatch spriteBatch) {
        if (mCurrentGameBall != null)
            mCurrentGameBall.draw(spriteBatch, mAssetManager, mTurnLength, mCurrentTurnDuration);
        for (Wall wall : mWalls)
            wall.draw(spriteBatch, mAssetManager);

        if (gameState != GameScreen.GameState.GamePaused)
            mPauseButton.draw(spriteBatch);
    }

    /**
     * Sets up a new game.
     */
    public void prepareNewGame() {
        mStartOfTurn = 0;
        mTurnLength = INITIAL_TURN_LENGTH;
        mCurrentTurnDuration = 0;
        mTotalTurns = 0;
        mGameStartTime = TimeUtils.millis();

        mCurrentGameBall = null;
    }

    /**
     * Starts the game.
     */
    public void startGame() {
        // Setting initial properties of entities
        BasicBall.initialize(mScreenWidth, mScreenHeight);
        Wall.initialize(mScreenWidth, mScreenHeight);

        // Creating the four walls
        Wall.getRandomWallColors(mRandomNumberGenerator, mWallColors, false);
        for (int i = 0; i < mWalls.length; i++) {
            mWalls[i] = new Wall(i, mWallColors[i], mScreenWidth, mScreenHeight);
        }

        // Creating the ball
        final int randomWall = mRandomNumberGenerator.nextInt(Wall.NUMBER_OF_WALLS);
        final boolean[] passableWalls = new boolean[Wall.NUMBER_OF_WALLS];
        passableWalls[randomWall] = true;
        mCurrentGameBall = new GameBall(mWallColors[randomWall], passableWalls, mScreenWidth / 2, mScreenHeight / 2);
    }

    /**
     * Ends the current game - it has been lost.
     */
    public void endGame() {
        if (mGameCallback != null)
            mGameCallback.endGame();
    }

    /**
     * Increases the player's score and starts a new turn.
     */
    private void turnSucceeded() {
        mTotalTurns++;
        mStartOfTurn = TimeUtils.millis();

        if (mTotalTurns % Wall.TURNS_BEFORE_NEW_COLOR == 0)
            Wall.addWallColorToActive();

        // Generates new colors for the wall
        int wallPairFirstIndex = Wall.getRandomWallColors(mRandomNumberGenerator,
                mWallColors,
                mTotalTurns > Wall.TURNS_BEFORE_SAME_WALL_COLORS);
        for (int i = 0; i < mWalls.length; i++)
            mWalls[i].updateWallColor(mWallColors[i]);

        // Generating new ball at center of screen
        final int randomWall;
        final boolean[] passableWalls = new boolean[Wall.NUMBER_OF_WALLS];
        if (wallPairFirstIndex == -1) {
            randomWall = mRandomNumberGenerator.nextInt(Wall.NUMBER_OF_WALLS);
            passableWalls[randomWall] = true;
        } else {
            randomWall = wallPairFirstIndex;
            passableWalls[randomWall] = true;
            for (int i = randomWall + 1; i < Wall.NUMBER_OF_WALLS; i++)
                passableWalls[i] = mWallColors[randomWall].equals(mWallColors[i]);
        }

        mCurrentGameBall = new GameBall(mWallColors[randomWall], passableWalls, mScreenWidth / 2, mScreenHeight / 2);

        if (mTotalTurns % TURNS_BEFORE_DECREMENT == 0)
            mTurnLength = Math.max(MINIMUM_TURN_LENGTH, mTurnLength - TURN_LENGTH_DECREMENT);
    }

    /**
     * Adjusts the size of the game objects to fit the new screen dimensions.
     *
     * @param width new screen width
     * @param height new screen height
     */
    public void resize(int width, int height) {
        mScreenWidth = width;
        mScreenHeight = height;

        // Resizing entities on screen
        if (mWalls != null) {
            for (Wall wall : mWalls)
                wall.resize(width, height);
        }

        if (mCurrentGameBall != null)
            mCurrentGameBall.resize(width, height);
    }

    /**
     * Frees references to objects.
     */
    public void dispose() {
        mAssetManager = null;
        mGameCallback = null;
    }

    /**
     * Provides callback methods for game interactions.
     */
    public interface GameCallback {
        /**
         * Should start a new game.
         */
        void startGame();

        /**
         * Should pause the current game and display a "pause" menu.
         */
        void pauseGame();

        /**
         * Should end the current game - the player has lost.
         */
        void endGame();
    }
}
