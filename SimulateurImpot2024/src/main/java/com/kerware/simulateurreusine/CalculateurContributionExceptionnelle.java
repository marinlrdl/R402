package com.kerware.simulateurreusine;

/**
 * Calcule la contribution exceptionnelle sur les hauts revenus (CEHR).
 * EXG_IMPOT_07 : contribution progressive par tranches, différente selon le nombre de déclarants.
 */
class CalculateurContributionExceptionnelle {

    // Seuils pour personne seule
    static final int SEUIL_SEUL_TRANCHE_1 = 250000;
    static final int SEUIL_SEUL_TRANCHE_2 = 500000;
    static final int SEUIL_SEUL_TRANCHE_3 = 1000000;

    // Taux pour personne seule
    static final double TAUX_SEUL_TRANCHE_1 = 0.03;
    static final double TAUX_SEUL_TRANCHE_2 = 0.04;
    static final double TAUX_SEUL_TRANCHE_3 = 0.04;

    // Seuils pour couple soumis à imposition commune
    static final int SEUIL_COUPLE_TRANCHE_1 = 500000;
    static final int SEUIL_COUPLE_TRANCHE_2 = 1000000;

    // Taux pour couple
    static final double TAUX_COUPLE_TRANCHE_1 = 0.03;
    static final double TAUX_COUPLE_TRANCHE_2 = 0.04;

    /**
     * Calcule la contribution exceptionnelle sur les hauts revenus.
     * EXG_IMPOT_07 : barème progressif appliqué sur le revenu fiscal de référence.
     *
     * @param revenuFiscalReference revenu fiscal de référence total
     * @param nbDeclarants          1 pour personne seule, 2 pour couple
     * @return contribution exceptionnelle arrondie à l'euro
     */
    static int calculer(int revenuFiscalReference, int nbDeclarants) {
        if (nbDeclarants == 1) {
            return calculerPersonneSeule(revenuFiscalReference);
        }
        return calculerCouple(revenuFiscalReference);
    }

    private static int calculerPersonneSeule(int rfr) {
        double contribution = 0.0;

        // Tranche 250 001 € à 500 000 € : 3 %
        if (rfr > SEUIL_SEUL_TRANCHE_1) {
            int base = Math.min(rfr, SEUIL_SEUL_TRANCHE_2) - SEUIL_SEUL_TRANCHE_1;
            contribution += base * TAUX_SEUL_TRANCHE_1;
        }
        // Tranche 500 001 € à 1 000 000 € : 4 %
        if (rfr > SEUIL_SEUL_TRANCHE_2) {
            int base = Math.min(rfr, SEUIL_SEUL_TRANCHE_3) - SEUIL_SEUL_TRANCHE_2;
            contribution += base * TAUX_SEUL_TRANCHE_2;
        }
        // Tranche au-delà de 1 000 000 € : 4 %
        if (rfr > SEUIL_SEUL_TRANCHE_3) {
            int base = rfr - SEUIL_SEUL_TRANCHE_3;
            contribution += base * TAUX_SEUL_TRANCHE_3;
        }

        return (int) Math.round(contribution);
    }

    private static int calculerCouple(int rfr) {
        double contribution = 0.0;

        // Tranche 500 001 € à 1 000 000 € : 3 %
        if (rfr > SEUIL_COUPLE_TRANCHE_1) {
            int base = Math.min(rfr, SEUIL_COUPLE_TRANCHE_2) - SEUIL_COUPLE_TRANCHE_1;
            contribution += base * TAUX_COUPLE_TRANCHE_1;
        }
        // Tranche au-delà de 1 000 000 € : 4 %
        if (rfr > SEUIL_COUPLE_TRANCHE_2) {
            int base = rfr - SEUIL_COUPLE_TRANCHE_2;
            contribution += base * TAUX_COUPLE_TRANCHE_2;
        }

        return (int) Math.round(contribution);
    }
}
