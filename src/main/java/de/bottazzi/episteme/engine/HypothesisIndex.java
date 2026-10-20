package de.bottazzi.episteme.engine;

import de.bottazzi.episteme.model.*;
import de.bottazzi.episteme.store.KnowledgeGraph;

import java.util.*;

/**
 * Indice de cada hipotesis, a partir de las estructuras con las que se relaciona (D28):
 * <pre>
 * L(H) = logit(NEUTRAL) + W * sum p(S que explica) - W * sum p(S con la que entra en tension)
 * </pre>
 * Las que deja sin explicar quedan registradas y se muestran, pero no mueven el numero. Como todo esta en
 * log-odds, el indice nunca llega a 0 ni a 1. Una hipotesis que no explica nada queda en NEUTRAL.
 *
 * <p>Una hipotesis compuesta (COMPOSED_OF) explica, deja sin explicar y tensiona lo suyo mas lo de sus
 * partes, contando cada estructura una vez. Cada parte tiene que explicar al menos una estructura que las
 * otras no explican; si no, sobra y queda marcada en {@link Report#redundantParts()} (D23).
 *
 * <p>No es una probabilidad de que la hipotesis sea cierta ni un ranking: es cuanto de lo establecido
 * explica, pesado por la credibilidad de cada estructura.
 */
public final class HypothesisIndex {

    public record Report(NodeId hypothesis, double logit, List<NodeId> explains, List<NodeId> unexplained,
                         List<NodeId> tension, List<NodeId> parts, List<NodeId> redundantParts) {
        public double p() { return LogOdds.p(logit); }
    }

    private final KnowledgeGraph g;
    private final Map<NodeId, Report> reports = new LinkedHashMap<>();

    public HypothesisIndex(KnowledgeGraph g, Credibility c) {
        this.g = g;
        for (Node h : g.nodesOfKind(NodeKind.HYPOTHESIS)) {
            Set<NodeId> explains = collect(h.id(), RelationType.EXPLAINS);
            Set<NodeId> tension = collect(h.id(), RelationType.IN_TENSION_WITH);
            Set<NodeId> unexplained = collect(h.id(), RelationType.LEAVES_UNEXPLAINED);
            unexplained.removeAll(explains);
            List<NodeId> parts = targets(h.id(), RelationType.COMPOSED_OF);
            List<NodeId> redundant = new ArrayList<>();
            for (NodeId part : parts) {
                Set<NodeId> others = new HashSet<>();
                for (NodeId o : parts) if (!o.equals(part)) others.addAll(collect(o, RelationType.EXPLAINS));
                if (others.containsAll(collect(part, RelationType.EXPLAINS))) redundant.add(part);
            }
            double logit = LogOdds.logit(Credibility.NEUTRAL)
                    + Credibility.W * sum(c, explains) - Credibility.W * sum(c, tension);
            reports.put(h.id(), new Report(h.id(), LogOdds.clamp(logit), List.copyOf(explains),
                    List.copyOf(unexplained), List.copyOf(tension), parts, List.copyOf(redundant)));
        }
    }

    /** Destinos de las relaciones de ese tipo que salen de h y de sus partes, sin repetir. */
    private Set<NodeId> collect(NodeId h, RelationType type) {
        Set<NodeId> out = new LinkedHashSet<>(targets(h, type));
        for (NodeId part : targets(h, RelationType.COMPOSED_OF)) out.addAll(collect(part, type));
        return out;
    }

    private List<NodeId> targets(NodeId h, RelationType type) {
        return g.outgoing(h).stream().filter(r -> r.type() == type).map(Relation::to).toList();
    }

    private static double sum(Credibility c, Collection<NodeId> structures) {
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
