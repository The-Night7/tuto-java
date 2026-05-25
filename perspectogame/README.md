# PerspectoGame: Point de Fuite

`perspectogame` est un jeu de puzzle 3D en Java avec libGDX. Le coeur du jeu repose sur des connexions impossibles entre plateformes : quand deux surfaces se superposent correctement dans la vue, la balle peut poursuivre sa route comme si un pont existait réellement.

## Modules

- `core` : logique du jeu, rendu, niveaux, physique et détection de perspective
- `lwjgl3` : lanceur desktop LWJGL3
- `assets` : tutoriel, niveaux et ressources du jeu

## Contrôles

- `Souris gauche + glisser` : orienter la caméra
- `Flèches` : déplacer la caméra pour inspecter tout le niveau
- `Espace` : démarrer / mettre en pause
- `Échap` : revenir au menu

## Commandes utiles

```bash
./gradlew core:compileJava
./gradlew lwjgl3:run
./gradlew lwjgl3:jar
```

## Release GitHub

La release courante est `v1.0.0`, nommée `PerspectoGame: Point de Fuite`.

- `CHANGELOG` : [`../CHANGELOG.md`](../CHANGELOG.md)
- notes de release : [`../.github/release-notes/v1.0.0.md`](../.github/release-notes/v1.0.0.md)
- workflow de publication : [`../.github/workflows/release.yml`](../.github/workflows/release.yml)
