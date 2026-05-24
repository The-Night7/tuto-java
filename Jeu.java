import java.util.Scanner;
import java.util.Random;

// 1. Classe abstraite : le squelette de base pour tout être vivant dans le jeu
abstract class Combattant {
    // 'protected' : accessible par la classe elle-même et ses classes filles (Heros, Monstre)
    protected String nom; 
    protected int pv;
    protected int force;
    protected Random random = new Random();

    public Combattant(String nom, int pv, int force) {
        this.nom = nom;
        this.pv = pv;
        this.force = force;
    }

    public boolean estVivant() {
        return pv > 0;
    }

    public String getNom() { return nom; }
    
    public void recevoirDegats(int degats) {
        this.pv -= degats;
        if (this.pv < 0) this.pv = 0;
        System.out.println(this.nom + " subit " + degats + " dégâts. (PV restants : " + this.pv + ")");
    }

    // Méthode abstraite : on force les classes filles à définir comment elles attaquent
    public abstract void attaquer(Combattant cible);
}

// 2. Le Monstre hérite de Combattant
class Monstre extends Combattant {
    public Monstre(String nom, int pv, int force) {
        super(nom, pv, force); // Appelle le constructeur de la classe parente
    }

    @Override
    public void attaquer(Combattant cible) {
        // Dégâts de base + une variation aléatoire entre 0 et 4
        int degats = force + random.nextInt(5); 
        System.out.println("👾 " + this.nom + " frappe " + cible.getNom() + " !");
        cible.recevoirDegats(degats);
    }
}

// 3. Le Héros hérite de Combattant
class Heros extends Combattant {
    public Heros(String nom, int pv, int force) {
        super(nom, pv, force);
    }

    @Override
    public void attaquer(Combattant cible) {
        int degats = force + random.nextInt(10);
        
        // Mécanique de RNG : 20% de chance de faire un coup critique (dégâts x2)
        if (random.nextInt(100) < 20) {
            System.out.println("💥 COUP CRITIQUE !");
            degats *= 2;
        }
        System.out.println("⚔️ " + this.nom + " attaque " + cible.getNom() + " !");
        cible.recevoirDegats(degats);
    }

    // Capacité spéciale unique au Héros
    public void seSoigner() {
        int soin = 20 + random.nextInt(15);
        this.pv += soin;
        System.out.println("✨ " + this.nom + " se soigne de " + soin + " PV. (PV actuels : " + this.pv + ")");
    }
}

// 4. Moteur du jeu
public class Jeu {
    public static void main(String[] args) {
        // Scanner permet de lire les entrées clavier dans le terminal
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("=== 🗡️ ARENE DE COMBAT ===");
        System.out.print("Entrez le nom de votre héros : ");
        String nomHeros = scanner.nextLine();

        Heros joueur = new Heros(nomHeros, 100, 15);
        Monstre boss = new Monstre("Golem d'Obsidienne", 150, 12);

        System.out.println("\nUn terrifiant " + boss.getNom() + " apparaît !");

        // La Game Loop (Boucle de jeu) : tourne tant que les deux sont en vie
        while (joueur.estVivant() && boss.estVivant()) {
            System.out.println("\n--- À vous de jouer ---");
            System.out.println("1. Attaquer");
            System.out.println("2. Se soigner");
            System.out.print("> ");

            String choix = scanner.nextLine();

            // Tour du joueur
            if (choix.equals("1")) {
                joueur.attaquer(boss);
            } else if (choix.equals("2")) {
                joueur.seSoigner();
            } else {
                System.out.println("Action invalide, vous trébuchez et perdez votre tour !");
            }

            // Riposte du monstre (s'il n'est pas mort de ton attaque)
            if (boss.estVivant()) {
                System.out.println();
                boss.attaquer(joueur);
            }
        }

        // Fin de partie
        System.out.println("\n=== FIN DU COMBAT ===");
        if (joueur.estVivant()) {
            System.out.println("🏆 Victoire ! Vous avez terrassé le boss.");
        } else {
            System.out.println("💀 Défaite... L'arène sera votre tombeau.");
        }

        scanner.close(); // Toujours fermer les flux ouverts
    }
}