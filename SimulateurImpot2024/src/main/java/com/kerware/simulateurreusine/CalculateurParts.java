package com.kerware.simulateurreusine;

import com.kerware.simulateur.SituationFamiliale;

/**
 * Calcule le nombre de parts fiscales du foyer.
 * EXG_IMPOT_03 : 1 part par déclarant, 0,5 part pour les 2 premiers enfants,
 * 1 part à partir du 3ème, 0,5 part par enfant handicapé, 0,5 part pour parent isolé,
 * le veuf avec enfants conserve la part du conjoint.
 */
class CalculateurParts {

    static final double PARTS_CELIBATAIRE = 1.0;
    static final double PARTS_COUPLE = 2.0;
    static final double PARTS_PAR_ENFANT_1_ET_2 = 0.5;
    static final double PARTS_PAR_ENFANT_3_ET_PLUS = 1.0;
    static final double PARTS_SUPPLEMENT_PARENT_ISOLE = 0.5;
    static final double PARTS_SUPPLEMENT_HANDICAP = 0.5;

    /**
     * Retourne le nombre de parts des déclarants (hors enfants).
     * EXG_IMPOT_03 : le veuf avec enfants conserve la part du conjoint décédé.
     *
     * @param situationFamiliale situation du foyer
     * @param nbEnfants          nombre d'enfants à charge
     * @return nombre de parts des déclarants
     */
    static double calculerPartsDeclarants(SituationFamiliale situationFamiliale, int nbEnfants) {
        switch (situationFamiliale) {
            case MARIE:
            case PACSE:
                return PARTS_COUPLE;
            case VEUF:
                // EXG_IMPOT_03 : le veuf conserve la part du conjoint décédé si des enfants
                return nbEnfants > 0 ? PARTS_COUPLE : PARTS_CELIBATAIRE;
            case CELIBATAIRE:
            case DIVORCE:
            default:
                return PARTS_CELIBATAIRE;
        }
    }

    /**
     * Retourne le nombre de parts apportées par les enfants.
     * EXG_IMPOT_03 : 0,5 pour les 2 premiers, 1 à partir du 3ème.
     *
     * @param nbEnfants nombre d'enfants à charge
     * @return parts supplémentaires pour les enfants
     */
    static double calculerPartsEnfants(int nbEnfants) {
        if (nbEnfants <= 0) {
            return 0.0;
        }
        if (nbEnfants <= 2) {
            return nbEnfants * PARTS_PAR_ENFANT_1_ET_2;
        }
        // 2 premiers enfants = 1.0 part, puis 1 part par enfant supplémentaire
        return 1.0 + (nbEnfants - 2) * PARTS_PAR_ENFANT_3_ET_PLUS;
    }

    /**
     * Calcule le nombre de parts total du foyer fiscal.
     *
     * @param situationFamiliale      situation du foyer
     * @param nbEnfants               enfants à charge
     * @param nbEnfantsHandicapes     enfants en situation de handicap
     * @param parentIsole             parent isolé avec garde exclusive
     * @return nombre total de parts fiscales
     */
    static double calculerTotal(SituationFamiliale situationFamiliale, int nbEnfants,
                                int nbEnfantsHandicapes, boolean parentIsole) {
        double parts = calculerPartsDeclarants(situationFamiliale, nbEnfants);
        parts += calculerPartsEnfants(nbEnfants);

        // EXG_IMPOT_03 : parent isolé avec au moins un enfant
        if (parentIsole && nbEnfants > 0) {
            parts += PARTS_SUPPLEMENT_PARENT_ISOLE;
        }

        // EXG_IMPOT_03 : enfants en situation de handicap
        parts += nbEnfantsHandicapes * PARTS_SUPPLEMENT_HANDICAP;

        return parts;
    }
}
