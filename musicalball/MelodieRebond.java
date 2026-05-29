import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javax.sound.midi.*;

/**
 * Un séquenceur génératif PANORAMIQUE avec ORGANISATION FLUIDE.
 * Les pistes s'organisent verticalement sur 5 emplacements.
 * La balle adapte sa gravité en temps réel si sa piste bouge pendant un saut.
 */
public class MelodieRebond extends JPanel implements ActionListener {

    private final int LARGEUR = 960; 
    private final int HAUTEUR = 600;
    private final int DELAI_TIMER = 16; 
    
    private final double VITESSE_AVANCE = 4.0; 
    private final double GRAVITE = 0.8;
    private final int LARGEUR_ETAPE = 60; 
    private int LARGEUR_MONDE; 

    private List<Balle> balles;
    private List<Piste> pistes;
    private Timer timer;
    
    // Tête de lecture (en pixels dans le monde)
    private double playheadX = 0;

    // Gestion des 5 slots verticaux
    private Piste[] slotsActifs = new Piste[5];
    // Priorité de remplissage : on commence par le milieu (index 2 = 3/5ème)
    private final int[] PREF_SLOTS = {2, 3, 1, 4, 0}; 

    // Gestion du son
    private Synthesizer synth;
    private MidiChannel[] canauxMidi;

    private final String PARTITION_DEFAUT = 
        "# Format : NomInstrument,ID_MIDI | notes (séparées par des espaces, -- pour un silence)\n" +
        "PIANO,1 | C4 -- D4 -- E4 -- C4 -- C4 -- D4 -- E4 -- C4 --\n" +
        "MARIMBA,13 | -- -- -- -- E4 -- F4 -- G4 -- -- -- -- -- -- -- -- -- E4 -- F4 -- G4 --\n" +
        "BASSE,34 | C2 -- -- -- G2 -- -- -- C2 -- -- -- G2 -- -- --\n" +
        "XYLO,14 | -- -- C5 -- -- -- C5 -- -- -- C5 -- -- -- C5 --\n" +
        "SYNTH,90 | C3 D3 E3 C3 E3 F3 G3 -- C3 D3 E3 C3 E3 F3 G3 --";

    public MelodieRebond() {
        setPreferredSize(new Dimension(LARGEUR, HAUTEUR));
        setBackground(new Color(10, 10, 20)); 

        initialiserAudio();
        String contenuPartition = lireFichierPartition("partition.txt");
        chargerPartition(contenuPartition);

        timer = new Timer(DELAI_TIMER, this);
        timer.start();
    }

    private void initialiserAudio() {
        try {
            synth = MidiSystem.getSynthesizer();
            synth.open();
            canauxMidi = synth.getChannels();
        } catch (MidiUnavailableException e) {
            e.printStackTrace();
        }
    }

    private String lireFichierPartition(String chemin) {
        try {
            File fichier = new File(chemin);
            if (fichier.exists()) {
                return new String(Files.readAllBytes(Paths.get(chemin)));
            }
        } catch (Exception e) {}
        return PARTITION_DEFAUT;
    }

    private void chargerPartition(String texte) {
        balles = new ArrayList<>();
        pistes = new ArrayList<>();
        
        String[] lignes = texte.split("\n");
        List<String> lignesValides = new ArrayList<>();
        for (String ligne : lignes) {
            ligne = ligne.trim();
            if (!ligne.isEmpty() && !ligne.startsWith("#")) lignesValides.add(ligne);
        }

        int nbPistes = Math.min(lignesValides.size(), 5); 
        if (nbPistes == 0) return;
        
        Color[] couleurs = {Color.RED, Color.CYAN, Color.GREEN, Color.ORANGE, Color.MAGENTA};

        // Déterminer la longueur maximale de la musique
        int maxEtapes = 16;
        for (int i = 0; i < nbPistes; i++) {
            try {
                String[] parts = lignesValides.get(i).split("\\|");
                String[] notesStr = parts[1].trim().split("\\s+");
                maxEtapes = Math.max(maxEtapes, notesStr.length);
            } catch (Exception e) {}
        }
        LARGEUR_MONDE = maxEtapes * LARGEUR_ETAPE;

        // Créer les pistes et les notes
        for (int i = 0; i < nbPistes; i++) {
            try {
                String[] parts = lignesValides.get(i).split("\\|");
                String[] instInfos = parts[0].split(",");
                String nom = instInfos[0].trim();
                int programMidi = Integer.parseInt(instInfos[1].trim()) - 1;

                int canalAUtiliser = i;
                if (canalAUtiliser == 9) canalAUtiliser = 10; 

                Color couleur = couleurs[i % couleurs.length];

                Piste piste = new Piste(nom, programMidi, canalAUtiliser, couleur);

                String[] notesStr = parts[1].trim().split("\\s+");
                for (int etape = 0; etape < notesStr.length; etape++) {
                    int noteMidi = decoderNote(notesStr[etape]);
                    if (noteMidi != -1) {
                        piste.ajouterNote(etape * LARGEUR_ETAPE + LARGEUR_ETAPE / 2, noteMidi);
                    }
                }

                if (!piste.notes.isEmpty()) {
                    piste.calculerSegmentsActifs();
                    pistes.add(piste);
                    balles.add(new Balle(couleur));
                }
            } catch (Exception e) {
                System.err.println("Erreur de syntaxe sur la partition.");
            }
        }
    }

    private int decoderNote(String s) {
        if (s.equals("--") || s.equals("-")) return -1;
        try { return Integer.parseInt(s); } catch (NumberFormatException e) {}
        String[] noms = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
        s = s.toUpperCase();
        try {
            int octave = Character.getNumericValue(s.charAt(s.length() - 1));
            String nomNote = s.substring(0, s.length() - 1);
            for (int i = 0; i < noms.length; i++) {
                if (noms[i].equals(nomNote)) return (octave + 1) * 12 + i;
            }
        } catch (Exception ex) {}
        return -1;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        double cameraX = playheadX - (LARGEUR / 3.0);
        AffineTransform oldTransform = g2d.getTransform();

        double camXWrapped = ((cameraX % LARGEUR_MONDE) + LARGEUR_MONDE) % LARGEUR_MONDE;
        int nbRendus = (int) Math.ceil((double) LARGEUR / LARGEUR_MONDE) + 1;
        
        g2d.translate(-camXWrapped, 0);
        for (int i = 0; i < nbRendus; i++) {
            dessinerMonde(g2d);
            g2d.translate(LARGEUR_MONDE, 0);
        }
        g2d.setTransform(oldTransform);

        int xEcranPlayhead = LARGEUR / 3;
        g2d.setColor(new Color(255, 255, 255, 40));
        g2d.drawLine(xEcranPlayhead, 0, xEcranPlayhead, HAUTEUR);

        for (Balle b : balles) b.dessiner(g2d, xEcranPlayhead);
    }

    private void dessinerMonde(Graphics2D g) {
        g.setColor(new Color(25, 25, 35));
        for (int i = 0; i <= LARGEUR_MONDE / LARGEUR_ETAPE; i++) {
            int xLigne = i * LARGEUR_ETAPE;
            g.drawLine(xLigne, 0, xLigne, HAUTEUR);
        }
        for (Piste p : pistes) p.dessiner(g);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        mettreAJourPhysique();
        repaint();
    }

    private void gererOrganisationFluide() {
        // Détermine quelle piste a besoin d'être à l'écran
        for (Piste p : pistes) {
            boolean active = p.estActiveA(playheadX, LARGEUR_MONDE);
            
            if (active && p.slotIndex == -1) {
                // Lui trouver une place parmi les 5 disponibles (le milieu d'abord)
                for (int s : PREF_SLOTS) {
                    if (slotsActifs[s] == null) {
                        slotsActifs[s] = p;
                        p.slotIndex = s;
                        p.targetY = (s + 0.5) * (HAUTEUR / 5.0);
                        break;
                    }
                }
            } else if (!active && p.slotIndex != -1) {
                // La piste n'est plus utilisée, elle libère sa place
                slotsActifs[p.slotIndex] = null;
                p.slotIndex = -1;
                p.targetY = HAUTEUR + 200; // Glisse vers le bas hors de l'écran
            }
            
            // Animation fluide de la piste
            p.currentY += (p.targetY - p.currentY) * 0.08; 
            p.mettreAJourNotes();
        }
    }

    private void mettreAJourPhysique() {
        gererOrganisationFluide();

        double lastPlayheadX = playheadX;
        playheadX += VITESSE_AVANCE;
        
        boolean boucle = false;
        if (playheadX >= LARGEUR_MONDE) {
            playheadX -= LARGEUR_MONDE;
            boucle = true;
        }

        for (int i = 0; i < balles.size(); i++) {
            Balle b = balles.get(i);
            Piste p = pistes.get(i);

            if (boucle) {
                b.etat = Balle.Etat.ATTENTE;
                b.indexNoteActuelle = 0;
            }

            double testLastX = boucle ? lastPlayheadX - LARGEUR_MONDE : lastPlayheadX;

            // Détection du moment exact où la ligne de lecture croise une note
            if (b.etat == Balle.Etat.ATTENTE || b.etat == Balle.Etat.EN_JEU) {
                if (b.indexNoteActuelle < p.notes.size()) {
                    Note cible = p.notes.get(b.indexNoteActuelle);
                    double centreX = cible.x + cible.largeur / 2.0;

                    if (testLastX <= centreX && playheadX > centreX) {
                        // LA BALLE ATTERRIT PILE SUR LA NOTE
                        cible.percuter();
                        if (canauxMidi != null) canauxMidi[p.canalMidi].noteOn(cible.noteMidi, 100);

                        b.y = p.currentY - b.rayon; 

                        boolean lastNote = (b.indexNoteActuelle == p.notes.size() - 1);
                        boolean gapTooBig = !lastNote && (p.notes.get(b.indexNoteActuelle + 1).x - centreX > 8 * LARGEUR_ETAPE);

                        // S'il y a un vide de plus de 8 temps, la balle se laisse tomber
                        if (lastNote || gapTooBig) {
                            b.etat = Balle.Etat.TOMBANT;
                            b.dy = -3; // Petit rebond de sortie
                            b.indexNoteActuelle++;
                        } else {
                            b.etat = Balle.Etat.EN_JEU;
                            b.indexNoteActuelle++; 
                        }
                    }
                }
            }

            // --- PHYSIQUE ADAPTATIVE EN TEMPS RÉEL ---
            if (b.etat == Balle.Etat.EN_JEU) {
                Note cible = p.notes.get(b.indexNoteActuelle);
                double centreX = cible.x + cible.largeur / 2.0;
                
                // Nombre de frames (temps) avant d'atteindre la prochaine note
                double tr = (centreX - playheadX) / VITESSE_AVANCE;

                if (tr > 1.0) {
                    // C'est ici la magie : on calcule en temps réel l'impulsion (dy) nécessaire
                    // pour que la balle atteigne le "targetY" actuel de la piste, dans "tr" frames.
                    // Si la piste est en train de descendre, la balle adaptera sa trajectoire instantanément.
                    b.dy = (p.targetY - b.rayon - b.y) / tr - 0.5 * GRAVITE * (tr - 1);
                }
                
                b.y += b.dy;
                b.dy += GRAVITE;
                
            } else if (b.etat == Balle.Etat.TOMBANT) {
                b.dy += GRAVITE;
                b.y += b.dy;
                // Si la balle sort de l'écran, elle passe en attente
                if (b.y > HAUTEUR + 100 && b.indexNoteActuelle < p.notes.size()) {
                    b.etat = Balle.Etat.ATTENTE;
                }
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Orchestre Génératif Intelligent");
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
        enum Etat { ATTENTE, EN_JEU, TOMBANT }
        Etat etat = Etat.ATTENTE;
        double y, dy;
        int rayon = 12;
        Color couleur;
        int indexNoteActuelle = 0;

        public Balle(Color couleur) { this.couleur = couleur; }

        public void dessiner(Graphics2D g, int xEcran) {
            if (etat == Etat.EN_JEU || etat == Etat.TOMBANT) {
                g.setColor(couleur);
                g.fillOval((int) (xEcran - rayon), (int) (y - rayon), rayon * 2, rayon * 2);
                g.setColor(Color.WHITE);
                g.drawOval((int) (xEcran - rayon), (int) (y - rayon), rayon * 2, rayon * 2);
            }
        }
    }

    class Piste {
        String nomInstrument;
        int canalMidi;
        int slotIndex = -1;
        double currentY = HAUTEUR + 200; // Commence hors de l'écran
        double targetY = HAUTEUR + 200;
        Color couleurPiste;
        List<Note> notes;
        
        // Périodes de temps où l'instrument doit être à l'écran
        List<double[]> segmentsActifs;

        public Piste(String nom, int programMidi, int canal, Color c) {
            this.nomInstrument = nom;
            this.canalMidi = canal;
            this.couleurPiste = c;
            this.notes = new ArrayList<>();
            this.segmentsActifs = new ArrayList<>();
            if (canauxMidi != null && canal < canauxMidi.length) {
                canauxMidi[canal].programChange(programMidi);
            }
        }

        public void ajouterNote(int xCentre, int noteMidi) {
            notes.add(new Note(xCentre - 15, 30, 8, noteMidi, couleurPiste));
        }

        public void calculerSegmentsActifs() {
            if (notes.isEmpty()) return;
            // Une piste apparaît un peu avant de jouer (4 étapes avant)
            double start = notes.get(0).x - 4 * LARGEUR_ETAPE;
            for (int i = 0; i < notes.size() - 1; i++) {
                // Si la pause > 8 temps, la piste va se cacher
                if (notes.get(i+1).x - notes.get(i).x > 8 * LARGEUR_ETAPE) {
                    segmentsActifs.add(new double[]{start, notes.get(i).x + 2 * LARGEUR_ETAPE});
                    start = notes.get(i+1).x - 4 * LARGEUR_ETAPE;
                }
            }
            segmentsActifs.add(new double[]{start, notes.get(notes.size()-1).x + 2 * LARGEUR_ETAPE});
        }

        public boolean estActiveA(double x, double largeurMonde) {
            for (double[] s : segmentsActifs) {
                if (x >= s[0] && x <= s[1]) return true;
                // Gestion du wrap-around (boucle de la timeline)
                if (s[0] < 0 && x >= s[0] + largeurMonde) return true;
                if (s[1] > largeurMonde && x <= s[1] - largeurMonde) return true;
            }
            return false;
        }

        public void mettreAJourNotes() {
            for (Note n : notes) n.mettreAJour();
        }

        public void dessiner(Graphics2D g) {
            int drawY = (int) currentY;
            
            // Ne dessine pas si la piste est trop bas (hors de l'écran)
            if (drawY > HAUTEUR + 50) return;

            g.setColor(new Color(40, 40, 50));
            g.drawLine(0, drawY, LARGEUR_MONDE, drawY);
            
            g.setColor(Color.LIGHT_GRAY);
            g.setFont(new Font("Arial", Font.BOLD, 12));
            g.drawString(nomInstrument, 10, drawY + 20);
            
            for (Note n : notes) n.dessiner(g, drawY);
        }
    }

    class Note {
        int x, largeur, hauteur;
        int noteMidi;
        Color couleurBase;
        int niveauIllumination = 0;

        public Note(int x, int largeur, int hauteur, int noteMidi, Color couleurBase) {
            this.x = x; this.largeur = largeur; this.hauteur = hauteur;
            this.noteMidi = noteMidi; this.couleurBase = couleurBase;
        }

        public void percuter() { niveauIllumination = 255; }

        public void mettreAJour() {
            if (niveauIllumination > 0) {
                niveauIllumination -= 15;
                if (niveauIllumination < 0) niveauIllumination = 0;
            }
        }

        public void dessiner(Graphics2D g, int yPiste) {
            if (niveauIllumination > 0) {
                g.setColor(new Color(255, 255, 255, niveauIllumination));
                g.fillRoundRect(x - 5, yPiste - 5, largeur + 10, hauteur + 10, 10, 10);
                g.setColor(Color.WHITE);
            } else {
                g.setColor(couleurBase);
            }
            g.fillRoundRect(x, yPiste, largeur, hauteur, 5, 5);
            g.setColor(Color.WHITE);
            g.drawRoundRect(x, yPiste, largeur, hauteur, 5, 5);
            
            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.BOLD, 10));
            g.drawString("♪", x + largeur/2 - 4, yPiste + hauteur/2 + 4);
        }
    }
}