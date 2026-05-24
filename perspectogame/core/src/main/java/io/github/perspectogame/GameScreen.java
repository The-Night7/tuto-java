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
    private static final class PontVisuel {
        private final Vector3 blocDestination;
        private final float zEntree;
        private final float ecartCarre;
        private final float profondeur;

        private PontVisuel(Vector3 blocDestination, float zEntree, float ecartCarre, float profondeur) {
            this.blocDestination = blocDestination;
            this.zEntree = zEntree;
            this.ecartCarre = ecartCarre;
            this.profondeur = profondeur;
        }
    }

    private static final class Atterrissage {
        private final Vector3 bloc;
        private final float tempsImpact;
        private final float xImpact;

        private Atterrissage(Vector3 bloc, float tempsImpact, float xImpact) {
            this.bloc = bloc;
            this.tempsImpact = tempsImpact;
            this.xImpact = xImpact;
        }
    }

    private static final float LARGEUR_CAMERA = 15f;
    private static final float TAILLE_BLOC = 2f;
    private static final float DEMI_BLOC = TAILLE_BLOC / 2f;
    private static final float RAYON_BALLE = 0.5f;
    private static final float HAUTEUR_BALLE = DEMI_BLOC + RAYON_BALLE;
    private static final float GRAVITE = 9.81f;
    private static final float FIXED_STEP = 1f / 120f;
    private static final float MAX_FRAME_TIME = 0.25f;
    private static final float SNAP_TOLERANCE_PIXELS = 8f;
    private static final float MIN_DEPTH_GAP = 0.05f;
    private static final float BORD_EPSILON = 0.01f;
    private static final float Y_DEFAITE = -15f;
    private static final float DISTANCE_VICTOIRE = 1.2f;

    private final Main game;

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

    private Vector3 blocCourant;

    private List<Vector3> niveauActuel;

    private final Vector3 positionBalle = new Vector3();
    private final Vector3 positionDepart = new Vector3();
    private Vector3 positionQuille;
    private float vitesseX = 2.5f;
    private float vitesseY = 0f;
    private float accumulateurPhysique = 0f;
    private boolean enPause = true;
    private boolean niveauComplete = false;

    public GameScreen(Main game, String nomFichier) {
        this.game = game;

        modelBatch = new ModelBatch();
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

        camera = new OrthographicCamera(LARGEUR_CAMERA, LARGEUR_CAMERA * (Gdx.graphics.getHeight() / (float) Gdx.graphics.getWidth()));
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
        balleInstance.transform.setToTranslation(positionBalle);

        quilleModel = modelBuilder.createCylinder(0.8f, 2f, 0.8f, 16,
            new Material(ColorAttribute.createDiffuse(Color.WHITE)),
            Usage.Position | Usage.Normal);
        quilleInstance = new ModelInstance(quilleModel);

        chargerNiveau(nomFichier);
        initialiserNiveau();
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

    private void initialiserNiveau() {
        if (niveauActuel.isEmpty()) {
            positionQuille = null;
            blocCourant = null;
            positionBalle.setZero();
            return;
        }

        Vector3 premierBloc = niveauActuel.get(0);
        blocCourant = premierBloc;
        positionDepart.set(premierBloc.x, premierBloc.y + HAUTEUR_BALLE, premierBloc.z);
        positionBalle.set(positionDepart);
        balleInstance.transform.setToTranslation(positionBalle);

        Vector3 dernierBloc = niveauActuel.get(niveauActuel.size() - 1);
        positionQuille = new Vector3(dernierBloc.x, dernierBloc.y + 2f, dernierBloc.z);
        quilleInstance.transform.setToTranslation(positionQuille);
    }

    private void simulerPhysique(float delta) {
        float tempsFrame = Math.min(delta, MAX_FRAME_TIME);
        accumulateurPhysique += tempsFrame;

        while (accumulateurPhysique >= FIXED_STEP) {
            avancerSimulation(FIXED_STEP);
            accumulateurPhysique -= FIXED_STEP;

            if (niveauComplete) {
                accumulateurPhysique = 0f;
                break;
            }
        }
    }

    private void avancerSimulation(float delta) {
        if (blocCourant != null) {
            avancerSurBloc(delta);
        } else {
            appliquerChute(delta);
        }

        verifierVictoire();
    }

    private void avancerSurBloc(float delta) {
        positionBalle.y = blocCourant.y + HAUTEUR_BALLE;
        positionBalle.z = borner(positionBalle.z, blocCourant.z - DEMI_BLOC, blocCourant.z + DEMI_BLOC);
        vitesseY = 0f;

        float bordSortieX = blocCourant.x + DEMI_BLOC;
        float prochainX = positionBalle.x + vitesseX * delta;
        if (prochainX < bordSortieX) {
            positionBalle.x = prochainX;
            return;
        }

        float distanceJusquAuBord = Math.max(0f, bordSortieX - positionBalle.x);
        float tempsJusquAuBord = vitesseX > 0f ? distanceJusquAuBord / vitesseX : 0f;
        float tempsRestant = Math.max(0f, delta - tempsJusquAuBord);
        positionBalle.x = bordSortieX;

        PontVisuel pont = trouverPontVisuel(blocCourant);
        if (pont != null) {
            blocCourant = pont.blocDestination;
            positionBalle.x = pont.blocDestination.x - DEMI_BLOC;
            positionBalle.y = pont.blocDestination.y + HAUTEUR_BALLE;
            positionBalle.z = pont.zEntree;

            if (tempsRestant > 0f) {
                positionBalle.x = Math.min(positionBalle.x + vitesseX * tempsRestant, blocCourant.x + DEMI_BLOC);
            }
            return;
        }

        blocCourant = null;
        positionBalle.x = bordSortieX + BORD_EPSILON;
        if (tempsRestant > 0f) {
            appliquerChute(tempsRestant);
        }
    }

    private void appliquerChute(float delta) {
        float xAvant = positionBalle.x;
        float yAvant = positionBalle.y;
        float vitesseYAvant = vitesseY;
        float xApres = xAvant + vitesseX * delta;
        float yApres = yAvant + vitesseYAvant * delta - 0.5f * GRAVITE * delta * delta;

        Atterrissage atterrissage = trouverAtterrissagePendantChute(xAvant, xApres, positionBalle.z, yAvant, vitesseYAvant, delta);
        if (atterrissage != null) {
            blocCourant = atterrissage.bloc;
            vitesseY = 0f;
            positionBalle.x = atterrissage.xImpact;
            positionBalle.y = atterrissage.bloc.y + HAUTEUR_BALLE;
            positionBalle.z = borner(positionBalle.z, atterrissage.bloc.z - DEMI_BLOC, atterrissage.bloc.z + DEMI_BLOC);

            float tempsRestant = delta - atterrissage.tempsImpact;
            if (tempsRestant > 0f) {
                avancerSurBloc(tempsRestant);
            }
            return;
        }

        positionBalle.x = xApres;
        positionBalle.y = yApres;
        vitesseY = vitesseYAvant - GRAVITE * delta;
        if (positionBalle.y < Y_DEFAITE) {
            reinitialiserBalle();
        }
    }

    private Atterrissage trouverAtterrissagePendantChute(float xAvant, float xApres, float z, float yAvant, float vitesseYAvant, float delta) {
        Atterrissage meilleurAtterrissage = null;

        for (Vector3 bloc : niveauActuel) {
            if (Math.abs(z - bloc.z) > DEMI_BLOC) {
                continue;
            }

            float hauteurSol = bloc.y + HAUTEUR_BALLE;
            if (yAvant < hauteurSol) {
                continue;
            }

            float tempsImpact = calculerTempsImpact(yAvant, vitesseYAvant, hauteurSol);
            if (tempsImpact < 0f || tempsImpact > delta) {
                continue;
            }

            float xImpact = xAvant + vitesseX * tempsImpact;
            if (xImpact < bloc.x - DEMI_BLOC || xImpact > bloc.x + DEMI_BLOC) {
                continue;
            }

            if (xImpact < Math.min(xAvant, xApres) - BORD_EPSILON || xImpact > Math.max(xAvant, xApres) + BORD_EPSILON) {
                continue;
            }

            if (meilleurAtterrissage == null
                || tempsImpact < meilleurAtterrissage.tempsImpact
                || (Math.abs(tempsImpact - meilleurAtterrissage.tempsImpact) < 0.0001f && bloc.y > meilleurAtterrissage.bloc.y)) {
                meilleurAtterrissage = new Atterrissage(bloc, tempsImpact, xImpact);
            }
        }

        return meilleurAtterrissage;
    }

    private float calculerTempsImpact(float yAvant, float vitesseYAvant, float hauteurSol) {
        float deltaHauteur = yAvant - hauteurSol;
        if (deltaHauteur < 0f) {
            return -1f;
        }

        float discriminant = vitesseYAvant * vitesseYAvant + 2f * GRAVITE * deltaHauteur;
        if (discriminant < 0f) {
            return -1f;
        }

        return (vitesseYAvant + (float) Math.sqrt(discriminant)) / GRAVITE;
    }

    private PontVisuel trouverPontVisuel(Vector3 blocSource) {
        Vector3 axeDroite = new Vector3(camera.direction).crs(camera.up).nor();
        Vector3 axeHaut = new Vector3(camera.up).nor();
        Vector3 axeVue = new Vector3(camera.direction).nor();
        float zLocalSource = borner(positionBalle.z - blocSource.z, -DEMI_BLOC, DEMI_BLOC);

        Vector3 sortie = new Vector3(blocSource.x + DEMI_BLOC, blocSource.y + HAUTEUR_BALLE, blocSource.z + zLocalSource);
        Vector3 sortieRelative = new Vector3(sortie).sub(camera.position);
        float sortieX = sortieRelative.dot(axeDroite);
        float sortieY = sortieRelative.dot(axeHaut);
        float sortieProfondeur = sortieRelative.dot(axeVue);

        float tolerance = calculerToleranceAlignement();
        float toleranceCarree = tolerance * tolerance;
        float segmentDirX = axeDroite.z;
        float segmentDirY = axeHaut.z;
        float segmentDirProfondeur = axeVue.z;
        float longueurSegmentCarree = segmentDirX * segmentDirX + segmentDirY * segmentDirY;
        PontVisuel meilleurPont = null;

        for (Vector3 bloc : niveauActuel) {
            if (bloc == blocSource || bloc.x <= blocSource.x) {
                continue;
            }

            Vector3 entreeBase = new Vector3(bloc.x - DEMI_BLOC, bloc.y + HAUTEUR_BALLE, bloc.z);
            Vector3 entreeRelative = new Vector3(entreeBase).sub(camera.position);
            float entreeBaseX = entreeRelative.dot(axeDroite);
            float entreeBaseY = entreeRelative.dot(axeHaut);
            float entreeBaseProfondeur = entreeRelative.dot(axeVue);

            float zLocalCible;
            float pointEntreeX;
            float pointEntreeY;
            float pointEntreeProfondeur;
            if (longueurSegmentCarree < 0.0001f) {
                zLocalCible = borner(positionBalle.z - bloc.z, -DEMI_BLOC, DEMI_BLOC);
                pointEntreeX = entreeBaseX;
                pointEntreeY = entreeBaseY;
                pointEntreeProfondeur = entreeBaseProfondeur + segmentDirProfondeur * zLocalCible;
            } else {
                float projection = ((sortieX - entreeBaseX) * segmentDirX + (sortieY - entreeBaseY) * segmentDirY) / longueurSegmentCarree;
                zLocalCible = borner(projection, -DEMI_BLOC, DEMI_BLOC);
                pointEntreeX = entreeBaseX + segmentDirX * zLocalCible;
                pointEntreeY = entreeBaseY + segmentDirY * zLocalCible;
                pointEntreeProfondeur = entreeBaseProfondeur + segmentDirProfondeur * zLocalCible;
            }

            float deltaX = pointEntreeX - sortieX;
            float deltaY = pointEntreeY - sortieY;
            float ecartCarre = deltaX * deltaX + deltaY * deltaY;
            if (ecartCarre > toleranceCarree) {
                continue;
            }

            float deltaProfondeur = pointEntreeProfondeur - sortieProfondeur;
            float profondeurAbsolue = Math.abs(deltaProfondeur);
            if (profondeurAbsolue <= MIN_DEPTH_GAP) {
                continue;
            }

            if (meilleurPont == null
                || ecartCarre < meilleurPont.ecartCarre
                || (Math.abs(ecartCarre - meilleurPont.ecartCarre) < 0.0001f && profondeurAbsolue < meilleurPont.profondeur)) {
                meilleurPont = new PontVisuel(bloc, bloc.z + zLocalCible, ecartCarre, profondeurAbsolue);
            }
        }

        return meilleurPont;
    }

    private float calculerToleranceAlignement() {
        float mondeParPixelX = camera.viewportWidth * camera.zoom / Gdx.graphics.getWidth();
        float mondeParPixelY = camera.viewportHeight * camera.zoom / Gdx.graphics.getHeight();
        return SNAP_TOLERANCE_PIXELS * Math.min(mondeParPixelX, mondeParPixelY);
    }

    private void verifierVictoire() {
        if (positionQuille != null && positionBalle.dst(positionQuille) <= DISTANCE_VICTOIRE) {
            niveauComplete = true;
        }
    }

    private void reinitialiserBalle() {
        positionBalle.set(positionDepart);
        blocCourant = niveauActuel.isEmpty() ? null : niveauActuel.get(0);
        vitesseY = 0f;
        accumulateurPhysique = 0f;
        enPause = true;
    }

    private float borner(float valeur, float min, float max) {
        return Math.max(min, Math.min(max, valeur));
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(new MenuScreen(game));
            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) && !niveauComplete) {
            enPause = !enPause;
        }

        if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
            float deltaX = Gdx.input.getDeltaX();
            float deltaY = Gdx.input.getDeltaY();
            float sensibilite = 0.5f;

            camera.rotateAround(Vector3.Zero, Vector3.Y, deltaX * sensibilite);
            Vector3 axeDroit = new Vector3(camera.direction).crs(camera.up).nor();
            camera.rotateAround(Vector3.Zero, axeDroit, deltaY * sensibilite);
            camera.update();
        }

        if (!enPause && !niveauComplete) {
            simulerPhysique(delta);
        }

        balleInstance.transform.setToTranslation(positionBalle);

        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        modelBatch.begin(camera);
        for (Vector3 position : niveauActuel) {
            instance.transform.setToTranslation(position);
            modelBatch.render(instance, environment);
        }
        modelBatch.render(balleInstance, environment);
        if (positionQuille != null) {
            modelBatch.render(quilleInstance, environment);
        }
        modelBatch.end();

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
    public void resize(int width, int height) {
        camera.viewportWidth = LARGEUR_CAMERA;
        camera.viewportHeight = LARGEUR_CAMERA * (height / (float) width);
        camera.update();
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
