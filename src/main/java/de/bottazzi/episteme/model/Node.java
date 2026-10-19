package de.bottazzi.episteme.model;

import java.util.Objects;

/** Nodo compartido. No guarda ningun numero: la credibilidad se calcula. */
public record Node(NodeId id, NodeKind kind, String statement) {
    public Node {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(statement, "statement");
        if (statement.isBlank()) throw new IllegalArgumentException("Enunciado vacio en " + id);
    }

    public static Node fact(String id, String statement) { return new Node(NodeId.of(id), NodeKind.FACT, statement); }

    public static Node observation(String id, String statement) { return new Node(NodeId.of(id), NodeKind.OBSERVATION, statement); }

    public static Node structure(String id, String statement) { return new Node(NodeId.of(id), NodeKind.STRUCTURE, statement); }

    public static Node prediction(String id, String statement) { return new Node(NodeId.of(id), NodeKind.PREDICTION, statement); }

    public static Node hypothesis(String id, String statement) { return new Node(NodeId.of(id), NodeKind.HYPOTHESIS, statement); }
}
