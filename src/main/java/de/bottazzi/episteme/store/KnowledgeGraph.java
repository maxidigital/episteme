package de.bottazzi.episteme.store;

import de.bottazzi.episteme.model.*;

import java.util.*;

/**
 * Estado materializado. Publicamente es de solo lectura: solo {@link EventLog} lo modifica.
 * Cada mutacion valida antes de cambiar nada.
 */
public final class KnowledgeGraph {

    private final Map<NodeId, Node> nodes = new LinkedHashMap<>();
    private final Map<NodeId, Source> sources = new LinkedHashMap<>();
    private final Map<RelationId, Relation> relations = new LinkedHashMap<>();
    private final Map<NodeId, List<Relation>> outgoing = new HashMap<>();
    private final Map<NodeId, List<Relation>> incoming = new HashMap<>();

    KnowledgeGraph() {}

    void apply(Event event) {
        if (event instanceof Event.NodeAdded e) addNode(e.node());
        else if (event instanceof Event.SourceAdded e) addSource(e.source());
        else if (event instanceof Event.RelationAdded e) addRelation(e.relation());
        else if (event instanceof Event.SourcePriorSet e) setPrior(e.source(), e.prior());
        else throw new IllegalStateException("Evento desconocido: " + event);
    }

    private void addNode(Node n) {
        if (n.kind() == NodeKind.SOURCE) throw new IllegalStateException("Las fuentes se agregan con SourceAdded: " + n.id());
        if (nodes.containsKey(n.id())) throw new IllegalStateException("Nodo duplicado: " + n.id());
        nodes.put(n.id(), n);
    }

    private void addSource(Source s) {
        if (nodes.containsKey(s.id())) throw new IllegalStateException("Nodo duplicado: " + s.id());
        // derivedFrom tiene que existir antes: eso impide ciclos entre fuentes.
        if (s.derivedFrom() != null && !sources.containsKey(s.derivedFrom()))
            throw new IllegalStateException("Fuente de origen desconocida: " + s.derivedFrom());
        nodes.put(s.id(), new Node(s.id(), NodeKind.SOURCE, s.description()));
        sources.put(s.id(), s);
    }

    private void setPrior(NodeId id, double p) {
        Source s = sources.get(id);
        if (s == null) throw new IllegalStateException("Fuente desconocida: " + id);
        sources.put(id, s.withPrior(p));
    }

    private void addRelation(Relation r) {
        if (relations.containsKey(r.id())) throw new IllegalStateException("Relacion duplicada: " + r.id());
        NodeKind from = requireNode(r.from()).kind();
        NodeKind to = requireNode(r.to()).kind();
        if (r.type().isUnweighted() && r.strength() != 1.0)
            throw new IllegalStateException("Las relaciones de prediccion no llevan peso: " + r.id());
        String problem = switch (r.type()) {
            case DERIVED_FROM -> from == NodeKind.STRUCTURE && to.isEvidence() && to != NodeKind.SOURCE
                    ? null : "DERIVED_FROM va de una estructura a factos, observaciones u otras estructuras";
            case SOURCED_FROM -> to == NodeKind.SOURCE && from != NodeKind.SOURCE && from != NodeKind.PREDICTION
                    ? null : "SOURCED_FROM va de una afirmacion a una fuente";
            case SUPPORTS, UNDERMINES -> from.isEvidence() && to.isEvidence()
                    ? null : r.type() + " es entre evidencias (las hipotesis se juzgan por sus predicciones)";
            case CONTRADICTS, TENSION -> (from.isEvidence() && to.isEvidence())
                    || (from == NodeKind.HYPOTHESIS && to == NodeKind.HYPOTHESIS)
                    ? null : "Una hipotesis choca con evidencia a traves de una prediccion que falla";
            case PREDICTS -> from == NodeKind.HYPOTHESIS && to == NodeKind.PREDICTION
                    ? null : "PREDICTS va de hipotesis a prediccion";
            case CONFIRMED_BY, REFUTED_BY -> from == NodeKind.PREDICTION && to.isEvidence() && to != NodeKind.SOURCE
                    ? null : r.type() + " va de una prediccion a evidencia";
        };
        if (problem != null) throw new IllegalStateException(problem + ": " + r.id());
        relations.put(r.id(), r);
        outgoing.computeIfAbsent(r.from(), k -> new ArrayList<>()).add(r);
        incoming.computeIfAbsent(r.to(), k -> new ArrayList<>()).add(r);
    }

    private Node requireNode(NodeId id) {
        Node n = nodes.get(id);
        if (n == null) throw new IllegalStateException("Nodo desconocido: " + id);
        return n;
    }

    // ---- Consultas ----

    public Node node(NodeId id) { return requireNode(id); }

    public Source source(NodeId id) {
        Source s = sources.get(id);
        if (s == null) throw new IllegalStateException("Fuente desconocida: " + id);
        return s;
    }

    public Collection<Node> nodes() { return Collections.unmodifiableCollection(nodes.values()); }

    public List<Node> nodesOfKind(NodeKind kind) {
        return nodes.values().stream().filter(n -> n.kind() == kind).toList();
    }

    public Collection<Relation> relations() { return Collections.unmodifiableCollection(relations.values()); }

    public List<Relation> outgoing(NodeId id) { return List.copyOf(outgoing.getOrDefault(id, List.of())); }

    public List<Relation> incoming(NodeId id) { return List.copyOf(incoming.getOrDefault(id, List.of())); }

    /** Fuente original a la que se reduce una fuente siguiendo derivedFrom. */
    public NodeId rootOf(NodeId source) {
        Source s = source(source);
        while (s.derivedFrom() != null) s = sources.get(s.derivedFrom());
        return s.id();
    }

    /** Fuentes citadas directamente por un nodo. */
    public List<NodeId> citedSources(NodeId id) {
        return outgoing(id).stream().filter(r -> r.type() == RelationType.SOURCED_FROM).map(Relation::to).toList();
    }

    /** Raices independientes entre las fuentes citadas por un nodo. */
    public Set<NodeId> independentRoots(NodeId id) {
        Set<NodeId> roots = new LinkedHashSet<>();
        for (NodeId s : citedSources(id)) roots.add(rootOf(s));
        return roots;
    }
}
