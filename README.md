# episteme

Núcleo de "Inteligencia Natural": un sistema para discutir con argumentos. Toda afirmación es debatible, incluidas las fuentes; la credibilidad de cada cosa sale de su propio debate, y las hipótesis se juzgan por sus predicciones.

Java 17, sin dependencias de runtime. JUnit 5 para tests.

```
mvn test
mvn -q compile exec:java      # corre Demo con los dos casos
```

## Estructura

```
model/   Node, Source, Relation, RelationType, NodeKind, LogOdds, FactGrade, ids
store/   Event (sealed), EventLog (append-only), KnowledgeGraph (estado de solo lectura)
engine/  Credibility, HypothesisRanker (RoH), ConflictDetector, Propagator
IceCreamCase, SemmelweisCase   casos de prueba con etapas
CaseBuilder                    atajos para escribir casos
Demo                           imprime cada etapa
```

## Principios

1. **Todo es debatible.** Fuentes, factos y estructuras reciben apoyos (`SUPPORTS`), objeciones (`UNDERMINES`) y contradicciones (`CONTRADICTS`). Cuestionar algo es agregar un nodo con su propia evidencia.
2. **Nada se borra.** No hay "retirar" ni "regradar". Todo es un evento en el log; `stateAt(v)` reconstruye cualquier momento y `history(nodo)` cuenta qué le pasó a un nodo.
3. **Nunca 0 ni 1** (regla de Cromwell). Todo se calcula en log-odds; ningún valor finito corresponde a 0 o 1, y un nodo muy debilitado puede volver si alguien rebate lo que lo debilitó.
4. **Lo único que se ingresa a mano es la confianza de base de cada fuente**, que funciona como ancla (como las semillas de TrustRank) y también se puede debatir o cambiar con un evento (`SourcePriorSet`).
5. **Las copias no suman.** Cada fuente puede declarar de cuál deriva. Tres diarios que copian un comunicado son una sola fuente independiente.

## Credibilidad (`Credibility`)

Se calcula como punto fijo, al estilo PageRank, iterando con amortiguación hasta que nada cambia más de 1e-10.

```
base(fuente)      = logit(confianza de base)
base(facto)       = logit(0.2) + citas
base(estructura)  = logit(mín. sobre lo que la sostiene de p × fuerza) + citas
citas             = Σ por raíz independiente  W × máx(calidad de la cita × p(fuente))
L(n)              = base(n) + Σ apoyos W·k·p − Σ objeciones W·k·p − Σ contradicciones W·k·p
p(n)              = 1 / (1 + e^−L)
```

con W = 4. Una afirmación sin nada que la sostenga vale 0.2: poco creíble, nunca cero. Una contradicción es mutua (cada lado empuja al otro), así que gana el lado con más fuentes independientes. Una objeción (`UNDERMINES`) es de un solo lado: ataca la confiabilidad de algo sin que ese algo la ataque a ella. El grado A-D es solo una etiqueta de salida (A ≥ 0.90, B ≥ 0.75, C ≥ 0.50).

## Hipótesis (`HypothesisRanker`)

Una hipótesis no tiene números propios: hace predicciones, y cada predicción se cumple (`CONFIRMED_BY`) o falla (`REFUTED_BY`) según la evidencia. Esas relaciones no llevan peso.

| Estado | Cuándo |
|---|---|
| se cumple | evidencia creíble (p > 0.5) a favor, nada creíble en contra |
| falla | lo contrario |
| en disputa | evidencia creíble en los dos sentidos |
| por probar | nada creíble en ningún sentido: un experimento para hacer |

```
cobertura(H)  = Σ p(S) de las estructuras que confirman alguna predicción de H / Σ p(S) de todas
refutación(H) = Σ por predicción, p de la evidencia más creíble que la contradice
ajuste(H)     = cobertura(H) − refutación(H)
```

Es continuo: una refutación débil resta poco, pero algo. Una hipótesis que no predice nada queda en 0 y se ve.

## Conflictos y propagación

`ConflictDetector` lista predicciones que fallan o están en disputa, objeciones creíbles, evidencias que se contradicen e hipótesis rivales. `Propagator` responde "si cambia este nodo, qué conviene revisar", con el mejor camino y su impacto; llega desde una fuente hasta las hipótesis.

## Casos

**Helado.** Base: "el calor causa ambas" +1.00, "el helado ahoga" −0.51. Una objeción al estudio sobre calambres (muestra de 12 personas) baja ese facto de 0.84 a 0.28 sin borrarlo, y "el helado ahoga" sube a +0.05. Una objeción al termómetro baja el dato de temperatura a 0.24 y deja por probar las predicciones del calor.

**Semmelweis.** Todas las fuentes sobre mortalidad derivan de los registros del hospital y cuentan como una. A principios de 1847 las cuatro hipótesis de la época quedan negativas y ninguna explica el patrón de los cadáveres. La hipótesis cadavérica entra con +0.81 y una predicción por probar (lavarse las manos), que se confirma en junio (+0.85). La objeción de 1848 (antes y después sin grupo de control) baja ese dato de 0.88 a 0.55, pero la hipótesis apenas cambia (+0.84) porque no se apoya solo en él.

## Limitaciones conocidas

- La diagnosticidad no se mide: una estructura que confirman todas las hipótesis igual suma cobertura.
- No distingue predicción de acomodación (predicciones escritas después de ver los datos).
- No modela supuestos auxiliares: una predicción que falla le pega a la hipótesis entera.
- `derivedFrom` es por fuente, no por afirmación: una fuente que copia datos pero aporta relatos propios queda como copia para todo.
- Detectar fuentes que dicen ser independientes y no lo son queda fuera del cálculo.
- Persistencia del log (hoy en memoria).
