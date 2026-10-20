package de.bottazzi.episteme.model;

/**
 * Tipos de relacion, siempre from -> to.
 *
 * <p>Ataques: CONTRADICTS es una refutacion mutua (cada lado empuja al otro hacia abajo);
 * UNDERMINES es un ataque de un solo lado a la confiabilidad de algo (Pollock: "undercutting defeater"),
 * por ejemplo "el termometro estaba mal calibrado" contra el dato de temperatura.
 */
public enum RelationType {
    /**
     * from se construyo a partir de to: una estructura a partir de sus elementos, un facto a partir de
     * las observaciones que lo sostienen, o una deduccion a partir de sus premisas. En una estructura o un
     * facto cada to es una pata; una duda contra una observacion le saca al facto solo esa pata.
     */
    DERIVED_FROM,
    /** from se apoya en la fuente to. strength = calidad de esa cita. */
    SOURCED_FROM,
    /** from apoya a to. */
    SUPPORTS,
    /** from debilita a to, sin que to debilite a from. */
    UNDERMINES,
    /** from y to se excluyen: cada uno debilita al otro. Solo entre evidencias o entre hipotesis. */
    CONTRADICTS,
    /** Friccion entre from y to sin contradiccion logica. Igual que CONTRADICTS a efectos de calculo. */
    TENSION,
    /** from (hipotesis) explica to (estructura). */
    EXPLAINS,
    /** from (hipotesis) deja sin explicar a to (estructura): queda registrado, no mueve el indice. */
    LEAVES_UNEXPLAINED,
    /** from (hipotesis) entra en tension con to (estructura). */
    IN_TENSION_WITH,
    /** from (hipotesis compuesta) tiene a to (hipotesis) como parte. */
    COMPOSED_OF;

    public boolean isSymmetric() { return this == CONTRADICTS || this == TENSION; }

    /** Relacion de una hipotesis con una estructura. No lleva peso: el peso es la credibilidad de la estructura. */
    public boolean isExplanatory() { return this == EXPLAINS || this == LEAVES_UNEXPLAINED || this == IN_TENSION_WITH; }

    /** Relaciones entre o desde hipotesis: no llevan peso propio. */
    public boolean isUnweighted() { return isExplanatory() || this == COMPOSED_OF; }
}
