package de.bottazzi.episteme;

import de.bottazzi.episteme.engine.Credibility;
import de.bottazzi.episteme.model.*;
import org.junit.jupiter.api.Test;

import static de.bottazzi.episteme.model.RelationType.*;
import static org.junit.jupiter.api.Assertions.*;

class CredibilityTest {

    private static NodeId id(String s) { return NodeId.of(s); }

    private static double p(CaseBuilder b, String node) { return new Credibility(b.log.current()).p(id(node)); }

    @Test
    void unsupportedClaimIsWeakButNeverZero() {
        CaseBuilder b = new CaseBuilder().fact("F", "algo sin fuente");
        assertEquals(Credibility.NEUTRAL, p(b, "F"), 1e-9);
    }

    @Test
    void oneCitationByHand() {
        CaseBuilder b = new CaseBuilder().source("s", "fuente").fact("F", "dato").cites("F", "s", 0.9);
        double logit = Math.log(0.2 / 0.8) + Credibility.W * 0.9 * Credibility.NEUTRAL;
        assertEquals(1 / (1 + Math.exp(-logit)), p(b, "F"), 1e-9);
    }

    @Test
    void copiesCountOnceIndependentSourcesAdd() {
        CaseBuilder one = new CaseBuilder().source("s", "original").fact("F", "dato").cites("F", "s", 0.9);
        CaseBuilder copies = new CaseBuilder().source("s", "original")
                .copy("c1", "copia", "s").copy("c2", "copia de copia", "c1")
                .fact("F", "dato").cites("F", "s", 0.9).cites("F", "c1", 0.9).cites("F", "c2", 0.9);
        CaseBuilder independent = new CaseBuilder().source("s", "una").source("t", "otra")
                .fact("F", "dato").cites("F", "s", 0.9).cites("F", "t", 0.9);
        assertEquals(p(one, "F"), p(copies, "F"), 1e-9);
        assertTrue(p(independent, "F") > p(one, "F"));
    }

    @Test
    void cromwellRuleHoldsEvenAtTheExtremes() {
        CaseBuilder b = new CaseBuilder();
        for (int i = 0; i < 20; i++) b.source("s" + i, "fuente " + i);
        b.fact("F", "dato con veinte fuentes independientes");
        for (int i = 0; i < 20; i++) b.cites("F", "s" + i, 1.0);
        b.fact("G", "dato atacado por todos");
        for (int i = 0; i < 20; i++) b.fact("X" + i, "objecion " + i).cites("X" + i, "s" + i, 1.0).rel("X" + i, UNDERMINES, "G", 1.0);
        Credibility c = new Credibility(b.log.current());
        assertTrue(c.p(id("F")) < 1.0);
        assertTrue(c.p(id("G")) > 0.0);
        assertThrows(IllegalArgumentException.class, () -> LogOdds.logit(1.0));
        assertThrows(IllegalArgumentException.class, () -> LogOdds.logit(0.0));
    }

    @Test
    void underminingIsOneSided() {
        CaseBuilder b = new CaseBuilder().source("s", "a").source("t", "b")
                .fact("F", "dato").cites("F", "s", 0.9)
                .fact("O", "objecion").cites("O", "t", 0.9);
        double before = p(b, "O");
        b.rel("O", UNDERMINES, "F", 1.0);
        assertTrue(p(b, "F") < 0.5);
        assertEquals(before, p(b, "O"), 1e-9);
    }

    /** Un dato oficial contra estimaciones alternativas: gana el lado con mas raices independientes. */
    @Test
    void contradictionIsWonByIndependence() {
        CaseBuilder independent = officialVsAlternatives(false);
        assertTrue(p(independent, "ALT") > p(independent, "OFICIAL"));
        CaseBuilder copied = officialVsAlternatives(true);
        assertTrue(p(copied, "OFICIAL") > p(copied, "ALT"));
    }

    private static CaseBuilder officialVsAlternatives(boolean alternativesCopyEachOther) {
        CaseBuilder b = new CaseBuilder().source("oficial", "Organismo oficial de estadistica")
                .fact("OFICIAL", "La inflacion del anio fue X segun el organismo oficial").cites("OFICIAL", "oficial", 0.9)
                .source("fmi", "Auditoria externa")
                .fact("MET", "El organismo publica su metodologia y sus microdatos").cites("MET", "fmi", 0.9)
                .about("MET", SUPPORTS, "oficial", Aspect.COMPETENCE, 1.0);
        b.source("a1", "Oficina provincial de estadistica");
        if (alternativesCopyEachOther) {
            b.copy("a2", "Consultora privada que usa los datos provinciales", "a1")
             .copy("a3", "Indice de precios online que usa los mismos datos", "a1");
        } else {
            b.source("a2", "Consultora privada").source("a3", "Indice de precios online");
        }
        return b.fact("ALT", "La inflacion del anio fue bastante mayor que X")
                .cites("ALT", "a1", 0.9).cites("ALT", "a2", 0.9).cites("ALT", "a3", 0.9)
                .rel("ALT", CONTRADICTS, "OFICIAL", 1.0);
    }

    /** D05, D16: apoyos y objeciones que salen de la misma raiz cuentan una sola vez. */
    @Test
    void repeatedObjectionsFromOneRootCountOnce() {
        CaseBuilder one = objections(1, false), copies = objections(5, true), independent = objections(5, false);
        assertEquals(p(one, "F"), p(copies, "F"), 1e-9);
        assertTrue(p(independent, "F") < p(one, "F"));
    }

    @Test
    void repeatedSupportFromOneRootCountsOnce() {
        CaseBuilder b = new CaseBuilder().source("s", "fuente").fact("F", "dato")
                .fact("A1", "apoyo").cites("A1", "s", 0.9).rel("A1", SUPPORTS, "F", 1.0);
        double once = p(b, "F");
        b.copy("c", "copia", "s").fact("A2", "el mismo apoyo, copiado").cites("A2", "c", 0.9).rel("A2", SUPPORTS, "F", 1.0);
        assertEquals(once, p(b, "F"), 1e-9);
    }

    /** n objeciones contra F, cada una con su fuente; si copied, todas derivan de la primera. */
    private static CaseBuilder objections(int n, boolean copied) {
        CaseBuilder b = new CaseBuilder().source("s", "fuente").fact("F", "dato").cites("F", "s", 0.9);
        for (int i = 0; i < n; i++) {
            if (copied && i > 0) b.copy("o" + i, "repite", "o0");
            else b.source("o" + i, "objetor " + i);
            b.fact("X" + i, "objecion " + i).cites("X" + i, "o" + i, 0.8).rel("X" + i, UNDERMINES, "F", 0.5);
        }
        return b;
    }

    /** D16: una duda contra una de las observaciones que sostienen un facto le saca solo esa pata. */
    @Test
    void doubtAgainstAnObservationRemovesOnlyThatLeg() {
        CaseBuilder clean = twoWitnesses(), onObservation = twoWitnesses(), onFact = twoWitnesses();
        onObservation.rel("D", UNDERMINES, "O1", 1.0);
        onFact.rel("D", UNDERMINES, "F", 1.0);
        double before = p(clean, "F");
        assertTrue(p(onObservation, "F") < before);
        assertTrue(p(onObservation, "F") > p(onFact, "F"), "la otra pata sigue en pie");
        assertTrue(p(onObservation, "O1") < p(onObservation, "O2"));
    }

    private static CaseBuilder twoWitnesses() {
        return new CaseBuilder().source("w1", "testigo 1").source("w2", "testigo 2").source("v", "video")
                .observation("O1", "el testigo 1 vio el choque").cites("O1", "w1", 0.9)
                .observation("O2", "el testigo 2 vio el choque").cites("O2", "w2", 0.9)
                .fact("F", "hubo un choque").observedIn("F", "O1", "O2")
                .fact("D", "el testigo 1 estaba de espaldas").cites("D", "v", 0.9);
    }

    /** D15: una deduccion vale lo que su premisa mas debil y no le agrega raices a una estructura. */
    @Test
    void deductionAddsNoContent() {
        CaseBuilder b = new CaseBuilder().source("a", "camara 1").source("c", "camara 2")
                .fact("E1", "el primer evento fue a las 9:03").cites("E1", "a", 0.9)
                .fact("E2", "el segundo evento fue a las 9:59").cites("E2", "a", 0.9).cites("E2", "c", 0.9)
                .deduction("ANTES", "el primer evento ocurrio antes que el segundo", "E1", "E2")
                .structure("S", "patron con los dos eventos").derive("S", "E1", "E2");
        assertEquals(Math.min(p(b, "E1"), p(b, "E2")), p(b, "ANTES"), 1e-9);
        double without = p(b, "S");
        b.derive("S", "ANTES");
        assertEquals(without, p(b, "S"), 1e-9);
        assertThrows(IllegalStateException.class, () -> b.cites("ANTES", "a", 0.9));
    }

    @Test
    void sourcesAreDebatableToo() {
        CaseBuilder b = new CaseBuilder().source("s", "Organismo").fact("F", "dato").cites("F", "s", 0.9);
        double before = p(b, "F");
        b.source("t", "Auditoria").fact("A", "El organismo fue intervenido").cites("A", "t", 0.9)
         .about("A", UNDERMINES, "s", Aspect.SINCERITY, 1.0);
        assertTrue(p(b, "s") < Credibility.NEUTRAL);
        assertTrue(p(b, "F") < before);
    }

    /** D18: nadie arranca con credibilidad propia; una fuente se mueve solo por argumentos, y queda en el log. */
    @Test
    void sourcesStartNeutralAndMoveOnlyByArguments() {
        CaseBuilder b = new CaseBuilder().source("s", "testigo").source("t", "otro testigo");
        assertEquals(Credibility.NEUTRAL, p(b, "s"), 1e-9);
        assertEquals(p(b, "s"), p(b, "t"), 1e-9);
        b.source("u", "registro de la escuela").fact("V", "El testigo estaba a dos metros, de dia").cites("V", "u", 0.9)
         .about("V", SUPPORTS, "s", Aspect.COMPETENCE, 1.0);
        assertTrue(p(b, "s") > p(b, "t"));
        assertTrue(b.log.history(id("s")).size() >= 2);
        b.fact("X", "otro dato");
        assertThrows(IllegalStateException.class, () -> b.about("V", SUPPORTS, "X", Aspect.SINCERITY, 1.0));
    }

    @Test
    void cyclesConverge() {
        CaseBuilder b = new CaseBuilder().source("s", "fuente");
        b.fact("A", "a").cites("A", "s", 0.9).fact("B", "b").fact("C", "c")
         .rel("A", SUPPORTS, "B", 1.0).rel("B", SUPPORTS, "C", 1.0).rel("C", SUPPORTS, "A", 1.0)
         .rel("A", CONTRADICTS, "C", 0.5);
        Credibility c = new Credibility(b.log.current());
        assertTrue(c.converged(), "iteraciones " + c.iterations());
    }

    @Test
    void circularDerivationIsRejected() {
        CaseBuilder b = new CaseBuilder().structure("S1", "a").structure("S2", "b").rel("S1", DERIVED_FROM, "S2", 1.0);
        assertThrows(IllegalStateException.class, () -> b.rel("S2", DERIVED_FROM, "S1", 1.0));
        assertTrue(p(b, "S1") < 0.5, "una estructura armada sobre algo vacio sigue siendo poco creible");
    }

    /** D20: una estructura crece con sus elementos independientes y no vale lo que su pata mas debil. */
    @Test
    void structureGrowsWithIndependentElements() {
        CaseBuilder one = testimonies(1, false), three = testimonies(3, false), copies = testimonies(3, true);
        assertTrue(p(three, "S") > p(one, "S"));
        assertEquals(p(one, "S"), p(copies, "S"), 1e-9);
        assertTrue(p(three, "S") > p(three, "T0"), "no queda por debajo de su elemento mas debil");
    }

    /** n relatos con una fuente cada uno; si copied, todas las fuentes derivan de la primera. */
    private static CaseBuilder testimonies(int n, boolean copied) {
        CaseBuilder b = new CaseBuilder().structure("S", "los relatos coinciden");
        for (int i = 0; i < n; i++) {
            if (copied && i > 0) b.copy("s" + i, "testigo " + i, "s0");
            else b.source("s" + i, "testigo " + i);
            b.fact("T" + i, "relato " + i).cites("T" + i, "s" + i, 0.8).derive("S", "T" + i);
        }
        return b;
    }

    @Test
    void invalidRelationsAreRejectedAndNotLogged() {
        CaseBuilder b = new CaseBuilder().source("s", "fuente").fact("F", "dato")
                .hypothesis("H", "hipotesis").structure("S", "patron");
        long v = b.version();
        assertThrows(IllegalStateException.class, () -> b.rel("H", EXPLAINS, "S", 0.7));         // explicar no lleva peso
        assertThrows(IllegalStateException.class, () -> b.explains("H", "F"));                  // se explican estructuras
        assertThrows(IllegalStateException.class, () -> b.rel("H", CONTRADICTS, "F", 1.0));     // hipotesis contra evidencia
        assertThrows(IllegalStateException.class, () -> b.rel("H", SUPPORTS, "F", 1.0));        // hipotesis como evidencia
        assertThrows(IllegalStateException.class, () -> b.rel("F", DERIVED_FROM, "S", 1.0));    // un facto no deriva de un patron
        assertThrows(IllegalArgumentException.class, () -> Relation.of("r", "F", SUPPORTS, "S", 0.0));
        assertEquals(v, b.log.version());
        b.explains("H", "S");
        assertThrows(IllegalStateException.class, () -> b.inTensionWith("H", "S"));             // una relacion por par
    }
}
