package de.bottazzi.episteme.model;

import java.util.Objects;

/** Identificador propio de una relacion, independiente de sus extremos. */
public record RelationId(String value) {
    public RelationId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) throw new IllegalArgumentException("RelationId vacio");
    }

    public static RelationId of(String value) { return new RelationId(value); }

    @Override public String toString() { return value; }
}
