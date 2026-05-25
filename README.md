# PerspectoGame: Point de Fuite

Jeu de puzzle 3D en Java/libGDX centré sur les illusions de perspective. La balle avance seule sur un parcours de blocs 3D, et le joueur doit faire pivoter la caméra pour aligner visuellement des plateformes impossibles.

## Contenu de la release `v1.0.0`

- moteur de déplacement à pas fixe pour éviter les ratés liés au framerate
- transitions impossibles par perspective avec point bleu de debug au point de passage
- chute naturelle hors plateforme
- caméra orientable à la souris et déplaçable avec les flèches
- tutoriel retravaillé
- 10 niveaux jouables en plus du tutoriel

## Contrôles

- `Souris gauche + glisser` : orienter la perspective
- `Flèches` : déplacer la caméra pour voir le parcours
- `Espace` : lancer la balle / mettre en pause
- `Échap` : retour au menu

## Lancer le jeu en local

```bash
cd perspectogame
./gradlew lwjgl3:run
```

## Construire le JAR desktop

```bash
cd perspectogame
./gradlew lwjgl3:jar
```

Le fichier généré est ensuite disponible dans `perspectogame/lwjgl3/build/libs/`.

## Publier la release GitHub

Le dépôt contient un workflow GitHub Actions qui construit le JAR et publie une release quand un tag `v*` est poussé.

```bash
git tag v1.0.0
git push origin v1.0.0
```

Les notes de release sont stockées dans `.github/release-notes/v1.0.0.md`.
