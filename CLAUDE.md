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
5. **Ningún valor se pone a mano: todo se deriva.** Nadie arranca con credibilidad propia: fuentes, factos y estructuras parten del mismo valor neutral (0.2, el de una afirmación sin sostén) y suben o bajan por argumentos. En una fuente, los argumentos son sobre su sinceridad, competencia o independencia (D18, D27).
6. **Las copias no suman:** fuentes que derivan de la misma raíz cuentan como una.
7. La "vida propia" del documento = un proceso que sigue corriendo y propagando cambios, como un thread. No es una metáfora biológica.

Lo distribuido (red de nodos, bots, usuarios con árboles personales) es una idea a futuro, no está descartada pero no es el foco.

**No es de Maxi, es de Claude:** juzgar hipótesis por predicciones y rankearlas. Está implementado (`HypothesisRanker`), pero Maxi lo considera sobreingeniería y lo dejó fuera de `IDEA.md`. No construir más sobre eso sin preguntarle.

## Estado actual

- Java 17, Maven, sin dependencias de runtime, JUnit 5. 21 tests (`mvn test`). `mvn -q compile exec:java` corre `Demo`.
- Niveles: fuentes, factos y observaciones (0), estructuras (1), predicciones e hipótesis (2). Ver `README.md` para fórmulas.
- `engine/Credibility`: punto fijo tipo PageRank en log-odds, amortiguado. W = 4, una afirmación sin sostén vale 0.2.
- `engine/HypothesisRanker`: ajuste = cobertura − refutación.
- Casos: `SemmelweisCase` (fiebre puerperal 1847, con fuentes reales y la objeción de 1848) y `BodyworkCase` (piloto del método de Maxi). Cada caso tiene etapas = versiones del log. El caso del helado se eliminó por pedido de Maxi: era demasiado pelado.
- `docs/lab.html` replica el motor en JS y da los mismos números que Java a 6 decimales. Si se cambia la lógica, cambiar los dos y verificar paridad.

## Antecedentes (para no reinventar)

Analysis of Competing Hypotheses (Heuer, CIA), ECHO / coherencia explicativa (Thagard 1989), Wigmore/Schum (evidencia legal, redundancia corroborativa), Micropublications, Wikidata (datos con fuentes), Kialo (debates en árbol, sin base de factos), Community Notes (acuerdo entre personas que suelen discrepar), PageRank/TrustRank. Evidencia en contra de métodos estructurados: Dhami et al. 2019 (ACH no mejoró la precisión), el debate de Rootclaim. Lógicas relacionadas: no monótona/rebatible (Pollock), paraconsistente, abducción, argumentación (Dung).

Lo distintivo de esta idea frente a eso: base de factos compartida entre debates + debate recursivo sobre cualquier nodo + propagación de cambios.

## Problemas abiertos

- Diagnosticidad: evidencia que confirman todas las hipótesis igual suma cobertura.
- Predicción vs acomodación (predicciones escritas después de ver los datos).
- Supuestos auxiliares (Duhem-Quine): una predicción que falla le pega a toda la hipótesis.
- Fuentes falsas que dicen ser independientes (el equivalente a las granjas de enlaces de PageRank).
- `derivedFrom` es por fuente, no por afirmación.
- Persistencia del log; la parte comunitaria; usar un LLM para proponer árboles a partir de una frase (bajar el costo de formalizar).
- Próximo caso pensado por Maxi: "el kirchnerismo desendeudó al país", descomponiendo la frase en métricas (deuda total vs con privados, moneda, % del PBI, período) con fuentes de distintos lados, sin tomar posición.

## Cómo trabajar con Maxi

Escribe en castellano rioplatense. Prefiere respuestas directas, sin relleno y sin guiones largos. Programa en Java. Le interesa más la lógica del sistema que la UI. Le gusta ver los casos funcionando (una página o una salida concreta), no solo explicaciones.
