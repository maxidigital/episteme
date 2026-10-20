package de.bottazzi.episteme;

import de.bottazzi.episteme.engine.Credibility;
import de.bottazzi.episteme.engine.HypothesisIndex;
import de.bottazzi.episteme.model.NodeId;
import de.bottazzi.episteme.store.KnowledgeGraph;
import org.junit.jupiter.api.Test;

import java.util.List;

import static de.bottazzi.episteme.model.RelationType.COMPOSED_OF;
import static org.junit.jupiter.api.Assertions.*;

class HypothesisIndexTest {

    private static NodeId id(String s) { return NodeId.of(s); }

    /** Tres estructuras con una fuente cada una. */
    private static CaseBuilder three() {
        CaseBuilder b = new CaseBuilder();
        for (String s : new String[]{"A", "B", "C"})
            b.source("src" + s, "fuente " + s).fact("F" + s, "dato " + s).cites("F" + s, "src" + s, 0.9)
             .structure(s, "patron " + s).derive(s, "F" + s);
        return b;
    }

    private static HypothesisIndex index(CaseBuilder b) {
        KnowledgeGraph g = b.log.current();
        return new HypothesisIndex(g, new Credibility(g));
    }

    private static double p(CaseBuilder b, String n) { return new Credibility(b.log.current()).p(id(n)); }

    /** D28: explica suma, tensiona resta, dejar sin explicar no pesa. */
    @Test
    void indexByHand() {
        CaseBuilder b = three().hypothesis("H", "h").explains("H", "A", "B").inTensionWith("H", "C")
                .hypothesis("G", "g").explains("G", "A", "B").leavesUnexplained("G", "C");
        double expected = Math.log(0.2 / 0.8) + Credibility.W * (p(b, "A") + p(b, "B") - p(b, "C"));
        assertEquals(expected, index(b).of(id("H")).logit(), 1e-9);
        assertEquals(Math.log(0.2 / 0.8) + Credibility.W * (p(b, "A") + p(b, "B")), index(b).of(id("G")).logit(), 1e-9);
        assertEquals(List.of(id("C")), index(b).of(id("G")).unexplained());
    }

    /** D23: la compuesta explica lo de sus partes una vez; la parte que no explica nada propio sobra. */
    @Test
    void compositeCountsEachStructureOnceAndMarksTheRedundantPart() {
        CaseBuilder b = three().hypothesis("P", "p").explains("P", "A", "B")
                .hypothesis("Q", "q").explains("Q", "B")
                .hypothesis("R", "r").explains("R", "C")
                .hypothesis("PQ", "p y q").composedOf("PQ", "P", "Q")
                .hypothesis("PR", "p y r").composedOf("PR", "P", "R");
        HypothesisIndex idx = index(b);
        assertEquals(idx.of(id("P")).logit(), idx.of(id("PQ")).logit(), 1e-9, "Q no agrega nada");
        assertEquals(List.of(id("Q")), idx.of(id("PQ")).redundantParts());
        assertTrue(idx.of(id("PR")).redundantParts().isEmpty());
        assertEquals(List.of(id("A"), id("B"), id("C")), idx.of(id("PR")).explains());
        assertThrows(IllegalStateException.class, () -> b.composedOf("P", "PQ"));
        assertThrows(IllegalStateException.class, () -> b.rel("PR", COMPOSED_OF, "A", 1.0));
    }
}
