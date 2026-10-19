package de.bottazzi.episteme.engine;

import de.bottazzi.episteme.engine.HypothesisRanker.Verdict;
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
 *   <li>CONTESTED_PREDICTION: evidencia creible a favor y en contra. min(confirmacion, refutacion).</li>
 *   <li>REFUTED_PREDICTION: una prediccion falla. p de lo que la refuta.</li>
 *   <li>RIVAL_HYPOTHESES: informativo. Fuerza.</li>
 * </ul>
 */
public final class ConflictDetector {

    public enum Kind { EVIDENCE_VS_EVIDENCE, UNDERMINED, CONTESTED_PREDICTION, REFUTED_PREDICTION, RIVAL_HYPOTHESES }

    public record Conflict(String id, NodeId a, NodeId b, Kind kind, double intensity) {}

    public List<Conflict> detect(KnowledgeGraph g, double minIntensity) {
        Credibility c = new Credibility(g);
        List<Conflict> out = new ArrayList<>();

        for (Relation r : g.relations()) {
            if (r.type().isSymmetric()) {
                boolean hyp = g.node(r.from()).kind() == NodeKind.HYPOTHESIS;
                out.add(hyp
                        ? new Conflict(r.id().value(), r.from(), r.to(), Kind.RIVAL_HYPOTHESES, r.strength())
                        : new Conflict(r.id().value(), r.from(), r.to(), Kind.EVIDENCE_VS_EVIDENCE,
                            Math.min(c.p(r.from()), c.p(r.to())) * r.strength()));
            } else if (r.type() == RelationType.UNDERMINES && c.p(r.from()) > HypothesisRanker.CREDIBLE) {
                out.add(new Conflict(r.id().value(), r.from(), r.to(), Kind.UNDERMINED, c.p(r.from()) * r.strength()));
            }
        }

        for (Node p : g.nodesOfKind(NodeKind.PREDICTION)) {
            var st = HypothesisRanker.status(g, c, p.id());
            NodeId against = st.refutedBy().stream().max(Comparator.comparingDouble(c::p)).orElse(null);
            if (st.verdict() == Verdict.CONTESTED)
                out.add(new Conflict(p.id().value(), p.id(), against, Kind.CONTESTED_PREDICTION,
                        Math.min(st.confirmation(), st.refutation())));
            else if (st.verdict() == Verdict.REFUTED)
                out.add(new Conflict(p.id().value(), p.id(), against, Kind.REFUTED_PREDICTION, st.refutation()));
        }

        return out.stream()
                .filter(x -> x.intensity() >= minIntensity)
                .sorted(Comparator.comparingDouble(Conflict::intensity).reversed().thenComparing(Conflict::id))
                .toList();
    }
}
