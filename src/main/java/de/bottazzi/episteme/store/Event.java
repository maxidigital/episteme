package de.bottazzi.episteme.store;

import de.bottazzi.episteme.model.*;

/**
 * Todo cambio es un evento y nada se borra. No hay "retirar" ni "regradar": para debilitar algo
 * se agrega una objecion con su propia evidencia, que tambien se puede debatir.
 */
public sealed interface Event {

    boolean touches(NodeId node);

    record NodeAdded(Node node) implements Event {
        public boolean touches(NodeId n) { return node.id().equals(n); }
    }

    /** Agrega el nodo SOURCE y sus metadatos. */
    record SourceAdded(Source source) implements Event {
        public boolean touches(NodeId n) { return source.id().equals(n); }
    }

    record RelationAdded(Relation relation) implements Event {
        public boolean touches(NodeId n) { return relation.touches(n); }
    }

    /** Cambia la confianza de base en una fuente, con el motivo. */
    record SourcePriorSet(NodeId source, double prior, String reason) implements Event {
        public boolean touches(NodeId n) { return source.equals(n); }
    }
}
