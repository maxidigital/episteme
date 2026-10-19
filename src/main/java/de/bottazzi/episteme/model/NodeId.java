package de.bottazzi.episteme.model;

import java.util.Objects;

/** Identificador estable de un nodo. Identifica, no certifica verdad ni valoracion. */
public record NodeId(String value) {
    public NodeId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) throw new IllegalArgumentException("NodeId vacio");
    }

    public static NodeId of(String value) { return new NodeId(value); }

    @Override public String toString() { return value; }
}
