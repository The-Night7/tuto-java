import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.FontUIResource;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TrackerQuetesRPG {

    private static final String NOM_FICHIER_SAUVEGARDE = "sauvegarde_quetes.json";
    private static final Path FICHIER_SAUVEGARDE = resoudreCheminSauvegarde();
    private static final List<Quete> quetes = new ArrayList<>();

    private static final Color FOND_GLOBAL = new Color(28, 30, 24);
    private static final Color FOND_PANEL = new Color(82, 87, 63);
    private static final Color FOND_PANEL_FONCE = new Color(64, 69, 52);
    private static final Color FOND_HEADER = new Color(102, 145, 63);
    private static final Color FOND_HEADER_FONCE = new Color(88, 66, 47);
    private static final Color TEXTE = new Color(247, 242, 224);
    private static final Color TEXTE_SECONDAIRE = new Color(212, 207, 186);
    private static final Color ACCENT = new Color(234, 199, 96);
    private static final Color SUCCES = new Color(116, 188, 92);
    private static final Color ATTENTION = new Color(210, 108, 87);
    private static final Color LIGNE_ALTERNEE = new Color(91, 97, 72);
    private static final Color LIGNE_TERMINEE = new Color(72, 103, 66);
    private static final Color SURBRILLANCE = new Color(141, 189, 93);

    private static final String FILTRE_STATUT_TOUS = "Toutes";
    private static final String FILTRE_STATUT_EN_COURS = "En cours";
    private static final String FILTRE_STATUT_TERMINEES = "Terminées";
    private static final String FILTRE_ZONE_TOUTES = "Toutes les zones";

    static class Quete {
        int id;
        String zone;
        String npc;
        String coordonnees;
        String demandes;
        int xp;
        int col;
        String autres;
        boolean terminee;

        Quete(int id, String zone, String npc, String coordonnees, String demandes, int xp, int col, String autres) {
            this.id = id;
            this.zone = zone;
            this.npc = npc;
            this.coordonnees = coordonnees;
            this.demandes = demandes;
            this.xp = xp;
            this.col = col;
            this.autres = autres;
            this.terminee = false;
        }

        String getStatutTexte() {
            return terminee ? "Terminée" : "En cours";
        }
    }

    @FunctionalInterface
    interface QuestChangeListener {
        void onQuestChanged(Quete quete);
    }

    static class QuestTableModel extends AbstractTableModel {
        private final String[] colonnes = {"OK", "ID", "Zone", "PNJ", "Objectif", "XP", "Cols"};
        private final List<Quete> donnees;
        private final QuestChangeListener listener;

        QuestTableModel(List<Quete> donnees, QuestChangeListener listener) {
            this.donnees = donnees;
            this.listener = listener;
        }

        @Override
        public int getRowCount() {
            return donnees.size();
        }

        @Override
        public int getColumnCount() {
            return colonnes.length;
        }

        @Override
        public String getColumnName(int column) {
            return colonnes[column];
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            switch (columnIndex) {
                case 0:
                    return Boolean.class;
                case 1:
                case 5:
                case 6:
                    return Integer.class;
                default:
                    return String.class;
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 0;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Quete q = donnees.get(rowIndex);
            switch (columnIndex) {
                case 0:
                    return q.terminee;
                case 1:
                    return q.id;
                case 2:
                    return q.zone;
                case 3:
                    return q.npc;
                case 4:
                    return raccourcir(q.demandes, 42);
                case 5:
                    return q.xp;
                case 6:
                    return q.col;
                default:
                    return "";
            }
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (columnIndex != 0 || !(aValue instanceof Boolean)) {
                return;
            }
            definirEtat(rowIndex, (Boolean) aValue);
        }

        void definirEtat(int rowIndex, boolean terminee) {
            Quete q = donnees.get(rowIndex);
            if (q.terminee == terminee) {
                return;
            }
            q.terminee = terminee;
            fireTableRowsUpdated(rowIndex, rowIndex);
            listener.onQuestChanged(q);
        }

        Quete getQueteAt(int rowIndex) {
            return donnees.get(rowIndex);
        }

        int trouverIndexParId(int id) {
            for (int i = 0; i < donnees.size(); i++) {
                if (donnees.get(i).id == id) {
                    return i;
                }
            }
            return -1;
        }
    }

    static class BlockPanel extends JPanel {
        private final Color baseColor;
        private final Color detailColor;

        BlockPanel(LayoutManager layout, Color baseColor, Color detailColor) {
            super(layout);
            this.baseColor = baseColor;
            this.detailColor = detailColor;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g2.setColor(baseColor);
            g2.fillRect(0, 0, getWidth(), getHeight());

            Color detail = new Color(detailColor.getRed(), detailColor.getGreen(), detailColor.getBlue(), 42);
            g2.setColor(detail);
            for (int y = 0; y < getHeight(); y += 16) {
                for (int x = (y / 16 % 2 == 0 ? 0 : 8); x < getWidth(); x += 16) {
                    g2.fillRect(x, y, 8, 8);
                }
            }

            g2.setColor(new Color(255, 255, 255, 28));
            g2.fillRect(0, 0, getWidth(), 4);
            g2.setColor(new Color(0, 0, 0, 55));
            g2.fillRect(0, getHeight() - 4, getWidth(), 4);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    static class TrackerFrame extends JFrame {
        private final QuestTableModel tableModel;
        private final TableRowSorter<QuestTableModel> sorter;
        private final JTable table;
        private final JTextField champRecherche;
        private final JComboBox<String> filtreZone;
        private final JComboBox<String> filtreStatut;
        private final JLabel totalValue;
        private final JLabel resteValue;
        private final JLabel xpValue;
        private final JLabel colValue;
        private final JLabel countLabel;
        private final JLabel statusLabel;
        private final JLabel detailTitre;
        private final JLabel detailSousTitre;
        private final JLabel detailZone;
        private final JLabel detailNpc;
        private final JLabel detailCoordonnees;
        private final JLabel detailXp;
        private final JLabel detailCol;
        private final JLabel detailStatut;
        private final JTextArea detailDemandes;
        private final JTextArea detailAutres;
        private final JButton toggleButton;
        private final JButton copyCoordsButton;
        private final JProgressBar progressBar;
        private Timer statusTimer;

        TrackerFrame() {
            setTitle("Tracker de Quêtes RPG");
            setMinimumSize(new Dimension(1180, 760));
            setSize(1360, 860);
            setLocationRelativeTo(null);
            setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    sauvegarderDonnees();
                    dispose();
                }
            });

            tableModel = new QuestTableModel(quetes, this::onQuestChanged);
            sorter = new TableRowSorter<>(tableModel);
            table = creerTable();
            champRecherche = creerChampRecherche();
            filtreZone = creerFiltreZone();
            filtreStatut = creerFiltreStatut();
            totalValue = creerValeurCarte();
            resteValue = creerValeurCarte();
            xpValue = creerValeurCarte();
            colValue = creerValeurCarte();
            countLabel = creerPetitLabel();
            statusLabel = creerPetitLabel();
            detailTitre = creerTitreDetail();
            detailSousTitre = creerSousTitreDetail();
            detailZone = creerValeurDetail();
            detailNpc = creerValeurDetail();
            detailCoordonnees = creerValeurDetail();
            detailXp = creerValeurDetail();
            detailCol = creerValeurDetail();
            detailStatut = creerValeurDetail();
            detailDemandes = creerZoneTexteDetail();
            detailAutres = creerZoneTexteDetail();
            toggleButton = creerBoutonAction("Marquer terminée", SUCCES);
            copyCoordsButton = creerBoutonSecondaire("Copier les coordonnées");
            progressBar = new JProgressBar(0, 100);

            setContentPane(construireInterface());
            configurerRaccourcis();
            appliquerTriParDefaut();
            appliquerFiltres();
            selectionnerPremiereQueteVisible();
            mettreAJourStatistiques();
            mettreAJourDetail();
        }

        private JPanel construireInterface() {
            JPanel root = new JPanel(new BorderLayout(18, 18));
            root.setBorder(new EmptyBorder(16, 16, 16, 16));
            root.setBackground(FOND_GLOBAL);

            root.add(creerHeader(), BorderLayout.NORTH);
            root.add(creerContenuPrincipal(), BorderLayout.CENTER);
            root.add(creerFooter(), BorderLayout.SOUTH);
            return root;
        }

        private JPanel creerHeader() {
            BlockPanel header = new BlockPanel(new BorderLayout(16, 16), FOND_HEADER, FOND_HEADER_FONCE);
            header.setBorder(new CompoundBorder(
                    BorderFactory.createLineBorder(new Color(55, 67, 38), 3),
                    new EmptyBorder(18, 20, 18, 20)
            ));

            JPanel textes = new JPanel(new BorderLayout(0, 8));
            textes.setOpaque(false);

            JLabel titre = new JLabel("Journal de quêtes - mode Minecraft");
            titre.setFont(new Font("Monospaced", Font.BOLD, 24));
            titre.setForeground(TEXTE);

            JLabel sousTitre = new JLabel("Recherche instantanée, suivi clair des récompenses et validation en un clic.");
            sousTitre.setFont(new Font("Monospaced", Font.PLAIN, 13));
            sousTitre.setForeground(TEXTE_SECONDAIRE);

            textes.add(titre, BorderLayout.NORTH);
            textes.add(sousTitre, BorderLayout.CENTER);

            JPanel progressionPanel = new JPanel(new BorderLayout(0, 10));
            progressionPanel.setOpaque(false);
            JLabel progressionLabel = new JLabel("Progression globale");
            progressionLabel.setForeground(TEXTE);
            progressionLabel.setFont(new Font("Monospaced", Font.BOLD, 13));

            progressBar.setStringPainted(true);
            progressBar.setFont(new Font("Monospaced", Font.BOLD, 12));
            progressBar.setForeground(ACCENT);
            progressBar.setBackground(new Color(73, 57, 41));
            progressBar.setBorder(new CompoundBorder(
                    BorderFactory.createLineBorder(new Color(60, 43, 32), 2),
                    new EmptyBorder(3, 3, 3, 3)
            ));

            progressionPanel.add(progressionLabel, BorderLayout.NORTH);
            progressionPanel.add(progressBar, BorderLayout.CENTER);

            JPanel cartes = new JPanel(new GridLayout(1, 4, 10, 10));
            cartes.setOpaque(false);
            cartes.add(creerCarteStat("Quêtes faites", totalValue));
            cartes.add(creerCarteStat("Quêtes restantes", resteValue));
            cartes.add(creerCarteStat("XP gagnée", xpValue));
            cartes.add(creerCarteStat("Cols gagnés", colValue));

            header.add(textes, BorderLayout.NORTH);
            header.add(progressionPanel, BorderLayout.CENTER);
            header.add(cartes, BorderLayout.SOUTH);
            return header;
        }

        private JSplitPane creerContenuPrincipal() {
            JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, creerPanneauQuetes(), creerPanneauDetail());
            split.setResizeWeight(0.67);
            split.setBorder(null);
            split.setDividerSize(12);
            split.setContinuousLayout(true);
            return split;
        }

        private JPanel creerPanneauQuetes() {
            BlockPanel panel = new BlockPanel(new BorderLayout(14, 14), FOND_PANEL, FOND_PANEL_FONCE);
            panel.setBorder(creerBordurePanel());

            JPanel entete = new JPanel(new BorderLayout(0, 10));
            entete.setOpaque(false);

            JPanel titrePanel = new JPanel(new BorderLayout());
            titrePanel.setOpaque(false);
            JLabel titre = new JLabel("Tableau des quêtes");
            titre.setFont(new Font("Monospaced", Font.BOLD, 18));
            titre.setForeground(TEXTE);
            countLabel.setHorizontalAlignment(SwingConstants.RIGHT);
            titrePanel.add(titre, BorderLayout.WEST);
            titrePanel.add(countLabel, BorderLayout.EAST);

            JPanel filtres = new JPanel(new GridBagLayout());
            filtres.setOpaque(false);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(0, 0, 8, 10);
            gbc.anchor = GridBagConstraints.WEST;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 0;

            JLabel rechercheLabel = creerLabelFiltre("Recherche");
            JLabel zoneLabel = creerLabelFiltre("Zone");
            JLabel statutLabel = creerLabelFiltre("Statut");

            gbc.gridx = 0;
            gbc.gridy = 0;
            filtres.add(rechercheLabel, gbc);
            gbc.gridx = 1;
            gbc.weightx = 1;
            filtres.add(champRecherche, gbc);
            gbc.gridx = 2;
            gbc.weightx = 0;
            filtres.add(zoneLabel, gbc);
            gbc.gridx = 3;
            filtres.add(filtreZone, gbc);
            gbc.gridx = 4;
            filtres.add(statutLabel, gbc);
            gbc.gridx = 5;
            filtres.add(filtreStatut, gbc);

            gbc.gridx = 0;
            gbc.gridy = 1;
            gbc.gridwidth = 6;
            gbc.insets = new Insets(0, 0, 0, 0);
            filtres.add(creerActionsRapides(), gbc);

            entete.add(titrePanel, BorderLayout.NORTH);
            entete.add(filtres, BorderLayout.CENTER);

            JScrollPane scrollPane = new JScrollPane(table);
            scrollPane.setBorder(BorderFactory.createLineBorder(new Color(52, 57, 44), 2));
            scrollPane.getViewport().setBackground(FOND_PANEL_FONCE);

            panel.add(entete, BorderLayout.NORTH);
            panel.add(scrollPane, BorderLayout.CENTER);
            return panel;
        }

        private JPanel creerPanneauDetail() {
            BlockPanel panel = new BlockPanel(new BorderLayout(14, 14), FOND_PANEL, FOND_PANEL_FONCE);
            panel.setBorder(creerBordurePanel());

            JPanel entete = new JPanel(new BorderLayout(0, 6));
            entete.setOpaque(false);
            detailTitre.setText("Sélectionnez une quête");
            detailSousTitre.setText("Les détails complets apparaissent ici.");
            entete.add(detailTitre, BorderLayout.NORTH);
            entete.add(detailSousTitre, BorderLayout.CENTER);

            JPanel meta = new JPanel(new GridLayout(3, 2, 10, 10));
            meta.setOpaque(false);
            meta.add(creerBlocMeta("Zone", detailZone));
            meta.add(creerBlocMeta("PNJ", detailNpc));
            meta.add(creerBlocMeta("Coordonnées", detailCoordonnees));
            meta.add(creerBlocMeta("Statut", detailStatut));
            meta.add(creerBlocMeta("XP", detailXp));
            meta.add(creerBlocMeta("Cols", detailCol));

            JPanel textes = new JPanel(new GridLayout(2, 1, 0, 10));
            textes.setOpaque(false);
            textes.add(creerBlocTexte("Objectif de quête", detailDemandes));
            textes.add(creerBlocTexte("Notes supplémentaires", detailAutres));

            JPanel actions = new JPanel(new GridLayout(1, 2, 10, 0));
            actions.setOpaque(false);

            toggleButton.addActionListener(e -> basculerQueteSelectionnee());
            copyCoordsButton.addActionListener(e -> copierCoordonneesSelectionnees());
            actions.add(toggleButton);
            actions.add(copyCoordsButton);

            panel.add(entete, BorderLayout.NORTH);
            panel.add(meta, BorderLayout.CENTER);
            panel.add(textes, BorderLayout.SOUTH);

            JPanel wrapper = new JPanel(new BorderLayout(14, 14));
            wrapper.setOpaque(false);
            wrapper.add(panel, BorderLayout.CENTER);
            wrapper.add(actions, BorderLayout.SOUTH);
            return wrapper;
        }

        private JPanel creerFooter() {
            JPanel footer = new JPanel(new BorderLayout(12, 12));
            footer.setOpaque(false);

            statusLabel.setText("Sauvegarde automatique active.");
            JLabel hints = creerPetitLabel();
            hints.setHorizontalAlignment(SwingConstants.RIGHT);
            hints.setText("Ctrl+F recherche | Ctrl+R réinitialise | Espace valide la quête sélectionnée");

            footer.add(statusLabel, BorderLayout.WEST);
            footer.add(hints, BorderLayout.CENTER);
            return footer;
        }

        private JTable creerTable() {
            JTable questTable = new JTable(tableModel) {
                @Override
                public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                    Component component = super.prepareRenderer(renderer, row, column);
                    int modelRow = convertRowIndexToModel(row);
                    Quete q = tableModel.getQueteAt(modelRow);

                    Color background;
                    if (isRowSelected(row)) {
                        background = SURBRILLANCE;
                    } else if (q.terminee) {
                        background = LIGNE_TERMINEE;
                    } else if (row % 2 == 0) {
                        background = FOND_PANEL_FONCE;
                    } else {
                        background = LIGNE_ALTERNEE;
                    }

                    component.setBackground(background);
                    component.setForeground(isRowSelected(row) ? new Color(33, 36, 29) : TEXTE);
                    return component;
                }
            };

            questTable.setRowHeight(34);
            questTable.setFillsViewportHeight(true);
            questTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            questTable.setGridColor(new Color(72, 77, 59));
            questTable.setShowGrid(true);
            questTable.setIntercellSpacing(new Dimension(0, 1));
            questTable.setBackground(FOND_PANEL_FONCE);
            questTable.setForeground(TEXTE);
            questTable.setFont(new Font("Monospaced", Font.PLAIN, 13));
            questTable.getTableHeader().setFont(new Font("Monospaced", Font.BOLD, 12));
            questTable.getTableHeader().setBackground(new Color(66, 78, 50));
            questTable.getTableHeader().setForeground(TEXTE);
            questTable.getTableHeader().setReorderingAllowed(false);
            questTable.setRowSorter(sorter);

            DefaultTableCellRenderer centered = new DefaultTableCellRenderer();
            centered.setHorizontalAlignment(SwingConstants.CENTER);
            DefaultTableCellRenderer left = new DefaultTableCellRenderer();
            left.setHorizontalAlignment(SwingConstants.LEFT);

            questTable.getColumnModel().getColumn(0).setMaxWidth(54);
            questTable.getColumnModel().getColumn(1).setMaxWidth(56);
            questTable.getColumnModel().getColumn(5).setMaxWidth(68);
            questTable.getColumnModel().getColumn(6).setMaxWidth(78);
            questTable.getColumnModel().getColumn(1).setCellRenderer(centered);
            questTable.getColumnModel().getColumn(5).setCellRenderer(centered);
            questTable.getColumnModel().getColumn(6).setCellRenderer(centered);
            questTable.getColumnModel().getColumn(2).setCellRenderer(left);
            questTable.getColumnModel().getColumn(3).setCellRenderer(left);
            questTable.getColumnModel().getColumn(4).setCellRenderer(left);

            questTable.getSelectionModel().addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) {
                    mettreAJourDetail();
                }
            });

            return questTable;
        }

        private JTextField creerChampRecherche() {
            JTextField field = new JTextField();
            field.setFont(new Font("Monospaced", Font.PLAIN, 13));
            field.setBackground(new Color(250, 246, 231));
            field.setForeground(new Color(37, 39, 34));
            field.setBorder(new CompoundBorder(
                    BorderFactory.createLineBorder(new Color(90, 67, 47), 2),
                    new EmptyBorder(8, 10, 8, 10)
            ));
            field.setToolTipText("Recherche par PNJ, zone, objectif, coordonnées ou notes.");
            field.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    appliquerFiltres();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    appliquerFiltres();
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    appliquerFiltres();
                }
            });
            return field;
        }

        private JComboBox<String> creerFiltreZone() {
            List<String> zones = new ArrayList<>();
            for (Quete q : quetes) {
                if (!zones.contains(q.zone)) {
                    zones.add(q.zone);
                }
            }
            zones.sort(String.CASE_INSENSITIVE_ORDER);

            JComboBox<String> combo = new JComboBox<>();
            combo.addItem(FILTRE_ZONE_TOUTES);
            for (String zone : zones) {
                combo.addItem(zone);
            }
            styliserCombo(combo);
            combo.addActionListener(e -> appliquerFiltres());
            return combo;
        }

        private JComboBox<String> creerFiltreStatut() {
            JComboBox<String> combo = new JComboBox<>(new String[]{
                    FILTRE_STATUT_TOUS,
                    FILTRE_STATUT_EN_COURS,
                    FILTRE_STATUT_TERMINEES
            });
            styliserCombo(combo);
            combo.addActionListener(e -> appliquerFiltres());
            return combo;
        }

        private void styliserCombo(JComboBox<String> combo) {
            combo.setFont(new Font("Monospaced", Font.PLAIN, 13));
            combo.setBackground(new Color(250, 246, 231));
            combo.setForeground(new Color(37, 39, 34));
            combo.setBorder(new CompoundBorder(
                    BorderFactory.createLineBorder(new Color(90, 67, 47), 2),
                    new EmptyBorder(4, 6, 4, 6)
            ));
            combo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        private JPanel creerActionsRapides() {
            JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            panel.setOpaque(false);

            JButton toutes = creerBoutonSecondaire("Voir tout");
            JButton enCours = creerBoutonSecondaire("Seulement en cours");
            JButton terminees = creerBoutonSecondaire("Seulement terminées");
            JButton reset = creerBoutonSecondaire("Réinitialiser");

            toutes.addActionListener(e -> {
                filtreStatut.setSelectedItem(FILTRE_STATUT_TOUS);
                filtreZone.setSelectedItem(FILTRE_ZONE_TOUTES);
                champRecherche.setText("");
            });
            enCours.addActionListener(e -> filtreStatut.setSelectedItem(FILTRE_STATUT_EN_COURS));
            terminees.addActionListener(e -> filtreStatut.setSelectedItem(FILTRE_STATUT_TERMINEES));
            reset.addActionListener(e -> reinitialiserFiltres());

            panel.add(toutes);
            panel.add(enCours);
            panel.add(terminees);
            panel.add(reset);
            return panel;
        }

        private JPanel creerCarteStat(String titre, JLabel valeur) {
            BlockPanel panel = new BlockPanel(new BorderLayout(0, 8), new Color(94, 79, 55), new Color(67, 57, 41));
            panel.setBorder(new CompoundBorder(
                    BorderFactory.createLineBorder(new Color(64, 48, 33), 2),
                    new EmptyBorder(10, 12, 10, 12)
            ));

            JLabel label = new JLabel(titre);
            label.setForeground(TEXTE_SECONDAIRE);
            label.setFont(new Font("Monospaced", Font.PLAIN, 12));

            panel.add(label, BorderLayout.NORTH);
            panel.add(valeur, BorderLayout.CENTER);
            return panel;
        }

        private JPanel creerBlocMeta(String titre, JLabel valeur) {
            BlockPanel panel = new BlockPanel(new BorderLayout(0, 6), new Color(90, 75, 54), new Color(64, 54, 39));
            panel.setBorder(new CompoundBorder(
                    BorderFactory.createLineBorder(new Color(64, 48, 33), 2),
                    new EmptyBorder(10, 12, 10, 12)
            ));

            JLabel label = new JLabel(titre);
            label.setFont(new Font("Monospaced", Font.BOLD, 12));
            label.setForeground(TEXTE_SECONDAIRE);

            panel.add(label, BorderLayout.NORTH);
            panel.add(valeur, BorderLayout.CENTER);
            return panel;
        }

        private JPanel creerBlocTexte(String titre, JTextArea contenu) {
            BlockPanel panel = new BlockPanel(new BorderLayout(0, 8), new Color(90, 75, 54), new Color(64, 54, 39));
            panel.setBorder(new CompoundBorder(
                    BorderFactory.createLineBorder(new Color(64, 48, 33), 2),
                    new EmptyBorder(10, 12, 10, 12)
            ));

            JLabel label = new JLabel(titre);
            label.setFont(new Font("Monospaced", Font.BOLD, 12));
            label.setForeground(TEXTE_SECONDAIRE);
            panel.add(label, BorderLayout.NORTH);
            panel.add(contenu, BorderLayout.CENTER);
            return panel;
        }

        private Border creerBordurePanel() {
            return new CompoundBorder(
                    BorderFactory.createLineBorder(new Color(55, 59, 45), 3),
                    new EmptyBorder(16, 16, 16, 16)
            );
        }

        private JLabel creerValeurCarte() {
            JLabel label = new JLabel("0");
            label.setForeground(TEXTE);
            label.setFont(new Font("Monospaced", Font.BOLD, 20));
            return label;
        }

        private JLabel creerPetitLabel() {
            JLabel label = new JLabel();
            label.setForeground(TEXTE_SECONDAIRE);
            label.setFont(new Font("Monospaced", Font.PLAIN, 12));
            return label;
        }

        private JLabel creerTitreDetail() {
            JLabel label = new JLabel();
            label.setForeground(TEXTE);
            label.setFont(new Font("Monospaced", Font.BOLD, 20));
            return label;
        }

        private JLabel creerSousTitreDetail() {
            JLabel label = new JLabel();
            label.setForeground(TEXTE_SECONDAIRE);
            label.setFont(new Font("Monospaced", Font.PLAIN, 13));
            return label;
        }

        private JLabel creerValeurDetail() {
            JLabel label = new JLabel("-");
            label.setForeground(TEXTE);
            label.setFont(new Font("Monospaced", Font.BOLD, 14));
            return label;
        }

        private JTextArea creerZoneTexteDetail() {
            JTextArea area = new JTextArea();
            area.setOpaque(false);
            area.setEditable(false);
            area.setLineWrap(true);
            area.setWrapStyleWord(true);
            area.setForeground(TEXTE);
            area.setFont(new Font("Monospaced", Font.PLAIN, 13));
            area.setBorder(null);
            area.setFocusable(false);
            return area;
        }

        private JButton creerBoutonAction(String texte, Color couleurFond) {
            JButton button = new JButton(texte);
            button.setFont(new Font("Monospaced", Font.BOLD, 13));
            button.setForeground(new Color(28, 31, 25));
            button.setBackground(couleurFond);
            button.setFocusPainted(false);
            button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            button.setBorder(new CompoundBorder(
                    BorderFactory.createLineBorder(new Color(49, 53, 44), 2),
                    new EmptyBorder(11, 12, 11, 12)
            ));
            return button;
        }

        private JButton creerBoutonSecondaire(String texte) {
            JButton button = new JButton(texte);
            button.setFont(new Font("Monospaced", Font.BOLD, 12));
            button.setForeground(TEXTE);
            button.setBackground(new Color(103, 81, 56));
            button.setFocusPainted(false);
            button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            button.setBorder(new CompoundBorder(
                    BorderFactory.createLineBorder(new Color(64, 48, 33), 2),
                    new EmptyBorder(8, 10, 8, 10)
            ));
            return button;
        }

        private JLabel creerLabelFiltre(String texte) {
            JLabel label = new JLabel(texte);
            label.setForeground(TEXTE_SECONDAIRE);
            label.setFont(new Font("Monospaced", Font.BOLD, 12));
            return label;
        }

        private void configurerRaccourcis() {
            JComponent root = (JComponent) getContentPane();
            root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                    KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK),
                    "focusSearch"
            );
            root.getActionMap().put("focusSearch", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    champRecherche.requestFocusInWindow();
                    champRecherche.selectAll();
                }
            });

            root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                    KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK),
                    "resetFilters"
            );
            root.getActionMap().put("resetFilters", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    reinitialiserFiltres();
                }
            });

            root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                    KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0),
                    "toggleQuest"
            );
            root.getActionMap().put("toggleQuest", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    basculerQueteSelectionnee();
                }
            });
        }

        private void appliquerTriParDefaut() {
            List<javax.swing.RowSorter.SortKey> sortKeys = new ArrayList<>();
            sortKeys.add(new javax.swing.RowSorter.SortKey(0, javax.swing.SortOrder.ASCENDING));
            sortKeys.add(new javax.swing.RowSorter.SortKey(2, javax.swing.SortOrder.ASCENDING));
            sortKeys.add(new javax.swing.RowSorter.SortKey(1, javax.swing.SortOrder.ASCENDING));
            sorter.setSortKeys(sortKeys);
            sorter.sort();
        }

        private void appliquerFiltres() {
            Integer selectedId = getSelectedQuestId();
            final String recherche = champRecherche.getText().trim().toLowerCase(Locale.ROOT);
            final String zone = (String) filtreZone.getSelectedItem();
            final String statut = (String) filtreStatut.getSelectedItem();

            sorter.setRowFilter(new javax.swing.RowFilter<QuestTableModel, Integer>() {
                @Override
                public boolean include(Entry<? extends QuestTableModel, ? extends Integer> entry) {
                    Quete q = tableModel.getQueteAt(entry.getIdentifier());

                    boolean statutOk = FILTRE_STATUT_TOUS.equals(statut)
                            || (FILTRE_STATUT_EN_COURS.equals(statut) && !q.terminee)
                            || (FILTRE_STATUT_TERMINEES.equals(statut) && q.terminee);

                    boolean zoneOk = FILTRE_ZONE_TOUTES.equals(zone) || q.zone.equalsIgnoreCase(zone);

                    boolean rechercheOk = recherche.isEmpty()
                            || contient(q.zone, recherche)
                            || contient(q.npc, recherche)
                            || contient(q.coordonnees, recherche)
                            || contient(q.demandes, recherche)
                            || contient(q.autres, recherche);

                    return statutOk && zoneOk && rechercheOk;
                }
            });

            countLabel.setText(table.getRowCount() + " quête(s) affichée(s)");
            restaurerSelection(selectedId);
            if (table.getSelectedRow() < 0) {
                selectionnerPremiereQueteVisible();
            }
            mettreAJourStatistiques();
            mettreAJourDetail();
        }

        private void reinitialiserFiltres() {
            champRecherche.setText("");
            filtreZone.setSelectedItem(FILTRE_ZONE_TOUTES);
            filtreStatut.setSelectedItem(FILTRE_STATUT_TOUS);
            afficherStatutTemporaire("Filtres réinitialisés.");
        }

        private void selectionnerPremiereQueteVisible() {
            if (table.getRowCount() > 0) {
                table.setRowSelectionInterval(0, 0);
            }
        }

        private Integer getSelectedQuestId() {
            int viewRow = table.getSelectedRow();
            if (viewRow < 0) {
                return null;
            }
            int modelRow = table.convertRowIndexToModel(viewRow);
            return tableModel.getQueteAt(modelRow).id;
        }

        private void restaurerSelection(Integer questId) {
            if (questId == null) {
                return;
            }
            int modelRow = tableModel.trouverIndexParId(questId);
            if (modelRow < 0) {
                return;
            }
            int viewRow = table.convertRowIndexToView(modelRow);
            if (viewRow >= 0) {
                table.setRowSelectionInterval(viewRow, viewRow);
                table.scrollRectToVisible(table.getCellRect(viewRow, 0, true));
            }
        }

        private void onQuestChanged(Quete quete) {
            sauvegarderDonnees();
            mettreAJourStatistiques();
            restaurerSelection(quete.id);
            mettreAJourDetail();
            afficherStatutTemporaire("Quête #" + quete.id + " - " + quete.npc + " : " + quete.getStatutTexte().toLowerCase(Locale.ROOT) + ".");
        }

        private void basculerQueteSelectionnee() {
            int viewRow = table.getSelectedRow();
            if (viewRow < 0) {
                Toolkit.getDefaultToolkit().beep();
                afficherStatutTemporaire("Aucune quête sélectionnée.");
                return;
            }
            int modelRow = table.convertRowIndexToModel(viewRow);
            Quete quete = tableModel.getQueteAt(modelRow);
            tableModel.definirEtat(modelRow, !quete.terminee);
        }

        private void copierCoordonneesSelectionnees() {
            Quete quete = getSelectedQuest();
            if (quete == null) {
                Toolkit.getDefaultToolkit().beep();
                afficherStatutTemporaire("Sélectionnez une quête avant de copier ses coordonnées.");
                return;
            }
            if (quete.coordonnees.trim().isEmpty() || "Inconnue".equalsIgnoreCase(quete.coordonnees.trim())) {
                Toolkit.getDefaultToolkit().beep();
                afficherStatutTemporaire("Aucune coordonnée exploitable pour cette quête.");
                return;
            }
            try {
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(quete.coordonnees), null);
                afficherStatutTemporaire("Coordonnées copiées : " + quete.coordonnees);
            } catch (IllegalStateException ex) {
                JOptionPane.showMessageDialog(this, "Impossible d'accéder au presse-papiers pour le moment.", "Copie indisponible", JOptionPane.WARNING_MESSAGE);
            }
        }

        private Quete getSelectedQuest() {
            int viewRow = table.getSelectedRow();
            if (viewRow < 0) {
                return null;
            }
            int modelRow = table.convertRowIndexToModel(viewRow);
            return tableModel.getQueteAt(modelRow);
        }

        private void mettreAJourDetail() {
            Quete q = getSelectedQuest();
            if (q == null) {
                detailTitre.setText(table.getRowCount() == 0 ? "Aucun résultat" : "Sélectionnez une quête");
                detailSousTitre.setText(table.getRowCount() == 0 ? "Ajustez les filtres pour retrouver une quête." : "Choisissez une ligne pour voir les détails.");
                detailZone.setText("-");
                detailNpc.setText("-");
                detailCoordonnees.setText("-");
                detailXp.setText("-");
                detailCol.setText("-");
                detailStatut.setText("-");
                detailDemandes.setText("Aucun objectif à afficher.");
                detailAutres.setText("Aucune note à afficher.");
                toggleButton.setEnabled(false);
                copyCoordsButton.setEnabled(false);
                return;
            }

            detailTitre.setText("Quête #" + q.id + " - " + q.npc);
            detailSousTitre.setText(q.zone + " | " + q.getStatutTexte());
            detailZone.setText(q.zone);
            detailNpc.setText(q.npc);
            detailCoordonnees.setText(q.coordonnees);
            detailXp.setText(q.xp + " XP");
            detailCol.setText(q.col + " cols");
            detailStatut.setText(q.getStatutTexte());
            detailDemandes.setText(q.demandes);
            detailAutres.setText(q.autres.trim().isEmpty() ? "Aucune note supplémentaire." : q.autres);
            toggleButton.setEnabled(true);
            copyCoordsButton.setEnabled(true);
            toggleButton.setText(q.terminee ? "Remettre en cours" : "Marquer terminée");
            toggleButton.setBackground(q.terminee ? ACCENT : SUCCES);
            toggleButton.setForeground(new Color(30, 31, 24));
        }

        private void mettreAJourStatistiques() {
            int terminees = 0;
            int totalXp = 0;
            int totalCol = 0;

            for (Quete q : quetes) {
                if (q.terminee) {
                    terminees++;
                    totalXp += q.xp;
                    totalCol += q.col;
                }
            }

            int restantes = quetes.size() - terminees;
            int pourcentage = quetes.isEmpty() ? 0 : (int) Math.round((terminees * 100.0) / quetes.size());

            totalValue.setText(terminees + " / " + quetes.size());
            resteValue.setText(String.valueOf(restantes));
            xpValue.setText(String.valueOf(totalXp));
            colValue.setText(String.valueOf(totalCol));
            progressBar.setValue(pourcentage);
            progressBar.setString(pourcentage + "% complété");
            countLabel.setText(table.getRowCount() + " quête(s) affichée(s)");
        }

        private void afficherStatutTemporaire(String texte) {
            statusLabel.setText(texte);
            if (statusTimer != null && statusTimer.isRunning()) {
                statusTimer.stop();
            }
            statusTimer = new Timer(2600, e -> statusLabel.setText("Sauvegarde automatique active."));
            statusTimer.setRepeats(false);
            statusTimer.start();
        }
    }

    public static void main(String[] args) {
        chargerDonnees();
        if (GraphicsEnvironment.isHeadless() || Arrays.asList(args).contains("--cli")) {
            lancerModeConsole();
            return;
        }

        SwingUtilities.invokeLater(() -> {
            configurerLookAndFeel();
            TrackerFrame frame = new TrackerFrame();
            frame.setVisible(true);
        });
    }

    private static void configurerLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        FontUIResource font = new FontUIResource(new Font("Monospaced", Font.PLAIN, 14));
        for (Object key : UIManager.getDefaults().keySet()) {
            Object value = UIManager.get(key);
            if (value instanceof FontUIResource) {
                UIManager.put(key, font);
            }
        }
    }

    private static void lancerModeConsole() {
        java.util.Scanner scanner = new java.util.Scanner(System.in);
        boolean enCours = true;

        System.out.println("==========================================");
        System.out.println("      TRACKER D'EVOLUTION RPG EN JAVA     ");
        System.out.println("==========================================");

        while (enCours) {
            System.out.println("\n--- MENU PRINCIPAL ---");
            System.out.println("1. Voir toutes les quêtes");
            System.out.println("2. Voir les quêtes en cours");
            System.out.println("3. Valider / Annuler une quête");
            System.out.println("4. Voir mes statistiques");
            System.out.println("5. Rechercher par PNJ ou Zone");
            System.out.println("6. Quitter et sauvegarder");
            System.out.print("Votre choix : ");

            String choix = scanner.nextLine();
            switch (choix) {
                case "1":
                    afficherQuetes(quetes);
                    break;
                case "2":
                    afficherQuetes(getQuetesFiltrees(false));
                    break;
                case "3":
                    System.out.print("Entrez l'ID de la quête à basculer : ");
                    try {
                        int id = Integer.parseInt(scanner.nextLine());
                        basculerEtatQuete(id);
                    } catch (NumberFormatException e) {
                        System.out.println("Veuillez entrer un numéro valide.");
                    }
                    break;
                case "4":
                    afficherStatistiques();
                    break;
                case "5":
                    System.out.print("Entrez le terme à rechercher : ");
                    afficherQuetes(rechercherQuetes(scanner.nextLine().toLowerCase(Locale.ROOT)));
                    break;
                case "6":
                    sauvegarderDonnees();
                    System.out.println("Sauvegarde effectuée. À bientôt.");
                    enCours = false;
                    break;
                default:
                    System.out.println("Choix invalide, veuillez réessayer.");
            }
        }
        scanner.close();
    }

    private static void basculerEtatQuete(int id) {
        for (Quete q : quetes) {
            if (q.id == id) {
                q.terminee = !q.terminee;
                sauvegarderDonnees();
                System.out.println("La quête de " + q.npc + " est maintenant : " + q.getStatutTexte().toUpperCase(Locale.ROOT));
                return;
            }
        }
        System.out.println("Aucune quête trouvée avec l'ID " + id + ".");
    }

    private static List<Quete> getQuetesFiltrees(boolean etatTerminee) {
        List<Quete> filtrees = new ArrayList<>();
        for (Quete q : quetes) {
            if (q.terminee == etatTerminee) {
                filtrees.add(q);
            }
        }
        return filtrees;
    }

    private static List<Quete> rechercherQuetes(String terme) {
        List<Quete> resultats = new ArrayList<>();
        for (Quete q : quetes) {
            if (contient(q.npc, terme) || contient(q.zone, terme) || contient(q.demandes, terme) || contient(q.autres, terme)) {
                resultats.add(q);
            }
        }
        return resultats;
    }

    private static boolean contient(String source, String terme) {
        return source != null && source.toLowerCase(Locale.ROOT).contains(terme);
    }

    private static void afficherQuetes(List<Quete> liste) {
        if (liste.isEmpty()) {
            System.out.println("Aucune quête à afficher.");
            return;
        }

        System.out.println("\n--- LISTE DES QUETES ---");
        for (Quete q : liste) {
            String statut = q.terminee ? "[X]" : "[ ]";
            System.out.printf(
                    "%s ID:%02d | Zone: %-18s | PNJ: %-12s | Objectif: %s%n",
                    statut, q.id, q.zone, q.npc, q.demandes
            );
        }
    }

    private static void afficherStatistiques() {
        int totalXp = 0;
        int totalCol = 0;
        int quetesTerminees = 0;

        for (Quete q : quetes) {
            if (q.terminee) {
                totalXp += q.xp;
                totalCol += q.col;
                quetesTerminees++;
            }
        }

        int pourcentage = quetes.isEmpty() ? 0 : (int) ((quetesTerminees * 100.0) / quetes.size());
        System.out.println("\n--- VOS STATISTIQUES ---");
        System.out.println("Progression : " + quetesTerminees + " / " + quetes.size() + " (" + pourcentage + "%)");
        System.out.println("XP gagnée   : " + totalXp);
        System.out.println("Cols gagnés : " + totalCol);
        System.out.println("------------------------");
    }

    private static void chargerDonnees() {
        File fichier = FICHIER_SAUVEGARDE.toFile();
        if (!fichier.exists()) {
            System.out.println("Aucune sauvegarde trouvée. Création de la base initiale...");
            initialiserQuetesDefaut();
            sauvegarderDonnees();
            return;
        }

        try {
            String contenu = new String(Files.readAllBytes(FICHIER_SAUVEGARDE), StandardCharsets.UTF_8);
            Matcher matcherObjet = Pattern.compile("\\{(.*?)\\}", Pattern.DOTALL).matcher(contenu);

            quetes.clear();
            while (matcherObjet.find()) {
                String obj = matcherObjet.group(1);
                int id = extraireInt(obj, "id");
                String zone = extraireString(obj, "zone");
                String npc = extraireString(obj, "npc");
                String coordonnees = extraireString(obj, "coordonnees");
                String demandes = extraireString(obj, "demandes");
                int xp = extraireInt(obj, "xp");
                int col = extraireInt(obj, "col");
                String autres = extraireString(obj, "autres");
                boolean terminee = extraireBoolean(obj, "terminee");

                Quete q = new Quete(id, zone, npc, coordonnees, demandes, xp, col, autres);
                q.terminee = terminee;
                quetes.add(q);
            }

            if (quetes.isEmpty()) {
                initialiserQuetesDefaut();
                sauvegarderDonnees();
            } else {
                System.out.println("Données chargées avec succès (" + quetes.size() + " quêtes).");
            }
        } catch (Exception e) {
            System.out.println("Erreur lors du chargement : " + e.getMessage());
            initialiserQuetesDefaut();
        }
    }

    private static void sauvegarderDonnees() {
        StringBuilder json = new StringBuilder();
        json.append("[\n");

        for (int i = 0; i < quetes.size(); i++) {
            Quete q = quetes.get(i);
            json.append("  {\n");
            json.append("    \"id\": ").append(q.id).append(",\n");
            json.append("    \"zone\": \"").append(echapperJson(q.zone)).append("\",\n");
            json.append("    \"npc\": \"").append(echapperJson(q.npc)).append("\",\n");
            json.append("    \"coordonnees\": \"").append(echapperJson(q.coordonnees)).append("\",\n");
            json.append("    \"demandes\": \"").append(echapperJson(q.demandes)).append("\",\n");
            json.append("    \"xp\": ").append(q.xp).append(",\n");
            json.append("    \"col\": ").append(q.col).append(",\n");
            json.append("    \"autres\": \"").append(echapperJson(q.autres)).append("\",\n");
            json.append("    \"terminee\": ").append(q.terminee).append("\n");
            json.append("  }");
            if (i < quetes.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }
        json.append("]");

        try {
            Path parent = FICHIER_SAUVEGARDE.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(FICHIER_SAUVEGARDE, json.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            System.out.println("Erreur lors de la sauvegarde : " + e.getMessage());
        }
    }

    private static String extraireString(String jsonObject, String cle) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(cle) + "\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"").matcher(jsonObject);
        return matcher.find() ? desEchapperJson(matcher.group(1)) : "";
    }

    private static int extraireInt(String jsonObject, String cle) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(cle) + "\"\\s*:\\s*(\\d+)").matcher(jsonObject);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
    }

    private static boolean extraireBoolean(String jsonObject, String cle) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(cle) + "\"\\s*:\\s*(true|false)").matcher(jsonObject);
        return matcher.find() && Boolean.parseBoolean(matcher.group(1));
    }

    private static String echapperJson(String texte) {
        return texte
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "")
                .replace("\n", " ");
    }

    private static String desEchapperJson(String texte) {
        return texte
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    private static String raccourcir(String texte, int longueurMax) {
        if (texte == null || texte.length() <= longueurMax) {
            return texte;
        }
        return texte.substring(0, Math.max(0, longueurMax - 1)) + "…";
    }

    private static Path resoudreCheminSauvegarde() {
        Path dossierProjet = Paths.get("quest_tracker", NOM_FICHIER_SAUVEGARDE);
        if (Files.exists(dossierProjet) || Files.isDirectory(Paths.get("quest_tracker"))) {
            return dossierProjet;
        }
        return Paths.get(NOM_FICHIER_SAUVEGARDE);
    }

    private static void initialiserQuetesDefaut() {
        quetes.clear();
        quetes.add(new Quete(1, "Candélia", "Yannis", "x 2014 z 834", "32 Bûches de bois", 146, 300, ""));
        quetes.add(new Quete(2, "Candélia", "Gilmar", "x 1959 z 791", "10 Eclat Magique Glacial + 5 Fragments Cassé Violet", 1292, 1000, ""));
        quetes.add(new Quete(3, "Candélia", "Tomoko", "x 1955 z 814", "20 Ecorce Sylvestre + 5 Racines Ancestrales", 294, 1000, ""));
        quetes.add(new Quete(4, "Candélia", "Pierre", "x 2018 z 877", "20 Buches de Bois", 147, 250, ""));
        quetes.add(new Quete(5, "Candélia", "Roméo", "x 1991 z 832", "16 Peaux de Cerf des Montagnes", 291, 600, ""));
        quetes.add(new Quete(6, "Candélia", "Émilie", "x 1984 z 753", "20 Blé", 146, 200, ""));
        quetes.add(new Quete(7, "SVF", "Gilbert", "x 1700 z 1018", "15 Tissus Spectral", 145, 250, ""));
        quetes.add(new Quete(8, "SVF", "Horace", "x 3084 z 1913", "12 Cuirs Usés + 8 Peaux de Cerf des Montagnes", 141, 450, ""));
        quetes.add(new Quete(9, "SVF", "Haruto", "x 1868 z 2112", "30 Gelées de Slime + 25 Carapaces de Requin", 291, 750, ""));
        quetes.add(new Quete(10, "Virelune", "Juliette", "x 1565 z 1968", "16 Bûches de Chêne + 16 Bûches de Bouleau", 147, 300, ""));
        quetes.add(new Quete(11, "Virelune", "Luc", "x 1624 z 1852", "3 Pioches Félés", 105, 0, "24 Minerais de Fer"));
        quetes.add(new Quete(12, "Virelune", "Sam", "x 1600 z 2006", "Tuer 20 Araignées", 145, 820, ""));
        quetes.add(new Quete(13, "Virelune", "Monique", "x 1563 z 2000", "Tuer 10 Requins", 145, 890, ""));
        quetes.add(new Quete(14, "Jardin des Géants", "Zébulgarath", "x 372 z 2477", "3 Racines Ancestrales", 439, 1000, ""));
        quetes.add(new Quete(15, "Camp Militaire", "Jean", "x 3225 z 2891", "40 Bûches de Chêne + 20 Minérais de Fer", 293, 200, ""));
        quetes.add(new Quete(16, "Camp Militaire", "Corentin", "x 3291 z 2905", "64 Bûches de Chêne", 145, 200, ""));
        quetes.add(new Quete(17, "Camp Militaire", "Fira", "x 3396 z 2947", "1 Eclat Magique Glacial + 3 Poussières d'Os", 146, 400, ""));
        quetes.add(new Quete(18, "Mizunari", "Michelle", "x 3131 z 3666", "10 Fragments de Feuilles", 146, 150, ""));
        quetes.add(new Quete(19, "Mizunari", "Martine", "x 3150 y 3672", "16 épisodes sauvages", 145, 120, ""));
        quetes.add(new Quete(20, "Mizunari", "Elwyn", "x 3111 z 3703", "10 Ecorces Sylvestre + 10 Brindilles Enchantés + 3 Racines Ancestrales", 146, 600, ""));
        quetes.add(new Quete(21, "Mizunari", "Louise", "x 3147 z 3704", "10 Lingots de Cuivre", 146, 100, ""));
        quetes.add(new Quete(22, "Mizunari", "Phares", "x 3149 z 3714", "20 Bûches de Bois", 147, 150, ""));
        quetes.add(new Quete(23, "Valhatt", "Saya", "x 492 z 3028", "4 Tissus Spectral + 3 Brindilles Enchantées", 145, 400, ""));
        quetes.add(new Quete(24, "Valhatt", "Ayaka", "x 479 z 3013", "25 Pousses de Sylves + 5 Mycélium Magique", 434, 750, ""));
        quetes.add(new Quete(25, "Valhatt", "Daiki", "x 430 z 3046", "4 Peaux de Sanglier + 2 Crocs Albal", 146, 600, ""));
        quetes.add(new Quete(26, "Hanaka", "Genzo", "x 1532 z 3376", "6 Crocs de Loup + 2 Eclats de Bois Magique", 146, 600, ""));
        quetes.add(new Quete(27, "Hanaka", "Bartók", "x 1526 z 3377", "25 Peaux de Sanglier + 25 Crocs de Loup", 145, 500, ""));
        quetes.add(new Quete(28, "Hanaka", "Greta", "x 1438 z 3407", "15 Fleurs d'Allium + 5 Fils d'Araignée", 290, 650, ""));
        quetes.add(new Quete(29, "Hanaka", "Sœur-Therra", "x 1406 z 3435", "20 Gelées de Slime + 1 Essence de Gorbel", 436, 1500, ""));
        quetes.add(new Quete(30, "Hanaka", "Toban", "x 1356 z 3441", "5 Ecorces de Titan + 2 Mycélium Magique", 440, 750, ""));
        quetes.add(new Quete(31, "Hanaka", "Rina", "x 1502 z 3533", "4 Fleurs d'Allium + 2 Gelées de Slime", 145, 250, ""));
        quetes.add(new Quete(32, "Hanaka", "Maya", "x 1500 z 3560", "8 Brindilles Enchanté + 2 Gelée de Slime", 147, 500, ""));
        quetes.add(new Quete(33, "Ville de Départ", "Varn", "Inconnue", "4 Fourrures de Loup + 2 Eclats de Bois Magique + 1 Os de Squelette Renforcé", 146, 250, ""));
        quetes.add(new Quete(34, "Ville de Départ", "Milla", "x 2207 z 4187", "15 Fragments de Feuilles + 2 Mycélium Magique", 438, 2500, ""));
        quetes.add(new Quete(35, "Ville de Départ", "Inari", "x 1760 z 4736", "Armure complète Ika", 438, 2500, ""));
        quetes.add(new Quete(36, "Ville de Départ", "Orin", "x 1745 z 4724", "5 Minérais de Fer + 5 Corde d'Arc Sylvestre", 291, 300, ""));
        quetes.add(new Quete(37, "Ville de Départ", "Rikyu", "x 1556 z 4316", "25 Fragments de Feuilles + 8 Fleur d'Allium", 146, 500, ""));
        quetes.add(new Quete(38, "Ville de Départ", "Bunta", "x 1500 z 4328", "5 Eclats Bois Magique + 5 Poussière d'Os + 3 Noyau de Slime", 293, 500, ""));
        quetes.add(new Quete(39, "Ville de Départ", "Meiko", "x 1273 z 4308", "5 Tissus d'Araignée + 3 Cuir Usé", 147, 300, ""));
        quetes.add(new Quete(40, "Ville de Départ", "Saria", "x 1274 z 4317", "50 Gelées de Slime + 10 Minérais de Fer", 293, 1000, ""));
        quetes.add(new Quete(41, "Ville de Départ", "Tilda", "x 1883 z 4010", "1 Arc Courbé", 291, 200, ""));
        quetes.add(new Quete(42, "Ville de Départ", "Lilas", "x 1878 z 3992", "1 Coeur de Bois", 292, 150, ""));
        quetes.add(new Quete(43, "Ville de Départ", "Nacht", "x 1635 z 4039", "1 Coeur de Bois + 10 Pousses de sylves", 0, 0, "1 Pinceau Magique"));
    }
}
