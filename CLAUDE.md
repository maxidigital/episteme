# Episteme, el motor de Inteligencia Natural

Inteligencia Natural (IN) es el proyecto de Maxi: una herramienta para que una comunidad discuta con argumentos y factos en vez de opiniones sueltas. Episteme es su motor: el núcleo lógico que calcula la credibilidad de la evidencia y el ajuste de las hipótesis. El nombre viene del griego: episteme, conocimiento justificado, frente a doxa, opinión. Este repo es solo el motor; la comunidad, la base de factos y la UI vienen después.

Originales (hechos con ChatGPT, congelados) en `docs/original/`: la charla donde nació la idea (`charla_chatgpt_original.md`) y el documento de diseño (`resumen_maestro_sistema_epistemologico.pdf`). El documento nuevo se escribe charlando con Maxi. Página interactiva con la misma lógica en JS: `docs/lab.html` (abrir en el navegador).

## Registro compartido

Las decisiones del proyecto viven en la planilla "Inteligencia Natural · Registro":
https://docs.google.com/spreadsheets/d/15ZFi_BMyPrQP24dY5NH8op1ipU06ik_vw1GRMrbeVAM

Maxi trabaja en paralelo con Claude (chat) y con Claude Code, y la planilla es el punto de sincronización.

- Leela antes de trabajar. Si no tenés acceso a Google Sheets, pedile a Maxi que te pase las filas relevantes.
- Al cerrar una discusión de diseño, agregá una fila por cada idea, decisión, conclusión, tensión o pregunta nueva. Si no podés escribir en la planilla, anotalas en `docs/decisiones.md` con el mismo formato (ID, Tipo, Contenido, Estado, Origen, Relacionado, Fecha, Dónde) para pasarlas después.
- No borres filas. Si algo cambia, marcá la vieja como "reemplazada" y agregá una nueva que la cite en Relacionado.
- Las filas con Origen "Maxi" solo las edita Maxi. Si proponés un cambio a una idea suya, agregalo como fila nueva con Origen "Claude".
- Fuentes de verdad: la idea está en `IDEA.md` (escrito por Maxi), el diseño validado en `docs/diseno.md` (y su PDF, `docs/diseno.pdf`), las decisiones en la planilla, la implementación en este repo. Si el código contradice la idea, se corrige el código.

## Principios decididos por Maxi (no cambiar sin preguntarle)

1. **Todo es debatible**, incluidas las fuentes (un dato del INDEC también). Cuestionar algo = agregar un nodo con su propia evidencia que lo apoya, objeta o contradice. Si alguien refuta un dato se abre un cluster nuevo con las mismas reglas: es recursivo.
2. **El sistema se regula solo con su matemática.** No hay moderadores que bloqueen debates: una objeción sin factos pesa casi nada y queda abajo.
3. **No existe el 0 ni el 1.** Todo valor está en el intervalo abierto (0, 1), tiende a 0 pero nunca llega (regla de Cromwell). Implementado en log-odds.
4. **Nada se borra.** Todo es un evento; las versiones y la historia salen del log.
5. **Ninguna credibilidad se pone a mano: todo se deriva.** Nadie arranca con credibilidad propia: fuentes, factos y estructuras parten del mismo valor neutral (0.2, el de una afirmación sin sostén) y suben o bajan por argumentos. En una fuente, los argumentos son sobre su sinceridad, competencia o independencia (D18, D27). Siguen a mano la calidad de cada cita y la fuerza de cada relación.
6. **Las copias no suman:** fuentes, elementos, apoyos y objeciones que derivan de la misma raíz cuentan como uno (D05, D16, D20).
7. La "vida propia" del documento = un proceso que sigue corriendo y propagando cambios, como un thread. No es una metáfora biológica.

Lo distribuido (red de nodos, bots, usuarios con árboles personales) es una idea a futuro, no está descartada pero no es el foco.

Las predicciones y el ranking de hipótesis (propuesta de Claude que Maxi consideró sobreingeniería) se sacaron del código el 21/10. Las hipótesis se juzgan por las estructuras que explican, dejan sin explicar o tensionan.

## Estado actual

- El diseño validado por Maxi está en `docs/diseno.md` y manda sobre el código. Fase 1 de "Qué cambia en el código" hecha el 21/10; fase 2 (clusters, D17) pendiente.
- Java 17, Maven, sin dependencias de runtime, JUnit 5. 34 tests (`mvn test`). `mvn -q compile exec:java` corre `Demo`. En esta máquina `java` es un JRE 21 sin `javac`: correr Maven con `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64`.
- Niveles: fuentes, observaciones, factos y deducciones (0), estructuras (1), hipótesis (2). Ver `README.md` para fórmulas.
- `engine/Credibility`: punto fijo tipo PageRank en log-odds, amortiguado. W = 4. Todo parte de 0.2 (`NEUTRAL`), fuentes incluidas. Patas, apoyos y ataques se agrupan por raíz: de cada grupo cuenta el mejor.
- `engine/HypothesisIndex`: L(H) = logit(0.2) + W·Σ p(S que explica) − W·Σ p(S que tensiona). Compuestas: unión de lo que explican sus partes, y se marca la parte que sobra.
- Casos: `SemmelweisCase` (fiebre puerperal 1847, con fuentes reales y la objeción de 1848) y `BodyworkCase` (piloto del método de Maxi). Cada caso tiene etapas = versiones del log. El caso del helado se eliminó por pedido de Maxi: era demasiado pelado.
- `docs/lab.html` replica el motor en JS (los dos casos) y da los mismos números que Java a 6 decimales. Si se cambia la lógica, cambiar los dos y correr `tools/parity.sh`.

## Antecedentes (para no reinventar)

Analysis of Competing Hypotheses (Heuer, CIA), ECHO / coherencia explicativa (Thagard 1989), Wigmore/Schum (evidencia legal, redundancia corroborativa), Micropublications, Wikidata (datos con fuentes), Kialo (debates en árbol, sin base de factos), Community Notes (acuerdo entre personas que suelen discrepar), PageRank/TrustRank. Evidencia en contra de métodos estructurados: Dhami et al. 2019 (ACH no mejoró la precisión), el debate de Rootclaim. Lógicas relacionadas: no monótona/rebatible (Pollock), paraconsistente, abducción, argumentación (Dung).

Lo distintivo de esta idea frente a eso: base de factos compartida entre debates + debate recursivo sobre cualquier nodo + propagación de cambios.

## Problemas abiertos

- Diagnosticidad: dos hipótesis que explican la misma estructura suman lo mismo (H08 y H09 en bodywork empatan).
- El índice de hipótesis satura: con W = 4, tres o cuatro estructuras creíbles lo llevan cerca de 1 y las diferencias se ven solo en log-odds.
- Con todo en 0.2, casi nada es creíble hasta que alguien argumenta sobre sus fuentes. Una copia (Wikipedia) no hereda la credibilidad de su raíz: vale por sus propios argumentos.
- El azar (D29) descuenta poco si el facto de fondo no tiene argumentos: su descuento se multiplica por su credibilidad.
- Una objeción de independencia baja la credibilidad de la fuente, pero no la une a la raíz de otra. Las raíces solo salen de `derivedFrom`.
- Fuentes falsas que dicen ser independientes (el equivalente a las granjas de enlaces de PageRank).
- `derivedFrom` es por fuente, no por afirmación: los relatos propios de Semmelweis cuentan como copia de los registros.
- La conjunción de D23 se revisa con los clusters (D26).
- Persistencia del log; la parte comunitaria; usar un LLM para proponer árboles a partir de una frase (bajar el costo de formalizar).
- Próximo caso pensado por Maxi: "el kirchnerismo desendeudó al país", descomponiendo la frase en métricas (deuda total vs con privados, moneda, % del PBI, período) con fuentes de distintos lados, sin tomar posición.

## Cómo trabajar con Maxi

Escribe en castellano rioplatense. Prefiere respuestas directas, sin relleno y sin guiones largos. Programa en Java. Le interesa más la lógica del sistema que la UI. Le gusta ver los casos funcionando (una página o una salida concreta), no solo explicaciones.
