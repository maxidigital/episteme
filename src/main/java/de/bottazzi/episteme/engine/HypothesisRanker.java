package de.bottazzi.episteme.engine;

import de.bottazzi.episteme.model.*;
import de.bottazzi.episteme.store.KnowledgeGraph;

import java.util.*;

/**
 * RoH: ranking de hipotesis. Puntaje de AJUSTE con su explicacion, no probabilidad de verdad.
 *
 * <p>Estado de cada prediccion, con p = credibilidad de la evidencia (nunca 0):
 * <ul>
 *   <li>CONFIRMED: alguna evidencia creible (p &gt; 0.5) la confirma y ninguna creible la refuta.</li>
 *   <li>REFUTED: lo contrario.</li>
 *   <li>CONTESTED: evidencia creible en los dos sentidos.</li>
 *   <li>PENDING: no hay evidencia creible en ningun sentido. Es un experimento para hacer.</li>
 * </ul>
 *
 * <pre>
 * cobertura(H)  = sum p(S) de las estructuras que confirman alguna prediccion de H / sum p(S) de todas
 * refutacion(H) = sum por prediccion, p de la evidencia mas creible que la refuta
 * ajuste(H)     = cobertura(H) - refutacion(H)
 * </pre>
 * Todo es continuo: una refutacion debil resta poco, pero nunca nada.
 */
public final class HypothesisRanker {

    public static final double CREDIBLE = 0.5;

    public enum Verdict { CONFIRMED, REFUTED, CONTESTED, PENDING }

    public record PredictionStatus(NodeId prediction, Verdict verdict, double confirmation, double refutation,
                                   List<NodeId> confirmedBy, List<NodeId> refutedBy) {}

    public record Report(
            NodeId hypothesis,
            double fit,
            double coverage,
            double explainedWeight,
            double totalWeight,
            double refutation,
            List<NodeId> explained,
            List<NodeId> unexplained,
            List<PredictionStatus> predictions,
            int citedSources,
            int independentSources) {

        public List<NodeId> predictionsWith(Verdict v) {
            return predictions.stream().filter(p -> p.verdict() == v).map(PredictionStatus::prediction).toList();
        }
    }

    public List<Report> rank(KnowledgeGraph g) { return rank(g, new Credibility(g)); }

    public List<Report> rank(KnowledgeGraph g, Credibility c) {
        List<Node> structures = g.nodesOfKind(NodeKind.STRUCTURE);
        return g.nodesOfKind(NodeKind.HYPOTHESIS).stream()
                .map(h -> evaluate(g, c, h.id(), structures))
                .sorted(Comparator.comparingDouble(Report::fit).reversed()
                        .thenComparing(r -> r.hypothesis().value()))
                .toList();
    }

    public static PredictionStatus status(KnowledgeGraph g, Credibility c, NodeId p) {
        double conf = 0, ref = 0;
        List<NodeId> confirmedBy = new ArrayList<>(), refutedBy = new ArrayList<>();
        for (Relation r : g.outgoing(p)) {
            if (r.type() == RelationType.CONFIRMED_BY) { conf = Math.max(conf, c.p(r.to())); confirmedBy.add(r.to()); }
            if (r.type() == RelationType.REFUTED_BY) { ref = Math.max(ref, c.p(r.to())); refutedBy.add(r.to()); }
        }
        boolean yes = conf > CREDIBLE, no = ref > CREDIBLE;
        Verdict v = yes && no ? Verdict.CONTESTED : yes ? Verdict.CONFIRMED : no ? Verdict.REFUTED : Verdict.PENDING;
        return new PredictionStatus(p, v, conf, ref, List.copyOf(confirmedBy), List.copyOf(refutedBy));
    }

    private Report evaluate(KnowledgeGraph g, Credibility c, NodeId h, List<Node> structures) {
        List<PredictionStatus> predictions = new ArrayList<>();
        Set<NodeId> confirming = new HashSet<>();
        double refutation = 0;
        for (Relation r : g.outgoing(h)) {
            if (r.type() != RelationType.PREDICTS) continue;
            PredictionStatus st = status(g, c, r.to());
            predictions.add(st);
            refutation += st.refutation();
            for (NodeId n : st.confirmedBy())
                if (g.node(n).kind() == NodeKind.STRUCTURE) confirming.add(n);
        }

        double weighted = 0, total = 0;
        List<NodeId> explained = new ArrayList<>(), unexplained = new ArrayList<>();
        for (Node s : structures) {
            double w = c.p(s.id());
            total += w;
            if (confirming.contains(s.id())) { weighted += w; explained.add(s.id()); }
            else unexplained.add(s.id());
        }
        double coverage = total == 0 ? 0 : weighted / total;

        return new Report(h, coverage - refutation, coverage, weighted, total, refutation,
                List.copyOf(explained), List.copyOf(unexplained), List.copyOf(predictions),
                g.citedSources(h).size(), g.independentRoots(h).size());
    }
}
