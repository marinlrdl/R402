# Simulateur d'impôt 2024

**Résumé**: Ce mini-projet implémente un simulateur d'impôt (calculateur) avec une batterie de tests unitaires et fonctionnels fournis pour valider le comportement.

**Auteurs**:
- Marin Lerondel--Touzé
- Hichem Chellal

**Structure du dépôt**:
- [src/main/java](src/main/java) : code source principal.
- [src/test/java](src/test/java) : tests unitaires et fonctionnels.
- [test-classes](test-classes) et [test/resources](test/resources) : ressources et jeux de données utilisés pour les tests (ex. data.csv).
- [surefire-reports](surefire-reports) : rapports de tests générés par Maven Surefire après exécution des tests.

**Remarque sur l'historique Git**: Si vous constatez qu'il n'y a qu'un seul commit dans l'historique, c'est parce que nous avons réalisé le projet ensemble sur le même PC en pair-programming (travail conjoint avec mon collègue).

**Prérequis**:
- Java (version compatible avec le projet)
- Maven

**Compiler et lancer les tests**:

```bash
mvn clean test
```

Les rapports de test détaillés sont disponibles dans le dossier [surefire-reports](surefire-reports) après exécution.

**Où trouver les consignes et la validation**:
- Les classes de test principales sont présentes dans [src/test/java](src/test/java) et couvrent notamment le calculateur d'impôt (voir les fichiers `TestCalculateurImpot` dans les rapports et le code de test).