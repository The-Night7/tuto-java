package io.github.perspectogame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class MenuScreen extends ScreenAdapter {
    static final class NiveauInfo {
        final String nom;
        final String fichier;

        NiveauInfo(String nom, String fichier) {
            this.nom = nom;
            this.fichier = fichier;
        }
    }

    static final NiveauInfo[] NIVEAUX = {
        new NiveauInfo("Tutoriel", "tuto.txt"),
        new NiveauInfo("Niveau 1", "niveau1.txt"),
        new NiveauInfo("Niveau 2", "niveau2.txt"),
        new NiveauInfo("Niveau 3", "niveau3.txt"),
        new NiveauInfo("Niveau 4", "niveau4.txt"),
        new NiveauInfo("Niveau 5", "niveau5.txt"),
        new NiveauInfo("Niveau 6", "niveau6.txt"),
        new NiveauInfo("Niveau 7", "niveau7.txt"),
        new NiveauInfo("Niveau 8", "niveau8.txt"),
        new NiveauInfo("Niveau 9", "niveau9.txt"),
        new NiveauInfo("Niveau 10", "niveau10.txt")
    };

    private final Main game;
    private final Stage stage;
    private final Skin skin;
    private final Preferences progression;
    private final TextButton[] boutonsNiveaux = new TextButton[NIVEAUX.length];
    private Label progressionLabel;
    private int selection = 0;

    public MenuScreen(Main game) {
        this.game = game;
        this.stage = new Stage(new ScreenViewport());
        this.skin = UiFactory.createSkin();
        this.progression = Gdx.app.getPreferences("perspectogame_progress");
        construireInterface();
        mettreAJourSelection();
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    private void construireInterface() {
        Table racine = new Table();
        racine.setFillParent(true);
        racine.setBackground(UiFactory.drawable(skin, "panel"));
        racine.pad(18f);
        stage.addActor(racine);

        Label titre = new Label(Main.GAME_TITLE, skin, "title");
        titre.setAlignment(Align.center);
        Label version = new Label(Main.RELEASE_VERSION + " / Puzzle de perspective", skin, "small");
        version.setAlignment(Align.center);

        Label aide = new Label("Fleches ou souris : choisir    Entree/Espace : lancer    T, 1..9, 0 : raccourcis", skin, "small");
        aide.setAlignment(Align.center);
        aide.setWrap(true);

        racine.add(titre).growX().padBottom(6f).row();
        racine.add(version).growX().padBottom(12f).row();
        racine.add(aide).growX().padBottom(14f).row();

        Table contenu = new Table();
        contenu.setBackground(UiFactory.drawable(skin, "panel-light"));
        contenu.pad(14f);

        Table liste = new Table();
        liste.defaults().growX().height(38f).spaceBottom(6f);
        for (int i = 0; i < NIVEAUX.length; i++) {
            final int index = i;
            TextButton bouton = new TextButton(libelleNiveau(index), skin);
            bouton.getLabel().setAlignment(Align.left);
            bouton.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    selection = index;
                    lancerSelection();
                }

                @Override
                public void enter(InputEvent event, float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor fromActor) {
                    selection = index;
                    mettreAJourSelection();
                }
            });
            boutonsNiveaux[i] = bouton;
            liste.add(bouton).growX().row();
        }

        ScrollPane defilement = new ScrollPane(liste, skin);
        defilement.setFadeScrollBars(false);
        contenu.add(defilement).grow().row();

        Table bas = new Table();
        bas.defaults().space(8f);
        bas.padTop(12f);

        Label objectif = new Label("Objectif : aligne les plateformes, lance la balle, atteins la quille.", skin, "small");
        objectif.setWrap(true);
        objectif.setAlignment(Align.left);

        progressionLabel = new Label(texteProgression(), skin, "success");
        progressionLabel.setAlignment(Align.right);

        TextButton lancer = new TextButton("Lancer", skin, "accent");
        lancer.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                lancerSelection();
            }
        });

        bas.add(objectif).growX().left();
        bas.add(progressionLabel).width(190f).right();
        bas.add(lancer).width(130f).height(42f).right();
        contenu.add(bas).growX().row();

        racine.add(contenu).grow().row();
    }

    private String libelleNiveau(int index) {
        String statut = progression.getBoolean(cleProgression(index), false) ? "OK" : "--";
        String raccourci = index == 0 ? "T" : index == 10 ? "0" : String.valueOf(index);
        return statut + "     " + raccourci + "     " + NIVEAUX[index].nom;
    }

    private String texteProgression() {
        return compterNiveauxTermines() + " / " + NIVEAUX.length + " termines";
    }

    private int compterNiveauxTermines() {
        int total = 0;
        for (int i = 0; i < NIVEAUX.length; i++) {
            if (progression.getBoolean(cleProgression(i), false)) {
                total++;
            }
        }
        return total;
    }

    static String cleProgression(int index) {
        return "niveau_" + index + "_termine";
    }

    @Override
    public void render(float delta) {
        gererEntrees();

        Gdx.gl.glClearColor(UiFactory.BACKGROUND.r, UiFactory.BACKGROUND.g, UiFactory.BACKGROUND.b, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(delta);
        stage.draw();
    }

    private void gererEntrees() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            selection = (selection + 1) % NIVEAUX.length;
            mettreAJourSelection();
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            selection = (selection - 1 + NIVEAUX.length) % NIVEAUX.length;
            mettreAJourSelection();
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            lancerSelection();
            return;
        }

        int raccourci = raccourciNiveauPresse();
        if (raccourci >= 0) {
            selection = raccourci;
            lancerSelection();
        }
    }

    private int raccourciNiveauPresse() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.T)) {
            return 0;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_0) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_0)) {
            return 10;
        }
        for (int i = 1; i <= 9; i++) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_0 + i) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_0 + i)) {
                return i;
            }
        }
        return -1;
    }

    private void mettreAJourSelection() {
        for (int i = 0; i < boutonsNiveaux.length; i++) {
            boutonsNiveaux[i].setChecked(i == selection);
        }
        stage.setKeyboardFocus(boutonsNiveaux[selection]);
    }

    private void lancerSelection() {
        NiveauInfo niveau = NIVEAUX[selection];
        game.setScreen(new GameScreen(game, selection, niveau.nom, niveau.fichier));
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
    }
}
