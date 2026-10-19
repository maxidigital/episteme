package de.bottazzi.episteme.model;

final class Unit {
    private Unit() {}

    static void check(double v, String name) {
        if (Double.isNaN(v) || v < 0 || v > 1)
            throw new IllegalArgumentException(name + " fuera de [0, 1]: " + v);
    }
}
