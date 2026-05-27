import java.util.ArrayList;
import java.util.List;

// 1. Les Records (apparus dans Java 14)
// Parfait pour modéliser des données simples et immuables sans écrire de getters/setters.
record Produit(String nom, double prix, int stock) {
    
    // On peut quand même y ajouter des méthodes
    public boolean enRupture() {
        return stock <= 0;
    }
}

// 2. Une Classe classique
class Inventaire {
    // Encapsulation : la liste est privée
    private final List<Produit> produits = new ArrayList<>();

    public void ajouter(Produit p) {
        produits.add(p);
    }

    public void afficherEnStock() {
        // 3. L'API Stream (très puissant pour manipuler les collections)
        produits.stream()
                .filter(p -> !p.enRupture())
                .forEach(p -> System.out.println("- " + p.nom() + " : " + p.prix() + "€ (Stock: " + p.stock() + ")"));
    }
    
    public void afficherValeurTotale() {
        double total = produits.stream()
                               .mapToDouble(p -> p.prix() * p.stock())
                               .sum();
        System.out.println("\nValeur totale de l'inventaire : " + total + "€");
    }
}

// 4. Le point d'entrée
public class Main {
    // 'public static void main' est obligatoire pour lancer un programme Java
    public static void main(String[] args) {
        Inventaire inv = new Inventaire();

        inv.ajouter(new Produit("Clavier Mécanique", 89.99, 10));
        inv.ajouter(new Produit("Écran 144Hz", 250.00, 5));
        inv.ajouter(new Produit("Souris Gamer", 45.50, 0)); // En rupture

        System.out.println("Produits actuellement en stock :");
        inv.afficherEnStock();
        
        inv.afficherValeurTotale();
    }
}