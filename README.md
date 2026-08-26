# Jow_version_perso

Jow devenant payant, pourquoi ne pas créer sa propre version ? Application Android personnelle de gestion de recettes, liste de courses et cuisine, inspirée de Jow.

## Fonctionnalités

- **Bibliothèque de recettes personnalisable** : créer, modifier, supprimer des recettes (nom, portions, catégorie, description, ingrédients avec quantité/unité, étapes de préparation).
- **Saisie efficace** : les ingrédients sont autocomplétés à partir de ceux déjà utilisés (recherche en direct pendant la saisie), les quantités s'associent à une unité (g, kg, ml, cl, l, pièce, c. à soupe, c. à café, pincée).
- **Hub principal** avec deux parcours :
  - 🛒 **Faire les courses** : sélectionner les recettes à cuisiner (avec un multiplicateur de portions par recette) → génère une liste de courses agrégée (les quantités communes sont additionnées et converties dans une unité pertinente, ex. 300g + 0,2kg = 500g).
  - 🍳 **Recettes en cours** : une fois les courses cochées comme terminées, retrouver chaque recette sélectionnée avec ses quantités mises à l'échelle et ses étapes, et la marquer comme cuisinée.
- **Historique** des recettes cuisinées (date, portions).
- **Favoris** : marquer/filtrer les recettes favorites depuis la bibliothèque.

## Architecture

Projet Gradle multi-module :

- `core/` — logique métier pure Kotlin (conversion d'unités, agrégation de la liste de courses), sans dépendance Android, couverte par des tests JUnit (`./gradlew :core:test`).
- `app/` — application Android (Kotlin + Jetpack Compose + Material 3 + Navigation Compose + Room pour la persistance locale + un conteneur d'injection de dépendances minimal fait main).

Pas de backend : toutes les données sont stockées localement (SQLite via Room) sur l'appareil.

## Build & lancement

Prérequis : [Android Studio](https://developer.android.com/studio) (Koala ou plus récent) avec le SDK Android 34 installé, JDK 17.

```bash
./gradlew :core:test        # tests de la logique métier (rapides, sans SDK Android)
./gradlew :app:assembleDebug  # build de l'APK debug (nécessite le SDK Android)
```

Ou simplement ouvrir le dossier dans Android Studio et lancer l'app sur un émulateur/téléphone (`minSdk 26`, `targetSdk 34`).

> Remarque : ce projet a été développé dans un environnement sans accès au dépôt Maven Google (`dl.google.com`), donc `app/` n'a pas pu être compilé dans cet environnement ; seule la logique métier du module `core` a été buildée et testée. La compilation complète de `app/` doit être vérifiée dans Android Studio.
