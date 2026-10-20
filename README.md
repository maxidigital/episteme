# episteme

Núcleo de "Inteligencia Natural": un sistema para discutir con argumentos. Toda afirmación es debatible, incluidas las fuentes; la credibilidad de cada cosa sale de su propio debate, y las hipótesis se juzgan por las estructuras que explican. El diseño validado está en `docs/diseno.md`.

Java 17, sin dependencias de runtime. JUnit 5 para tests.

```
mvn test
mvn -q compile exec:java      # corre Demo con los dos casos
tools/parity.sh               # verifica que docs/lab.html dé los mismos números que Java
```

## Estructura

```
model/   Node, NodeKind, Source, Relation, RelationType, Aspect, LogOdds, FactGrade, ids
store/   Event (sealed), EventLog (append-only), KnowledgeGraph (estado de solo lectura)
engine/  Credibility, HypothesisIndex, ConflictDetector, Propagator
SemmelweisCase, BodyworkCase   casos con etapas
CaseBuilder                    atajos para escribir casos
Demo                           imprime cada etapa
LabParity                      números para comparar con el lab
```

## Principios

1. **Todo es debatible.** Fuentes, factos y estructuras reciben apoyos (`SUPPORTS`), objeciones (`UNDERMINES`) y contradicciones (`CONTRADICTS`). Cuestionar algo es agregar un nodo con su propia evidencia.
2. **Nada se borra.** Todo es un evento en el log; `stateAt(v)` reconstruye cualquier momento y `history(nodo)` cuenta qué le pasó a un nodo.
3. **Nunca 0 ni 1** (regla de Cromwell). Todo se calcula en log-odds; ningún valor finito corresponde a 0 o 1, y un nodo muy debilitado puede volver si alguien rebate lo que lo debilitó.
4. **Nadie arranca con credibilidad propia.** Todo parte de 0.2 (`NEUTRAL`), fuentes incluidas, y sube o baja por argumentos. Los argumentos sobre una fuente se etiquetan con sinceridad, competencia o independencia (`Aspect`).
5. **Las copias no suman.** Cada fuente puede declarar de cuál deriva. Lo que comparte raíz cuenta una sola vez: citas, elementos de una estructura, apoyos y objeciones.

## Niveles

| Nivel | Nodos |
|---|---|
| 0 | fuentes, observaciones, factos, deducciones |
| 1 | estructuras: patrones entre factos, sin decir por qué existen |
| 2 | hipótesis, simples o compuestas |

Un facto puede derivar de las observaciones que lo sostienen; una estructura, de sus elementos; una deducción, de sus premisas (todo con `DERIVED_FROM`).

## Credibilidad (`Credibility`)

Se calcula como punto fijo, al estilo PageRank, iterando con amortiguación hasta que nada cambia más de 1e-10.

```
L(n)          = logit(0.2) + W · sostén − W · ataques + W · apoyos
L(deducción)  = logit(mín sobre sus premisas de k · p) − W · ataques + W · apoyos
sostén        = Σ por raíz independiente de máx(k · p) de sus patas      (× azar, ver abajo)
apoyos        = Σ por raíz independiente de máx(k · p) de lo que la apoya
ataques       = Σ por raíz independiente de máx(k · p) de lo que la objeta, contradice o tensiona
p(n)          = 1 / (1 + e^−L)
```

con W = 4. Las patas de un nodo son las fuentes que cita (k = calidad de la cita) y los elementos de los que deriva. La raíz de una fuente sale de seguir `derivedFrom`; la de un facto o una estructura, de sus fuentes y sus elementos. Lo que no sale de ninguna fuente comparte una sola raíz: cien afirmaciones sin fuente cuentan como una.

- **Estructuras (D20).** Crecen con cada elemento independiente que las forma; no valen lo que su pata más débil.
- **Dudas contra observaciones (D16).** Si un facto se sostiene en varias observaciones, una duda contra una de ellas le saca solo esa pata.
- **Deducciones (D15).** No agregan contenido: valen lo que su premisa más débil, no citan fuentes y no le suman raíces a una estructura.
- **Azar (D22, D29).** Un facto de conocimiento de fondo puede decir cuántos elementos de una estructura daría el azar (`EXPECTED_BY_CHANCE`, con e). El sostén se multiplica por 1 − p(fondo) · mín(1, e/n), con n la cantidad de elementos independientes: cuentan solo los que superan al azar.

Una contradicción es mutua (cada lado empuja al otro), así que gana el lado con más fuentes independientes. Una objeción es de un solo lado. El grado A-D es solo una etiqueta de salida (A ≥ 0.90, B ≥ 0.75, C ≥ 0.50).

## Hipótesis (`HypothesisIndex`)

Una hipótesis se relaciona con estructuras: las explica (`EXPLAINS`), las deja sin explicar (`LEAVES_UNEXPLAINED`) o entra en tensión con ellas (`IN_TENSION_WITH`). Esas relaciones no llevan peso: el peso es la credibilidad de la estructura.

```
L(H) = logit(0.2) + W · Σ p(S que explica) − W · Σ p(S con la que entra en tensión)
```

Lo que deja sin explicar se muestra pero no mueve el número. No es un ranking ni la probabilidad de que la hipótesis sea cierta.

**Compuestas (D23).** Una hipótesis compuesta (`COMPOSED_OF`) explica lo suyo más lo de sus partes, cada estructura una vez. Cada parte tiene que explicar al menos una estructura que las otras no explican; si no, sobra y queda marcada en `redundantParts`.

## Conflictos y propagación

`ConflictDetector` lista hipótesis en tensión con una estructura, objeciones creíbles, evidencias que se contradicen e hipótesis rivales. `Propagator` responde "si cambia este nodo, qué conviene revisar", con el mejor camino y su impacto; llega desde una fuente hasta las hipótesis.

## Casos

**Semmelweis.** Todas las fuentes sobre mortalidad derivan de los registros del hospital y cuentan como una; los registros valen 0.49 porque los historiadores los usan como fuente. A principios de 1847 las cuatro hipótesis de la época entran en tensión con algo y ninguna explica que la mortalidad acompañe al contacto con cadáveres. La hipótesis cadavérica explica eso y tres estructuras más, sin tensiones. La objeción de 1848 baja el dato del lavado de manos de 0.75 a 0.57, pero pesa poco porque la Gazette no tiene argumentos a su favor.

**Bodywork.** El piloto del método de Maxi (cluster "desconexión"), con sus observaciones y las respuestas del 19/10. Casi todo se apoya en una sola raíz, la práctica de Maxi; lo más creíble es la liberación rápida dentro de una sesión, que tiene dos patas independientes (lo que vio Maxi y lo que dijo la bailarina). Incluye dos hipótesis compuestas: una donde cada parte aporta y otra donde las dos partes sobran.

## Limitaciones conocidas

- La diagnosticidad no se mide: dos hipótesis que explican la misma estructura suman lo mismo.
- El índice de hipótesis satura cerca de 1 con pocas estructuras creíbles; las diferencias se ven en log-odds.
- Con todo en 0.2, casi nada es creíble hasta que alguien argumenta sobre sus fuentes; una copia no hereda la credibilidad de su raíz.
- Una objeción de independencia baja la credibilidad de una fuente, pero no la une a otra raíz.
- `derivedFrom` es por fuente, no por afirmación.
- Detectar fuentes que dicen ser independientes y no lo son queda fuera del cálculo.
- Siguen a mano la calidad de cada cita y la fuerza de cada relación.
- Persistencia del log (hoy en memoria).
