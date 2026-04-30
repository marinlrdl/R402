package com.kerware.simulateurreusine;

import com.kerware.simulateur.SituationFamiliale;

/**
 * Simulateur réusiné du calcul de l'impôt sur le revenu 2024.
 * Implémente l'ensemble des exigences EXG_IMPOT_01 à EXG_IMPOT_07.
 *
 * <p>Améliorations par rapport au code hérité :
 * <ul>
 *   <li>Responsabilités séparées en classes dédiées</li>
 *   <li>Constantes nommées à la place des nombres magiques</li>
 *   <li>Correction du bug VEUF (EXG_IMPOT_03)</li>
 *   <li>Correction du bug PACSE (EXG_IMPOT_03)</li>
 *   <li>Implémentation de EXG_IMPOT_07 (contribution exceptionnelle)</li>
 *   <li>Validation des entrées avec exceptions explicites</li>
 * </ul>
 */
public class Simulateur {

    // Plafonnement des demi-parts supplémentaires (EXG_IMPOT_05)
    static final double PLAFOND_GAIN_PAR_DEMI_PART = 1759.0;
    static final int NB_ENFANTS_MAX = 7;
    static final double UN_DEMI = 0.5;

    private int revenusNet;
    private SituationFamiliale situationFamiliale;
    private int nbEnfantsACharge;
    private int nbEnfantsHandicapes;
    private boolean parentIsole;

    // Résultats intermédiaires et finaux
    private int abattement;
    private int revenuFiscalReference;
    private double nbPartsDeclarants;
    private double nbPartsFoyer;
    private int impotDeclarantsSeuls;
    private int impotAvantDecote;
    private int decote;
    private int impotSurRevenuNet;
    private int contributionExceptionnelle;

    /**
     * Initialise le simulateur avec les données du foyer fiscal.
     *
     * @param revenusNet          revenu net total du foyer
     * @param situationFamiliale  situation familiale
     * @param nbEnfantsACharge    nombre d'enfants à charge (0 à 7)
     * @param nbEnfantsHandicapes nombre d'enfants en situation de handicap
     * @param parentIsole         vrai si parent isolé avec garde exclusive
     */
    public void initialiser(int revNet, SituationFamiliale sitFam,
                            int nbEnf, int nbEnfH, boolean parIsole) {
        validerEntrees(revNet, sitFam, nbEnf, nbEnfH, parIsole);
        this.revenusNet = revNet;
        this.situationFamiliale = sitFam;
        this.nbEnfantsACharge = nbEnf;
        this.nbEnfantsHandicapes = nbEnfH;
        this.parentIsole = parIsole;
    }

    /**
     * Déclenche le calcul complet de l'impôt.
     * EXG_IMPOT_01 : tous les montants sont arrondis à l'euro.
     */
    public void calculer() {
        nbPartsDeclarants = CalculateurParts.calculerPartsDeclarants(
                situationFamiliale, nbEnfantsACharge);
        nbPartsFoyer = CalculateurParts.calculerTotal(
                situationFamiliale, nbEnfantsACharge, nbEnfantsHandicapes, parentIsole);

        // EXG_IMPOT_02 : abattement 10%, min 495 €, max 14 171 € par déclarant
        abattement = CalculateurAbattement.calculerFoyer(revenusNet, (int) nbPartsDeclarants);
        revenuFiscalReference = revenusNet - abattement;

        // EXG_IMPOT_04 : barème progressif sans enfants (pour plafonnement EXG_IMPOT_05)
        impotDeclarantsSeuls = CalculateurImpotProgressif.calculerFoyer(
                revenuFiscalReference, nbPartsDeclarants);

        // EXG_IMPOT_04 : barème progressif avec toutes les parts
        int impotFoyerComplet = CalculateurImpotProgressif.calculerFoyer(
                revenuFiscalReference, nbPartsFoyer);

        // EXG_IMPOT_05 : plafonnement du gain lié aux demi-parts supplémentaires
        impotAvantDecote = appliquerPlafonnement(impotDeclarantsSeuls, impotFoyerComplet);

        // EXG_IMPOT_06 : décote pour revenus modestes
        decote = CalculateurDecote.calculer(impotAvantDecote, (int) nbPartsDeclarants);
        impotSurRevenuNet = impotAvantDecote - decote;

        // EXG_IMPOT_07 : contribution exceptionnelle sur les hauts revenus
        contributionExceptionnelle = CalculateurContributionExceptionnelle.calculer(
                revenuFiscalReference, (int) nbPartsDeclarants);
    }

    /**
     * Applique le plafonnement du gain lié aux demi-parts supplémentaires.
     * EXG_IMPOT_05 : le gain max par demi-part est de 1 759 € en 2024.
     */
    private int appliquerPlafonnement(int impotSansEnfants, int impotAvecEnfants) {
        double gainBrut = impotSansEnfants - impotAvecEnfants;
        double nbDemiPartsSupplementaires = (nbPartsFoyer - nbPartsDeclarants) / UN_DEMI;
        double plafond = nbDemiPartsSupplementaires * PLAFOND_GAIN_PAR_DEMI_PART;

        if (gainBrut >= plafond) {
            return (int) Math.round(impotSansEnfants - plafond);
        }
        return impotAvecEnfants;
    }

    // --- Validation des entrées ---

    private void validerEntrees(int revenu, SituationFamiliale sf, int nbEnf,
                                int nbEnfH, boolean parIsole) {
        if (sf == null) {
            throw new IllegalArgumentException("La situation familiale ne peut pas être nulle");
        }
        if (revenu < 0) {
            throw new IllegalArgumentException("Le revenu net ne peut pas être négatif");
        }
        if (nbEnf < 0) {
            throw new IllegalArgumentException("Le nombre d'enfants ne peut pas être négatif");
        }
        if (nbEnf > NB_ENFANTS_MAX) {
            throw new IllegalArgumentException("Le nombre d'enfants ne peut pas dépasser 7");
        }
        if (nbEnfH < 0) {
            throw new IllegalArgumentException(
                    "Le nombre d'enfants handicapés ne peut pas être négatif");
        }
        if (nbEnfH > nbEnf) {
            throw new IllegalArgumentException(
                    "Le nombre d'enfants handicapés ne peut pas dépasser le nombre d'enfants");
        }
        if (parIsole && (sf == SituationFamiliale.MARIE || sf == SituationFamiliale.PACSE)) {
            throw new IllegalArgumentException(
                    "Un déclarant marié ou pacsé ne peut pas être parent isolé");
        }
    }

    // --- Getters ---

    /** EXG_IMPOT_02 */
    public int getAbattement() {
        return abattement;
    }

    /** EXG_IMPOT_02 */
    public int getRevenuFiscalReference() {
        return revenuFiscalReference;
    }

    /** EXG_IMPOT_03 */
    public double getNbPartsFoyerFiscal() {
        return nbPartsFoyer;
    }

    /** EXG_IMPOT_05 */
    public int getImpotAvantDecote() {
        return impotAvantDecote;
    }

    /** EXG_IMPOT_06 */
    public int getDecote() {
        return decote;
    }

    /** EXG_IMPOT_04/05/06 */
    public int getImpotSurRevenuNet() {
        return impotSurRevenuNet;
    }

    /** EXG_IMPOT_07 */
    public int getContributionExceptionnelle() {
        return contributionExceptionnelle;
    }
}
