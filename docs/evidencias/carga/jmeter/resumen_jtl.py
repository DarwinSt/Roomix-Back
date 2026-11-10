"""Resume un archivo de resultados de JMeter (.jtl, CSV) por escenario (grupo de hilos) y por petición, con la fila TOTAL.
Uso: python resumen_jtl.py <resultados.jtl> <resumen-por-escenario.json>"""
import csv
import json
import sys
from collections import defaultdict


def percentil(valores, p):
    v = sorted(valores)
    if not v:
        return 0
    k = max(0, min(len(v) - 1, int(round(p / 100 * len(v) + 0.5)) - 1))
    return v[k]


def fila(etiqueta, ms, ok, duracion_s):
    return {"etiqueta": etiqueta, "muestras": len(ms), "promedio": round(sum(ms) / len(ms)), "mediana": percentil(ms, 50),
            "p90": percentil(ms, 90), "p95": percentil(ms, 95), "p99": percentil(ms, 99), "min": min(ms), "max": max(ms),
            "errorPct": round(100 * (len(ok) - sum(ok)) / len(ok), 2), "throughput": round(len(ms) / duracion_s, 2)}


entrada, salida = sys.argv[1], sys.argv[2]
grupos = defaultdict(lambda: defaultdict(lambda: ([], [])))
tiempos = defaultdict(lambda: [float("inf"), 0])
with open(entrada, encoding="utf8") as fh:
    for r in csv.DictReader(fh):
        g = r["threadName"].rsplit(" ", 1)[0]
        ms, ok = int(r["elapsed"]), r["success"] == "true"
        ts = int(r["timeStamp"])
        grupos[g][r["label"]][0].append(ms)
        grupos[g][r["label"]][1].append(ok)
        tiempos[g][0] = min(tiempos[g][0], ts)
        tiempos[g][1] = max(tiempos[g][1], ts + ms)

res = {}
for g, etiquetas in grupos.items():
    dur = (tiempos[g][1] - tiempos[g][0]) / 1000
    filas = [fila(e, *etiquetas[e], dur) for e in sorted(etiquetas)]
    todos_ms = [m for e in etiquetas.values() for m in e[0]]
    todos_ok = [o for e in etiquetas.values() for o in e[1]]
    filas.append(fila("TOTAL", todos_ms, todos_ok, dur))
    res[g] = filas
orden = ["Carga normal", "Carga alta", "Estrés"]
res = {k: res[k] for k in orden if k in res} | {k: v for k, v in res.items() if k not in orden}
json.dump(res, open(salida, "w", encoding="utf8"), ensure_ascii=False, indent=1)
for g, f in res.items():
    t = f[-1]
    print(f"{g}: {t['muestras']} muestras, promedio {t['promedio']} ms, p95 {t['p95']} ms, errores {t['errorPct']} %, {t['throughput']}/s")
