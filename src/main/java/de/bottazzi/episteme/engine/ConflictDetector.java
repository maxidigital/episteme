package de.bottazzi.episteme.engine;

import de.bottazzi.episteme.model.*;
import de.bottazzi.episteme.store.KnowledgeGraph;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Conflictos del arbol, con intensidad derivada de la credibilidad:
 * <ul>
 *   <li>EVIDENCE_VS_EVIDENCE: CONTRADICTS o TENSION entre evidencias. min(p) * fuerza.</li>
 *   <li>UNDERMINED: una objecion creible contra algo. p(objecion) * fuerza.</li>
 *   <li>HYPOTHESIS_IN_TENSION: una hipotesis en tension con una estructura. p(estructura).</li>
 *   <li>RIVAL_HYPOTHESES: informativo. Fuerza.</li>
 * </ul>
 */
public final class ConflictDetector {

    /** Una objecion cuenta como conflicto cuando es mas creible que no. */
    public static final double CREDIBLE = 0.5;

    public enum Kind { EVIDENCE_VS_EVIDENCE, UNDERMINED, HYPOTHESIS_IN_TENSION, RIVAL_HYPOTHESES }

    public record Conflict(String id, NodeId a, NodeId b, Kind kind, double intensity) {}

    public List<Conflict> detect(KnowledgeGraph g, double minIntensity) {
        Credibility c = new Credibility(g);
        List<Conflict> out = new ArrayList<>();

        for (Relation r : g.relations()) {
            String id = r.id().value();
            if (r.type().isSymmetric()) {
                boolean hyp = g.node(r.from()).kind() == NodeKind.HYPOTHESIS;
                out.add(hyp
                        ? new Conflict(id, r.from(), r.to(), Kind.RIVAL_HYPOTHESES, r.strength())
                        : new Conflict(id, r.from(), r.to(), Kind.EVIDENCE_VS_EVIDENCE,
                            Math.min(c.p(r.from()), c.p(r.to())) * r.strength()));
            } else if (r.type() == RelationType.UNDERMINES && c.p(r.from()) > CREDIBLE) {
                out.add(new Conflict(id, r.from(), r.to(), Kind.UNDERMINED, c.p(r.from()) * r.strength()));
            } else if (r.type() == RelationType.IN_TENSION_WITH) {
                out.add(new Conflict(id, r.from(), r.to(), Kind.HYPOTHESIS_IN_TENSION, c.p(r.to())));
            }
        }

        return out.stream()
                .filter(x -> x.intensity() >= minIntensity)
                .sorted(Comparator.comparingDouble(Conflict::intensity).reversed().thenComparing(Conflict::id))
                .toList();
    }
}
