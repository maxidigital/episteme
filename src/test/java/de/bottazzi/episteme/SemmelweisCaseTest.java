package de.bottazzi.episteme;

import de.bottazzi.episteme.engine.ConflictDetector;
import de.bottazzi.episteme.engine.ConflictDetector.Kind;
import de.bottazzi.episteme.engine.Credibility;
import de.bottazzi.episteme.engine.HypothesisIndex;
import de.bottazzi.episteme.engine.Propagator;
import de.bottazzi.episteme.model.NodeId;
import de.bottazzi.episteme.store.KnowledgeGraph;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class SemmelweisCaseTest {

    private final SemmelweisCase.Stages stages = SemmelweisCase.seed();

    private KnowledgeGraph at(long v) { return stages.log().stateAt(v); }

    private HypothesisIndex indexAt(long v) {
        KnowledgeGraph g = at(v);
        return new HypothesisIndex(g, new Credibility(g));
    }

    private long[] all() { return new long[]{stages.early(), stages.kolletschka(), stages.handwashing(), stages.objection()}; }

    private static NodeId id(String s) { return NodeId.of(s); }

    @Test
    void earlyIn1847NoHypothesisExplainsTheCadavers() {
        List<HypothesisIndex.Report> hs = indexAt(stages.early()).all();
        assertEquals(4, hs.size());
        assertTrue(hs.stream().allMatch(r -> r.unexplained().contains(id("S4"))));
        assertTrue(hs.stream().allMatch(r -> !r.tension().isEmpty()), "todas chocan con algo");
        HypothesisIndex.Report miasma = indexAt(stages.early()).of(id("H1"));
        assertTrue(miasma.explains().isEmpty());
        assertTrue(miasma.p() < Credibility.NEUTRAL, "el miasma solo tiene tensiones");
    }

    @Test
    void allMortalityDataHasOneRoot() {
        assertEquals(Set.of(id("src-reg")), at(stages.early()).rootsOf(id("F1")));
        assertEquals(Set.of(id("src-reg")), at(stages.early()).rootsOf(id("S1")));
        assertEquals(2, at(stages.kolletschka()).rootsOf(id("F9")).size());
    }

    @Test
    void cadavericHypothesisExplainsWhatTheOthersLeaveUnexplained() {
        HypothesisIndex idx = indexAt(stages.kolletschka());
        HypothesisIndex.Report h5 = idx.of(id("H5"));
        assertTrue(h5.explains().contains(id("S4")));
        assertTrue(h5.tension().isEmpty());
        for (String other : new String[]{"H1", "H2", "H3", "H4"})
            assertTrue(h5.logit() > idx.of(id(other)).logit(), other);
    }

    @Test
    void handwashingAddsAnExplainedStructure() {
        HypothesisIndex.Report before = indexAt(stages.kolletschka()).of(id("H5"));
        HypothesisIndex.Report after = indexAt(stages.handwashing()).of(id("H5"));
        assertTrue(after.explains().contains(id("S6")));
        assertTrue(after.logit() > before.logit());
    }

    @Test
    void methodologicalObjectionWeakensTheInterventionButNotTheHypothesis() {
        Credibility before = new Credibility(at(stages.handwashing())), after = new Credibility(at(stages.objection()));
        assertTrue(after.p(id("S6")) < before.p(id("S6")));
        HypothesisIndex.Report h5 = indexAt(stages.objection()).of(id("H5"));
        assertTrue(h5.logit() < indexAt(stages.handwashing()).of(id("H5")).logit());
        assertTrue(h5.p() > 0.99, "no se apoya solo en S6");
    }

    @Test
    void everyStageConverges() {
        for (long v : all()) assertTrue(new Credibility(at(v)).converged());
    }

    @Test
    void noValueIsEverZeroOrOne() {
        for (long v : all()) {
            KnowledgeGraph g = at(v);
            Credibility c = new Credibility(g);
            g.nodes().stream().filter(n -> n.kind().isEvidence()).forEach(n -> {
                double p = c.p(n.id());
                assertTrue(p > 0 && p < 1, n.id() + " = " + p);
            });
            new HypothesisIndex(g, c).all().forEach(r -> assertTrue(r.p() > 0 && r.p() < 1, r.hypothesis() + " = " + r.p()));
        }
    }

    @Test
    void conflictsAreClassified() {
        Map<String, ConflictDetector.Conflict> c = new ConflictDetector().detect(at(stages.objection()), 0).stream()
                .collect(Collectors.toMap(ConflictDetector.Conflict::id, x -> x));
        assertEquals(Kind.HYPOTHESIS_IN_TENSION, c.get("r-h1-in_tension_with-s1").kind());
        assertEquals(Kind.RIVAL_HYPOTHESES, c.get("r-h5-contradicts-h1").kind());
        assertFalse(c.containsKey("r-f11-undermines-s6"), "la Gazette no tiene argumentos: su objecion no es creible");
    }

    @Test
    void propagationFromASourceReachesHypotheses() {
        Map<String, Double> impacts = new Propagator().propagate(at(stages.handwashing()), id("src-aut"), 0.1).stream()
                .collect(Collectors.toMap(i -> i.node().value(), Propagator.Impact::impact));
        assertTrue(impacts.containsKey("F9") && impacts.containsKey("S5") && impacts.containsKey("H5"));
        assertFalse(impacts.containsKey("H2"));
        assertFalse(impacts.containsKey("F1"));
    }

    @Test
    void historyIsKept() {
        assertTrue(stages.log().history(id("S6")).size() >= 3);
    }
}
