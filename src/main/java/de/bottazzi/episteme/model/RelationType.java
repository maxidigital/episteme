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
     * from se construyo a partir de to: una estructura a partir de sus elementos, o un facto a partir de
     * las observaciones que lo sostienen. Cada to es una pata; una duda contra una observacion le saca
     * al facto solo esa pata.
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
    /** from (hipotesis) predice to (prediccion). */
    PREDICTS,
    /** from (prediccion) se cumple segun to (evidencia). */
    CONFIRMED_BY,
    /** from (prediccion) falla segun to (evidencia). */
    REFUTED_BY;

    public boolean isSymmetric() { return this == CONTRADICTS || this == TENSION; }

    /** Relacion sin peso propio: su fuerza es siempre 1 y el peso sale de la evidencia. */
    public boolean isUnweighted() { return this == PREDICTS || this == CONFIRMED_BY || this == REFUTED_BY; }
}
