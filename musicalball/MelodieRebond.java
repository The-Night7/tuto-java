import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;
import javax.sound.midi.*;

/**
 * Un séquenceur génératif avec un effet de PANORAMA (scrolling infini).
 * Le monde défile, et les balles sautent sur place pour atterrir sur les notes.
 */
public class MelodieRebond extends JPanel implements ActionListener {

    // 960 est idéal car très bien divisible par notre grille de 16 temps (60px)
    private final int LARGEUR = 960; 
    private final int HAUTEUR = 600;
    private final int DELAI_TIMER = 16; // Environ 60 FPS
    
    private final double VITESSE_AVANCE = 4.0; 
    private final double GRAVITE = 0.8;
    private final int LARGEUR_ETAPE = 60; // 960 / 16 étapes musicales

    private List<Balle> balles;
    private List<Piste> pistes;
    private Timer timer;
    
    // Position de la "caméra" pour l'effet panorama.
    // En la décalant, on force les balles à s'afficher au premier tiers (LARGEUR / 3).
    private double cameraX = LARGEUR - (LARGEUR / 3.0) + (LARGEUR_ETAPE / 2.0);

    // Gestion du son
    private Synthesizer synth;
    private MidiChannel canalMidi;

    public MelodieRebond() {
        setPreferredSize(new Dimension(LARGEUR, HAUTEUR));
        setBackground(new Color(10, 10, 20)); // Fond sombre élégant

        initialiserAudio();
        initialiserObjets();

        // Le Timer appelle actionPerformed() toutes les 16ms
        timer = new Timer(DELAI_TIMER, this);
        timer.start();
    }

    private void initialiserAudio() {
        try {
            synth = MidiSystem.getSynthesizer();
            synth.open();
            canalMidi = synth.getChannels()[0]; 
            // Instrument 12 = Marimba/Vibraphone (très agréable pour des rebonds)
            canalMidi.programChange(12); 
        } catch (MidiUnavailableException e) {
            System.err.println("Erreur: Le synthétiseur MIDI n'est pas disponible.");
            e.printStackTrace();
        }
    }

    private void initialiserObjets() {
        balles = new ArrayList<>();
        pistes = new ArrayList<>();

        // Gamme pentatonique (7 pistes)
        int[] gamme = {60, 62, 64, 67, 69, 72, 74};
        Color[] couleurs = {
            Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN, 
            Color.CYAN, new Color(100, 150, 255), Color.MAGENTA
        };

        int nombreDePistes = 7;
        int hauteurPiste = HAUTEUR / nombreDePistes;

        for (int i = 0; i < nombreDePistes; i++) {
            Color couleurPiste = couleurs[i % couleurs.length];
            int yPiste = i * hauteurPiste + hauteurPiste / 2;
            
            Piste piste = new Piste(yPiste, hauteurPiste, gamme[i], couleurPiste);
            
            // On crée un rythme aléatoire
            int stepCourant = 0;
            while (stepCourant < 16) {
                piste.ajouterNote(stepCourant * LARGEUR_ETAPE + LARGEUR_ETAPE / 2);
                
                double r = Math.random();
                int saut;
                if (r < 0.4) saut = 2;       // 40% de chance d'un espace moyen
                else if (r < 0.7) saut = 1;  // 30% de chance d'un espace court
                else saut = 3;               // 30% de chance d'un grand saut
                
                stepCourant += saut;
            }
            
            if (piste.notes.size() < 2) {
                piste.ajouterNote(8 * LARGEUR_ETAPE + LARGEUR_ETAPE / 2);
            }
            pistes.add(piste);

            Balle balle = new Balle(couleurPiste);
            balle.initialiser(piste);
            balles.add(balle);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Sauvegarde de la transformation initiale
        AffineTransform oldTransform = g2d.getTransform();

        // Pour créer l'illusion d'un panorama infini, on dessine le monde deux fois côte à côte
        // 1er rendu (décalé vers la gauche par la caméra)
        g2d.translate(-cameraX, 0);
        dessinerMonde(g2d);
        
        // 2ème rendu (exactement à la suite du premier pour combler le vide à droite)
        g2d.translate(LARGEUR, 0);
        dessinerMonde(g2d);

        // Restauration de la caméra fixe pour dessiner les balles
        g2d.setTransform(oldTransform);

        // Les balles sont dessinées par-dessus, fixes horizontalement sur l'écran !
        for (Balle b : balles) {
            b.dessiner(g2d, cameraX, LARGEUR);
        }
    }

    /**
     * Dessine le décor, la grille et les notes (tout ce qui défile).
     */
    private void dessinerMonde(Graphics2D g) {
        // Grille de fond pour accentuer fortement l'effet de vitesse panoramique
        g.setColor(new Color(25, 25, 35));
        for (int i = 0; i <= 16; i++) {
            int xLigne = i * LARGEUR_ETAPE;
            g.drawLine(xLigne, 0, xLigne, HAUTEUR);
        }

        // Dessiner les pistes et leurs notes
        for (Piste p : pistes) {
            p.dessiner(g);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        mettreAJourPhysique();
        repaint();
    }

    private void mettreAJourPhysique() {
        // 1. Mise à jour de la caméra globale pour le défilement
        cameraX = (cameraX + VITESSE_AVANCE) % LARGEUR;

        // 2. Mise à jour des illuminations de notes
        for (Piste p : pistes) {
            p.mettreAJourNotes();
        }

        // 3. Physique des balles
        for (int i = 0; i < balles.size(); i++) {
            Balle b = balles.get(i);
            Piste p = pistes.get(i);

            // Mouvement horizontal (virtuel dans le monde)
            b.x += b.dx;
            if (b.x > LARGEUR) {
                b.x -= LARGEUR; 
            }

            // Application de la gravité
            b.dy += GRAVITE;
            b.y += b.dy;

            // Détection de l'atterrissage parfait calculé mathématiquement
            if (b.y + b.rayon >= p.y && b.dy > 0) {
                b.y = p.y - b.rayon;
                
                Note cible = p.notes.get(b.indexNoteActuelle);
                cible.percuter(); 
                
                if (canalMidi != null) {
                    canalMidi.noteOn(p.noteMidi, 100); 
                }
                
                b.x = cible.x + cible.largeur / 2.0; 
                b.sauterVersProchaineNote(p);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Mélodie Rebondissante - Panorama Infini");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.add(new MelodieRebond());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    // --- Classes internes ---

    class Balle {
        double x, y;
        double dx = VITESSE_AVANCE;
        double dy;
        int rayon = 12;
        Color couleur;
        int indexNoteActuelle = 0;

        public Balle(Color couleur) {
            this.couleur = couleur;
        }

        public void initialiser(Piste p) {
            Note premiereNote = p.notes.get(0);
            this.x = premiereNote.x + premiereNote.largeur / 2.0;
            this.y = p.y - this.rayon;
            sauterVersProchaineNote(p); 
        }

        public void sauterVersProchaineNote(Piste piste) {
            Note courante = piste.notes.get(indexNoteActuelle);
            indexNoteActuelle = (indexNoteActuelle + 1) % piste.notes.size();
            Note cible = piste.notes.get(indexNoteActuelle);
            
            double cibleX = cible.x + cible.largeur / 2.0;
            double couranteX = courante.x + courante.largeur / 2.0;
            
            double dist_x = cibleX - couranteX;
            if (dist_x <= 0) { 
                dist_x += LARGEUR;
            }
            
            double tempsDeVol = dist_x / dx; 
            this.dy = -GRAVITE * (tempsDeVol + 1) / 2.0; 
        }

        public void dessiner(Graphics2D g, double camX, int largeurMonde) {
            // La magie du panorama : on calcule la position de la balle PAR RAPPORT à la caméra.
            // Comme la balle et la caméra avancent à la même vitesse, cette valeur reste fixe sur l'écran !
            double renderX = this.x - camX;
            renderX = ((renderX % largeurMonde) + largeurMonde) % largeurMonde;
            
            g.setColor(couleur);
            g.fillOval((int) (renderX - rayon), (int) (y - rayon), rayon * 2, rayon * 2);
            g.setColor(Color.WHITE);
            g.drawOval((int) (renderX - rayon), (int) (y - rayon), rayon * 2, rayon * 2);
        }
    }

    class Piste {
        int y, hauteurPiste, noteMidi;
        Color couleurPiste;
        List<Note> notes;

        public Piste(int y, int hauteurPiste, int noteMidi, Color couleurPiste) {
            this.y = y;
            this.hauteurPiste = hauteurPiste;
            this.noteMidi = noteMidi;
            this.couleurPiste = couleurPiste;
            this.notes = new ArrayList<>();
        }

        public void ajouterNote(int xCentre) {
            int largeurNote = 30;
            int hauteurNote = 8;
            notes.add(new Note(xCentre - largeurNote / 2, y, largeurNote, hauteurNote, couleurPiste));
        }

        public void mettreAJourNotes() {
            for (Note n : notes) n.mettreAJour();
        }

        public void dessiner(Graphics2D g) {
            g.setColor(new Color(30, 30, 40));
            g.drawLine(0, y, LARGEUR, y);
            
            for (Note n : notes) {
                n.dessiner(g);
            }
        }
    }

    class Note {
        int x, y, largeur, hauteur;
        Color couleurBase;
        int niveauIllumination = 0;

        public Note(int x, int y, int largeur, int hauteur, Color couleurBase) {
            this.x = x;
            this.y = y;
            this.largeur = largeur;
            this.hauteur = hauteur;
            this.couleurBase = couleurBase;
        }

        public void percuter() {
            niveauIllumination = 255;
        }

        public void mettreAJour() {
            if (niveauIllumination > 0) {
                niveauIllumination -= 15;
                if (niveauIllumination < 0) niveauIllumination = 0;
            }
        }

        public void dessiner(Graphics2D g) {
            if (niveauIllumination > 0) {
                g.setColor(new Color(255, 255, 255, niveauIllumination));
                g.fillRoundRect(x - 5, y - 5, largeur + 10, hauteur + 10, 10, 10);
                g.setColor(Color.WHITE);
            } else {
                g.setColor(couleurBase);
            }
            
            g.fillRoundRect(x, y, largeur, hauteur, 5, 5);
            g.setColor(Color.WHITE);
            g.drawRoundRect(x, y, largeur, hauteur, 5, 5);
        }
    }
}