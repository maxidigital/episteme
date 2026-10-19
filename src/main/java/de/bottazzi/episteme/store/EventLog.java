package de.bottazzi.episteme.store;

import de.bottazzi.episteme.model.NodeId;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

/**
 * Log append-only. version() es la cantidad de eventos aplicados.
 * stateAt(v) reconstruye cualquier estado historico: eso son los snapshots.
 */
public final class EventLog {

    private final List<LoggedEvent> events = new ArrayList<>();
    private final KnowledgeGraph current = new KnowledgeGraph();
    private final Clock clock;

    public EventLog() { this(Clock.systemUTC()); }

    public EventLog(Clock clock) { this.clock = clock; }

    /** Aplica y registra. Si el evento es invalido, lanza excepcion y no queda registrado. */
    public synchronized long append(String author, Event event) {
        current.apply(event);
        long version = events.size() + 1;
        events.add(new LoggedEvent(version, clock.instant(), author, event));
        return version;
    }

    public synchronized long version() { return events.size(); }

    /** Estado vigente. Es de solo lectura desde fuera del paquete. */
    public KnowledgeGraph current() { return current; }

    /** Snapshot: estado tal como estaba en la version dada (0 = vacio). */
    public synchronized KnowledgeGraph stateAt(long version) {
        if (version < 0 || version > events.size())
            throw new IllegalArgumentException("Version inexistente: " + version);
        KnowledgeGraph g = new KnowledgeGraph();
        for (int i = 0; i < version; i++) g.apply(events.get(i).event());
        return g;
    }

    /** Todo lo que paso con un nodo, en orden. */
    public synchronized List<LoggedEvent> history(NodeId node) {
        return events.stream().filter(e -> e.event().touches(node)).toList();
    }

    public synchronized List<LoggedEvent> events() { return List.copyOf(events); }
}
