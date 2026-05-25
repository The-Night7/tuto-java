package io.github.perspectogame;

import com.badlogic.gdx.Game;

public class Main extends Game {
    public static final String GAME_TITLE = "PerspectoGame: Point de Fuite";
    public static final String RELEASE_VERSION = "v1.0.0";

    @Override
    public void create() {
        // Au lancement, on affiche le menu principal
        this.setScreen(new MenuScreen(this));
    }
}
