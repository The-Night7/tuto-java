package io.github.perspectogame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class MenuScreen extends ScreenAdapter {
    private static final String[][] NIVEAUX = {
        {"Tutoriel", "tuto.txt"},
        {"Niveau 1", "niveau1.txt"},
        {"Niveau 2", "niveau2.txt"},
        {"Niveau 3", "niveau3.txt"},
        {"Niveau 4", "niveau4.txt"},
        {"Niveau 5", "niveau5.txt"},
        {"Niveau 6", "niveau6.txt"},
        {"Niveau 7", "niveau7.txt"},
        {"Niveau 8", "niveau8.txt"},
        {"Niveau 9", "niveau9.txt"},
        {"Niveau 10", "niveau10.txt"}
    };

    private final Main game;
    private final SpriteBatch batch;
    private final BitmapFont font;
    private int selection = 0;

    public MenuScreen(Main game) {
        this.game = game;
        this.batch = new SpriteBatch();
        this.font = new BitmapFont();
        this.font.getData().setScale(1.35f);
    }

    @Override
    public void render(float delta) {
        gererEntrees();

        Gdx.gl.glClearColor(0.1f, 0.1f, 0.3f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        float largeur = Gdx.graphics.getWidth();
        float hauteur = Gdx.graphics.getHeight();
        float y = hauteur - 70f;

        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch, "=== PERSPECTO GAME ===", largeur / 2f - 170f, y);
        y -= 45f;
        font.draw(batch, "[HAUT/BAS] selectionner  [ENTREE] lancer", 70f, y);
        y -= 28f;
        font.draw(batch, "[T] tutoriel  [1..9] niveaux 1 a 9  [0] niveau 10", 70f, y);
        y -= 42f;

        for (int i = 0; i < NIVEAUX.length; i++) {
            font.setColor(i == selection ? Color.CYAN : Color.WHITE);
            String prefixe = i == selection ? "> " : "  ";
            font.draw(batch, prefixe + NIVEAUX[i][0], 90f, y);
            y -= 34f;
        }
        font.setColor(Color.WHITE);
        batch.end();
    }

    private void gererEntrees() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            selection = (selection + 1) % NIVEAUX.length;
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            selection = (selection - 1 + NIVEAUX.length) % NIVEAUX.length;
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            lancerSelection();
            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.T)) {
            selection = 0;
            lancerSelection();
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_1)) {
            selection = 1;
            lancerSelection();
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_2)) {
            selection = 2;
            lancerSelection();
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_3)) {
            selection = 3;
            lancerSelection();
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_4) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_4)) {
            selection = 4;
            lancerSelection();
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_5) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_5)) {
            selection = 5;
            lancerSelection();
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_6) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_6)) {
            selection = 6;
            lancerSelection();
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_7) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_7)) {
            selection = 7;
            lancerSelection();
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_8) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_8)) {
            selection = 8;
            lancerSelection();
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_9) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_9)) {
            selection = 9;
            lancerSelection();
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_0) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_0)) {
            selection = 10;
            lancerSelection();
        }
    }

    private void lancerSelection() {
        game.setScreen(new GameScreen(game, NIVEAUX[selection][1]));
    }

    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
    }
}
