"""Resume los resultados de los gemelos sintéticos y de la prueba de carga: tablas (JSON) y gráficos (PNG)."""
import glob
import json
import sys
from pathlib import Path

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt

OUT = Path(sys.argv[1])
CARGA = Path(sys.argv[2]) if len(sys.argv) > 2 else None
FIG = OUT / "fig"
FIG.mkdir(exist_ok=True)
VERDE, OCRE, AZUL, TINTA, TINTA2, EJE = "#397d54", "#c9a227", "#3b6ea5", "#1f2a24", "#5b6660", "#c9cfcb"
plt.rcParams.update({"font.family": "Times New Roman", "font.size": 10.5})


def p(v, q):
    v = sorted(v)
    return v[min(len(v) - 1, max(0, int(round(q / 100 * len(v) + 0.5)) - 1))] if v else 0


def estilo(ax):
    for s in ("top", "right"):
        ax.spines[s].set_visible(False)
    for s in ("left", "bottom"):
        ax.spines[s].set_color(EJE)
    ax.tick_params(colors=TINTA2, labelsize=9.5)
    ax.grid(True, color=EJE, linewidth=0.5)
    ax.set_axisbelow(True)


filas, lat_tam = [], {}
for f in sorted(x for x in glob.glob(str(OUT / "G*.json")) if Path(x).name[1:3].isdigit()):
    r = json.load(open(f, encoding="utf8"))
    g, s, x, i = r["gemelo"], r["simulacion"], r["roomix"], r["integridad"]
    por_op = {}
    for etq, ms, st in r["latencias"]:
        por_op.setdefault(etq, []).append(ms)
    lat_tam[g["id"]] = (g["habitaciones"], {k: p(v, 95) for k, v in por_op.items()})
    filas.append({
        "id": g["id"], "municipio": g["municipio"].title(), "tipo": g["tipo"].title(), "habitaciones": g["habitaciones"], "estrato": g["estrato"],
        "reservas": s["reservas"], "canceladas": s["canceladas"], "llamadas": r["llamadas"],
        "ocup_sim": round(100 * s["ocupacion"], 1), "ocup_roomix": x["ocupacionPct"], "noches_sim": s["nochesVendidas"], "noches_roomix": x["nochesVendidas"],
        "adr_sim": round(s["adr"]), "adr_roomix": round(x["adr"]), "revpar_sim": round(s["revpar"]), "revpar_roomix": round(x["revpar"]),
        "cruces": i["cruces"], "errores": i["nErrores"], "rechazadas": i["rechazadas"], "h01": i["reservasAnticipadasEnHabitacionNoLibre"],
        "facturado": i["totalFacturado"], "pagado": i["totalPagado"], "ingresos_fin": i["ingresosFinanzas"],
        # abonos recibidos al reservar que aún no están en una factura (incluye cancelaciones); 0 en corridas anteriores
        "abonos_sin_facturar": i.get("abonosSinFacturar", 0), "abonos_cancelados": i.get("abonosCancelados", 0), "penalidad": i.get("penalidadRetenida", 0), "devuelto": i.get("devueltoCancelaciones", 0),
        "p95_reserva": round(por_op.get("Crear reserva", [0]) and p(por_op["Crear reserva"], 95)),
        "p95_checkout": round(p(por_op.get("Check-out", [0]), 95)),
        "p95_dashboard": round(p(por_op.get("Consulta: dashboard", [0]), 95)),
        "p95_reporte": round(p(por_op.get("Consulta: reporte 60 días", [0]), 95)),
        "p95_disponibilidad": round(p(por_op.get("Consulta: disponibilidad", [0]), 95)),
        "muestra_errores": i["errores"][:5],
    })
tot = {
    "gemelos": len(filas), "habitaciones": sum(f["habitaciones"] for f in filas), "reservas": sum(f["reservas"] for f in filas),
    "llamadas": sum(f["llamadas"] for f in filas), "cruces": sum(f["cruces"] for f in filas), "errores": sum(f["errores"] for f in filas),
    "h01": sum(f["h01"] for f in filas), "noches_coinciden": sum(f["noches_sim"] == f["noches_roomix"] for f in filas),
    "adr_coinciden": sum(abs(f["adr_sim"] - f["adr_roomix"]) <= 1 for f in filas),
    "revpar_coinciden": sum(abs(f["revpar_sim"] - f["revpar_roomix"]) <= 1 for f in filas),
    "caja_cuadra": sum(abs(f["facturado"] - f["pagado"]) < 1 and abs(f["pagado"] + f["abonos_sin_facturar"] - f["ingresos_fin"]) < 1 for f in filas),
    "max_dif_ocup": max(abs(f["ocup_sim"] - f["ocup_roomix"]) for f in filas),
}
json.dump({"filas": filas, "totales": tot}, open(OUT / "resumen.json", "w", encoding="utf8"), ensure_ascii=False, indent=1)
print(json.dumps(tot, ensure_ascii=False))

# Figura 1: ocupación simulada frente a la calculada por Roomix
fig, ax = plt.subplots(figsize=(5.6, 4.0), dpi=300)
ax.plot([0, 90], [0, 90], color=EJE, linewidth=1, linestyle="--", label="Coincidencia exacta")
ax.scatter([f["ocup_sim"] for f in filas], [f["ocup_roomix"] for f in filas], s=[12 + f["habitaciones"] * 1.2 for f in filas],
           color=VERDE, edgecolor="white", linewidth=0.8, zorder=3, label="Gemelo (el tamaño del punto indica las habitaciones)")
ax.set_xlabel("Ocupación de la simulación (%)", color=TINTA2)
ax.set_ylabel("Ocupación calculada por Roomix (%)", color=TINTA2)
ax.set_xlim(0, 90); ax.set_ylim(0, 90)
estilo(ax)
ax.legend(frameon=False, fontsize=9, loc="upper left")
fig.tight_layout(); fig.savefig(FIG / "ocupacion.png", facecolor="white"); plt.close(fig)

# Figura 2: percentil 95 del tiempo de respuesta según el tamaño del hotel
fig, ax = plt.subplots(figsize=(6.2, 3.6), dpi=300)
for etq, color, nombre in [("Crear reserva", VERDE, "Crear reserva"), ("Check-out", OCRE, "Check-out con factura"),
                           ("Consulta: dashboard", AZUL, "Panel del día"), ("Consulta: reporte 60 días", TINTA, "Reporte de 60 días")]:
    pts = sorted((h, d.get(etq, 0)) for h, d in lat_tam.values())
    ax.plot([a for a, _ in pts], [b for _, b in pts], marker="o", markersize=3.5, linewidth=1.6, color=color, label=nombre)
ax.set_xscale("log")
ax.set_xlabel("Habitaciones del gemelo (escala logarítmica)", color=TINTA2)
ax.set_ylabel("Percentil 95 (ms)", color=TINTA2)
estilo(ax)
ax.legend(frameon=False, fontsize=9, ncol=2)
fig.tight_layout(); fig.savefig(FIG / "latencia-tamano.png", facecolor="white"); plt.close(fig)

# Prueba de carga
if CARGA and CARGA.exists():
    c = json.load(open(CARGA, encoding="utf8"))
    fig, ax = plt.subplots(figsize=(6.2, 3.4), dpi=300)
    for gr, color in zip(c["grupos"], [VERDE, OCRE, AZUL]):
        ax.plot([s["t"] for s in gr["serie"]], [s["promedio"] for s in gr["serie"]], color=color, linewidth=1.6,
                label=f'{gr["grupo"]["nombre"]} ({gr["grupo"]["hilos"]} usuarios)')
    ax.set_xlabel("Tiempo desde el inicio del grupo (s)", color=TINTA2)
    ax.set_ylabel("Tiempo de respuesta promedio (ms)", color=TINTA2)
    estilo(ax)
    ax.legend(frameon=False, fontsize=9)
    fig.tight_layout(); fig.savefig(FIG / "carga-tiempo.png", facecolor="white"); plt.close(fig)
    fig, ax = plt.subplots(figsize=(6.2, 3.4), dpi=300)
    for gr, color in zip(c["grupos"], [VERDE, OCRE, AZUL]):
        ax.plot([s["t"] for s in gr["serie"]], [s["muestras"] / 5 for s in gr["serie"]], color=color, linewidth=1.6,
                label=f'{gr["grupo"]["nombre"]} ({gr["grupo"]["hilos"]} usuarios)')
    ax.set_xlabel("Tiempo desde el inicio del grupo (s)", color=TINTA2)
    ax.set_ylabel("Peticiones por segundo", color=TINTA2)
    estilo(ax)
    ax.legend(frameon=False, fontsize=9)
    fig.tight_layout(); fig.savefig(FIG / "carga-throughput.png", facecolor="white"); plt.close(fig)
    for gr in c["grupos"]:
        t = gr["agregado"][-1]
        print(gr["grupo"]["nombre"], t, gr["errores"])
