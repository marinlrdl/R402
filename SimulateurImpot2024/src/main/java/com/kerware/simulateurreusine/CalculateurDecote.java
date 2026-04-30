package com.kerware.simulateurreusine;

/**
 * Calcule la décote applicable aux revenus modestes.
 * EXG_IMPOT_06 : mécanisme de réduction de l'impôt pour les contribuables modestes.
 */
class CalculateurDecote {

    // Seuils en dessous desquels la décote s'applique
    static final double SEUIL_SEUL = 1929.0;
    static final double SEUIL_COUPLE = 3191.0;

    // Montants maximaux de décote
    static final double DECOTE_MAX_SEUL = 873.0;
    static final double DECOTE_MAX_COUPLE = 1444.0;

    // Taux de réduction de la décote en fonction de l'impôt
    static final double TAUX_REDUCTION_DECOTE = 0.4525;

    /**
     * Calcule le montant de la décote à appliquer.
     * EXG_IMPOT_06 : la décote ne peut pas dépasser le montant de l'impôt.
     *
     * @param impotAvantDecote montant de l'impôt avant décote
     * @param nbDeclarants     1 pour personne seule, 2 pour couple
     * @return montant de la décote (0 si non applicable)
     */
    static int calculer(int impotAvantDecote, int nbDeclarants) {
        double seuil = nbDeclarants == 1 ? SEUIL_SEUL : SEUIL_COUPLE;
        double decoteMax = nbDeclarants == 1 ? DECOTE_MAX_SEUL : DECOTE_MAX_COUPLE;

        if (impotAvantDecote >= seuil) {
            return 0;
        }

        double decote = decoteMax - TAUX_REDUCTION_DECOTE * impotAvantDecote;
        int decoteArrondie = (int) Math.round(decote);

        // La décote ne peut pas dépasser le montant de l'impôt
        return Math.min(decoteArrondie, impotAvantDecote);
    }
}
