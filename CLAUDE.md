# Inteligencia Natural (episteme)

Proyecto personal de Maxi. Idea de hace ~20 años: una herramienta para que una comunidad discuta con argumentos en vez de opiniones sueltas (como una sección de comentarios, pero con factos, fuentes e hipótesis). Este repo es el núcleo lógico. La UI y la parte comunitaria vienen después.

Documento original de diseño (hecho con ChatGPT): `docs/resumen_maestro_sistema_epistemologico.pdf`. Página interactiva con la misma lógica en JS: `docs/lab.html` (abrir en el navegador).

## Principios decididos por Maxi (no cambiar sin preguntarle)

1. **Todo es debatible**, incluidas las fuentes (un dato del INDEC también). Cuestionar algo = agregar un nodo con su propia evidencia que lo apoya, objeta o contradice. Si alguien refuta un dato se abre un cluster nuevo con las mismas reglas: es recursivo.
2. **El sistema se regula solo con su matemática.** No hay moderadores que bloqueen debates: una objeción sin factos pesa casi nada y queda abajo.
3. **No existe el 0 ni el 1.** Todo valor está en el intervalo abierto (0, 1), tiende a 0 pero nunca llega (regla de Cromwell). Implementado en log-odds.
4. **Nada se borra.** Todo es un evento; las versiones y la historia salen del log.
5. **Los valores de estructuras e hipótesis no se ponen a mano: se derivan.** Lo único que se ingresa es la confianza de base de cada fuente.
6. **Las hipótesis se juzgan por sus predicciones**, que se cumplen o fallan según la evidencia. Las relaciones de predicción no llevan peso.
7. **Las copias no suman:** fuentes que derivan de la misma raíz cuentan como una.
8. La "vida propia" del documento = un proceso que sigue corriendo y propagando cambios, como un thread. No es una metáfora biológica.

Lo distribuido (red de nodos, bots, usuarios con árboles personales) es una idea a futuro, no está descartada pero no es el foco.

## Estado actual

- Java 21, Maven, sin dependencias de runtime, JUnit 5. 25 tests (`mvn test`). `mvn -q compile exec:java` corre `Demo`.
- Niveles: fuentes, factos y observaciones (0), estructuras (1), predicciones e hipótesis (2). Ver `README.md` para fórmulas.
- `engine/Credibility`: punto fijo tipo PageRank en log-odds, amortiguado. W = 4, una afirmación sin sostén vale 0.2.
- `engine/HypothesisRanker`: ajuste = cobertura − refutación.
- Casos: `IceCreamCase` (correlación no es causa) y `SemmelweisCase` (fiebre puerperal 1847, con fuentes reales y la objeción de 1848). Cada caso tiene etapas = versiones del log.
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
