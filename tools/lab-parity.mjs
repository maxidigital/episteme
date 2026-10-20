// Corre el motor de docs/lab.html sin navegador e imprime los mismos numeros que LabParity.java.
// Uso: tools/parity.sh
import { readFileSync } from 'node:fs';
import vm from 'node:vm';

const html = readFileSync(new URL('../docs/lab.html', import.meta.url), 'utf8');
const script = html.slice(html.lastIndexOf('<script>') + 8, html.lastIndexOf('</script>'));

// DOM falso: cualquier propiedad o llamada devuelve el mismo objeto, y no hace nada.
const dom = new Proxy(function () {}, {
  get: (t, k) => k === Symbol.toPrimitive ? () => '' : k === 'forEach' ? () => {} : dom,
  set: () => true,
  apply: () => dom,
});
const window = {};
vm.runInNewContext(script, { window, document: dom, console, Math, Object, Set, Map, Array, JSON });

// Prueba del azar (D29), que ningun caso usa todavia: la misma que LabParity.chance().
const R = (stage, from, type, to, s = 1.0) => ({ id: `${from}-${type}-${to}`, from, type, to, s, stage });
const azar = { sources: { cal: { d: 'calendario lunar', stage: 1 } }, nodes: { S: { kind: 'S', stage: 0 }, B: { kind: 'F', stage: 1 } },
  rels: [R(1, 'B', 'SOURCED_FROM', 'cal', 0.9), { ...R(1, 'B', 'EXPECTED_BY_CHANCE', 'S'), expected: 3 }],
  scenarios: [{ stage: 0 }, { stage: 1 }] };
for (let i = 0; i < 8; i++) {
  azar.sources['s' + i] = { d: 'testigo ' + i, stage: 0 };
  azar.nodes['T' + i] = { kind: 'F', stage: 0 };
  azar.rels.push(R(0, 'T' + i, 'SOURCED_FROM', 's' + i, 0.8), R(0, 'S', 'DERIVED_FROM', 'T' + i));
}
window.__episteme.CASES.azar = azar;

for (const line of window.__episteme.parity()) console.log(line);
