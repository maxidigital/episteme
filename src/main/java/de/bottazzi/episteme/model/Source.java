package de.bottazzi.episteme.model;

import java.util.Objects;

/**
 * Metadatos de un nodo SOURCE. Una fuente no trae credibilidad propia: parte del valor neutral, como
 * cualquier afirmacion, y sube o baja por los apoyos y objeciones que recibe.
 *
 * @param derivedFrom  fuente de la que esta copia o depende, o null. Fuentes con la misma raiz no son independientes.
 */
public record Source(NodeId id, String description, NodeId derivedFrom) {
    public Source {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(description, "description");
        if (id.equals(derivedFrom)) throw new IllegalArgumentException("Fuente derivada de si misma: " + id);
    }

    public static Source original(String id, String description) {
        return new Source(NodeId.of(id), description, null);
    }

    public static Source derived(String id, String description, String derivedFrom) {
        return new Source(NodeId.of(id), description, NodeId.of(derivedFrom));
    }
}
