package de.bottazzi.episteme.model;

/** Tipo de nodo y su nivel. Todos los nodos de nivel 0 y 1 son evidencia y se pueden debatir. */
public enum NodeKind {
    /** Nivel 0: una fuente (organismo, dataset, publicacion). Su confiabilidad tambien es debatible. */
    SOURCE(0),
    /** Nivel 0: registro de algo observado, sin fuente revisada. */
    OBSERVATION(0),
    /** Nivel 0: afirmacion factual. Su credibilidad sale de sus fuentes, observaciones, apoyos y objeciones. */
    FACT(0),
    /**
     * Nivel 0: deduccion obvia a partir de otros factos (lo de las 9:03 paso antes que lo de las 9:59).
     * No agrega contenido, cambia la representacion: vale lo que su premisa mas debil y no suma raices propias.
     */
    DEDUCTION(0),
    /** Nivel 1: patron entre factos. No causal. */
    STRUCTURE(1),
    /** Nivel 2: hipotesis. Se evalua por las estructuras que explica, deja sin explicar o tensiona. */
    HYPOTHESIS(2);

    private final int level;

    NodeKind(int level) { this.level = level; }

    public int level() { return level; }

    public boolean isEvidence() { return level < 2; }
}
