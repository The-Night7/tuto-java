package io.github.perspectogame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class MenuScreen extends ScreenAdapter {
    private Main game;
    private SpriteBatch batch;
    private BitmapFont font;

    public MenuScreen(Main game) {
        this.game = game;
        this.batch = new SpriteBatch();
        this.font = new BitmapFont(); // Police par défaut de libGDX
        this.font.getData().setScale(2f);
    }

    @Override
    public void render(float delta) {
        // Fond bleu foncé pour le menu
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.3f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();
        font.draw(batch, "=== PERSPECTO GAME ===", Gdx.graphics.getWidth() / 2f - 150, Gdx.graphics.getHeight() - 100);
        font.draw(batch, "Appuyez sur [1] pour le Tutoriel", 100, Gdx.graphics.getHeight() - 200);
        font.draw(batch, "Appuyez sur [2] pour le Niveau 1", 100, Gdx.graphics.getHeight() - 250);
        batch.end();

        // Logique de sélection des niveaux
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_1)) {
            game.setScreen(new GameScreen(game, "tuto.txt"));
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_2)) {
            game.setScreen(new GameScreen(game, "niveau1.txt"));
        }
    }

    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
    }
}
