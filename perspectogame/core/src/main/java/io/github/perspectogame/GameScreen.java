package io.github.perspectogame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Preferences;
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
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import java.util.ArrayList;
import java.util.List;

public class GameScreen extends ScreenAdapter {
    private static final class PontVisuel {
        private final Vector3 blocDestination;
        private final Vector3 pointSortie;
        private final Vector3 pointEntree;
        private final float ecartCarre;
        private final float profondeur;

        private PontVisuel(Vector3 blocDestination, Vector3 pointSortie, Vector3 pointEntree, float ecartCarre, float profondeur) {
            this.blocDestination = blocDestination;
            this.pointSortie = new Vector3(pointSortie);
            this.pointEntree = new Vector3(pointEntree);
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
    private static final float SNAP_TOLERANCE_PIXELS = 12f;
    private static final float MIN_DEPTH_GAP = 0.05f;
    private static final float BORD_EPSILON = 0.01f;
    private static final float Y_DEFAITE = -15f;
    private static final float DISTANCE_VICTOIRE = 1.2f;
    private static final float VITESSE_CAMERA = 10f;

    private final Main game;
    private final int niveauIndex;
    private final String nomNiveau;
    private final String fichierNiveau;
    private final Preferences progression;

    private OrthographicCamera camera;
    private ModelBatch modelBatch;
    private Model model;
    private ModelInstance instance;
    private Environment environment;

    private Stage uiStage;
    private Skin skin;
    private Label statutLabel;
    private Label actionLabel;
    private Label pontLabel;
    private Label niveauLabel;
    private TextButton pauseButton;
    private Table victoireOverlay;

    private Model balleModel;
    private ModelInstance balleInstance;
    private Model quilleModel;
    private ModelInstance quilleInstance;
    private Model pointPontModel;
    private ModelInstance pointPontInstance;

    private Vector3 blocCourant;
    private PontVisuel pontActif;

    private List<Vector3> niveauActuel;

    private final Vector3 positionBalle = new Vector3();
    private final Vector3 positionDepart = new Vector3();
    private Vector3 positionQuille;
    private float vitesseX = 2.5f;
    private float vitesseY = 0f;
    private float accumulateurPhysique = 0f;
    private boolean enPause = true;
    private boolean niveauComplete = false;
    private boolean victoireSauvegardee = false;
    private final Vector3 cameraPivot = new Vector3();
    private final Vector2 pointeurStage = new Vector2();

    public GameScreen(Main game, String nomFichier) {
        this(game, -1, "Niveau", nomFichier);
    }

    public GameScreen(Main game, int niveauIndex, String nomNiveau, String nomFichier) {
        this.game = game;
        this.niveauIndex = niveauIndex;
        this.nomNiveau = nomNiveau;
        this.fichierNiveau = nomFichier;
        this.progression = Gdx.app.getPreferences("perspectogame_progress");

        modelBatch = new ModelBatch();
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

        camera = new OrthographicCamera(LARGEUR_CAMERA, LARGEUR_CAMERA * (Gdx.graphics.getHeight() / (float) Gdx.graphics.getWidth()));
        camera.position.set(10f, 10f, 10f);
        cameraPivot.setZero();
        camera.near = 1f;
        camera.far = 100f;
        orienterCameraVersPivot();

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

        pointPontModel = modelBuilder.createSphere(0.35f, 0.35f, 0.35f, 16, 16,
            new Material(ColorAttribute.createDiffuse(Color.CYAN)),
            Usage.Position | Usage.Normal);
        pointPontInstance = new ModelInstance(pointPontModel);

        chargerNiveau(nomFichier);
        initialiserNiveau();
        initialiserInterface();
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(new InputMultiplexer(uiStage));
    }

    private void initialiserInterface() {
        uiStage = new Stage(new ScreenViewport());
        skin = UiFactory.createSkin();

        Table racine = new Table();
        racine.setFillParent(true);
        racine.setTouchable(Touchable.childrenOnly);
        racine.pad(12f);
        uiStage.addActor(racine);

        Table hud = new Table();
        hud.setBackground(UiFactory.drawable(skin, "hud"));
        hud.pad(12f);
        hud.defaults().left().spaceBottom(4f);

        niveauLabel = new Label(nomNiveau, skin, "accent");
        statutLabel = new Label("", skin);
        actionLabel = new Label("", skin, "small");
        pontLabel = new Label("", skin, "small");
        actionLabel.setWrap(true);
        pontLabel.setWrap(true);
        hud.add(niveauLabel).width(340f).row();
        hud.add(statutLabel).width(340f).row();
        hud.add(actionLabel).width(340f).row();
        hud.add(pontLabel).width(340f).row();

        Table commandes = new Table();
        commandes.defaults().height(38f).minWidth(112f).spaceBottom(7f);
        pauseButton = new TextButton("", skin, "accent");
        pauseButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                basculerPause();
            }
        });
        TextButton recommencer = new TextButton("Rejouer", skin, "ghost");
        recommencer.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                reinitialiserBalle();
            }
        });
        TextButton menu = new TextButton("Menu", skin, "ghost");
        menu.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                retournerMenu();
            }
        });
        commandes.add(pauseButton).growX().row();
        commandes.add(recommencer).growX().row();
        commandes.add(menu).growX().row();

        racine.top();
        racine.add(hud).width(364f).top().left();
        racine.add().growX();
        racine.add(commandes).width(132f).top().right();

        victoireOverlay = creerOverlay(
            "Victoire !",
            "Progression sauvegardee.",
            "Menu",
            new Runnable() {
                @Override
                public void run() {
                    retournerMenu();
                }
            }
        );

        mettreAJourInterface();
    }

    private Table creerOverlay(String titre, String message, String action, final Runnable actionPrincipale) {
        Table overlay = new Table();
        overlay.setFillParent(true);
        overlay.setTouchable(Touchable.childrenOnly);
        overlay.setVisible(false);

        Table carte = new Table();
        carte.setBackground(UiFactory.drawable(skin, "panel-light"));
        carte.pad(20f);
        carte.defaults().growX().spaceBottom(10f);

        Label titreLabel = new Label(titre, skin, "title");
        titreLabel.setAlignment(Align.center);
        Label messageLabel = new Label(message, skin);
        messageLabel.setAlignment(Align.center);
        messageLabel.setWrap(true);

        TextButton overlayActionButton = new TextButton(action, skin, "accent");
        overlayActionButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                actionPrincipale.run();
            }
        });
        TextButton recommencer = new TextButton("Rejouer", skin, "ghost");
        recommencer.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                reinitialiserBalle();
            }
        });
        TextButton menu = new TextButton("Menu", skin, "ghost");
        menu.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                retournerMenu();
            }
        });

        carte.add(titreLabel).row();
        carte.add(messageLabel).width(300f).row();
        carte.add(overlayActionButton).height(42f).row();
        carte.add(recommencer).height(38f).row();
        carte.add(menu).height(38f).row();

        overlay.add(carte).width(350f).center();
        uiStage.addActor(overlay);
        return overlay;
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
        mettreAJourPontActif();
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
        pontActif = pont;
        if (pont != null) {
            blocCourant = pont.blocDestination;
            positionBalle.set(pont.pointEntree);

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
        float toleranceAlignement = calculerToleranceAlignement();
        float toleranceCarree = toleranceAlignement * toleranceAlignement;
        float zLocalTrajectoire = borner(positionBalle.z - blocSource.z, -DEMI_BLOC, DEMI_BLOC);
        Vector3 pointSortie = new Vector3(blocSource.x + DEMI_BLOC, blocSource.y + HAUTEUR_BALLE, blocSource.z + zLocalTrajectoire);
        Vector3 projectionSortie = projeterPointCamera(pointSortie, axeDroite, axeHaut, axeVue);
        PontVisuel meilleurPont = null;

        for (Vector3 bloc : niveauActuel) {
            if (bloc == blocSource) {
                continue;
            }

            PontVisuel pont = chercherPontProjete(bloc, pointSortie, projectionSortie, axeDroite, axeHaut, axeVue, toleranceCarree);
            if (pont == null || pont.pointEntree.x < pointSortie.x - BORD_EPSILON) {
                continue;
            }

            if (meilleurPont == null
                || pont.ecartCarre < meilleurPont.ecartCarre
                || (Math.abs(pont.ecartCarre - meilleurPont.ecartCarre) < 0.0001f && pont.profondeur < meilleurPont.profondeur)) {
                meilleurPont = pont;
            }
        }

        return meilleurPont;
    }

    private PontVisuel chercherPontProjete(
        Vector3 blocCible,
        Vector3 pointSortie,
        Vector3 projectionSortie,
        Vector3 axeDroite,
        Vector3 axeHaut,
        Vector3 axeVue,
        float toleranceCarree
    ) {
        float centreLocalX = 0f;
        float centreLocalZ = 0f;
        float rayonRecherche = DEMI_BLOC;
        float meilleureDistanceCarree = Float.MAX_VALUE;
        float meilleureProfondeur = Float.MAX_VALUE;
        Vector3 meilleurPoint = null;
        Vector3 pointTeste = new Vector3();
        Vector3 projectionTestee = new Vector3();
        Vector3 ecart = new Vector3();

        for (int passe = 0; passe < 4; passe++) {
            float meilleurLocalX = centreLocalX;
            float meilleurLocalZ = centreLocalZ;

            for (int ix = 0; ix < 5; ix++) {
                float facteurX = ix / 4f * 2f - 1f;
                float localX = borner(centreLocalX + facteurX * rayonRecherche, -DEMI_BLOC, DEMI_BLOC);

                for (int iz = 0; iz < 5; iz++) {
                    float facteurZ = iz / 4f * 2f - 1f;
                    float localZ = borner(centreLocalZ + facteurZ * rayonRecherche, -DEMI_BLOC, DEMI_BLOC);

                    pointTeste.set(blocCible.x + localX, blocCible.y + HAUTEUR_BALLE, blocCible.z + localZ);
                    projeterPointCamera(pointTeste, axeDroite, axeHaut, axeVue, projectionTestee);

                    float deltaX = projectionTestee.x - projectionSortie.x;
                    float deltaY = projectionTestee.y - projectionSortie.y;
                    float distanceCarree = deltaX * deltaX + deltaY * deltaY;
                    float profondeur = Math.abs(ecart.set(pointTeste).sub(pointSortie).dot(axeVue));

                    if (distanceCarree < meilleureDistanceCarree
                        || (Math.abs(distanceCarree - meilleureDistanceCarree) < 0.0001f && profondeur < meilleureProfondeur)) {
                        meilleureDistanceCarree = distanceCarree;
                        meilleureProfondeur = profondeur;
                        meilleurLocalX = localX;
                        meilleurLocalZ = localZ;
                        if (meilleurPoint == null) {
                            meilleurPoint = new Vector3();
                        }
                        meilleurPoint.set(pointTeste);
                    }
                }
            }

            centreLocalX = meilleurLocalX;
            centreLocalZ = meilleurLocalZ;
            rayonRecherche *= 0.35f;
        }

        if (meilleurPoint == null || meilleureDistanceCarree > toleranceCarree) {
            return null;
        }

        return new PontVisuel(blocCible, pointSortie, meilleurPoint, meilleureDistanceCarree, meilleureProfondeur);
    }

    private Vector3 projeterPointCamera(Vector3 point, Vector3 axeDroite, Vector3 axeHaut, Vector3 axeVue) {
        return projeterPointCamera(point, axeDroite, axeHaut, axeVue, new Vector3());
    }

    private Vector3 projeterPointCamera(Vector3 point, Vector3 axeDroite, Vector3 axeHaut, Vector3 axeVue, Vector3 resultat) {
        resultat.set(point).sub(camera.position);
        float projectionX = resultat.dot(axeDroite);
        float projectionY = resultat.dot(axeHaut);
        float projectionZ = resultat.dot(axeVue);
        return resultat.set(projectionX, projectionY, projectionZ);
    }

    private void mettreAJourPontActif() {
        if (blocCourant == null || niveauComplete) {
            pontActif = null;
            return;
        }

        pontActif = trouverPontVisuel(blocCourant);
        if (pontActif != null) {
            pointPontInstance.transform.setToTranslation(pontActif.pointEntree);
        }
    }

    private float calculerToleranceAlignement() {
        float mondeParPixelX = camera.viewportWidth * camera.zoom / Gdx.graphics.getWidth();
        float mondeParPixelY = camera.viewportHeight * camera.zoom / Gdx.graphics.getHeight();
        return SNAP_TOLERANCE_PIXELS * Math.min(mondeParPixelX, mondeParPixelY);
    }

    private void deplacerCamera(float delta) {
        float distance = VITESSE_CAMERA * delta;
        Vector3 axeDroite = new Vector3(camera.direction).crs(camera.up);
        axeDroite.y = 0f;
        if (axeDroite.isZero(0.0001f)) {
            axeDroite.set(1f, 0f, 0f);
        } else {
            axeDroite.nor();
        }

        Vector3 axeAvant = new Vector3(camera.direction);
        axeAvant.y = 0f;
        if (axeAvant.isZero(0.0001f)) {
            axeAvant.set(0f, 0f, -1f);
        } else {
            axeAvant.nor();
        }

        Vector3 decalage = new Vector3();
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            decalage.mulAdd(axeDroite, -distance);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            decalage.mulAdd(axeDroite, distance);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
            decalage.mulAdd(axeAvant, distance);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            decalage.mulAdd(axeAvant, -distance);
        }

        if (decalage.isZero(0.0001f)) {
            return;
        }

        camera.position.add(decalage);
        cameraPivot.add(decalage);
        orienterCameraVersPivot();
    }

    private void orienterCameraVersPivot() {
        camera.up.set(Vector3.Y);
        camera.lookAt(cameraPivot);
        camera.update();
    }

    private void verifierVictoire() {
        if (positionQuille != null && positionBalle.dst(positionQuille) <= DISTANCE_VICTOIRE) {
            niveauComplete = true;
            marquerNiveauTermine();
        }
    }

    private void reinitialiserBalle() {
        positionBalle.set(positionDepart);
        blocCourant = niveauActuel.isEmpty() ? null : niveauActuel.get(0);
        vitesseY = 0f;
        accumulateurPhysique = 0f;
        enPause = true;
        niveauComplete = false;
        victoireSauvegardee = false;
        mettreAJourPontActif();
    }

    private void marquerNiveauTermine() {
        if (victoireSauvegardee || niveauIndex < 0) {
            return;
        }
        progression.putBoolean(MenuScreen.cleProgression(niveauIndex), true);
        progression.flush();
        victoireSauvegardee = true;
    }

    private void basculerPause() {
        if (!niveauComplete) {
            enPause = !enPause;
        }
    }

    private void retournerMenu() {
        game.setScreen(new MenuScreen(game));
    }

    private void mettreAJourInterface() {
        if (niveauComplete) {
            statutLabel.setText("Statut : niveau termine");
            actionLabel.setText("Progression sauvegardee - Retourne au menu ou recommence.");
        } else if (enPause) {
            statutLabel.setText("Statut : balle immobile");
            actionLabel.setText("Ajuste l'angle, puis appuie sur Espace ou Lance.");
        } else {
            statutLabel.setText("Statut : balle en mouvement");
            actionLabel.setText("Espace met en pause - Souris et fleches ajustent la camera.");
        }
        pontLabel.setText(pontActif == null ? "Pont visuel : aucun alignement" : "Pont visuel : alignement detecte");
        pauseButton.setText(enPause ? "Lancer" : "Pause");
        pauseButton.setDisabled(niveauComplete);
        victoireOverlay.setVisible(niveauComplete);
    }

    private boolean pointeurSurInterface() {
        pointeurStage.set(Gdx.input.getX(), Gdx.input.getY());
        uiStage.screenToStageCoordinates(pointeurStage);
        Actor acteur = uiStage.hit(pointeurStage.x, pointeurStage.y, true);
        return acteur != null;
    }

    private float borner(float valeur, float min, float max) {
        return Math.max(min, Math.min(max, valeur));
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            retournerMenu();
            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) && !niveauComplete) {
            basculerPause();
        }

        deplacerCamera(delta);

        if (Gdx.input.isButtonPressed(Input.Buttons.LEFT) && !pointeurSurInterface()) {
            float deltaX = Gdx.input.getDeltaX();
            float deltaY = Gdx.input.getDeltaY();
            float sensibilite = 0.5f;

            camera.rotateAround(cameraPivot, Vector3.Y, deltaX * sensibilite);
            Vector3 axeDroit = new Vector3(camera.direction).crs(camera.up).nor();
            camera.rotateAround(cameraPivot, axeDroit, deltaY * sensibilite);
            orienterCameraVersPivot();
        }

        mettreAJourPontActif();
        if (!enPause && !niveauComplete) {
            simulerPhysique(delta);
        }
        mettreAJourPontActif();
        mettreAJourInterface();

        balleInstance.transform.setToTranslation(positionBalle);

        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        modelBatch.begin(camera);
        for (Vector3 position : niveauActuel) {
            instance.transform.setToTranslation(position);
            modelBatch.render(instance, environment);
        }
        modelBatch.render(balleInstance, environment);
        if (pontActif != null) {
            modelBatch.render(pointPontInstance, environment);
        }
        if (positionQuille != null) {
            modelBatch.render(quilleInstance, environment);
        }
        modelBatch.end();

        uiStage.act(delta);
        uiStage.draw();
    }

    @Override
    public void resize(int width, int height) {
        camera.viewportWidth = LARGEUR_CAMERA;
        camera.viewportHeight = LARGEUR_CAMERA * (height / (float) width);
        orienterCameraVersPivot();
        uiStage.getViewport().update(width, height, true);
    }

    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose() {
        modelBatch.dispose();
        model.dispose();
        balleModel.dispose();
        quilleModel.dispose();
        pointPontModel.dispose();
        uiStage.dispose();
        skin.dispose();
    }
}
