package de.bottazzi.episteme.model;

/**
 * Credibilidad en log-odds: L = ln(p / (1 - p)).
 *
 * <p>Regla de Cromwell: ningun valor finito de L corresponde a p = 0 o p = 1, asi que nada queda
 * muerto ni seguro del todo. L se limita a [-MAX, MAX] para que la conversion a p tampoco
 * redondee a 0.0 o 1.0 en double.
 */
public final class LogOdds {

    public static final double MAX = 30.0;

    private LogOdds() {}

    public static double logit(double p) {
        requireOpen(p, "p");
        return Math.log(p / (1 - p));
    }

    public static double p(double logit) {
        return 1 / (1 + Math.exp(-clamp(logit)));
    }

    public static double clamp(double logit) {
        return Math.max(-MAX, Math.min(MAX, logit));
    }

    /** Exige p en el intervalo abierto (0, 1). */
    public static void requireOpen(double p, String name) {
        if (Double.isNaN(p) || p <= 0 || p >= 1)
            throw new IllegalArgumentException(name + " tiene que estar en (0, 1), sin incluir los extremos: " + p);
    }
}
