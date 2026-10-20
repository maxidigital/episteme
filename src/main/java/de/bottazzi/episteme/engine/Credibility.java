package de.bottazzi.episteme.engine;

import de.bottazzi.episteme.model.*;
import de.bottazzi.episteme.store.KnowledgeGraph;

import java.util.*;

/**
 * Credibilidad de toda la evidencia (fuentes, factos, observaciones, estructuras), calculada como
 * punto fijo, al estilo PageRank. No se ingresa ningun valor a mano: todo parte de {@link #NEUTRAL},
 * fuentes incluidas, y sube o baja por argumentos.
 *
 * <p>Todo se suma en log-odds, asi que p nunca llega a 0 ni a 1 (regla de Cromwell):
 * <pre>
 * L(n)                    = logit(NEUTRAL) + W * sosten - W * ataques + W * apoyos
 * sosten, apoyos, ataques = sum por raiz independiente de max(k * p(x))
 * L(deduccion)            = logit(min sobre sus premisas de k * p) - W * ataques + W * apoyos
 * </pre>
 * Si un facto de fondo dice que el azar daria e de los n elementos independientes de una estructura, su
 * sosten se multiplica por 1 - p(fondo) * min(1, e/n): cuentan solo los que superan al azar.
 * Los ataques son objeciones, contradicciones y tensiones.
 * Las patas de un nodo son las fuentes que cita (k = calidad de la cita) y los elementos de los que deriva.
 * Patas que comparten una raiz son copias: de ellas cuenta solo la mejor. Tres diarios que copian el mismo
 * comunicado aportan una sola cita; una estructura crece con cada elemento independiente que la forma;
 * cien personas repitiendo la misma objecion desde la misma raiz son una sola objecion.
 * Una afirmacion sin fuentes ni apoyos queda en NEUTRAL: poco creible, pero no cero. Una fuente no cita
 * ni deriva de nada: solo la mueven los apoyos y objeciones a su sinceridad, competencia o independencia.
 *
 * <p>Se itera con amortiguacion hasta que ningun valor cambia mas que {@link #TOLERANCE}.
 */
public final class Credibility {

    /** Peso de una pieza de evidencia de credibilidad 1 y fuerza 1, en log-odds. */
    public static final double W = 4.0;
    /**
     * Valor neutral: de donde parte todo, fuentes incluidas. Es tambien lo que vale una afirmacion sin
     * nada que la sostenga. Es el unico valor de partida del sistema (D18, D27).
     */
    public static final double NEUTRAL = 0.2;
    public static final double DAMPING = 0.5;
    public static final double TOLERANCE = 1e-10;
    public static final int MAX_ITERATIONS = 1000;

    private final KnowledgeGraph g;
    private final List<NodeId> evidence;
    private final Map<NodeId, Double> logit = new HashMap<>();
    private final Map<NodeId, Set<NodeId>> roots = new HashMap<>();
    private final int iterations;
    private final boolean converged;

    public Credibility(KnowledgeGraph g) {
        this.g = g;
        this.evidence = g.nodes().stream().filter(n -> n.kind().isEvidence()).map(Node::id).toList();
        for (NodeId id : evidence) {
            logit.put(id, LogOdds.logit(NEUTRAL));
            roots.put(id, g.rootsOf(id));
        }

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

    private double target(NodeId id) {
        double base = g.node(id).kind() == NodeKind.DEDUCTION ? weakestPremise(id) : LogOdds.logit(NEUTRAL) + sustain(id);
        List<Leg> pro = new ArrayList<>(), con = new ArrayList<>();
        for (Relation r : g.incoming(id)) {
            switch (r.type()) {
                case SUPPORTS -> pro.add(leg(r, r.from()));
                case UNDERMINES, CONTRADICTS, TENSION -> con.add(leg(r, r.from()));
                default -> { }
            }
        }
        for (Relation r : g.outgoing(id))
            if (r.type().isSymmetric()) con.add(leg(r, r.to()));
        return base + W * sum(independentGroups(pro)) - W * sum(independentGroups(con));
    }

    private double sustain(NodeId id) {
        List<Leg> legs = new ArrayList<>();
        for (Relation r : g.outgoing(id)) {
            if (r.type() == RelationType.SOURCED_FROM || r.type() == RelationType.DERIVED_FROM)
                legs.add(leg(r, r.to()));
        }
        List<Double> groups = independentGroups(legs);
        return W * sum(groups) * beyondChance(id, groups.size());
    }

    /**
     * Que parte de los n elementos independientes supera al azar (D29): 1 - p(fondo) * min(1, e/n). De 40
     * relatos, si el azar daria 14, cuentan 26. Con varios factos de fondo, cuenta el que mas descuenta.
     */
    private double beyondChance(NodeId id, int n) {
        double discount = 0;
        for (Relation r : g.incoming(id))
            if (r.type() == RelationType.EXPECTED_BY_CHANCE)
                discount = Math.max(discount, p(r.from()) * Math.min(1, r.expected() / n));
        return 1 - discount;
    }

    /** Una deduccion no agrega contenido: vale lo que su premisa mas debil. Sin premisas, lo neutral. */
    private double weakestPremise(NodeId id) {
        double min = Double.NaN;
        for (Relation r : g.outgoing(id)) {
            if (r.type() != RelationType.DERIVED_FROM) continue;
            double v = r.strength() * p(r.to());
            min = Double.isNaN(min) ? v : Math.min(min, v);
        }
        return LogOdds.logit(Double.isNaN(min) ? NEUTRAL : min);
    }

    private record Leg(Set<NodeId> roots, double value) {}

    /** La pata que aporta el nodo other a traves de la relacion r. */
    private Leg leg(Relation r, NodeId other) { return new Leg(roots.get(other), r.strength() * p(other)); }

    /** Une las patas que comparten alguna raiz y devuelve la mejor de cada grupo. */
    private static List<Double> independentGroups(List<Leg> legs) {
        List<Set<NodeId>> groups = new ArrayList<>();
        List<Double> best = new ArrayList<>();
        for (Leg leg : legs) {
            Set<NodeId> rs = new HashSet<>(leg.roots());
            double b = leg.value();
            for (int i = groups.size() - 1; i >= 0; i--) {
                if (Collections.disjoint(groups.get(i), rs)) continue;
                rs.addAll(groups.remove(i));
                b = Math.max(b, best.remove(i));
            }
            groups.add(rs);
            best.add(b);
        }
        return best;
    }

    private static double sum(List<Double> xs) { return xs.stream().mapToDouble(Double::doubleValue).sum(); }

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
