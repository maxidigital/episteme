package de.bottazzi.episteme;

import de.bottazzi.episteme.model.*;
import de.bottazzi.episteme.store.Event;
import de.bottazzi.episteme.store.Event.*;
import de.bottazzi.episteme.store.EventLog;

import static de.bottazzi.episteme.model.RelationType.*;

/** Atajos para escribir casos de prueba legibles. */
final class CaseBuilder {

    final EventLog log = new EventLog();

    long version() { return log.version(); }

    CaseBuilder source(String id, String description) {
        return add(new SourceAdded(Source.original(id, description)));
    }

    CaseBuilder copy(String id, String description, String from) {
        return add(new SourceAdded(Source.derived(id, description, from)));
    }

    /** from apoya (SUPPORTS) u objeta (UNDERMINES) a la fuente src en uno de sus tres aspectos. */
    CaseBuilder about(String from, RelationType type, String src, Aspect aspect, double strength) {
        return add(new RelationAdded(Relation.about(id(from, type, src), from, type, src, aspect, strength)));
    }

    CaseBuilder fact(String id, String statement) { return add(new NodeAdded(Node.fact(id, statement))); }

    CaseBuilder observation(String id, String statement) { return add(new NodeAdded(Node.observation(id, statement))); }

    /** Deduccion obvia a partir de las premisas dadas. */
    CaseBuilder deduction(String id, String statement, String... premises) {
        add(new NodeAdded(Node.deduction(id, statement)));
        return derive(id, premises);
    }

    CaseBuilder structure(String id, String statement) { return add(new NodeAdded(Node.structure(id, statement))); }


    CaseBuilder hypothesis(String id, String statement) { return add(new NodeAdded(Node.hypothesis(id, statement))); }

    /** from se apoya en la fuente src con esa calidad de cita. */
    CaseBuilder cites(String from, String src, double quality) { return rel(from, SOURCED_FROM, src, quality); }

    CaseBuilder derive(String structure, String... from) {
        for (String f : from) rel(structure, DERIVED_FROM, f, 1.0);
        return this;
    }

    /** El facto se sostiene en esas observaciones. */
    CaseBuilder observedIn(String fact, String... observations) { return derive(fact, observations); }

    CaseBuilder explains(String h, String... structures) { return links(h, EXPLAINS, structures); }

    CaseBuilder leavesUnexplained(String h, String... structures) { return links(h, LEAVES_UNEXPLAINED, structures); }

    CaseBuilder inTensionWith(String h, String... structures) { return links(h, IN_TENSION_WITH, structures); }

    private CaseBuilder links(String from, RelationType type, String... to) {
        for (String t : to) link(from, type, t);
        return this;
    }

    CaseBuilder rel(String from, RelationType type, String to, double strength) {
        return add(new RelationAdded(Relation.of(id(from, type, to), from, type, to, strength)));
    }

    CaseBuilder link(String from, RelationType type, String to) {
        return add(new RelationAdded(Relation.link(id(from, type, to), from, type, to)));
    }

    private static String id(String from, RelationType type, String to) {
        return "r-" + from.toLowerCase() + "-" + type.name().toLowerCase() + "-" + to.toLowerCase();
    }

    CaseBuilder add(Event e) {
        log.append("seed", e);
        return this;
    }
}
