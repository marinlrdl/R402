package com.kerware.simulateurreusine;

/**
 * Calcule l'abattement sur le revenu net déclaré.
 * EXG_IMPOT_02 : abattement de 10%, minimum 495 €, maximum 14 171 €.
 */
class CalculateurAbattement {

    static final double TAUX_ABATTEMENT = 0.10;
    static final int ABATTEMENT_MIN = 495;
    static final int ABATTEMENT_MAX = 14171;

    /**
     * Calcule l'abattement pour un seul déclarant.
     *
     * @param revenuNetDeclarant revenu net du déclarant
     * @return abattement en euros, dans la plage [ABATTEMENT_MIN, ABATTEMENT_MAX]
     */
    static int calculerParDeclarant(int revenuNetDeclarant) {
        int abattement = (int) (revenuNetDeclarant * TAUX_ABATTEMENT);
        if (abattement < ABATTEMENT_MIN) {
            abattement = ABATTEMENT_MIN;
        }
        if (abattement > ABATTEMENT_MAX) {
            abattement = ABATTEMENT_MAX;
        }
        return abattement;
    }

    /**
     * Calcule l'abattement total du foyer.
     * Pour un seul déclarant, l'abattement s'applique au revenu total.
     * Pour un couple, on approxime en divisant par 2 (interface limitée à un seul revenu).
     *
     * @param revenuNetFoyer   revenu net total du foyer
     * @param nbDeclarants     1 pour célibataire/divorcé/veuf, 2 pour marié/pacsé
     * @return abattement total du foyer
     */
    static int calculerFoyer(int revenuNetFoyer, int nbDeclarants) {
        if (nbDeclarants == 1) {
            return calculerParDeclarant(revenuNetFoyer);
        }
        // EXG_IMPOT_02 : abattement calculé séparément pour chaque déclarant
        int revenuParDeclarant = revenuNetFoyer / nbDeclarants;
        int reste = revenuNetFoyer % nbDeclarants;
        int abattementDeclarant1 = calculerParDeclarant(revenuParDeclarant + reste);
        int abattementDeclarant2 = calculerParDeclarant(revenuParDeclarant);
        return abattementDeclarant1 + abattementDeclarant2;
    }
}
