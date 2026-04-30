package com.kerware.simulateur;

/**
 * Interface commune pour le calcul de l'impôt sur le revenu 2024.
 * Permet de tester le code hérité et le code réusiné avec les mêmes tests.
 *
 * <p><b>Libertés prises par rapport au schéma UML du cours :</b>
 * <ul>
 *   <li>{@code setRevenusNet(int rn)} : le slide proposait deux méthodes distinctes
 *       {@code setRevenusNetDeclarant1} et {@code setRevenusNetDeclarant2}. On conserve
 *       une seule méthode (revenu total du foyer) car l'interface du projet fourni ne
 *       disposait que d'un seul point d'entrée. Conséquence : le calcul de l'abattement
 *       pour les couples est approximé (50/50) dans le code réusiné.</li>
 *   <li>{@code getNbPartsFoyerFiscal()} : retourne {@code double} et non {@code int}
 *       (l'interface originale retournait {@code int}), car les parts sont fractionnaires
 *       (1,5 ; 2,5…). Le schéma UML du cours indique bien {@code : double}.</li>
 *   <li>{@code getContributionExceptionnelle()} : méthode ajoutée à l'interface originale
 *       pour permettre de tester EXG_IMPOT_07 via l'interface commune. Elle retourne
 *       toujours 0 dans l'adaptateur du code hérité.</li>
 * </ul>
 */
public interface ICalculateurImpot {

    /**
     * Fixe le revenu net total du foyer fiscal (somme des deux déclarants).
     * SIMPLIFICATION : le cours proposait deux méthodes séparées (déclarant 1 et déclarant 2).
     * Ici, c'est le total combiné. Pour les couples, l'abattement par déclarant est approximé
     * en supposant une répartition égale (50/50). Voir EXG_IMPOT_02 et CalculateurAbattement.
     */
    void setRevenusNet(int rn);

    // EXG_IMPOT_03 : situation familiale
    void setSituationFamiliale(SituationFamiliale sf);

    // EXG_IMPOT_03 : nombre d'enfants à charge (0 à 7)
    void setNbEnfantsACharge(int nbe);

    // EXG_IMPOT_03 : nombre d'enfants en situation de handicap
    void setNbEnfantsSituationHandicap(int nbesh);

    // EXG_IMPOT_03 : parent isolé avec garde exclusive
    void setParentIsole(boolean pi);

    // Déclenche le calcul complet de l'impôt
    void calculImpotSurRevenuNet();

    // EXG_IMPOT_02 : revenu fiscal de référence (après abattement)
    int getRevenuFiscalReference();

    // EXG_IMPOT_02 : montant de l'abattement appliqué
    int getAbattement();

    /**
     * Retourne le nombre de parts fiscales du foyer.
     * MODIFICATION vs interface originale : retourne {@code double} au lieu de {@code int},
     * car les parts peuvent être fractionnaires (1,5 ; 2,5 ; 3,5...). EXG_IMPOT_03.
     */
    double getNbPartsFoyerFiscal();

    // EXG_IMPOT_05 : impôt avant application de la décote
    int getImpotAvantDecote();

    // EXG_IMPOT_06 : montant de la décote
    int getDecote();

    // EXG_IMPOT_04/05/06 : impôt sur le revenu net final (hors contribution exceptionnelle)
    int getImpotSurRevenuNet();

    /**
     * Retourne la contribution exceptionnelle sur les hauts revenus (EXG_IMPOT_07).
     * AJOUT : absente de l'interface originale. Introduite pour que les tests EXG_IMPOT_07
     * puissent s'exécuter via l'interface commune.
     * Dans AdaptateurCodeHerite, retourne toujours 0 (non implémentée dans le code hérité).
     */
    int getContributionExceptionnelle();
}
