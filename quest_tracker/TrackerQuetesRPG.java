import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class TrackerQuetesRPG {

    private static final String FICHIER_SAUVEGARDE = "sauvegarde_quetes.json";
    private static List<Quete> quetes = new ArrayList<>();

    // --- MODÈLE DE DONNÉES ---
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

        public Quete(int id, String zone, String npc, String coordonnees, String demandes, int xp, int col, String autres) {
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
    }

    // --- POINT D'ENTRÉE ---
    public static void main(String[] args) {
        chargerDonnees();
        Scanner scanner = new Scanner(System.in);
        boolean enCours = true;

        System.out.println("==========================================");
        System.out.println("   ⚔️  TRACKER D'ÉVOLUTION RPG EN JAVA ⚔️   ");
        System.out.println("==========================================");

        while (enCours) {
            System.out.println("\n--- MENU PRINCIPAL ---");
            System.out.println("1. Voir toutes les quêtes");
            System.out.println("2. Voir les quêtes en cours (non terminées)");
            System.out.println("3. Valider / Annuler une quête (par ID)");
            System.out.println("4. Voir mes statistiques (XP, Cols, Progression)");
            System.out.println("5. Rechercher par PNJ ou Zone");
            System.out.println("6. Quitter et Sauvegarder");
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
                    String terme = scanner.nextLine().toLowerCase();
                    afficherQuetes(rechercherQuetes(terme));
                    break;
                case "6":
                    System.out.println("Sauvegarde en cours...");
                    sauvegarderDonnees();
                    System.out.println("À bientôt !");
                    enCours = false;
                    break;
                default:
                    System.out.println("Choix invalide, veuillez réessayer.");
            }
        }
        scanner.close();
    }

    // --- LOGIQUE MÉTIER ---

    private static void basculerEtatQuete(int id) {
        for (Quete q : quetes) {
            if (q.id == id) {
                q.terminee = !q.terminee;
                System.out.println("✅ La quête de " + q.npc + " est maintenant : " + (q.terminee ? "TERMINÉE" : "EN COURS"));
                sauvegarderDonnees(); // Sauvegarde automatique à chaque changement
                return;
            }
        }
        System.out.println("❌ Aucune quête trouvée avec l'ID " + id);
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
            if (q.npc.toLowerCase().contains(terme) || q.zone.toLowerCase().contains(terme) || q.demandes.toLowerCase().contains(terme)) {
                resultats.add(q);
            }
        }
        return resultats;
    }

    private static void afficherQuetes(List<Quete> liste) {
        if (liste.isEmpty()) {
            System.out.println("Aucune quête à afficher.");
            return;
        }
        System.out.println("\n--- LISTE DES QUÊTES ---");
        for (Quete q : liste) {
            String statut = q.terminee ? "[X]" : "[ ]";
            System.out.printf("%s ID:%02d | Zone: %-18s | PNJ: %-12s | Demande: %s\n", statut, q.id, q.zone, q.npc, q.demandes);
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

        int pourcentage = (int) (((double) quetesTerminees / quetes.size()) * 100);

        System.out.println("\n--- 📊 VOS STATISTIQUES ---");
        System.out.println("Progression : " + quetesTerminees + " / " + quetes.size() + " (" + pourcentage + "%)");
        System.out.println("XP Gagnée   : ⭐ " + totalXp);
        System.out.println("Col Récolté : 💰 " + totalCol);
        System.out.println("---------------------------");
    }

    // --- GESTION DU JSON (SANS LIBRAIRIE EXTERNE) ---

    private static void chargerDonnees() {
        File fichier = new File(FICHIER_SAUVEGARDE);
        if (!fichier.exists()) {
            System.out.println("Aucun fichier de sauvegarde trouvé. Création de la base de données initiale...");
            initialiserQuetesDefaut();
            sauvegarderDonnees();
            return;
        }

        try {
            String contenu = new String(Files.readAllBytes(Paths.get(FICHIER_SAUVEGARDE)), StandardCharsets.UTF_8);
            
            // Expression régulière pour extraire chaque objet JSON de la liste
            Matcher matcherObjet = Pattern.compile("\\{([^}]+)\\}").matcher(contenu);
            
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
            System.out.println("✅ Données chargées avec succès (" + quetes.size() + " quêtes).");
        } catch (Exception e) {
            System.out.println("❌ Erreur lors du chargement : " + e.getMessage());
            initialiserQuetesDefaut(); // Fallback de sécurité
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
            Files.write(Paths.get(FICHIER_SAUVEGARDE), json.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            System.out.println("❌ Erreur lors de la sauvegarde : " + e.getMessage());
        }
    }

    // --- UTILITAIRES POUR LE PARSING JSON ---
    private static String extraireString(String jsonObject, String cle) {
        Matcher m = Pattern.compile("\"" + cle + "\"\\s*:\\s*\"([^\"]*)\"").matcher(jsonObject);
        return m.find() ? m.group(1) : "";
    }

    private static int extraireInt(String jsonObject, String cle) {
        Matcher m = Pattern.compile("\"" + cle + "\"\\s*:\\s*(\\d+)").matcher(jsonObject);
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }

    private static boolean extraireBoolean(String jsonObject, String cle) {
        Matcher m = Pattern.compile("\"" + cle + "\"\\s*:\\s*(true|false)").matcher(jsonObject);
        return m.find() ? Boolean.parseBoolean(m.group(1)) : false;
    }

    private static String echapperJson(String texte) {
        return texte.replace("\"", "\\\"").replace("\n", " ");
    }

    // --- BASE DE DONNÉES PAR DÉFAUT ---
    private static void initialiserQuetesDefaut() {
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