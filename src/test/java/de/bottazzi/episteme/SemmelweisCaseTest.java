package de.bottazzi.episteme;

import de.bottazzi.episteme.engine.Credibility;
import de.bottazzi.episteme.engine.HypothesisRanker;
import de.bottazzi.episteme.engine.HypothesisRanker.Verdict;
import de.bottazzi.episteme.model.NodeId;
import de.bottazzi.episteme.store.KnowledgeGraph;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SemmelweisCaseTest {

    private final HypothesisRanker ranker = new HypothesisRanker();
    private final SemmelweisCase.Stages stages = SemmelweisCase.seed();

    private KnowledgeGraph at(long v) { return stages.log().stateAt(v); }

    private List<HypothesisRanker.Report> rankAt(long v) { return ranker.rank(at(v)); }

    private static NodeId id(String s) { return NodeId.of(s); }

    @Test
    void earlyIn1847NoHypothesisWorks() {
        List<HypothesisRanker.Report> roh = rankAt(stages.early());
        assertEquals(4, roh.size());
        assertTrue(roh.stream().allMatch(r -> r.fit() < 0), "todas negativas: " + roh);
        assertTrue(roh.stream().allMatch(r -> r.unexplained().contains(id("S4"))));
        assertTrue(roh.stream().allMatch(r -> !r.predictionsWith(Verdict.REFUTED).isEmpty()));
    }

    @Test
    void allMortalityDataHasOneRoot() {
        KnowledgeGraph g = at(stages.early());
        assertEquals(1, g.independentRoots(id("F1")).size());
        assertEquals(2, at(stages.kolletschka()).independentRoots(id("F9")).size());
    }

    @Test
    void cadavericHypothesisLeadsAndLeavesHandwashingToTest() {
        HypothesisRanker.Report top = rankAt(stages.kolletschka()).get(0);
        assertEquals("H5", top.hypothesis().value());
        assertEquals(0.0, top.refutation(), 1e-9);
        assertEquals(List.of(id("P9")), top.predictionsWith(Verdict.PENDING));
    }

    @Test
    void handwashingConfirmsThePendingPrediction() {
        double before = rankAt(stages.kolletschka()).get(0).fit();
        HypothesisRanker.Report after = rankAt(stages.handwashing()).get(0);
        assertEquals("H5", after.hypothesis().value());
        assertTrue(after.fit() > before);
        assertTrue(after.predictionsWith(Verdict.PENDING).isEmpty());
    }

    @Test
    void methodologicalObjectionWeakensTheInterventionButNotTheHypothesisRanking() {
        Credibility before = new Credibility(at(stages.handwashing())), after = new Credibility(at(stages.objection()));
        assertTrue(after.p(id("S6")) < before.p(id("S6")));
        HypothesisRanker.Report top = rankAt(stages.objection()).get(0);
        assertEquals("H5", top.hypothesis().value());
        assertTrue(top.fit() < rankAt(stages.handwashing()).get(0).fit());
    }

    @Test
    void everyStageConverges() {
        for (long v : new long[]{stages.early(), stages.kolletschka(), stages.handwashing(), stages.objection()})
            assertTrue(new Credibility(at(v)).converged());
    }
}
