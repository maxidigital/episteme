package de.bottazzi.episteme.engine;

import de.bottazzi.episteme.model.*;
import de.bottazzi.episteme.store.KnowledgeGraph;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Indice de cada hipotesis, a partir de las estructuras con las que se relaciona (D28):
 * <pre>
 * L(H) = logit(NEUTRAL) + W * sum p(S que explica) - W * sum p(S con la que entra en tension)
 * </pre>
 * Las que deja sin explicar quedan registradas y se muestran, pero no mueven el numero. Como todo esta en
 * log-odds, el indice nunca llega a 0 ni a 1. Una hipotesis que no explica nada queda en NEUTRAL.
 *
 * <p>No es una probabilidad de que la hipotesis sea cierta ni un ranking: es cuanto de lo establecido
 * explica, pesado por la credibilidad de cada estructura.
 */
public final class HypothesisIndex {

    public record Report(NodeId hypothesis, double logit, List<NodeId> explains, List<NodeId> unexplained,
                         List<NodeId> tension) {
        public double p() { return LogOdds.p(logit); }
    }

    private final Map<NodeId, Report> reports = new LinkedHashMap<>();

    public HypothesisIndex(KnowledgeGraph g, Credibility c) {
        for (Node h : g.nodesOfKind(NodeKind.HYPOTHESIS)) {
            List<NodeId> explains = new ArrayList<>(), unexplained = new ArrayList<>(), tension = new ArrayList<>();
            for (Relation r : g.outgoing(h.id())) {
                switch (r.type()) {
                    case EXPLAINS -> explains.add(r.to());
                    case LEAVES_UNEXPLAINED -> unexplained.add(r.to());
                    case IN_TENSION_WITH -> tension.add(r.to());
                    default -> { }
                }
            }
            double logit = LogOdds.logit(Credibility.NEUTRAL)
                    + Credibility.W * sum(c, explains) - Credibility.W * sum(c, tension);
            reports.put(h.id(), new Report(h.id(), LogOdds.clamp(logit),
                    List.copyOf(explains), List.copyOf(unexplained), List.copyOf(tension)));
        }
    }

    private static double sum(Credibility c, List<NodeId> structures) {
        return structures.stream().mapToDouble(c::p).sum();
    }

    public Report of(NodeId h) {
        Report r = reports.get(h);
        if (r == null) throw new IllegalArgumentException("No es una hipotesis: " + h);
        return r;
    }

    /** Todas las hipotesis, en el orden en que se agregaron. */
    public List<Report> all() { return List.copyOf(reports.values()); }
}
