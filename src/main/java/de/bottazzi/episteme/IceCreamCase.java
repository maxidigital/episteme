package de.bottazzi.episteme;

import de.bottazzi.episteme.store.EventLog;

import static de.bottazzi.episteme.model.RelationType.*;

/**
 * Caso chico y clasico: correlacion no es causa. Tres etapas en un solo log:
 * <ol>
 *   <li>base: datos de julio, tres correlaciones, tres hipotesis con sus predicciones.</li>
 *   <li>f4Challenged: alguien objeta el estudio sobre calambres (muestra chica).</li>
 *   <li>f3Challenged: alguien objeta el dato de temperatura (termometro mal calibrado).</li>
 * </ol>
 * Nada se borra ni se regrada: se agregan objeciones con su propia fuente.
 * Tres diarios que copian el mismo comunicado apoyan H1: una sola raiz independiente.
 */
public final class IceCreamCase {

    public record Stages(EventLog log, long base, long f4Challenged, long f3Challenged) {}

    private IceCreamCase() {}

    public static Stages seed() {
        CaseBuilder b = new CaseBuilder();

        b.source("src-comercio", "Registro de ventas de la camara de comercio", 0.90)
         .source("src-municipio", "Registro municipal de incidentes en playas", 0.90)
         .source("src-meteo", "Servicio meteorologico nacional", 0.95)
         .source("src-medico", "Estudio clinico sobre calambres y digestion", 0.85)
         .source("src-comunicado", "Comunicado de una asociacion de guardavidas", 0.60)
         .copy("src-diario-a", "Diario A reproduce el comunicado", "src-comunicado", 0.60)
         .copy("src-diario-b", "Diario B reproduce el comunicado", "src-comunicado", 0.60)
         .copy("src-diario-c", "Diario C cita al diario A", "src-diario-a", 0.50);

        b.fact("F1", "Las ventas de helado en julio fueron las mas altas del anio").cites("F1", "src-comercio", 0.9)
         .fact("F2", "Los ahogamientos en julio fueron los mas altos del anio").cites("F2", "src-municipio", 0.9)
         .fact("F3", "La temperatura media de julio fue la mas alta del anio").cites("F3", "src-meteo", 0.95)
         .fact("F4", "Comer antes de nadar no aumenta de forma relevante el riesgo de calambres").cites("F4", "src-medico", 0.9);

        b.structure("S1", "Ventas de helado y ahogamientos suben juntas").derive("S1", "F1", "F2")
         .structure("S2", "Temperatura y ventas de helado suben juntas").derive("S2", "F3", "F1")
         .structure("S3", "Temperatura y ahogamientos suben juntas").derive("S3", "F3", "F2");

        b.hypothesis("H1", "Comer helado causa ahogamientos")
         .hypothesis("H2", "El calor aumenta tanto el consumo de helado como el tiempo en el agua")
         .hypothesis("H3", "La coincidencia es casual");
        b.cites("H1", "src-diario-a", 0.4).cites("H1", "src-diario-b", 0.4).cites("H1", "src-diario-c", 0.3);

        b.prediction("P1", "Ventas de helado y ahogamientos deberian subir juntas")
         .prediction("P2", "Comer antes de nadar deberia aumentar el riesgo de calambres")
         .prediction("P3", "Con mas calor deberia venderse mas helado")
         .prediction("P4", "Con mas calor deberia haber mas ahogamientos")
         .prediction("P5", "La relacion no deberia repetirse en otros anios ni lugares");
        b.predicts("H1", "P1", "P2").predicts("H2", "P1", "P3", "P4").predicts("H3", "P5");
        b.confirmedBy("P1", "S1").refutedBy("P2", "F4").confirmedBy("P3", "S2").confirmedBy("P4", "S3");
        b.rel("H1", CONTRADICTS, "H2", 0.5);
        long base = b.version();

        b.source("src-revision", "Revision metodologica de estudios sobre calambres", 0.80)
         .fact("F5", "El estudio de F4 se hizo con una muestra de 12 personas").cites("F5", "src-revision", 0.9)
         .rel("F5", UNDERMINES, "F4", 0.8);
        long f4 = b.version();

        b.source("src-informe", "Informe tecnico de calibracion de estaciones", 0.85)
         .fact("F6", "El termometro de la estacion central estuvo mal calibrado en julio").cites("F6", "src-informe", 0.9)
         .rel("F6", UNDERMINES, "F3", 1.0);
        long f3 = b.version();

        return new Stages(b.log, base, f4, f3);
    }
}
