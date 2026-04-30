package com.kerware.simulateurreusine;

/**
 * Calcule l'impôt selon le barème progressif 2024.
 * EXG_IMPOT_04 : tranches progressives appliquées sur le revenu par part.
 */
class CalculateurImpotProgressif {

    // Bornes des tranches d'imposition (en euros)
    static final int BORNE_TRANCHE_0 = 0;
    static final int BORNE_TRANCHE_1 = 11294;
    static final int BORNE_TRANCHE_2 = 28797;
    static final int BORNE_TRANCHE_3 = 82341;
    static final int BORNE_TRANCHE_4 = 177106;

    // Taux d'imposition par tranche
    static final double TAUX_TRANCHE_0 = 0.00;
    static final double TAUX_TRANCHE_1 = 0.11;
    static final double TAUX_TRANCHE_2 = 0.30;
    static final double TAUX_TRANCHE_3 = 0.41;
    static final double TAUX_TRANCHE_4 = 0.45;

    private static final int[] BORNES = {
        BORNE_TRANCHE_0, BORNE_TRANCHE_1, BORNE_TRANCHE_2,
        BORNE_TRANCHE_3, BORNE_TRANCHE_4, Integer.MAX_VALUE
    };

    private static final double[] TAUX = {
        TAUX_TRANCHE_0, TAUX_TRANCHE_1, TAUX_TRANCHE_2, TAUX_TRANCHE_3, TAUX_TRANCHE_4
    };

    /**
     * Calcule l'impôt pour un revenu imposable donné (pour une part).
     * EXG_IMPOT_04 : le calcul est progressif sur toutes les tranches.
     *
     * @param revenuImposableParPart revenu imposable pour une part
     * @return impôt calculé pour cette part (non arrondi)
     */
    static double calculerParPart(double revenuImposableParPart) {
        double impot = 0.0;
        for (int i = 0; i < TAUX.length; i++) {
            if (revenuImposableParPart <= BORNES[i]) {
                break;
            }
            double baseImposable = Math.min(revenuImposableParPart, BORNES[i + 1]) - BORNES[i];
            impot += baseImposable * TAUX[i];
        }
        return impot;
    }

    /**
     * Calcule l'impôt total du foyer pour un nombre de parts donné.
     *
     * @param revenuFiscalReference revenu fiscal de référence du foyer
     * @param nbParts               nombre de parts fiscales
     * @return impôt total arrondi à l'euro
     */
    static int calculerFoyer(int revenuFiscalReference, double nbParts) {
        double revenuParPart = revenuFiscalReference / nbParts;
        double impotParPart = calculerParPart(revenuParPart);
        return (int) Math.round(impotParPart * nbParts);
    }
}
