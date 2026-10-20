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

    private void addRelation(Relation r) {
        if (relations.containsKey(r.id())) throw new IllegalStateException("Relacion duplicada: " + r.id());
        NodeKind from = requireNode(r.from()).kind();
        NodeKind to = requireNode(r.to()).kind();
        if (r.aspect() != null && (to != NodeKind.SOURCE || (r.type() != RelationType.SUPPORTS && r.type() != RelationType.UNDERMINES)))
            throw new IllegalStateException("Sinceridad, competencia e independencia son apoyos u objeciones a una fuente: " + r.id());
        if (r.type().isUnweighted() && r.strength() != 1.0)
            throw new IllegalStateException("Las relaciones de una hipotesis no llevan peso: " + r.id());
        if (r.type().isExplanatory() && outgoing.getOrDefault(r.from(), List.of()).stream()
                .anyMatch(x -> x.type().isExplanatory() && x.to().equals(r.to())))
            throw new IllegalStateException("La hipotesis ya tiene una relacion con esa estructura: " + r.id());
        String problem = switch (r.type()) {
            case DERIVED_FROM -> !derivable(from, to)
                    ? "DERIVED_FROM va de una estructura a sus elementos, de un facto a sus observaciones o de una deduccion a sus premisas"
                    : reaches(r.to(), r.from(), RelationType.DERIVED_FROM) ? "Nada puede derivar de si mismo" : null;
            case SOURCED_FROM -> to == NodeKind.SOURCE && from.isEvidence() && from != NodeKind.SOURCE
                    && from != NodeKind.DEDUCTION
                    ? null : "SOURCED_FROM va de una afirmacion a una fuente (una deduccion sale solo de sus premisas)";
            case SUPPORTS, UNDERMINES -> from.isEvidence() && to.isEvidence()
                    ? null : r.type() + " es entre evidencias (las hipotesis se juzgan por las estructuras que explican)";
            case CONTRADICTS, TENSION -> (from.isEvidence() && to.isEvidence())
                    || (from == NodeKind.HYPOTHESIS && to == NodeKind.HYPOTHESIS)
                    ? null : "Una hipotesis choca con evidencia entrando en tension con una estructura";
            case EXPLAINS, LEAVES_UNEXPLAINED, IN_TENSION_WITH -> from == NodeKind.HYPOTHESIS && to == NodeKind.STRUCTURE
                    ? null : r.type() + " va de una hipotesis a una estructura";
            case COMPOSED_OF -> from != NodeKind.HYPOTHESIS || to != NodeKind.HYPOTHESIS
                    ? "COMPOSED_OF va de una hipotesis compuesta a una de sus partes"
                    : reaches(r.to(), r.from(), RelationType.COMPOSED_OF) ? "Una hipotesis no puede ser parte de si misma" : null;
        };
        if (problem != null) throw new IllegalStateException(problem + ": " + r.id());
        relations.put(r.id(), r);
        outgoing.computeIfAbsent(r.from(), k -> new ArrayList<>()).add(r);
        incoming.computeIfAbsent(r.to(), k -> new ArrayList<>()).add(r);
    }

    /** Una estructura sale de evidencia de nivel 0 o 1; un facto, de las observaciones que lo sostienen. */
    private static boolean derivable(NodeKind from, NodeKind to) {
        return switch (from) {
            case STRUCTURE -> to.isEvidence() && to != NodeKind.SOURCE;
            case FACT -> to == NodeKind.OBSERVATION;
            case DEDUCTION -> to == NodeKind.FACT || to == NodeKind.OBSERVATION || to == NodeKind.DEDUCTION;
            default -> false;
        };
    }

    /** Si from llega a to siguiendo relaciones del tipo dado. */
    private boolean reaches(NodeId from, NodeId to, RelationType type) {
        if (from.equals(to)) return true;
        for (Relation r : outgoing.getOrDefault(from, List.of()))
            if (r.type() == type && reaches(r.to(), to, type)) return true;
        return false;
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

    /** Raiz comun de todo lo que no se apoya en ninguna fuente: cien afirmaciones sin fuente cuentan como una. */
    public static final NodeId NO_SOURCE = NodeId.of("(sin fuente)");

    /**
     * Raices independientes de las que sale un nodo: las de las fuentes que cita y las de los elementos de
     * los que deriva. Lo que no sale de ninguna fuente tiene la raiz {@link #NO_SOURCE}.
     */
    public Set<NodeId> rootsOf(NodeId id) {
        if (requireNode(id).kind() == NodeKind.SOURCE) return Set.of(rootOf(id));
        Set<NodeId> roots = new LinkedHashSet<>();
        for (Relation r : outgoing(id)) {
            if (r.type() == RelationType.SOURCED_FROM) roots.add(rootOf(r.to()));
            if (r.type() == RelationType.DERIVED_FROM) roots.addAll(rootsOf(r.to()));
        }
        if (roots.isEmpty()) roots.add(NO_SOURCE);
        return roots;
    }
}
