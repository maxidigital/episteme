package de.bottazzi.episteme;

import de.bottazzi.episteme.engine.ConflictDetector;
import de.bottazzi.episteme.engine.Credibility;
import de.bottazzi.episteme.engine.HypothesisRanker;
import de.bottazzi.episteme.model.NodeKind;
import de.bottazzi.episteme.store.KnowledgeGraph;

import java.util.Locale;

public final class Demo {

    private static final HypothesisRanker RANKER = new HypothesisRanker();
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
        g.nodes().stream().filter(n -> n.kind() == NodeKind.FACT || n.kind() == NodeKind.STRUCTURE)
                .forEach(n -> sb.append(String.format(" %s=%.3f(%s)", n.id(), c.p(n.id()), c.grade(n.id()))));
        System.out.println(sb);
        for (HypothesisRanker.Report r : RANKER.rank(g, c)) {
            System.out.printf("  %-3s ajuste %+.3f  cobertura %.3f  refutacion %.3f%n",
                    r.hypothesis(), r.fit(), r.coverage(), r.refutation());
            r.predictions().forEach(p -> System.out.printf("      %s %-9s +%.3f -%.3f%n",
                    p.prediction(), p.verdict(), p.confirmation(), p.refutation()));
        }
        CONFLICTS.detect(g, 0.3).forEach(x -> System.out.printf("  conflicto %s %s %.3f%n", x.kind(), x.id(), x.intensity()));
    }
}
