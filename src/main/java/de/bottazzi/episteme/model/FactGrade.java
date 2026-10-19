package de.bottazzi.episteme.model;

/** Etiqueta legible de una credibilidad. Es una salida del calculo, nunca una entrada. */
public enum FactGrade {
    A, B, C, D;

    public static FactGrade of(double p) {
        if (p >= 0.90) return A;
        if (p >= 0.75) return B;
        if (p >= 0.50) return C;
        return D;
    }
}
