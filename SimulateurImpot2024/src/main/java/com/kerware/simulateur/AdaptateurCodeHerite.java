package com.kerware.simulateur;

/**
 * Adaptateur permettant d'utiliser le code hérité (Simulateur) via l'interface ICalculateurImpot.
 * Ajoute la validation des entrées et la gestion des exceptions.
 *
 * <p><b>Limitations héritées du code legacy :</b>
 * <ul>
 *   <li>EXG_IMPOT_02 : l'abattement est calculé sur le revenu total du foyer avec un seul
 *       plafond global, au lieu d'être calculé séparément pour chaque déclarant comme
 *       l'exige EXG_IMPOT_02. Pour les couples à hauts revenus, le résultat diffère
 *       du code réusiné.</li>
 *   <li>EXG_IMPOT_03 (bug VEUF) : le cas VEUF avec enfants retourne 1 part déclarant
 *       au lieu de 2. Ce bug est conservé intentionnellement dans le code hérité pour
 *       illustrer l'objectif des tests. Le test {@code testVeufAvecEnfantsConserveLaPart}
 *       échoue contre cet adaptateur.</li>
 *   <li>EXG_IMPOT_07 : la contribution exceptionnelle sur les hauts revenus n'est pas
 *       implémentée dans le code hérité. {@code getContributionExceptionnelle()} retourne 0.</li>
 * </ul>
 */
public class AdaptateurCodeHerite implements ICalculateurImpot {

    private final Simulateur simulateur = new Simulateur();

    private int revenusNet = 0;
    private SituationFamiliale situationFamiliale = null;
    private int nbEnfantsACharge = 0;
    private int nbEnfantsSituationHandicap = 0;
    private boolean parentIsole = false;

    @Override
    public void setRevenusNet(int rn) {
        this.revenusNet = rn;
    }

    @Override
    public void setSituationFamiliale(SituationFamiliale sf) {
        this.situationFamiliale = sf;
    }

    @Override
    public void setNbEnfantsACharge(int nbe) {
        this.nbEnfantsACharge = nbe;
    }

    @Override
    public void setNbEnfantsSituationHandicap(int nbesh) {
        this.nbEnfantsSituationHandicap = nbesh;
    }

    @Override
    public void setParentIsole(boolean pi) {
        this.parentIsole = pi;
    }

    @Override
    public void calculImpotSurRevenuNet() {
        validerEntrees();
        simulateur.calculImpot(revenusNet, situationFamiliale,
                nbEnfantsACharge, nbEnfantsSituationHandicap, parentIsole);
    }

    /** Validation des entrées — lance IllegalArgumentException si les données sont invalides */
    private void validerEntrees() {
        if (situationFamiliale == null) {
            throw new IllegalArgumentException("La situation familiale ne peut pas être nulle");
        }
        if (revenusNet < 0) {
            throw new IllegalArgumentException("Le revenu net ne peut pas être négatif");
        }
        if (nbEnfantsACharge < 0) {
            throw new IllegalArgumentException("Le nombre d'enfants ne peut pas être négatif");
        }
        if (nbEnfantsACharge > 7) {
            throw new IllegalArgumentException("Le nombre d'enfants ne peut pas dépasser 7");
        }
        if (nbEnfantsSituationHandicap < 0) {
            throw new IllegalArgumentException("Le nombre d'enfants handicapés ne peut pas être négatif");
        }
        if (nbEnfantsSituationHandicap > nbEnfantsACharge) {
            throw new IllegalArgumentException(
                    "Le nombre d'enfants handicapés ne peut pas dépasser le nombre d'enfants");
        }
        if (parentIsole && (situationFamiliale == SituationFamiliale.MARIE
                || situationFamiliale == SituationFamiliale.PACSE)) {
            throw new IllegalArgumentException(
                    "Un déclarant marié ou pacsé ne peut pas être parent isolé");
        }
    }

    @Override
    public int getRevenuFiscalReference() {
        return simulateur.getRevenuFiscalReference();
    }

    @Override
    public int getAbattement() {
        return simulateur.getAbattement();
    }

    @Override
    public double getNbPartsFoyerFiscal() {
        return simulateur.getNbPartsFoyerFiscal();
    }

    @Override
    public int getImpotAvantDecote() {
        return simulateur.getImpotAvantDecote();
    }

    @Override
    public int getDecote() {
        return simulateur.getDecote();
    }

    @Override
    public int getImpotSurRevenuNet() {
        return simulateur.getImpotSurRevenuNet();
    }

    /** EXG_IMPOT_07 : non implémenté dans le code hérité — retourne toujours 0 */
    @Override
    public int getContributionExceptionnelle() {
        return 0;
    }
}
