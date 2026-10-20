package de.bottazzi.episteme;

import de.bottazzi.episteme.store.EventLog;

import static de.bottazzi.episteme.model.RelationType.*;

/**
 * Piloto de bodywork: el cluster "desconexion" de la base teorica del metodo de Maxi.
 * Tomado de la planilla "Bodywork · Piloto esquema epistemologico" (nodos O01-O09, S01-S05, H01-H06)
 * mas las respuestas de Maxi del 19/10 (liberacion rapida y lenta, recuperacion del uso, mensaje de voz).
 *
 * <p>Las predicciones las propuso Claude y Maxi las acepto al responder; H07 y H08 son de Maxi;
 * H06 y H09 son rivales propuestas por Claude para que H04 y H08 compitan con algo.
 * Correccion de Maxi (19/10): la fascia tambien cede dentro de una sesion. La prediccion
 * "la fascia no deberia ceder de golpe" la habia escrito Claude y era incorrecta; se saco.
 * Sin nombres de clientes.
 */
public final class BodyworkCase {

    public record Stages(EventLog log, long pilot, long answers) {}

    private BodyworkCase() {}

    public static Stages seed() {
        CaseBuilder b = new CaseBuilder();

        b.source("src-practica", "Practica de Maxi: lo que observa al palpar y trabajar, sin medicion")
         .source("src-testimonios", "Testimonios elegidos para el sitio");

        // ---- Etapa 1: el piloto tal como estaba en la planilla ----
        b.fact("F1", "Al palpar encuentra pelotas duras en los musculos, a veces tan duras que parecen hueso").cites("F1", "src-practica", 0.9)
         .fact("F2", "La mayoria llega con cierto grado de dureza; muchas personas tienen partes petrificadas").cites("F2", "src-practica", 0.8)
         .fact("F3", "Hay partes que la persona no puede usar: triceps, omoplatos, micromovimientos de mano, cuello, tobillo").cites("F3", "src-practica", 0.85)
         .fact("F4", "En zonas duras aparecen emociones guardadas o reacciones de defensa").cites("F4", "src-practica", 0.7)
         .fact("F5", "Con personas ultraflexibles que reportan mucho dolor no logro mucho").cites("F5", "src-practica", 0.6)
         .fact("F6", "Invitar a la zona a moverse muchas veces no progresa cuando esta demasiado dura").cites("F6", "src-practica", 0.8)
         .fact("F7", "Amigos con durezas fuertes cambiaron el cuerpo y no lo notaron o no lo asociaron al trabajo").cites("F7", "src-practica", 0.6)
         .fact("F8", "Testimonios: alivio concreto de dolor fisico").cites("F8", "src-testimonios", 0.8)
         .fact("F9", "Testimonios: aparecen traumas sin buscarlos").cites("F9", "src-testimonios", 0.8);

        b.structure("S1", "Dureza y perdida de uso aparecen en las mismas zonas").derive("S1", "F1", "F3")
         .structure("S2", "La dureza aparece junto con carga emocional").derive("S2", "F4", "F9")
         .structure("S4", "Hay cambio corporal sin registro consciente de la persona").derive("S4", "F7")
         .structure("S5", "Hay desconexion por dureza y tambien sin dureza (atrofia, desuso)").derive("S5", "F3", "F5");

        b.hypothesis("H01", "Donde hay dureza no hay flujo; la zona se desconecta y la persona no puede usarla")
         .hypothesis("H02", "Las durezas son fascia endurecida")
         .hypothesis("H03", "La desconexion tiene varios mecanismos: proteccion sin cierre, impulso contradicho, imposicion, desuso, cascada, desincronia")
         .hypothesis("H04", "El cuerpo adopta el cambio aunque la mente no lo registre")
         .hypothesis("H05", "La dureza es contraccion neuromuscular sostenida (puntos gatillo, defensa), no fascia")
         .hypothesis("H06", "En los casos sin registro no hubo un cambio relevante para la persona");

        b.prediction("P1", "Dureza y perdida de uso deberian coincidir en las mismas zonas")
         .prediction("P5", "La dureza deberia poder ceder de golpe dentro de una sesion")
         .prediction("P6", "Deberia haber desconexion tambien sin dureza")
         .prediction("P7", "La carga emocional deberia aparecer junto con la dureza")
         .prediction("P8", "Una medicion antes y despues deberia mostrar cambio aunque la persona no lo note")
         .prediction("P9", "Personas cercanas deberian cambiar sin notarlo")
         .prediction("P10", "Una medicion antes y despues no deberia mostrar cambio relevante");
        b.predicts("H01", "P1").predicts("H02", "P5").predicts("H05", "P5")
         .predicts("H03", "P1", "P6", "P7").predicts("H04", "P8", "P9").predicts("H06", "P10");
        b.confirmedBy("P1", "S1").confirmedBy("P6", "S5").confirmedBy("P7", "S2").confirmedBy("P9", "S4");
        b.rel("H02", CONTRADICTS, "H05", 0.5).rel("H04", CONTRADICTS, "H06", 1.0);
        long pilot = b.version();

        // ---- Etapa 2: respuestas de Maxi del 19/10 ----
        b.source("src-bailarina", "Lo que expreso una bailarina sobre su propio movimiento, y su review")
         .source("src-audio", "Mensaje de voz espontaneo de un paciente (dos sesiones)");

        b.fact("F10", "En una sesion aflojan muchas cosas; en el caso mas extremo, una bailarina se movia distinto esa misma noche")
         .cites("F10", "src-practica", 0.9).cites("F10", "src-bailarina", 0.85)
         .fact("F11", "Hay cambios que se ven a lo largo de semanas, entre 4 y 8").cites("F11", "src-practica", 0.8)
         .fact("F12", "Cuando una zona dura se ablanda, la persona recupera el uso (Maxi: siempre)").cites("F12", "src-practica", 0.85)
         .fact("F13", "Un paciente reporta que esta todo el dia sin cansarse, con una energia extra que antes no tenia").cites("F13", "src-audio", 0.9);

        b.fact("F14", "Despues de una sesion la persona se va con la movilidad ampliada").cites("F14", "src-practica", 0.9)
         .fact("F15", "Algunos notan el cambio, otros no, y otros solo notan la relajacion").cites("F15", "src-practica", 0.85);

        b.structure("S6", "Hay liberacion rapida, dentro de una sesion").derive("S6", "F10")
         .structure("S10", "El cambio de movilidad ocurre aunque la persona no siempre lo registre").derive("S10", "F14", "F15")
         .structure("S7", "Hay cambios lentos, de semanas").derive("S7", "F11")
         .structure("S8", "Al ablandarse una zona, vuelve el uso").derive("S8", "F12")
         .structure("S9", "Despues de las sesiones aparece mas energia").derive("S9", "F13");

        b.hypothesis("H07", "Las 4 a 8 semanas son el tiempo que necesita el cuerpo para retonificar los musculos de las zonas liberadas")
         .hypothesis("H08", "La energia extra es energia que la persona gastaba sosteniendo la tension")
         .hypothesis("H09", "La energia extra se explica por expectativa u otros cambios en la vida del paciente");

        b.prediction("P2", "Al ablandarse una zona deberia volver el uso")
         .prediction("P4", "Los cambios deberian llevar semanas, no aparecer de golpe")
         .prediction("P11", "Los cambios visibles deberian aparecer entre 4 y 8 semanas")
         .prediction("P12", "Medido semana a semana, el tono de las zonas liberadas deberia subir en ese plazo")
         .prediction("P13", "Despues de liberar tension deberia aparecer mas energia")
         .prediction("P14", "Deberia repetirse en otros pacientes, sin sugerirselo")
         .prediction("P15", "Deberia aparecer tambien sin sesiones, o no repetirse en otros pacientes");
        b.predicts("H01", "P2").predicts("H02", "P4").predicts("H07", "P11", "P12")
         .predicts("H08", "P13", "P14").predicts("H09", "P15");
        b.confirmedBy("P2", "S8").confirmedBy("P4", "S7").confirmedBy("P5", "S6")
         .confirmedBy("P11", "S7").confirmedBy("P13", "S9").confirmedBy("P9", "S10");
        b.rel("H08", CONTRADICTS, "H09", 0.6);
        long answers = b.version();

        return new Stages(b.log, pilot, answers);
    }
}
