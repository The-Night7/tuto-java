package io.github.perspectogame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.files.FileHandle;

import java.util.ArrayList;
import java.util.List;

public class GameScreen extends ScreenAdapter {
    private Main game;

    private OrthographicCamera camera;
    private ModelBatch modelBatch;
    private Model model;
    private ModelInstance instance;
    private Environment environment;

    private SpriteBatch batch;
    private BitmapFont font;

    private Model balleModel;
    private ModelInstance balleInstance;
    private Model quilleModel;
    private ModelInstance quilleInstance;

    private final float MARGE_TOLERANCE = 30.0f; // On est plus généreux !
    private Vector3 blocCourant = null;          // Mémorise le bloc sous la balle

    private List<Vector3> niveauActuel;

    // --- NOUVELLES VARIABLES DE GAMEPLAY ---
    private Vector3 positionBalle;
    private Vector3 positionQuille;   // Position dynamique de l'objectif
    private float vitesseX = 2.5f;
    private float vitesseY = 0f;
    private boolean enPause = true;        // Le jeu commence immobile
    private boolean niveauComplete = false; // État de victoire

    public GameScreen(Main game, String nomFichier) {
        this.game = game;

        modelBatch = new ModelBatch();
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

        camera = new OrthographicCamera(15, 15 * (Gdx.graphics.getHeight() / (float)Gdx.graphics.getWidth()));
        camera.position.set(10f, 10f, 10f);
        camera.lookAt(0, 0, 0);
        camera.near = 1f;
        camera.far = 100f;
        camera.update();

        batch = new SpriteBatch();
        font = new BitmapFont();
        font.getData().setScale(1.5f);

        ModelBuilder modelBuilder = new ModelBuilder();
        model = modelBuilder.createBox(2f, 2f, 2f,
            new Material(ColorAttribute.createDiffuse(Color.GOLD)),
            Usage.Position | Usage.Normal);
        instance = new ModelInstance(model);

        balleModel = modelBuilder.createSphere(1f, 1f, 1f, 20, 20,
            new Material(ColorAttribute.createDiffuse(Color.RED)),
            Usage.Position | Usage.Normal);
        balleInstance = new ModelInstance(balleModel);
        positionBalle = new Vector3(0f, 1.5f, 0f);
        balleInstance.transform.setToTranslation(positionBalle);

        quilleModel = modelBuilder.createCylinder(0.8f, 2f, 0.8f, 16,
            new Material(ColorAttribute.createDiffuse(Color.WHITE)),
            Usage.Position | Usage.Normal);
        quilleInstance = new ModelInstance(quilleModel);

        // Chargement du niveau
        chargerNiveau(nomFichier);

        // POSITIONNEMENT AUTOMATIQUE DE LA QUILLE SUR LE DERNIER BLOC
        if (!niveauActuel.isEmpty()) {
            Vector3 dernierBloc = niveauActuel.get(niveauActuel.size() - 1);
            positionQuille = new Vector3(dernierBloc.x, dernierBloc.y + 2f, dernierBloc.z);
            quilleInstance.transform.setToTranslation(positionQuille);
        }
    }

    private void chargerNiveau(String nomFichier) {
        niveauActuel = new ArrayList<>();
        FileHandle fichier = Gdx.files.internal(nomFichier);
        String texteComplet = fichier.readString();
        String[] lignes = texteComplet.split("\\r?\\n");

        for (String ligne : lignes) {
            if (ligne.trim().isEmpty() || ligne.startsWith("#")) {
                continue;
            }
            String[] coords = ligne.split(",");
            if (coords.length == 3) {
                float x = Float.parseFloat(coords[0].trim());
                float y = Float.parseFloat(coords[1].trim());
                float z = Float.parseFloat(coords[2].trim());
                niveauActuel.add(new Vector3(x, y, z));
            }
        }
    }

    @Override
    public void render(float delta) {
        // --- GESTION DU RETOUR MENU ---
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(new MenuScreen(game));
            return;
        }

        // --- GESTION PAUSE / LANCE (Touche ESPACE pour éviter les conflits AZERTY/QWERTY) ---
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) && !niveauComplete) {
            enPause = !enPause;
        }

        // --- GESTION DE LA CAMÉRA ---
        if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
            float deltaX = Gdx.input.getDeltaX();
            float deltaY = Gdx.input.getDeltaY();
            float sensibilite = 0.5f;

            camera.rotateAround(Vector3.Zero, Vector3.Y, deltaX * sensibilite);
            Vector3 axeDroit = new Vector3(camera.direction).crs(camera.up).nor();
            camera.rotateAround(Vector3.Zero, axeDroit, deltaY * sensibilite);
            camera.update();
        }

        // --- BOUCLE PHYSIQUE (Avec Snap d'Illusion sur les arêtes) ---
        if (!enPause && !niveauComplete) {
            positionBalle.x += vitesseX * delta;

            boolean surUnBloc = false;

            // 1. Test Physique : La balle est-elle physiquement sur un bloc ?
            for (Vector3 bloc : niveauActuel) {
                // On vérifie les limites X et Z du bloc actuel
                if (Math.abs(positionBalle.x - bloc.x) <= 1.0f && Math.abs(positionBalle.z - bloc.z) <= 1.0f) {
                    if (Math.abs(positionBalle.y - (bloc.y + 1.5f)) < 0.5f) {
                        surUnBloc = true;
                        positionBalle.y = bloc.y + 1.5f;
                        positionBalle.z = bloc.z; // L'aimant pour garder la balle bien droite
                        vitesseY = 0f;
                        break;
                    }
                }
            }

            // 2. Test d'Illusion (Se déclenche uniquement à la microseconde où elle quitte le bord)
            if (!surUnBloc && vitesseY == 0) {
                // On projette la balle en plein vol sur l'écran
                Vector3 projBalle = camera.project(new Vector3(positionBalle));

                for (Vector3 bloc : niveauActuel) {
                    // C'est ICI la magie : on cible l'arête GAUCHE du bloc cible
                    Vector3 bordGauche = new Vector3(bloc.x - 1.0f, bloc.y + 1.5f, bloc.z);
                    Vector3 projBordGauche = camera.project(new Vector3(bordGauche));

                    // Distance euclidienne 2D sur l'écran : d = √((x2-x1)² + (y2-y1)²)
                    float distPixels = (float) Math.sqrt(
                        Math.pow(projBalle.x - projBordGauche.x, 2) +
                        Math.pow(projBalle.y - projBordGauche.y, 2)
                    );

                    if (distPixels <= MARGE_TOLERANCE) {
                        // L'alignement est validé : on téléporte la balle sur l'arête cible
                        positionBalle.set(bordGauche);

                        // On la pousse très légèrement de 5 cm pour valider
                        // le Test Physique à l'itération suivante et éviter de boucler
                        positionBalle.x += 0.05f;
                        surUnBloc = true;
                        break;
                    }
                }
            }

            // 3. Gravité et Défaite
            if (!surUnBloc) {
                vitesseY -= 9.81f * delta;
                positionBalle.y += vitesseY * delta;

                // Reset automatique si elle tombe trop bas
                if (positionBalle.y < -15f) {
                    positionBalle.set(0, 1.5f, 0);
                    vitesseY = 0f;
                    enPause = true;
                }
            }

            balleInstance.transform.setToTranslation(positionBalle);

            // 4. Victoire
            if (positionQuille != null && positionBalle.dst(positionQuille) <= 1.2f) {
                niveauComplete = true;
            }
        }

        // --- RENDER SCÈNE 3D ---
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        modelBatch.begin(camera);
        for (Vector3 position : niveauActuel) {
            instance.transform.setToTranslation(position);
            modelBatch.render(instance, environment);
        }
        modelBatch.render(balleInstance, environment);
        modelBatch.render(quilleInstance, environment);
        modelBatch.end();

        // --- RENDER INTERFACE UI 2D ---
        batch.begin();
        if (niveauComplete) {
            font.draw(batch, "VICTOIRE ! Niveau Complete !", Gdx.graphics.getWidth() / 2f - 120, Gdx.graphics.getHeight() / 2f + 20);
            font.draw(batch, "Appuyez sur [ESC] pour retourner au menu.", Gdx.graphics.getWidth() / 2f - 160, Gdx.graphics.getHeight() / 2f - 20);
        } else {
            font.draw(batch, "Controles : Glisser la souris pour orienter la perspective.", 20, Gdx.graphics.getHeight() - 20);
            font.draw(batch, "Statut : " + (enPause ? "IMMOBILE (Ajuste ton angle)" : "EN MOUVEMENT"), 20, Gdx.graphics.getHeight() - 50);
            font.draw(batch, "Appuie sur [ESPACE] pour " + (enPause ? "LANCER LA BALLE" : "METTRE EN PAUSE"), 20, Gdx.graphics.getHeight() - 80);
        }
        batch.end();
    }

    @Override
    public void dispose() {
        modelBatch.dispose();
        model.dispose();
        balleModel.dispose();
        quilleModel.dispose();
        batch.dispose();
        font.dispose();
    }
}
