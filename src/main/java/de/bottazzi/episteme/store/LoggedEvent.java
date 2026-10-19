package de.bottazzi.episteme.store;

import java.time.Instant;

/** Evento con procedencia: version, momento y autor. */
public record LoggedEvent(long version, Instant at, String author, Event event) {}
