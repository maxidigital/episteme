package de.bottazzi.episteme;

import de.bottazzi.episteme.store.EventLog;

import static de.bottazzi.episteme.model.RelationType.*;

/**
 * Piloto de bodywork: el cluster "desconexion" de la base teorica del metodo de Maxi.
 * Tomado de la planilla "Bodywork · Piloto esquema epistemologico" (nodos O01-O09, S01-S05, H01-H06)
 * mas las respuestas de Maxi del 19/10 (liberacion rapida y lenta, recuperacion del uso, mensaje de voz).
 *
 * <p>Las relaciones "explica / deja sin explicar / tensiona" salen de las predicciones que Claude propuso y
 * Maxi acepto al responder, pasadas a estructuras (20/10): cada prediccion que se cumplia con una estructura
 * es ahora "explica" esa estructura. H07 y H08 son de Maxi; H06 y H09 son rivales propuestas por Claude para
 * que H04 y H08 compitan con algo. Correccion de Maxi (19/10): la fascia tambien cede dentro de una sesion.
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

        b.explains("H01", "S1")
         .explains("H03", "S1", "S2", "S5")
         .explains("H04", "S4")
         .leavesUnexplained("H06", "S4");
        b.rel("H02", CONTRADICTS, "H05", 0.5).rel("H04", CONTRADICTS, "H06", 1.0);
        long pilot = b.version();

        // ---- Etapa 2: respuestas de Maxi del 19/10 ----
        b.source("src-bailarina", "Lo que expreso una bailarina sobre su propio movimiento, y su review")
         .source("src-audio", "Mensaje de voz espontaneo de un paciente (dos sesiones)");

        // Dos patas independientes: lo que vio Maxi y lo que dijo la bailarina. Dudar de una le saca solo esa.
        b.observation("O10", "La bailarina dijo que esa misma noche se movia distinto, y lo escribio en su review")
         .cites("O10", "src-bailarina", 0.85)
         .fact("F10", "En una sesion aflojan muchas cosas; en el caso mas extremo, una bailarina se movia distinto esa misma noche")
         .cites("F10", "src-practica", 0.9).observedIn("F10", "O10")
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

        b.explains("H01", "S8")
         .explains("H02", "S6", "S7")
         .explains("H05", "S6")
         .explains("H04", "S10")
         .leavesUnexplained("H06", "S10")
         .explains("H07", "S7")
         .explains("H08", "S9")
         .explains("H09", "S9");
        b.rel("H08", CONTRADICTS, "H09", 0.6);
        long answers = b.version();

        return new Stages(b.log, pilot, answers);
    }
}
