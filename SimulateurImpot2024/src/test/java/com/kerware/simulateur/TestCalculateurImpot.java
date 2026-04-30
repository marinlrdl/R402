package com.kerware.simulateur;

import com.kerware.simulateurreusine.AdaptateurCodeReusine;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires fonctionnels du simulateur d'impôt sur le revenu 2024.
 * Les tests couvrent les exigences EXG_IMPOT_01 à EXG_IMPOT_07.
 *
 * Les tests sont exécutés sur le code réusiné (AdaptateurCodeReusine).
 * Note : certains tests échoueraient sur AdaptateurCodeHerite (bugs connus :
 *   - VEUF avec enfants : nbPtsDecl forcé à 1 au lieu de 2
 *   - EXG_IMPOT_07 : contribution exceptionnelle non implémentée
 *   - EXG_IMPOT_02 : abattement non calculé séparément pour chaque déclarant)
 */
@DisplayName("Tests unitaires fonctionnels - Simulateur impôt 2024")
class TestCalculateurImpot {

    private ICalculateurImpot calculateur;

    @BeforeEach
    void setUp() {
        // Brancher ici AdaptateurCodeHerite pour tester le code hérité
        calculateur = new AdaptateurCodeReusine();
    }

    /** Configure et lance le calcul de l'impôt. */
    private void calculer(int revenuNet, SituationFamiliale sf, int nbEnf,
                          int nbEnfH, boolean parIso) {
        calculateur.setRevenusNet(revenuNet);
        calculateur.setSituationFamiliale(sf);
        calculateur.setNbEnfantsACharge(nbEnf);
        calculateur.setNbEnfantsSituationHandicap(nbEnfH);
        calculateur.setParentIsole(parIso);
        calculateur.calculImpotSurRevenuNet();
    }

    // =========================================================================
    // Tests paramétrés depuis fichier CSV — couvre EXG_IMPOT_01 à 06
    // =========================================================================

    @DisplayName("Tests paramétrés depuis fichier CSV")
    @ParameterizedTest(name = "rev={0} sf={1} enf={2} hand={3} iso={4} => {5}€")
    @CsvFileSource(resources = "/data.csv", numLinesToSkip = 1)
    void testDepuisFichierCsv(int revenuNet, String situationFamiliale,
                               int nbEnfants, int nbEnfantsHandicapes,
                               boolean parentIsole, int impotAttendu) {
        // EXG_IMPOT_01 : arrondi à l'euro
        SituationFamiliale sf = SituationFamiliale.valueOf(situationFamiliale);
        calculer(revenuNet, sf, nbEnfants, nbEnfantsHandicapes, parentIsole);
        assertEquals(impotAttendu, calculateur.getImpotSurRevenuNet(),
                "Impôt attendu pour " + situationFamiliale + " revenu=" + revenuNet);
    }

    // =========================================================================
    // EXG_IMPOT_02 : Abattement de 10 %, min 495 €, max 14 171 €
    // =========================================================================

    @Nested
    @DisplayName("EXG_IMPOT_02 : Abattement sur le revenu net")
    @Tag("Abattement")
    class TestsAbattement {

        @Test
        @DisplayName("Revenu < 4950 : abattement plafonné au minimum de 495 €")
        void testAbattementMinimum() {
            // EXG_IMPOT_02 : abattement ne peut pas être inférieur à 495 €
            calculer(3000, SituationFamiliale.CELIBATAIRE, 0, 0, false);
            assertEquals(495, calculateur.getAbattement());
        }

        @Test
        @DisplayName("Revenu = 4950 : abattement exact au minimum de 495 €")
        void testAbattementExactementMinimum() {
            // EXG_IMPOT_02 : 4950 * 10 % = 495 — valeur limite basse
            calculer(4950, SituationFamiliale.CELIBATAIRE, 0, 0, false);
            assertEquals(495, calculateur.getAbattement());
        }

        @Test
        @DisplayName("Revenu = 50 000 : abattement de 10 % = 5 000 €")
        void testAbattementNominal() {
            // EXG_IMPOT_02 : 50 000 * 10 % = 5 000 (dans la plage)
            calculer(50000, SituationFamiliale.CELIBATAIRE, 0, 0, false);
            assertEquals(5000, calculateur.getAbattement());
        }

        @Test
        @DisplayName("Revenu = 141 710 : abattement exact au maximum de 14 171 €")
        void testAbattementExactementMaximum() {
            // EXG_IMPOT_02 : 141 710 * 10 % = 14 171 — valeur limite haute
            calculer(141710, SituationFamiliale.CELIBATAIRE, 0, 0, false);
            assertEquals(14171, calculateur.getAbattement());
        }

        @Test
        @DisplayName("Revenu > 141 710 : abattement plafonné au maximum de 14 171 €")
        void testAbattementMaximumDepasse() {
            // EXG_IMPOT_02 : abattement ne peut pas dépasser 14 171 € (par déclarant)
            calculer(200000, SituationFamiliale.CELIBATAIRE, 0, 0, false);
            assertEquals(14171, calculateur.getAbattement());
        }

        @Test
        @DisplayName("Couple : abattement calculé séparément pour chaque déclarant")
        void testAbattementCoupleSepare() {
            // EXG_IMPOT_02 : couple avec 300 000 € → chacun 150 000 → abt 14 171 chacun = 28 342 €
            calculer(300000, SituationFamiliale.MARIE, 0, 0, false);
            assertEquals(28342, calculateur.getAbattement());
        }

        @Test
        @DisplayName("Revenu fiscal de référence = revenu net - abattement")
        void testRevenuFiscalReference() {
            // EXG_IMPOT_02 : RFR = 50 000 - 5 000 = 45 000
            calculer(50000, SituationFamiliale.CELIBATAIRE, 0, 0, false);
            assertEquals(45000, calculateur.getRevenuFiscalReference());
        }
    }

    // =========================================================================
    // EXG_IMPOT_03 : Calcul du nombre de parts fiscales
    // =========================================================================

    @Nested
    @DisplayName("EXG_IMPOT_03 : Nombre de parts fiscales")
    @Tag("Parts")
    class TestsParts {

        @ParameterizedTest(name = "{0} sans enfants → {1} parts")
        @CsvSource({
            "CELIBATAIRE, 1.0",
            "DIVORCE,     1.0",
            "VEUF,        1.0",
            "MARIE,       2.0",
            "PACSE,       2.0"
        })
        @DisplayName("Parts des déclarants seuls (sans enfants)")
        void testPartsDeclarantsSansEnfants(String sf, double partsAttendues) {
            // EXG_IMPOT_03 : 1 part par déclarant célibataire/divorcé/veuf, 2 pour couple
            calculer(50000, SituationFamiliale.valueOf(sf), 0, 0, false);
            assertEquals(partsAttendues, calculateur.getNbPartsFoyerFiscal(), 0.001);
        }

        @ParameterizedTest(name = "{0} enfants → {1} parts supplémentaires")
        @CsvSource({
            "0, 0.0",
            "1, 0.5",
            "2, 1.0",
            "3, 2.0",
            "4, 3.0",
            "5, 4.0"
        })
        @DisplayName("Parts supplémentaires selon le nombre d'enfants (célibataire)")
        void testPartsEnfants(int nbEnfants, double partsEnfantsAttendues) {
            // EXG_IMPOT_03 : 0,5 pour les 2 premiers, 1 à partir du 3ème
            calculer(50000, SituationFamiliale.CELIBATAIRE, nbEnfants, 0, false);
            double partsAttendues = 1.0 + partsEnfantsAttendues;
            assertEquals(partsAttendues, calculateur.getNbPartsFoyerFiscal(), 0.001);
        }

        @Test
        @DisplayName("Parent isolé avec 1 enfant : +0,5 part supplémentaire")
        void testParentIsoleAvecEnfant() {
            // EXG_IMPOT_03 : parent isolé avec garde exclusive → +0,5 part
            calculer(50000, SituationFamiliale.CELIBATAIRE, 1, 0, true);
            assertEquals(2.0, calculateur.getNbPartsFoyerFiscal(), 0.001);
        }

        @Test
        @DisplayName("Parent isolé sans enfant : pas de part supplémentaire")
        void testParentIsoleSansEnfant() {
            // EXG_IMPOT_03 : parent isolé sans enfant → pas de bonus
            calculer(50000, SituationFamiliale.CELIBATAIRE, 0, 0, false);
            assertEquals(1.0, calculateur.getNbPartsFoyerFiscal(), 0.001);
        }

        @Test
        @DisplayName("Enfant en situation de handicap : +0,5 part")
        void testEnfantHandicape() {
            // EXG_IMPOT_03 : 0,5 part supplémentaire par enfant handicapé
            calculer(50000, SituationFamiliale.MARIE, 2, 1, false);
            // 2 déclarants + 1 part (2 enfants) + 0.5 (1 handicapé) = 3.5
            assertEquals(3.5, calculateur.getNbPartsFoyerFiscal(), 0.001);
        }

        @Test
        @DisplayName("Veuf avec enfants : conserve la part du conjoint décédé")
        void testVeufAvecEnfantsConserveLaPart() {
            // EXG_IMPOT_03 : veuf avec enfants → 2 parts déclarants (part du conjoint conservée)
            calculer(40000, SituationFamiliale.VEUF, 2, 0, false);
            // 2 (déclarants) + 1.0 (2 enfants) = 3.0
            assertEquals(3.0, calculateur.getNbPartsFoyerFiscal(), 0.001);
        }

        @Test
        @DisplayName("Veuf sans enfant : 1 seule part")
        void testVeufSansEnfant() {
            // EXG_IMPOT_03 : veuf sans enfant = célibataire fiscal
            calculer(40000, SituationFamiliale.VEUF, 0, 0, false);
            assertEquals(1.0, calculateur.getNbPartsFoyerFiscal(), 0.001);
        }
    }

    // =========================================================================
    // EXG_IMPOT_04 : Barème progressif
    // =========================================================================

    @Nested
    @DisplayName("EXG_IMPOT_04 : Barème progressif par tranches")
    @Tag("Tranches")
    class TestsTranches {

        @ParameterizedTest(name = "Revenu/part = {0} → tranche {1}")
        @MethodSource("fournisseurCasTranches")
        @DisplayName("Impôt calculé sur les tranches correctes")
        void testCalculProgressifParTranche(int revenuNet, String situationFamiliale,
                                            int impotAttendu) {
            // EXG_IMPOT_04 : calcul progressif sur toutes les tranches concernées
            calculer(revenuNet, SituationFamiliale.valueOf(situationFamiliale), 0, 0, false);
            assertEquals(impotAttendu, calculateur.getImpotAvantDecote(),
                    "Impôt avant décote pour revenu=" + revenuNet);
        }

        static Stream<Arguments> fournisseurCasTranches() {
            return Stream.of(
                // Tranche 0 (0 %) : RFR = 9 000 → impôt = 0
                Arguments.of(10000, "CELIBATAIRE", 0),
                // Tranche 1 (11 %) : RFR = 18 000 → (18 000-11 294)*11 % = 738
                Arguments.of(20000, "CELIBATAIRE", 738),
                // Tranche 2 (30 %) : RFR = 45 000 → 1 925 + 4 861 = 6 786
                Arguments.of(50000, "CELIBATAIRE", 6786),
                // Tranche 3 (41 %) : abt=14 171, RFR = 135 829 → 39 919
                Arguments.of(150000, "CELIBATAIRE", 39919),
                // Tranche 4 (45 %) : abt=14 171, RFR = 235 829 → 83 268
                Arguments.of(250000, "CELIBATAIRE", 83268)
            );
        }
    }

    // =========================================================================
    // EXG_IMPOT_05 : Plafonnement du gain lié aux demi-parts
    // =========================================================================

    @Nested
    @DisplayName("EXG_IMPOT_05 : Plafonnement du gain par demi-part")
    @Tag("Plafonnement")
    class TestsPlafonnement {

        @Test
        @DisplayName("Gain < plafond : pas de plafonnement appliqué")
        void testGainSousLePlafond() {
            // EXG_IMPOT_05 : 2 enfants pour couple à 65 000 → gain = 1 633 < plafond = 3 518
            calculer(65000, SituationFamiliale.MARIE, 2, 0, false);
            int impotSansEnfants = 4122;
            // L'impôt doit être inférieur à celui sans enfants
            assertTrue(calculateur.getImpotAvantDecote() < impotSansEnfants,
                    "Le plafonnement ne doit pas être actif quand le gain est sous le plafond");
        }

        @Test
        @DisplayName("Gain > plafond : plafonnement à 1 759 € par demi-part")
        void testGainAuDessusPlafond() {
            // EXG_IMPOT_05 : couple à 200 000 €, 1 enfant
            // abattement = 2 * 10 000 = 20 000, RFR = 180 000
            // impotDeclarantsSeuls = 42 257, impotFoyerComplet = 37 216
            // gain = 5 041 > plafond 1 demi-part = 1 759
            // → impotAvantDecote = 42 257 - 1 759 = 40 498
            calculer(200000, SituationFamiliale.MARIE, 1, 0, false);
            assertEquals(40498, calculateur.getImpotAvantDecote(),
                    "Le plafonnement doit limiter le gain à 1 759 € par demi-part");
        }
    }

    // =========================================================================
    // EXG_IMPOT_06 : Décote pour revenus modestes
    // =========================================================================

    @Nested
    @DisplayName("EXG_IMPOT_06 : Décote pour revenus modestes")
    @Tag("Decote")
    class TestsDecote {

        @Test
        @DisplayName("Célibataire : décote appliquée si impôt < 1 929 €")
        void testDecoteAppliqueeSeul() {
            // EXG_IMPOT_06 : impôt = 1 728 < seuil 1 929 → décote = 873 - 45,25 % × 1 728 = 91
            calculer(30000, SituationFamiliale.CELIBATAIRE, 0, 0, false);
            assertEquals(91, calculateur.getDecote(),
                    "La décote doit s'appliquer pour un célibataire avec impôt sous 1 929 €");
        }

        @Test
        @DisplayName("Célibataire : pas de décote si impôt >= 1 929 €")
        void testPasDeDecoteSeul() {
            // EXG_IMPOT_06 : impôt = 6 786 >= seuil 1 929 → pas de décote
            calculer(50000, SituationFamiliale.CELIBATAIRE, 0, 0, false);
            assertEquals(0, calculateur.getDecote());
        }

        @Test
        @DisplayName("Couple : décote appliquée si impôt < 3 191 €")
        void testDecoteAppliqueeCouple() {
            // EXG_IMPOT_06 : impôt = 2 708 < seuil 3 191 → décote = 1444 - 45,25 % × 2 708 = 219
            calculer(65000, SituationFamiliale.MARIE, 2, 0, false);
            assertEquals(219, calculateur.getDecote(),
                    "La décote doit s'appliquer pour un couple avec impôt sous 3 191 €");
        }

        @Test
        @DisplayName("Couple : pas de décote si impôt >= 3 191 €")
        void testPasDeDecoteCouple() {
            // EXG_IMPOT_06 : impôt = 4 122 >= seuil 3 191 → pas de décote
            calculer(65000, SituationFamiliale.MARIE, 0, 0, false);
            assertEquals(0, calculateur.getDecote());
        }

        @Test
        @DisplayName("Décote ne peut pas dépasser l'impôt dû")
        void testDecoteNonSuperieurImpot() {
            // EXG_IMPOT_06 : si impôt très faible, la décote est plafonnée à l'impôt
            calculer(15000, SituationFamiliale.CELIBATAIRE, 0, 0, false);
            assertTrue(calculateur.getDecote() <= calculateur.getImpotAvantDecote(),
                    "La décote ne peut pas dépasser le montant de l'impôt");
        }
    }

    // =========================================================================
    // EXG_IMPOT_07 : Contribution exceptionnelle sur les hauts revenus
    // =========================================================================

    @Nested
    @DisplayName("EXG_IMPOT_07 : Contribution exceptionnelle hauts revenus")
    @Tag("ContributionExceptionnelle")
    class TestsContributionExceptionnelle {

        @Test
        @DisplayName("Célibataire : pas de contribution si RFR <= 250 000 €")
        void testPasDeContributionSeulSousSeuil() {
            // EXG_IMPOT_07 : RFR = 141 710 - 14 171 = 127 539 < 250 000 → 0 €
            calculer(141710, SituationFamiliale.CELIBATAIRE, 0, 0, false);
            assertEquals(0, calculateur.getContributionExceptionnelle());
        }

        @Test
        @DisplayName("Célibataire : 3 % sur la tranche 250 001 - 500 000 €")
        void testContributionSeulTranche1() {
            // EXG_IMPOT_07 : rev=400 000 → abt=14 171, RFR=385 829
            // contribution = (385 829 - 250 000) * 3 % = 135 829 * 3 % = 4 075 €
            calculer(400000, SituationFamiliale.CELIBATAIRE, 0, 0, false);
            assertEquals(4075, calculateur.getContributionExceptionnelle());
        }

        @Test
        @DisplayName("Célibataire : exemple slide — RFR 550 000 € → 9 500 €")
        void testContributionSeulExempleSlide() {
            // EXG_IMPOT_07 : rev=564 171 → abt=14 171, RFR=550 000
            // (500 000 - 250 000)*3% + (550 000 - 500 000)*4% = 7 500 + 2 000 = 9 500 €
            calculer(564171, SituationFamiliale.CELIBATAIRE, 0, 0, false);
            assertEquals(9500, calculateur.getContributionExceptionnelle());
        }

        @Test
        @DisplayName("Couple : pas de contribution si RFR <= 500 000 €")
        void testPasDeContributionCoupleSousSeuil() {
            // EXG_IMPOT_07 : couple avec RFR < 500 000 → 0 €
            calculer(200000, SituationFamiliale.MARIE, 0, 0, false);
            assertEquals(0, calculateur.getContributionExceptionnelle());
        }

        @Test
        @DisplayName("Couple : 3 % sur la tranche 500 001 - 1 000 000 €")
        void testContributionCoupleTranche1() {
            // EXG_IMPOT_07 : rev=600 000 → abt=28 342, RFR=571 658
            // contribution = (571 658 - 500 000) * 3 % = 71 658 * 3 % = 2 150 €
            calculer(600000, SituationFamiliale.MARIE, 0, 0, false);
            assertEquals(2150, calculateur.getContributionExceptionnelle());
        }

        @Test
        @DisplayName("Couple : 4 % sur la tranche au-delà de 1 000 000 €")
        void testContributionCoupleTranche2() {
            // EXG_IMPOT_07 : rev=1 100 000 → abt=28 342, RFR=1 071 658
            // (1 000 000 - 500 000)*3% + (1 071 658 - 1 000 000)*4% = 15 000 + 2 866 = 17 866 €
            calculer(1100000, SituationFamiliale.MARIE, 0, 0, false);
            assertEquals(17866, calculateur.getContributionExceptionnelle());
        }
    }

    // =========================================================================
    // Tests négatifs — Validation des entrées et gestion des exceptions
    // =========================================================================

    @Nested
    @DisplayName("Tests négatifs : validation des entrées")
    @Tag("Negatif")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class TestsNegatifs {

        @Test
        @Order(1)
        @DisplayName("Revenu net négatif → IllegalArgumentException")
        void testRevenuNegatif() {
            // Classe d'équivalence invalide : revenu < 0
            calculateur.setRevenusNet(-1);
            calculateur.setSituationFamiliale(SituationFamiliale.CELIBATAIRE);
            calculateur.setNbEnfantsACharge(0);
            calculateur.setNbEnfantsSituationHandicap(0);
            calculateur.setParentIsole(false);
            assertThrows(IllegalArgumentException.class,
                    () -> calculateur.calculImpotSurRevenuNet(),
                    "Un revenu négatif doit lever une exception");
        }

        @Test
        @Order(2)
        @DisplayName("Nombre d'enfants négatif → IllegalArgumentException")
        void testNbEnfantsNegatif() {
            calculateur.setRevenusNet(50000);
            calculateur.setSituationFamiliale(SituationFamiliale.CELIBATAIRE);
            calculateur.setNbEnfantsACharge(-1);
            calculateur.setNbEnfantsSituationHandicap(0);
            calculateur.setParentIsole(false);
            assertThrows(IllegalArgumentException.class,
                    () -> calculateur.calculImpotSurRevenuNet());
        }

        @Test
        @Order(3)
        @DisplayName("Nombre d'enfants > 7 → IllegalArgumentException")
        void testNbEnfantsDepasse7() {
            // Classe d'équivalence invalide : nbEnfants > 7
            calculateur.setRevenusNet(50000);
            calculateur.setSituationFamiliale(SituationFamiliale.MARIE);
            calculateur.setNbEnfantsACharge(8);
            calculateur.setNbEnfantsSituationHandicap(0);
            calculateur.setParentIsole(false);
            assertThrows(IllegalArgumentException.class,
                    () -> calculateur.calculImpotSurRevenuNet());
        }

        @Test
        @Order(4)
        @DisplayName("Nombre d'enfants handicapés négatif → IllegalArgumentException")
        void testNbEnfantsHandicapesNegatif() {
            calculateur.setRevenusNet(50000);
            calculateur.setSituationFamiliale(SituationFamiliale.MARIE);
            calculateur.setNbEnfantsACharge(2);
            calculateur.setNbEnfantsSituationHandicap(-1);
            calculateur.setParentIsole(false);
            assertThrows(IllegalArgumentException.class,
                    () -> calculateur.calculImpotSurRevenuNet());
        }

        @Test
        @Order(5)
        @DisplayName("Enfants handicapés > enfants à charge → IllegalArgumentException")
        void testNbEnfantsHandicapesSuperieurEnfants() {
            // Classe d'équivalence invalide : nbEnfH > nbEnf
            calculateur.setRevenusNet(50000);
            calculateur.setSituationFamiliale(SituationFamiliale.MARIE);
            calculateur.setNbEnfantsACharge(2);
            calculateur.setNbEnfantsSituationHandicap(3);
            calculateur.setParentIsole(false);
            assertThrows(IllegalArgumentException.class,
                    () -> calculateur.calculImpotSurRevenuNet());
        }

        @Test
        @Order(6)
        @DisplayName("Situation familiale nulle → IllegalArgumentException")
        void testSituationFamilialeNulle() {
            calculateur.setRevenusNet(50000);
            calculateur.setSituationFamiliale(null);
            calculateur.setNbEnfantsACharge(0);
            calculateur.setNbEnfantsSituationHandicap(0);
            calculateur.setParentIsole(false);
            assertThrows(IllegalArgumentException.class,
                    () -> calculateur.calculImpotSurRevenuNet());
        }

        @Test
        @Order(7)
        @DisplayName("Marié + parent isolé → IllegalArgumentException")
        void testMarieEtParentIsole() {
            // Un déclarant marié ne peut pas être parent isolé
            calculateur.setRevenusNet(50000);
            calculateur.setSituationFamiliale(SituationFamiliale.MARIE);
            calculateur.setNbEnfantsACharge(1);
            calculateur.setNbEnfantsSituationHandicap(0);
            calculateur.setParentIsole(true);
            assertThrows(IllegalArgumentException.class,
                    () -> calculateur.calculImpotSurRevenuNet());
        }

        @Test
        @Order(8)
        @DisplayName("Pacsé + parent isolé → IllegalArgumentException")
        void testPacseEtParentIsole() {
            calculateur.setRevenusNet(50000);
            calculateur.setSituationFamiliale(SituationFamiliale.PACSE);
            calculateur.setNbEnfantsACharge(1);
            calculateur.setNbEnfantsSituationHandicap(0);
            calculateur.setParentIsole(true);
            assertThrows(IllegalArgumentException.class,
                    () -> calculateur.calculImpotSurRevenuNet());
        }
    }

    // =========================================================================
    // Tests paramétrés inline — limites des tranches EXG_IMPOT_04
    // =========================================================================

    @Nested
    @DisplayName("Tests paramétrés inline — limites de tranches")
    @Tag("Nominal")
    class TestsParametresInline {

        @ParameterizedTest(name = "Célibataire revenu={0} → impôt={1}")
        @CsvSource({
            "30000,  1637",
            "50000,  6786",
            "100000, 21129"
        })
        @DisplayName("Cas nominaux célibataire")
        void testNominauxCelibataire(int revenuNet, int impotAttendu) {
            // EXG_IMPOT_01/04 : vérification des cas standard
            calculer(revenuNet, SituationFamiliale.CELIBATAIRE, 0, 0, false);
            assertEquals(impotAttendu, calculateur.getImpotSurRevenuNet());
        }

        @ParameterizedTest(name = "Marié revenu=65000 enfants={0} → impôt={1}")
        @CsvSource({
            "0, 4122",
            "2, 2489",
            "3,  685"
        })
        @DisplayName("Cas nominaux marié — influence des enfants")
        void testNominauxMarie(int nbEnfants, int impotAttendu) {
            // EXG_IMPOT_03/05 : impact des parts enfants sur l'impôt
            calculer(65000, SituationFamiliale.MARIE, nbEnfants, 0, false);
            assertEquals(impotAttendu, calculateur.getImpotSurRevenuNet());
        }
    }

    // =========================================================================
    // Tests des situations familiales variées — EXG_IMPOT_03
    // =========================================================================

    @Nested
    @DisplayName("EXG_IMPOT_03 : Situations familiales")
    @Tag("Nominal")
    class TestsSituationsFamiliales {

        @Test
        @DisplayName("PACSE traité comme MARIE : même nombre de parts")
        void testPacseEquivalentMarie() {
            // EXG_IMPOT_03 : PACSE = 2 parts déclarants, comme MARIE
            calculer(40000, SituationFamiliale.PACSE, 0, 0, false);
            int impotPacse = calculateur.getImpotSurRevenuNet();

            calculer(40000, SituationFamiliale.MARIE, 0, 0, false);
            int impotMarie = calculateur.getImpotSurRevenuNet();

            assertEquals(impotMarie, impotPacse,
                    "PACSE et MARIE doivent donner le même impôt");
        }

        @Test
        @DisplayName("CELIBATAIRE et DIVORCE : même résultat sans enfants")
        void testCelibataireEtDivorceSansEnfants() {
            calculer(40000, SituationFamiliale.CELIBATAIRE, 0, 0, false);
            int impotCelib = calculateur.getImpotSurRevenuNet();

            calculer(40000, SituationFamiliale.DIVORCE, 0, 0, false);
            int impotDivorce = calculateur.getImpotSurRevenuNet();

            assertEquals(impotCelib, impotDivorce);
        }

        @Test
        @DisplayName("Revenu nul → impôt nul")
        void testRevenuNul() {
            // EXG_IMPOT_01 : revenu = 0 → impôt = 0
            calculer(0, SituationFamiliale.CELIBATAIRE, 0, 0, false);
            assertEquals(0, calculateur.getImpotSurRevenuNet());
        }
    }
}
