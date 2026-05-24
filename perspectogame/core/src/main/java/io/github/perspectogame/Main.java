package io.github.perspectogame;

import com.badlogic.gdx.Game;

public class Main extends Game {
    @Override
    public void create() {
        // Au lancement, on affiche le menu principal
        this.setScreen(new MenuScreen(this));
    }
}
