package de.bottazzi.episteme;

import de.bottazzi.episteme.engine.Credibility;
import de.bottazzi.episteme.engine.HypothesisIndex;
import de.bottazzi.episteme.model.Node;
import de.bottazzi.episteme.store.EventLog;
import de.bottazzi.episteme.store.KnowledgeGraph;

import java.util.Locale;

/**
 * Imprime la credibilidad de cada nodo y el indice de cada hipotesis en cada etapa, con 6 decimales, en el mismo formato que
 * tools/lab-parity.mjs. Sirve para verificar que docs/lab.html da los mismos numeros que Java:
 * <pre>
 * tools/parity.sh
 * </pre>
 */
public final class LabParity {

    public static void main(String[] args) {
        var sem = SemmelweisCase.seed();
        print("semmelweis", sem.log(), sem.early(), sem.kolletschka(), sem.handwashing(), sem.objection());
        var bw = BodyworkCase.seed();
        print("bodywork", bw.log(), bw.pilot(), bw.answers());
    }

    private static void print(String caseId, EventLog log, long... stages) {
        for (int i = 0; i < stages.length; i++) {
            KnowledgeGraph g = log.stateAt(stages[i]);
            Credibility c = new Credibility(g);
            for (Node n : g.nodes())
                if (n.kind().isEvidence())
                    System.out.println(String.format(Locale.ROOT, "%s %d %s %.6f", caseId, i, n.id(), c.p(n.id())));
            for (HypothesisIndex.Report r : new HypothesisIndex(g, c).all())
                System.out.println(String.format(Locale.ROOT, "%s %d %s %.6f", caseId, i, r.hypothesis(), r.p()));
        }
    }
}
