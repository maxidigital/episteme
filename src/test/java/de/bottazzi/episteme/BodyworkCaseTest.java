package de.bottazzi.episteme;

import de.bottazzi.episteme.engine.Credibility;
import de.bottazzi.episteme.engine.HypothesisIndex;
import de.bottazzi.episteme.model.Node;
import de.bottazzi.episteme.model.NodeId;
import de.bottazzi.episteme.model.NodeKind;
import de.bottazzi.episteme.model.Relation;
import de.bottazzi.episteme.store.Event;
import de.bottazzi.episteme.store.KnowledgeGraph;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.Set;

import static de.bottazzi.episteme.model.RelationType.UNDERMINES;
import static org.junit.jupiter.api.Assertions.*;

class BodyworkCaseTest {

    private final BodyworkCase.Stages stages = BodyworkCase.seed();

    private static NodeId id(String s) { return NodeId.of(s); }

    /** La liberacion rapida es lo mas creible: es el unico facto con dos raices, Maxi y la bailarina. */
    @Test
    void fastReleaseIsTheMostCredibleFact() {
        KnowledgeGraph g = stages.log().stateAt(stages.answers());
        Credibility c = new Credibility(g);
        assertEquals(Set.of(id("src-practica"), id("src-bailarina")), g.rootsOf(id("F10")));
        NodeId top = g.nodesOfKind(NodeKind.FACT).stream().map(Node::id).max(Comparator.comparingDouble(c::p)).orElseThrow();
        assertEquals(id("F10"), top);
    }

    /** D16: dudar de lo que dijo la bailarina le saca a F10 esa pata y lo deja como el resto. */
    @Test
    void doubtingTheDancerRemovesOnlyHerLeg() {
        var log = stages.log();
        double before = new Credibility(log.current()).p(id("F10"));
        log.append("test", new Event.NodeAdded(Node.fact("D1", "La review la escribio a pedido, para el sitio")));
        log.append("test", new Event.RelationAdded(Relation.of("r-d1", "D1", UNDERMINES, "O10", 1.0)));
        Credibility c = new Credibility(log.current());
        assertTrue(c.p(id("F10")) < before);
        assertTrue(c.p(id("F10")) > c.p(id("F14")), "la pata de Maxi sigue en pie");
    }

    /** Limitacion conocida: dos hipotesis que explican lo mismo quedan iguales (diagnosticidad). */
    @Test
    void rivalsExplainingTheSameStructureTie() {
        KnowledgeGraph g = stages.log().stateAt(stages.answers());
        HypothesisIndex idx = new HypothesisIndex(g, new Credibility(g));
        assertEquals(idx.of(id("H08")).logit(), idx.of(id("H09")).logit(), 1e-12);
    }

    @Test
    void aHypothesisThatExplainsNothingStaysNeutral() {
        KnowledgeGraph g = stages.log().stateAt(stages.answers());
        HypothesisIndex.Report h06 = new HypothesisIndex(g, new Credibility(g)).of(id("H06"));
        assertEquals(Credibility.NEUTRAL, h06.p(), 1e-9);
        assertEquals(2, h06.unexplained().size());
    }
}
