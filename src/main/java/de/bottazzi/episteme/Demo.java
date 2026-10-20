package de.bottazzi.episteme;

import de.bottazzi.episteme.engine.ConflictDetector;
import de.bottazzi.episteme.engine.Credibility;
import de.bottazzi.episteme.engine.HypothesisIndex;
import de.bottazzi.episteme.model.NodeKind;
import de.bottazzi.episteme.store.KnowledgeGraph;

import java.util.List;
import java.util.Locale;

public final class Demo {

    private static final ConflictDetector CONFLICTS = new ConflictDetector();

    public static void main(String[] args) {
        Locale.setDefault(Locale.ROOT);
        var sem = SemmelweisCase.seed();
        stage("Semmelweis: principios de 1847", sem.log().stateAt(sem.early()));
        stage("Semmelweis: muere Kolletschka", sem.log().stateAt(sem.kolletschka()));
        stage("Semmelweis: cal clorada", sem.log().stateAt(sem.handwashing()));
        stage("Semmelweis: objecion de 1848", sem.log().stateAt(sem.objection()));

        var bw = BodyworkCase.seed();
        stage("Bodywork: piloto", bw.log().stateAt(bw.pilot()));
        stage("Bodywork: con las respuestas de Maxi", bw.log().stateAt(bw.answers()));
    }

    private static void stage(String title, KnowledgeGraph g) {
        Credibility c = new Credibility(g);
        System.out.printf("%n%s  (%d iteraciones, %s)%n", title, c.iterations(), c.converged() ? "converge" : "NO converge");
        StringBuilder sb = new StringBuilder("  credibilidad:");
        g.nodes().stream().filter(n -> n.kind().isEvidence() && n.kind() != NodeKind.SOURCE)
                .forEach(n -> sb.append(String.format(" %s=%.3f(%s)", n.id(), c.p(n.id()), c.grade(n.id()))));
        System.out.println(sb);
        for (HypothesisIndex.Report r : new HypothesisIndex(g, c).all()) {
            System.out.printf("  %-3s indice %.3f  explica %s%s%s%n", r.hypothesis(), r.p(), r.explains(),
                    r.tension().isEmpty() ? "" : "  tensiona " + r.tension(),
                    r.unexplained().isEmpty() ? "" : "  no explica " + r.unexplained());
        }
        CONFLICTS.detect(g, 0.3).forEach(x -> System.out.printf("  conflicto %s %s %.3f%n", x.kind(), x.id(), x.intensity()));
    }
}
