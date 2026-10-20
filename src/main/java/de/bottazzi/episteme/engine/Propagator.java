package de.bottazzi.episteme.engine;

import de.bottazzi.episteme.model.NodeId;
import de.bottazzi.episteme.model.Relation;
import de.bottazzi.episteme.store.KnowledgeGraph;

import java.util.*;

/**
 * Propagacion diferencial: dado un nodo que cambio, que otros nodos conviene revisar y cuanto.
 *
 * <p>Impacto de un camino = producto de las fuerzas de sus relaciones. Para cada nodo se guarda el
 * mejor camino (best-first, como Dijkstra con producto maximo). Se corta cuando el impacto cae
 * bajo el umbral. Como toda fuerza es menor o igual a 1, un ciclo nunca mejora el impacto de un
 * nodo ya visitado, asi que el algoritmo termina aun con ciclos.
 *
 * <p>Direcciones (quien se ve afectado cuando cambia n):
 * <ul>
 *   <li>DERIVED_FROM x->n, SOURCED_FROM x->n: x depende de n, se revisa x.</li>
 *   <li>EXPLAINS, LEAVES_UNEXPLAINED, IN_TENSION_WITH h->n: la hipotesis h se revisa.</li>
 *   <li>COMPOSED_OF h->n: la hipotesis compuesta h, de la que n es parte, se revisa.</li>
 *   <li>SUPPORTS n->y, UNDERMINES n->y: lo apoyado o atacado por n se revisa.</li>
 *   <li>EXPECTED_BY_CHANCE n->y: la estructura y que se compara con el azar segun n se revisa.</li>
 *   <li>CONTRADICTS, TENSION: en ambas direcciones.</li>
 * </ul>
 *
 * <p>No modifica nada: devuelve una agenda de revision. Que se recalcula es decision de quien la consume.
 */
public final class Propagator {

    public record Impact(NodeId node, double impact, List<NodeId> path) {}

    private record Step(NodeId node, double impact) {}

    private static final double EPS = 1e-9;

    public List<Impact> propagate(KnowledgeGraph g, NodeId origin, double threshold) {
        g.node(origin);
        Map<NodeId, Double> best = new HashMap<>();
        Map<NodeId, NodeId> parent = new HashMap<>();
        PriorityQueue<Step> queue = new PriorityQueue<>(Comparator.comparingDouble(Step::impact).reversed());

        best.put(origin, 1.0);
        queue.add(new Step(origin, 1.0));

        while (!queue.isEmpty()) {
            Step cur = queue.poll();
            if (cur.impact() < best.get(cur.node()) - EPS) continue; // entrada vieja
            for (Relation r : affectedBy(g, cur.node())) {
                NodeId next = r.other(cur.node());
                double impact = cur.impact() * r.strength();
                if (impact < threshold) continue;
                if (impact > best.getOrDefault(next, 0.0) + EPS) {
                    best.put(next, impact);
                    parent.put(next, cur.node());
                    queue.add(new Step(next, impact));
                }
            }
        }

        return best.entrySet().stream()
                .filter(e -> !e.getKey().equals(origin))
                .map(e -> new Impact(e.getKey(), e.getValue(), path(parent, origin, e.getKey())))
                .sorted(Comparator.comparingDouble(Impact::impact).reversed()
                        .thenComparing(i -> i.node().value()))
                .toList();
    }

    private static List<Relation> affectedBy(KnowledgeGraph g, NodeId n) {
        List<Relation> out = new ArrayList<>();
        for (Relation r : g.incoming(n)) {
            switch (r.type()) {
                case DERIVED_FROM, SOURCED_FROM, EXPLAINS, LEAVES_UNEXPLAINED, IN_TENSION_WITH, COMPOSED_OF, CONTRADICTS, TENSION -> out.add(r);
                case SUPPORTS, UNDERMINES, EXPECTED_BY_CHANCE -> { }
            }
        }
        for (Relation r : g.outgoing(n)) {
            switch (r.type()) {
                case SUPPORTS, UNDERMINES, EXPECTED_BY_CHANCE, CONTRADICTS, TENSION -> out.add(r);
                case DERIVED_FROM, SOURCED_FROM, EXPLAINS, LEAVES_UNEXPLAINED, IN_TENSION_WITH, COMPOSED_OF -> { }
            }
        }
        return out;
    }

    private static List<NodeId> path(Map<NodeId, NodeId> parent, NodeId origin, NodeId target) {
        LinkedList<NodeId> p = new LinkedList<>();
        for (NodeId n = target; n != null; n = parent.get(n)) {
            p.addFirst(n);
            if (n.equals(origin)) break;
        }
        return List.copyOf(p);
    }
}
