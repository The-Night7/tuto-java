import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.sound.midi.*;

/**
 * Un logiciel génératif où des balles avancent rectilignement sur des pistes.
 * Elles percutent des notes qui jouent de la musique et s'illuminent.
 */
public class MelodieRebond extends JPanel implements ActionListener {

    private final int LARGEUR = 1000;
    private final int HAUTEUR = 600;
    private final int DELAI_TIMER = 16; // Environ 60 FPS
    
    // Vitesse à laquelle les balles avancent (pixels par frame)
    private final double VITESSE_AVANCE = 2.5; 

    private List<Balle> balles;
    private List<Piste> pistes;
    private Timer timer;

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
            // Utiliser le canal 0 (souvent le piano par défaut)
            canalMidi = synth.getChannels()[0]; 
            // Optionnel : Changer d'instrument (ex: 12 = Vibraphone)
            // canalMidi.programChange(12);
        } catch (MidiUnavailableException e) {
            System.err.println("Erreur: Le synthétiseur MIDI n'est pas disponible.");
            e.printStackTrace();
        }
    }

    private void initialiserObjets() {
        balles = new ArrayList<>();
        pistes = new ArrayList<>();

        // Gamme pentatonique pour que ça sonne toujours harmonieux
        int[] gamme = {60, 62, 64, 67, 69, 72, 74, 76, 79};
        Color[] couleurs = {Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE, Color.MAGENTA};

        int nombreDePistes = 7;
        int hauteurPiste = HAUTEUR / nombreDePistes;

        // Création des pistes et des balles
        for (int i = 0; i < nombreDePistes; i++) {
            Color couleurPiste = couleurs[i % couleurs.length];
            int yPiste = i * hauteurPiste + hauteurPiste / 2;
            
            // Créer la piste avec un instrument (ici, simple note MIDI)
            Piste piste = new Piste(yPiste, hauteurPiste, gamme[i], couleurPiste);
            
            // Ajouter des "notes" (plateformes) sur cette piste à des positions X aléatoires
            for (int j = 0; j < 6; j++) {
                int xNote = 150 + (int)(Math.random() * (LARGEUR - 200));
                piste.ajouterNote(xNote);
            }
            pistes.add(piste);

            // Créer une balle pour cette piste, qui commence à gauche (x=0)
            Balle balle = new Balle(0, yPiste, VITESSE_AVANCE, couleurPiste);
            balles.add(balle);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        // Anti-aliasing pour des bords lisses
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Dessiner les pistes et leurs notes
        for (Piste p : pistes) {
            p.dessiner(g2d);
        }

        // Dessiner les balles
        for (Balle b : balles) {
            b.dessiner(g2d);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        mettreAJourPhysique();
        repaint(); // Redessine l'écran
    }

    private void mettreAJourPhysique() {
        // Mettre à jour l'illumination des notes
        for (Piste p : pistes) {
            p.mettreAJourNotes();
        }

        // Faire avancer chaque balle
        for (int i = 0; i < balles.size(); i++) {
            Balle b = balles.get(i);
            Piste p = pistes.get(i);

            // Avance rectiligne
            b.x += b.dx;

            // Si la balle atteint le bord droit, elle recommence à gauche
            if (b.x > LARGEUR) {
                b.x = 0;
                p.resetNotesPercutees(); // Permet de rejouer les notes
            }

            // Vérifier les collisions avec les notes de la piste correspondante
            for (Note n : p.notes) {
                if (!n.percutee && // La note n'a pas encore été jouée ce tour-ci
                    b.x + b.rayon >= n.x && 
                    b.x - b.rayon <= n.x + n.largeur) {
                    
                    // Jouer la note MIDI (canal 0, note de la piste, vélocité 100)
                    if (canalMidi != null) {
                        canalMidi.noteOn(p.noteMidi, 100);
                    }
                    
                    // Marquer la note comme percutée et l'illuminer
                    n.percuter();
                }
            }
        }
    }

    public static void main(String[] args) {
        // Lancer l'interface graphique sur le thread approprié (EDT)
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Mélodie Avancée - Séquenceur Visuel");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.add(new MelodieRebond());
            frame.pack();
            frame.setLocationRelativeTo(null); // Centrer la fenêtre
            frame.setVisible(true);
        });
    }

    // --- Classes internes pour représenter les objets du séquenceur ---

    class Balle {
        double x, y;
        double dx; // Vitesse horizontale unique
        int rayon = 12;
        Color couleur;

        public Balle(double x, double y, double dx, Color couleur) {
            this.x = x;
            this.y = y;
            this.dx = dx;
            this.couleur = couleur;
        }

        public void dessiner(Graphics2D g) {
            g.setColor(couleur);
            g.fillOval((int) (x - rayon), (int) (y - rayon), rayon * 2, rayon * 2);
            // Contour blanc pour la visibilité
            g.setColor(Color.WHITE);
            g.drawOval((int) (x - rayon), (int) (y - rayon), rayon * 2, rayon * 2);
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

        public void ajouterNote(int x) {
            int largeurNote = 15;
            // Centrer la note verticalement sur la piste
            notes.add(new Note(x, y - hauteurPiste / 4, largeurNote, hauteurPiste / 2, couleurPiste));
        }

        public void resetNotesPercutees() {
            for (Note n : notes) n.percutee = false;
        }

        public void mettreAJourNotes() {
            for (Note n : notes) n.mettreAJour();
        }

        public void dessiner(Graphics2D g) {
            // Ligne horizontale de la piste
            g.setColor(new Color(40, 40, 50));
            g.drawLine(0, y, LARGEUR, y);
            
            // Dessiner les notes
            for (Note n : notes) {
                n.dessiner(g);
            }
        }
    }

    class Note {
        int x, y, largeur, hauteur;
        Color couleurBase;
        boolean percutee = false;
        int niveauIllumination = 0;

        public Note(int x, int y, int largeur, int hauteur, Color couleurBase) {
            this.x = x;
            this.y = y;
            this.largeur = largeur;
            this.hauteur = hauteur;
            this.couleurBase = couleurBase;
        }

        public void percuter() {
            percutee = true;
            niveauIllumination = 255; // Brillance maximale
        }

        public void mettreAJour() {
            if (niveauIllumination > 0) {
                niveauIllumination -= 15; // Diminution progressive
                if (niveauIllumination < 0) niveauIllumination = 0;
            }
        }

        public void dessiner(Graphics2D g) {
            // Halo lumineux si percutée
            if (niveauIllumination > 0) {
                g.setColor(new Color(255, 255, 255, niveauIllumination));
                g.fillRoundRect(x - 5, y - 5, largeur + 10, hauteur + 10, 10, 10);
            }
            
            // La note elle-même
            g.setColor(percutee ? Color.WHITE : couleurBase);
            g.fillRoundRect(x, y, largeur, hauteur, 5, 5);
            
            // Contour
            g.setColor(Color.WHITE);
            g.drawRoundRect(x, y, largeur, hauteur, 5, 5);
        }
    }
}