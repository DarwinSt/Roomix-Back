#!/bin/bash
# Ejecuta el plan de JMeter (modo no gráfico) contra un gemelo y genera el archivo de resultados, el reporte HTML y el resumen.
# Uso: jmeter_run.sh <jmeter-home> <jar-backend> <base-del-gemelo> <carpeta-salida>
# La credencial de prueba se lee de ../sim/credencial-prueba.txt y se entrega por archivo de propiedades (no se imprime).
set -e
AQUI="$(cd "$(dirname "$0")" && pwd)"
JMH="$1"; JAR="$2"; DB="$3"; OUTD="$4"
rm -rf "$OUTD"; mkdir -p "$OUTD/ejecucion"
DATABASE_URL="postgresql://postgres:1234@localhost:5432/$DB" PORT=8091 APP_SEED_ENABLED=false \
  JWT_SECRET="$(node -e "process.stdout.write(require('crypto').randomBytes(32).toString('hex'))")" CORS_ALLOWED_ORIGINS='http://localhost:*' \
  java -Xmx1024m -jar "$JAR" > "$OUTD/backend-jmeter.log" 2>&1 &
for i in $(seq 1 120); do curl -s -o /dev/null http://localhost:8091/actuator/health && break; sleep 1; done
PROPS="$(mktemp -d)/roomix.properties"
printf 'host=localhost\nport=8091\nclave=%s\n' "$(cat "$AQUI/../sim/credencial-prueba.txt")" > "$PROPS"
"$JMH/bin/jmeter.bat" -n -t "$(cygpath -w "$AQUI/Roomix-carga.jmx")" -q "$(cygpath -w "$PROPS")" \
  -l "$(cygpath -w "$OUTD/ejecucion/resultados.jtl")" -j "$(cygpath -w "$OUTD/ejecucion/jmeter.log")" \
  -e -o "$(cygpath -w "$OUTD/ejecucion/reporte")" 2>&1 | tail -15
rm -f "$PROPS"
powershell -NoProfile -Command '$c = Get-NetTCPConnection -LocalPort 8091 -State Listen -ErrorAction SilentlyContinue; if ($c) { Stop-Process -Id $c.OwningProcess -Force }'
python -X utf8 "$AQUI/jmeter/resumen_jtl.py" "$OUTD/ejecucion/resultados.jtl" "$OUTD/ejecucion/resumen-por-escenario.json"
