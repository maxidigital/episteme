package de.bottazzi.episteme;

import de.bottazzi.episteme.model.Aspect;
import de.bottazzi.episteme.store.EventLog;

import static de.bottazzi.episteme.model.RelationType.*;

/**
 * Semmelweis y la fiebre puerperal, Hospital General de Viena, 1841-1848. Cuatro etapas en un log:
 * <ol>
 *   <li>early: principios de 1847, datos de las dos clinicas y las cuatro explicaciones en circulacion.</li>
 *   <li>kolletschka: marzo de 1847, la hipotesis cadaverica; el lavado de manos queda por probar.</li>
 *   <li>handwashing: junio de 1847, el lavado con cal clorada confirma esa prediccion.</li>
 *   <li>objection: 1848, la objecion de la Gazette medicale: antes y despues sin grupo de control.</li>
 * </ol>
 * Las fuentes modernas (Wikipedia, James Lind Library) derivan de los escritos de Semmelweis, y estos
 * de los registros del hospital: todo cuenta como una sola raiz independiente. La autopsia de
 * Kolletschka y la Gazette son raices aparte.
 */
public final class SemmelweisCase {

    public record Stages(EventLog log, long early, long kolletschka, long handwashing, long objection) {}

    private SemmelweisCase() {}

    public static Stages seed() {
        CaseBuilder b = new CaseBuilder();

        b.source("src-reg", "Registros de mortalidad del Hospital General de Viena")
         .copy("src-etio", "Semmelweis, Etiologia de la fiebre puerperal (1861)", "src-reg")
         .copy("src-wiki", "Wikipedia, Ignaz Semmelweis", "src-etio")
         .copy("src-jll", "James Lind Library, estudios de Semmelweis", "src-etio")
         .source("src-hist", "Historiadores de la medicina (Loudon 2000; Carter, traduccion de la Etiologia, 1983)");

        // Nadie arranca con credibilidad propia: los registros valen por lo que se sabe de ellos.
        b.fact("F0", "Los registros de mortalidad eran oficiales y anuales, y los historiadores los usan como fuente")
         .cites("F0", "src-hist", 0.9)
         .about("F0", SUPPORTS, "src-reg", Aspect.COMPETENCE, 1.0);

        // ---- Etapa 1: principios de 1847 ----
        b.fact("F1", "Clinica 1 (medicos y estudiantes): mortalidad cercana al 10% entre 1841 y 1846").cites("F1", "src-reg", 0.95).cites("F1", "src-wiki", 0.9)
         .fact("F2", "Clinica 2 (parteras): mortalidad por debajo del 4% en el mismo periodo").cites("F2", "src-reg", 0.95).cites("F2", "src-wiki", 0.9)
         .fact("F3", "Las pacientes se asignaban a una u otra clinica en dias alternos").cites("F3", "src-jll", 0.9)
         .fact("F4", "La clinica 2 estaba siempre mas hacinada que la 1").cites("F4", "src-etio", 0.8).cites("F4", "src-wiki", 0.8)
         .fact("F5", "Entre las mujeres que parian en la calle la fiebre era rara").cites("F5", "src-etio", 0.7)
         .fact("F6", "En la clinica 1 medicos y estudiantes venian de hacer autopsias; las parteras no tocaban cadaveres").cites("F6", "src-etio", 0.9).cites("F6", "src-wiki", 0.9)
         .fact("F7", "Cambiar el recorrido del cura, sin la campanilla, no redujo la mortalidad").cites("F7", "src-etio", 0.8)
         .fact("F8", "Adoptar en la clinica 1 la posicion de parto de la clinica 2 no redujo la mortalidad").cites("F8", "src-etio", 0.8);

        b.structure("S1", "La clinica 1 mata mucho mas que la 2, con pacientes comparables").derive("S1", "F1", "F2", "F3")
         .structure("S2", "La diferencia no sigue al hacinamiento: la clinica mas llena mata menos").derive("S2", "F4", "F1", "F2")
         .structure("S3", "Fuera de la clinica (partos en la calle) hay menos fiebre que en la clinica 1").derive("S3", "F5", "F1")
         .structure("S4", "La diferencia acompana al contacto con cadaveres").derive("S4", "F6", "F1", "F2");

        b.hypothesis("H1", "Miasma: un aire viciado o una atmosfera epidemica")
         .hypothesis("H2", "Hacinamiento")
         .hypothesis("H3", "El miedo que provoca el cura al pasar con la campanilla")
         .hypothesis("H4", "La posicion de parto");

        b.prediction("P1", "La clinica 1 deberia matar mas que la 2")
         .prediction("P2", "Dos clinicas vecinas, con el mismo aire, deberian tener mortalidad parecida")
         .prediction("P3", "La clinica mas llena deberia matar mas")
         .prediction("P4", "Fuera del hospital deberia haber menos fiebre")
         .prediction("P5", "Cambiar el recorrido del cura deberia bajar la mortalidad")
         .prediction("P6", "Cambiar la posicion de parto deberia bajar la mortalidad");
        b.predicts("H1", "P2").predicts("H2", "P3", "P4").predicts("H3", "P1", "P5").predicts("H4", "P1", "P6");
        b.confirmedBy("P1", "S1").refutedBy("P2", "S1").refutedBy("P3", "S2").confirmedBy("P4", "S3")
         .refutedBy("P5", "F7").refutedBy("P6", "F8");
        long early = b.version();

        // ---- Etapa 2: marzo de 1847, muere Kolletschka ----
        b.source("src-aut", "Informe de autopsia de Jakob Kolletschka")
         .fact("F9", "Kolletschka murio tras cortarse en una autopsia, con hallazgos iguales a los de la fiebre puerperal")
         .cites("F9", "src-aut", 0.9).cites("F9", "src-etio", 0.9)
         .structure("S5", "Una herida con material de autopsia produce el mismo cuadro").derive("S5", "F9")
         .hypothesis("H5", "Particulas cadavericas que los medicos llevan en las manos")
         .prediction("P7", "La mortalidad deberia acompanar al contacto con cadaveres")
         .prediction("P8", "Una herida con material de autopsia deberia producir el mismo cuadro")
         .prediction("P9", "Lavarse las manos antes de atender partos deberia bajar la mortalidad")
         .predicts("H5", "P1", "P4", "P7", "P8", "P9")
         .confirmedBy("P7", "S4").confirmedBy("P8", "S5")
         .rel("H5", CONTRADICTS, "H1", 0.6);
        long kolletschka = b.version();

        // ---- Etapa 3: junio de 1847, lavado con cal clorada ----
        b.fact("F10", "Con lavado de manos con cal clorada (desde mayo), la clinica 1 bajo de 18.3% en abril a 2.2% en junio y 1.2% en julio")
         .cites("F10", "src-reg", 0.95).cites("F10", "src-wiki", 0.9)
         .structure("S6", "Al cortar el contacto con material cadaverico, la mortalidad cae").derive("S6", "F10")
         .confirmedBy("P9", "S6");
        long handwashing = b.version();

        // ---- Etapa 4: 1848, la objecion metodologica ----
        b.source("src-gaz", "Gazette medicale de Paris, editorial")
         .fact("F11", "Una caida de la mortalidad despues de una intervencion, sin grupo de control, no prueba que la intervencion la causo")
         .cites("F11", "src-gaz", 0.9)
         .rel("F11", UNDERMINES, "S6", 0.6);
        long objection = b.version();

        return new Stages(b.log, early, kolletschka, handwashing, objection);
    }
}
