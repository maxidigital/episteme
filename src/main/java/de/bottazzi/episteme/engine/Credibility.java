package de.bottazzi.episteme.engine;

import de.bottazzi.episteme.model.*;
import de.bottazzi.episteme.store.KnowledgeGraph;

import java.util.*;

/**
 * Credibilidad de toda la evidencia (fuentes, factos, observaciones, estructuras), calculada como
 * punto fijo, al estilo PageRank. Nada se ingresa a mano salvo la confianza de base de las fuentes.
 *
 * <p>Todo se suma en log-odds, asi que p nunca llega a 0 ni a 1 (regla de Cromwell):
 * <pre>
 * base(fuente)      = logit(prior)
 * base(facto)       = logit(PRIOR_CLAIM)      + citas
 * base(observacion) = logit(PRIOR_OBSERVATION) + citas
 * base(estructura)  = logit(min sobre lo que la sostiene de p * fuerza) + citas
 * citas             = sum por raiz independiente de W * max(calidad * p(fuente))
 * L(n)              = base(n) + sum apoyos W*k*p(x) - sum objeciones W*k*p(x) - sum contradicciones W*k*p(x)
 * </pre>
 * Tres diarios que copian el mismo comunicado aportan una sola cita (una raiz).
 * Una afirmacion sin fuentes ni apoyos queda en PRIOR_CLAIM: poco creible, pero no cero.
 *
 * <p>Se itera con amortiguacion hasta que ningun valor cambia mas que {@link #TOLERANCE}.
 */
public final class Credibility {

    /** Peso de una pieza de evidencia de credibilidad 1 y fuerza 1, en log-odds. */
    public static final double W = 4.0;
    public static final double PRIOR_CLAIM = 0.2;
    public static final double PRIOR_OBSERVATION = 0.1;
    public static final double DAMPING = 0.5;
    public static final double TOLERANCE = 1e-10;
    public static final int MAX_ITERATIONS = 1000;

    private final KnowledgeGraph g;
    private final List<NodeId> evidence;
    private final Map<NodeId, Double> logit = new HashMap<>();
    private final int iterations;
    private final boolean converged;

    public Credibility(KnowledgeGraph g) {
        this.g = g;
        this.evidence = g.nodes().stream().filter(n -> n.kind().isEvidence()).map(Node::id).toList();
        for (NodeId id : evidence) logit.put(id, initial(id));

        int it = 0;
        boolean done = false;
        while (it < MAX_ITERATIONS && !done) {
            it++;
            Map<NodeId, Double> next = new HashMap<>();
            double delta = 0;
            for (NodeId id : evidence) {
                double old = logit.get(id);
                double v = LogOdds.clamp((1 - DAMPING) * target(id) + DAMPING * old);
                next.put(id, v);
                delta = Math.max(delta, Math.abs(v - old));
            }
            logit.putAll(next);
            done = delta < TOLERANCE;
        }
        this.iterations = it;
        this.converged = done;
    }

    private double initial(NodeId id) {
        Node n = g.node(id);
        return switch (n.kind()) {
            case SOURCE -> LogOdds.logit(g.source(id).prior());
            case OBSERVATION -> LogOdds.logit(PRIOR_OBSERVATION);
            default -> LogOdds.logit(PRIOR_CLAIM);
        };
    }

    private double target(NodeId id) {
        Node n = g.node(id);
        double base = switch (n.kind()) {
            case SOURCE -> LogOdds.logit(g.source(id).prior());
            case FACT -> LogOdds.logit(PRIOR_CLAIM) + citations(id);
            case OBSERVATION -> LogOdds.logit(PRIOR_OBSERVATION) + citations(id);
            case STRUCTURE -> derived(id) + citations(id);
            case PREDICTION, HYPOTHESIS -> throw new IllegalStateException("No es evidencia: " + id);
        };
        double push = 0;
        for (Relation r : g.incoming(id)) {
            switch (r.type()) {
                case SUPPORTS -> push += W * r.strength() * p(r.from());
                case UNDERMINES, CONTRADICTS, TENSION -> push -= W * r.strength() * p(r.from());
                default -> { }
            }
        }
        for (Relation r : g.outgoing(id))
            if (r.type().isSymmetric()) push -= W * r.strength() * p(r.to());
        return base + push;
    }

    private double citations(NodeId id) {
        Map<NodeId, Double> bestPerRoot = new HashMap<>();
        for (Relation r : g.outgoing(id)) {
            if (r.type() != RelationType.SOURCED_FROM) continue;
            double v = r.strength() * p(r.to());
            bestPerRoot.merge(g.rootOf(r.to()), v, Math::max);
        }
        return W * bestPerRoot.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    /** Un patron es tan creible como su pata mas debil. Sin sosten: como una afirmacion suelta. */
    private double derived(NodeId id) {
        double min = Double.NaN;
        for (Relation r : g.outgoing(id)) {
            if (r.type() != RelationType.DERIVED_FROM) continue;
            double v = r.strength() * p(r.to());
            min = Double.isNaN(min) ? v : Math.min(min, v);
        }
        return Double.isNaN(min) ? LogOdds.logit(PRIOR_CLAIM) : LogOdds.logit(min);
    }

    /** Credibilidad en (0, 1). */
    public double p(NodeId id) { return LogOdds.p(logit(id)); }

    public double logit(NodeId id) {
        Double v = logit.get(id);
        if (v == null) throw new IllegalArgumentException("Solo la evidencia tiene credibilidad: " + id);
        return v;
    }

    public FactGrade grade(NodeId id) { return FactGrade.of(p(id)); }

    public int iterations() { return iterations; }

    public boolean converged() { return converged; }
}
