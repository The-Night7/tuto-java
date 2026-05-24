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
    // pour l'interface 2D (Le Texte)
    private SpriteBatch batch;
    private BitmapFont font;

    // pour les entités de jeu
    private Model balleModel;
    private ModelInstance balleInstance;
    private Model quilleModel;
    private ModelInstance quilleInstance;

    // physiques de la balle
    private Vector3 positionBalle;
    private float vitesseX = 2.5f;
    private float vitesseY = 0f;

    private final float MARGE_TOLERANCE = 15.0f;

    private List<Vector3> niveauActuel;

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

        ModelBuilder modelBuilder = new ModelBuilder();
        model = modelBuilder.createBox(2f, 2f, 2f,
            new Material(ColorAttribute.createDiffuse(Color.GOLD)),
            Usage.Position | Usage.Normal);

        instance = new ModelInstance(model);

        // --- 1. INITIALISATION DE L'INTERFACE 2D ---
        batch = new SpriteBatch();
        font = new BitmapFont();
        font.getData().setScale(1.5f); // On grossit un peu le texte

        // --- 2. CRÉATION DE LA BALLE ROUGE ---
        balleModel = modelBuilder.createSphere(1f, 1f, 1f, 20, 20,
            new Material(ColorAttribute.createDiffuse(Color.RED)),
            Usage.Position | Usage.Normal);
        balleInstance = new ModelInstance(balleModel);

        // On place la balle sur le premier bloc (0, 0, 0).
        // Comme le bloc fait 2 de haut, son sommet est à Y=1. On pose la balle à Y=1.5
        positionBalle = new Vector3(0f, 1.5f, 0f);

        // --- 3. CRÉATION DE LA QUILLE BLANCHE ---
        quilleModel = modelBuilder.createCylinder(0.8f, 2f, 0.8f, 16,
            new Material(ColorAttribute.createDiffuse(Color.WHITE)),
            Usage.Position | Usage.Normal);
        quilleInstance = new ModelInstance(quilleModel);

        // On place la quille sur le dernier bloc de notre tuto.txt (2, 0, -4)
        quilleInstance.transform.setToTranslation(2f, 2f, -4f);

        chargerNiveau(nomFichier);
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
        // --- PHYSIQUE ET LOGIQUE D'ILLUSION ---

        // 1. La balle roule inexorablement vers l'avant (axe X)
        positionBalle.x += vitesseX * delta;

        boolean surUnBloc = false;
        Vector3 blocCibleIllusion = null;

        // 2. On analyse tous les blocs du niveau
        for (Vector3 bloc : niveauActuel) {

            // Nos blocs font 2x2x2. On vérifie si la balle est au-dessus (Bounding Box mathématique)
            if (Math.abs(positionBalle.x - bloc.x) <= 1.0f && Math.abs(positionBalle.z - bloc.z) <= 1.0f) {
                surUnBloc = true;
                positionBalle.y = bloc.y + 1.5f; // On la maintient fermement sur le sol
                vitesseY = 0f; // On annule la vitesse de chute
                break; // On a trouvé notre sol, on arrête de chercher
            }

            // 3. LE TEST D'ILLUSION D'OPTIQUE
            // Si on n'est pas physiquement sur ce bloc, on regarde s'il est "visuellement" connecté
            // à la position actuelle de la balle à l'écran.
            Vector3 surfaceBloc = new Vector3(bloc.x, bloc.y + 1.5f, bloc.z);
            if (illusionsSontConnectees(positionBalle, surfaceBloc)) {
                // On a trouvé un bloc qui s'aligne parfaitement avec notre trajectoire !
                blocCibleIllusion = bloc;
            }
        }

        // 4. Résolution du mouvement
        if (!surUnBloc) {
            if (blocCibleIllusion != null && vitesseY >= 0) {
                // TÉLÉPORTATION Z ! Le joueur a réussi l'illusion.
                // La balle passe instantanément sur l'autre bloc en profondeur.
                positionBalle.z = blocCibleIllusion.z;
                positionBalle.y = blocCibleIllusion.y + 1.5f;
            } else {
                // Le joueur a raté l'illusion (ou la caméra est mal tournée).
                // La gravité (9.81 m/s²) attire la balle dans le vide.
                vitesseY -= 9.81f * delta;
                positionBalle.y += vitesseY * delta;
            }
        }

        // 5. On applique la position calculée au modèle 3D juste avant de le dessiner
        balleInstance.transform.setToTranslation(positionBalle);

        // --- RETOUR AU MENU ---
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(new MenuScreen(game));
            return;
        }

        // --- GESTION DES ENTRÉES SOURIS ---
        if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
            float deltaX = Gdx.input.getDeltaX();
            float deltaY = Gdx.input.getDeltaY();
            float sensibilite = 0.5f;

            camera.rotateAround(Vector3.Zero, Vector3.Y, deltaX * sensibilite);

            Vector3 axeDroit = new Vector3(camera.direction).crs(camera.up).nor();
            camera.rotateAround(Vector3.Zero, axeDroit, deltaY * sensibilite);
            camera.update();
        }

        // --- DESSIN DE LA SCÈNE ---
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        // --- A. DESSIN DE LA 3D ---
        modelBatch.begin(camera);

        // Dessin du niveau (les blocs)
        for (Vector3 position : niveauActuel) {
            instance.transform.setToTranslation(position);
            modelBatch.render(instance, environment);
        }

        // Dessin des entités
        modelBatch.render(balleInstance, environment);
        modelBatch.render(quilleInstance, environment);

        modelBatch.end();

        // --- B. DESSIN DE L'INTERFACE 2D (HUD) ---
        batch.begin();
        // Le texte s'affiche en haut de l'écran
        font.draw(batch, "TUTORIEL : Faites glisser la souris pour tourner la camera.", 20, Gdx.graphics.getHeight() - 20);
        font.draw(batch, "Objectif : Alignez visuellement le bloc de depart avec le bloc d'arrivee.", 20, Gdx.graphics.getHeight() - 50);
        batch.end();
    }

    private boolean illusionsSontConnectees(Vector3 pointMondeA, Vector3 pointMondeB) {
        Vector3 projA = new Vector3(pointMondeA);
        Vector3 projB = new Vector3(pointMondeB);

        camera.project(projA);
        camera.project(projB);

        float distanceX = projA.x - projB.x;
        float distanceY = projA.y - projB.y;
        float distanceEnPixels = (float) Math.sqrt((distanceX * distanceX) + (distanceY * distanceY));

        return distanceEnPixels <= MARGE_TOLERANCE;
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
