package com.kerware.simulateur;

/**
 *  Cette classe permet de simuler le calcul de l'impôt sur le revenu
 *  en France pour l'année 2024 sur les revenus de l'année 2023 pour
 *  des cas simples de contribuables célibataires, mariés, divorcés, veufs
 *  ou pacsés avec ou sans enfants à charge ou enfants en situation de handicap
 *  et parent isolé.
 *
 *  EXEMPLE DE CODE DE TRES MAUVAISE QUALITE FAIT PAR UN DEBUTANT
 *
 *  Pas de lisibilité, pas de commentaires, pas de tests
 *  Pas de documentation, pas de gestion des erreurs
 *  Pas de logique métier, pas de modularité
 *  Pas de gestion des exceptions, pas de gestion des logs
 *  Principe "Single Responsability" non respecté
 *  Pas de traçabilité vers les exigences métier
 *
 *  Pourtant ce code fonctionne correctement (avec quelques bugs résiduels)
 *  Il s'agit d'un "legacy" code qui est difficile à maintenir
 *  L'auteur n'a pas fourni de tests unitaires
 *
 *  MODIFICATIONS APPORTEES POUR RENDRE LE CODE TESTABLE :
 *  - Ajout des getters pour observer les valeurs intermédiaires (EXG observabilité)
 *  - Correction du bug PACSE (division par zéro)
 *  - Ajout d'un champ mImpAvantDecote
 *  - BUG CONNU : cas VEUF avec enfants -> nbPtsDecl reste à 1 (devrait être 2)
 **/

public class Simulateur {

    private int l00 = 0;
    private int l01 = 11294;
    private int l02 = 28797;
    private int l03 = 82341;
    private int l04 = 177106;
    private int l05 = Integer.MAX_VALUE;

    private int[] limites = new int[6];

    private double t00 = 0.0;
    private double t01 = 0.11;
    private double t02 = 0.3;
    private double t03 = 0.41;
    private double t04 = 0.45;

    private double[] taux = new double[5];

    private int lAbtMax = 14171;
    private int lAbtMin = 495;
    private double tAbt = 0.1;

    private double plafDemiPart = 1759;

    private double seuilDecoteDeclarantSeul = 1929;
    private double seuilDecoteDeclarantCouple = 3191;

    private double decoteMaxDeclarantSeul = 873;
    private double decoteMaxDeclarantCouple = 1444;
    private double tauxDecote = 0.4525;

    private int rNet = 0;
    private int nbEnf = 0;
    private int nbEnfH = 0;

    private double rFRef = 0;
    private double rImposable = 0;

    private double abt = 0;

    private double nbPtsDecl = 0;
    private double nbPts = 0;
    private double decote = 0;

    private double mImpDecl = 0;
    private double mImp = 0;
    // Ajouté pour rendre la valeur observable depuis les tests
    private double mImpAvantDecote = 0;

    private boolean parIso = false;


    public long calculImpot(int revNet, SituationFamiliale sitFam, int nbEnfants,
                             int nbEnfantsHandicapes, boolean parentIsol) {

        rNet = revNet;
        nbEnf = nbEnfants;
        nbEnfH = nbEnfantsHandicapes;
        parIso = parentIsol;

        limites[0] = l00;
        limites[1] = l01;
        limites[2] = l02;
        limites[3] = l03;
        limites[4] = l04;
        limites[5] = l05;

        taux[0] = t00;
        taux[1] = t01;
        taux[2] = t02;
        taux[3] = t03;
        taux[4] = t04;

        // EXG_IMPOT_02 : abattement de 10% (min 495, max 14171)
        // NON-CONFORMITE : calculé sur le revenu TOTAL du foyer avec un seul plafond,
        // alors que EXG_IMPOT_02 exige un calcul SÉPARÉ par déclarant (max 14171 chacun).
        // Pour un couple, le code hérité plafonne à 14171 au lieu de 2 × 14171 = 28342.
        // Corrigé dans CalculateurAbattement du code réusiné.
        abt = rNet * tAbt;
        if (abt > lAbtMax) {
            abt = lAbtMax;
        }
        if (abt < lAbtMin) {
            abt = lAbtMin;
        }

        rFRef = rNet - abt;

        // EXG_IMPOT_03 : parts déclarants
        // CORRECTIF : ajout du cas PACSE (était manquant -> division par 0)
        // BUG CONNU : VEUF avec enfants -> nbPtsDecl est forcé à 1 (ligne 133)
        switch (sitFam) {
            case CELIBATAIRE:
                nbPtsDecl = 1;
                break;
            case MARIE:
            case PACSE: // CORRECTIF : PACSE = 2 déclarants comme MARIE
                nbPtsDecl = 2;
                break;
            case DIVORCE:
                nbPtsDecl = 1;
                break;
            case VEUF:
                if (nbEnf == 0) {
                    nbPtsDecl = 1;
                } else {
                    nbPtsDecl = 2;
                }
                // BUG : cette ligne écrase le résultat du if ci-dessus
                nbPtsDecl = 1;
                break;
            default:
                nbPtsDecl = 1;
                break;
        }

        // EXG_IMPOT_03 : parts enfants à charge
        if (nbEnf <= 2) {
            nbPts = nbPtsDecl + nbEnf * 0.5;
        } else if (nbEnf > 2) {
            nbPts = nbPtsDecl + 1.0 + (nbEnf - 2);
        }

        // EXG_IMPOT_03 : parent isolé
        if (parIso) {
            if (nbEnf > 0) {
                nbPts = nbPts + 0.5;
            }
        }

        // EXG_IMPOT_03 : enfants handicapés
        nbPts = nbPts + nbEnfH * 0.5;

        // EXG_IMPOT_04 : impôt des déclarants seuls (sans enfants)
        rImposable = rFRef / nbPtsDecl;
        mImpDecl = 0;
        int i = 0;
        do {
            if (rImposable >= limites[i] && rImposable < limites[i + 1]) {
                mImpDecl += (rImposable - limites[i]) * taux[i];
                break;
            } else {
                mImpDecl += (limites[i + 1] - limites[i]) * taux[i];
            }
            i++;
        } while (i < 5);
        mImpDecl = mImpDecl * nbPtsDecl;
        mImpDecl = Math.round(mImpDecl);

        // EXG_IMPOT_04 : impôt foyer fiscal complet
        rImposable = rFRef / nbPts;
        mImp = 0;
        i = 0;
        do {
            if (rImposable >= limites[i] && rImposable < limites[i + 1]) {
                mImp += (rImposable - limites[i]) * taux[i];
                break;
            } else {
                mImp += (limites[i + 1] - limites[i]) * taux[i];
            }
            i++;
        } while (i < 5);
        mImp = mImp * nbPts;
        mImp = Math.round(mImp);

        // EXG_IMPOT_05 : plafonnement du gain lié aux demi-parts
        double baisseImpot = mImpDecl - mImp;
        double ecartPts = nbPts - nbPtsDecl;
        double plafond = (ecartPts / 0.5) * plafDemiPart;
        if (baisseImpot >= plafond) {
            mImp = mImpDecl - plafond;
        }

        // Sauvegarde de l'impôt avant décote pour les getters
        mImpAvantDecote = mImp;

        decote = 0;
        // EXG_IMPOT_06 : décote pour les revenus modestes
        if (nbPtsDecl == 1) {
            if (mImp < seuilDecoteDeclarantSeul) {
                decote = decoteMaxDeclarantSeul - (mImp * tauxDecote);
            }
        }
        if (nbPtsDecl == 2) {
            if (mImp < seuilDecoteDeclarantCouple) {
                decote = decoteMaxDeclarantCouple - (mImp * tauxDecote);
            }
        }
        decote = Math.round(decote);
        if (mImp <= decote) {
            decote = mImp;
        }

        mImp = mImp - decote;

        return Math.round(mImp);
    }

    // --- Getters ajoutés pour rendre le code observable depuis les tests ---

    /** EXG_IMPOT_02 : abattement appliqué */
    public int getAbattement() {
        return (int) Math.round(abt);
    }

    /** EXG_IMPOT_02 : revenu fiscal de référence */
    public int getRevenuFiscalReference() {
        return (int) Math.round(rFRef);
    }

    /** EXG_IMPOT_03 : nombre de parts du foyer fiscal */
    public double getNbPartsFoyerFiscal() {
        return nbPts;
    }

    /** EXG_IMPOT_05 : impôt avant décote */
    public int getImpotAvantDecote() {
        return (int) Math.round(mImpAvantDecote);
    }

    /** EXG_IMPOT_06 : montant de la décote */
    public int getDecote() {
        return (int) Math.round(decote);
    }

    /** Impôt final sur le revenu net */
    public int getImpotSurRevenuNet() {
        return (int) Math.round(mImp);
    }

    public static void main(String[] args) {
        Simulateur simulateur = new Simulateur();
        long impot = simulateur.calculImpot(65000, SituationFamiliale.MARIE, 3, 0, false);
        System.out.println("Impot sur le revenu net : " + impot);
        impot = simulateur.calculImpot(65000, SituationFamiliale.MARIE, 3, 1, false);
        System.out.println("Impot sur le revenu net : " + impot);
        impot = simulateur.calculImpot(35000, SituationFamiliale.DIVORCE, 1, 0, true);
        System.out.println("Impot sur le revenu net : " + impot);
        impot = simulateur.calculImpot(35000, SituationFamiliale.DIVORCE, 2, 0, true);
        System.out.println("Impot sur le revenu net : " + impot);
        impot = simulateur.calculImpot(50000, SituationFamiliale.DIVORCE, 3, 0, true);
        System.out.println("Impot sur le revenu net : " + impot);
        impot = simulateur.calculImpot(50000, SituationFamiliale.DIVORCE, 3, 1, true);
        System.out.println("Impot sur le revenu net : " + impot);
        impot = simulateur.calculImpot(200000, SituationFamiliale.CELIBATAIRE, 0, 0, false);
        System.out.println("Impot sur le revenu net : " + impot);
    }
}
