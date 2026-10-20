package de.bottazzi.episteme.model;

import java.util.Objects;

/**
 * Relacion compartida. strength en (0, 1]: nunca cero, una relacion de fuerza cero no existe.
 * aspect: en un apoyo u objecion a una fuente, a cual de las tres preguntas responde, o null.
 */
public record Relation(RelationId id, NodeId from, NodeId to, RelationType type, double strength, Aspect aspect) {
    public Relation {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(type, "type");
        if (Double.isNaN(strength) || strength <= 0 || strength > 1)
            throw new IllegalArgumentException("strength tiene que estar en (0, 1]: " + strength);
        if (from.equals(to)) throw new IllegalArgumentException("Relacion reflexiva: " + id);
    }

    public static Relation of(String id, String from, RelationType type, String to, double strength) {
        return new Relation(RelationId.of(id), NodeId.of(from), NodeId.of(to), type, strength, null);
    }

    /** Apoyo u objecion a una fuente, sobre su sinceridad, competencia o independencia. */
    public static Relation about(String id, String from, RelationType type, String source, Aspect aspect, double strength) {
        return new Relation(RelationId.of(id), NodeId.of(from), NodeId.of(source), type, strength, Objects.requireNonNull(aspect, "aspect"));
    }

    /** Relacion sin peso propio (predicciones). */
    public static Relation link(String id, String from, RelationType type, String to) {
        return new Relation(RelationId.of(id), NodeId.of(from), NodeId.of(to), type, 1.0, null);
    }

    public NodeId other(NodeId node) {
        if (from.equals(node)) return to;
        if (to.equals(node)) return from;
        return null;
    }

    public boolean touches(NodeId node) { return from.equals(node) || to.equals(node); }
}
