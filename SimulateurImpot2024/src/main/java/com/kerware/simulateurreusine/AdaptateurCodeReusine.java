package com.kerware.simulateurreusine;

import com.kerware.simulateur.ICalculateurImpot;
import com.kerware.simulateur.SituationFamiliale;

/**
 * Adaptateur qui connecte le code réusiné à l'interface ICalculateurImpot.
 * Permet d'exécuter les mêmes tests unitaires sur le code réusiné et sur le code hérité.
 */
public final class AdaptateurCodeReusine implements ICalculateurImpot {

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
        simulateur.initialiser(revenusNet, situationFamiliale,
                nbEnfantsACharge, nbEnfantsSituationHandicap, parentIsole);
        simulateur.calculer();
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

    @Override
    public int getContributionExceptionnelle() {
        return simulateur.getContributionExceptionnelle();
    }
}
