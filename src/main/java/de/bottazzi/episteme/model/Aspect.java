package de.bottazzi.episteme.model;

/**
 * Las tres preguntas en que se parte la credibilidad de un observador (D18). Etiquetan los apoyos y
 * objeciones que recibe una fuente; las tres pesan igual en el calculo.
 */
public enum Aspect {
    /** Cree lo que dice? */
    SINCERITY,
    /** Podia percibir bien? */
    COMPETENCE,
    /** Su relato esta contaminado por otros? */
    INDEPENDENCE
}
