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

for (const line of window.__episteme.parity()) console.log(line);
