package de.bottazzi.episteme.model;

import java.util.Objects;

/**
 * Metadatos de un nodo SOURCE.
 *
 * @param prior        confianza de base en (0, 1). Es el ancla del calculo (como las semillas de TrustRank)
 *                     y se puede cambiar con un evento; ademas la fuente puede recibir apoyos y objeciones.
 * @param derivedFrom  fuente de la que esta copia o depende, o null. Fuentes con la misma raiz no son independientes.
 */
public record Source(NodeId id, String description, NodeId derivedFrom, double prior) {
    public Source {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(description, "description");
        LogOdds.requireOpen(prior, "prior");
        if (id.equals(derivedFrom)) throw new IllegalArgumentException("Fuente derivada de si misma: " + id);
    }

    public static Source original(String id, String description, double prior) {
        return new Source(NodeId.of(id), description, null, prior);
    }

    public static Source derived(String id, String description, String derivedFrom, double prior) {
        return new Source(NodeId.of(id), description, NodeId.of(derivedFrom), prior);
    }

    public Source withPrior(double p) { return new Source(id, description, derivedFrom, p); }
}
