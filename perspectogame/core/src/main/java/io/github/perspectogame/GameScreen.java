package io.github.perspectogame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
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

        modelBatch.begin(camera);
        for (Vector3 position : niveauActuel) {
            instance.transform.setToTranslation(position);
            modelBatch.render(instance, environment);
        }
        modelBatch.end();
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
    }
}
