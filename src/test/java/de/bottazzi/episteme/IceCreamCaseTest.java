package de.bottazzi.episteme;

import de.bottazzi.episteme.engine.ConflictDetector;
import de.bottazzi.episteme.engine.ConflictDetector.Kind;
import de.bottazzi.episteme.engine.Credibility;
import de.bottazzi.episteme.engine.HypothesisRanker;
import de.bottazzi.episteme.engine.HypothesisRanker.Verdict;
import de.bottazzi.episteme.engine.Propagator;
import de.bottazzi.episteme.model.NodeId;
import de.bottazzi.episteme.model.NodeKind;
import de.bottazzi.episteme.store.KnowledgeGraph;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class IceCreamCaseTest {

    private final HypothesisRanker ranker = new HypothesisRanker();
    private final IceCreamCase.Stages stages = IceCreamCase.seed();

    private static NodeId id(String s) { return NodeId.of(s); }

    private KnowledgeGraph at(long v) { return stages.log().stateAt(v); }

    private Map<String, HypothesisRanker.Report> byId(KnowledgeGraph g) {
        return ranker.rank(g).stream().collect(Collectors.toMap(r -> r.hypothesis().value(), Function.identity()));
    }

    private List<String> order(KnowledgeGraph g) {
        return ranker.rank(g).stream().map(r -> r.hypothesis().value()).toList();
    }

    @Test
    void baseCommonCauseWinsAndCoincidenceIsUntested() {
        KnowledgeGraph g = at(stages.base());
        assertEquals(List.of("H2", "H3", "H1"), order(g));
        var r = byId(g);
        assertEquals(1.0, r.get("H2").fit(), 1e-9);
        assertEquals(List.of(id("P5")), r.get("H3").predictionsWith(Verdict.PENDING));
        assertEquals(List.of(id("P2")), r.get("H1").predictionsWith(Verdict.REFUTED));
        assertTrue(r.get("H1").fit() < 0);
    }

    @Test
    void copiedPressIsOneIndependentSource() {
        var h1 = byId(at(stages.base())).get("H1");
        assertEquals(3, h1.citedSources());
        assertEquals(1, h1.independentSources());
    }

    @Test
    void objectingToTheCrampStudyHelpsTheIceCreamHypothesis() {
        Credibility before = new Credibility(at(stages.base())), after = new Credibility(at(stages.f4Challenged()));
        assertTrue(after.p(id("F4")) < 0.5 && after.p(id("F4")) > 0);
        assertEquals(List.of("H2", "H1", "H3"), order(at(stages.f4Challenged())));
        var h1 = byId(at(stages.f4Challenged())).get("H1");
        assertEquals(Verdict.PENDING, h1.predictions().stream().filter(p -> p.prediction().equals(id("P2"))).findFirst().orElseThrow().verdict());
        assertTrue(h1.refutation() > 0, "una refutacion debil sigue restando algo");
        assertEquals(before.p(id("F1")), after.p(id("F1")), 1e-9);
    }

    @Test
    void objectingToTemperatureLeavesItsPredictionsPendingNotDead() {
        KnowledgeGraph g = at(stages.f3Challenged());
        Credibility c = new Credibility(g);
        assertTrue(c.p(id("F3")) < 0.5 && c.p(id("F3")) > 0);
        assertTrue(c.p(id("S2")) <= c.p(id("F3")) + 1e-6);              // un patron no es mas solido que su pata mas debil
        var h2 = byId(g).get("H2");
        assertEquals(List.of(id("P3"), id("P4")), h2.predictionsWith(Verdict.PENDING));
        assertTrue(byId(g).get("H1").coverage() > byId(at(stages.base())).get("H1").coverage());
    }

    @Test
    void noValueIsEverZeroOrOne() {
        for (long v : new long[]{stages.base(), stages.f4Challenged(), stages.f3Challenged()}) {
            KnowledgeGraph g = at(v);
            Credibility c = new Credibility(g);
            assertTrue(c.converged());
            g.nodes().stream().filter(n -> n.kind().isEvidence()).forEach(n -> {
                double p = c.p(n.id());
                assertTrue(p > 0 && p < 1, n.id() + " = " + p);
            });
        }
    }

    @Test
    void conflictsAreClassified() {
        Map<String, ConflictDetector.Conflict> base = new ConflictDetector().detect(at(stages.base()), 0).stream()
                .collect(Collectors.toMap(ConflictDetector.Conflict::id, Function.identity()));
        assertEquals(Kind.REFUTED_PREDICTION, base.get("P2").kind());
        assertEquals(id("F4"), base.get("P2").b());
        assertEquals(Kind.RIVAL_HYPOTHESES, base.get("r-h1-contradicts-h2").kind());
        var later = new ConflictDetector().detect(at(stages.f3Challenged()), 0);
        assertTrue(later.stream().anyMatch(c -> c.kind() == Kind.UNDERMINED && c.b().equals(id("F3"))));
    }

    @Test
    void propagationFromASourceReachesHypotheses() {
        Map<String, Double> impacts = new Propagator().propagate(at(stages.base()), id("src-meteo"), 0.1).stream()
                .collect(Collectors.toMap(i -> i.node().value(), Propagator.Impact::impact));
        assertEquals(0.95, impacts.get("F3"), 1e-9);
        assertTrue(impacts.containsKey("S2") && impacts.containsKey("P3") && impacts.containsKey("H2"));
        assertFalse(impacts.containsKey("S1"));
        assertFalse(impacts.containsKey("H3"));
    }

    @Test
    void stagesAreSnapshotsAndNothingIsDeleted() {
        assertThrows(IllegalStateException.class, () -> at(stages.base()).node(id("F6")));
        long facts = at(stages.f3Challenged()).nodesOfKind(NodeKind.FACT).size();
        assertEquals(6, facts);
        assertTrue(stages.log().history(id("F3")).size() >= 3);
    }
}
