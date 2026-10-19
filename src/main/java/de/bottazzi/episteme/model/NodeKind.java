package de.bottazzi.episteme.model;

/** Tipo de nodo y su nivel. Todos los nodos de nivel 0 y 1 son evidencia y se pueden debatir. */
public enum NodeKind {
    /** Nivel 0: una fuente (organismo, dataset, publicacion). Su confiabilidad tambien es debatible. */
    SOURCE(0),
    /** Nivel 0: registro de algo observado, sin fuente revisada. */
    OBSERVATION(0),
    /** Nivel 0: afirmacion factual. Su credibilidad sale de sus fuentes, apoyos y objeciones. */
    FACT(0),
    /** Nivel 1: patron entre factos. No causal. */
    STRUCTURE(1),
    /** Nivel 2: algo que deberia observarse si una hipotesis fuera cierta. */
    PREDICTION(2),
    /** Nivel 2: hipotesis. Se evalua solo por sus predicciones. */
    HYPOTHESIS(2);

    private final int level;

    NodeKind(int level) { this.level = level; }

    public int level() { return level; }

    public boolean isEvidence() { return level < 2; }
}
