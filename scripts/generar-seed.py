# Genera src/main/resources/db/seed-data.sql (determinístico: misma salida en cada ejecución).
# Uso: python scripts/generar-seed.py src/main/resources/db/seed-data.sql
import random
import sys

rnd = random.Random(20261015)
OUT = sys.argv[1]

def q(s):
    return "NULL" if s is None else "'" + str(s).replace("'", "''") + "'"

def d(off):
    """Fecha relativa a hoy (hora Colombia)."""
    if off == 0:
        return "pg_temp.hoy()"
    return f"(pg_temp.hoy() {'+' if off > 0 else '-'} {abs(off)})"

def ts(off, hhmm):
    return f"({d(off)} + TIME '{hhmm}')"

def hace(minutos):
    """Instante de hoy, `minutos` antes de ahora (nunca antes de las 00:00 de hoy)."""
    return f"pg_temp.hace({minutos})"

out = []
w = out.append

# ---------------------------------------------------------------------------
# Tarifas (COP)
# ---------------------------------------------------------------------------
TARIFAS = {
    "INDIVIDUAL": 185000, "DOBLE": 265000, "TRIPLE": 330000,
    "FAMILIAR": 390000, "EJECUTIVA": 430000, "SUITE": 680000,
}

# ---------------------------------------------------------------------------
# Habitaciones: (numero, tipo, precio, descripcion, caracteristicas)
# ---------------------------------------------------------------------------
BASE = ["WiFi", "Aire acondicionado", "Baño privado", "Agua caliente", "TV Smart 43\""]
H = []
def hab(num, tipo, desc, extra, recargo=0):
    precio = TARIFAS[tipo] + recargo
    H.append(dict(numero=num, tipo=tipo, precio=precio, desc=desc, car=BASE + extra))

hab("101", "INDIVIDUAL", "Habitación individual con cama sencilla de 1,20 m, escritorio y ventana hacia el patio colonial.", ["Escritorio de trabajo"])
hab("102", "INDIVIDUAL", "Habitación individual tranquila junto al patio interior, ideal para viajeros de negocios.", ["Escritorio de trabajo", "Caja fuerte"])
hab("103", "INDIVIDUAL", "Habitación individual accesible en primer piso, baño adaptado con barras de apoyo.", ["Accesible para silla de ruedas", "Escritorio de trabajo"])
hab("104", "DOBLE", "Habitación doble con cama queen y vista al patio colonial.", ["Minibar", "Caja fuerte"])
hab("105", "DOBLE", "Habitación doble con dos camas sencillas, cerca de recepción.", ["Minibar"])
hab("106", "DOBLE", "Habitación doble con cama queen, techos altos y ventanales de madera.", ["Minibar", "Caja fuerte", "Secador de cabello"])
hab("107", "DOBLE", "Habitación doble con dos camas semidobles y acceso directo a la piscina.", ["Minibar", "Acceso a piscina"], 20000)
hab("108", "DOBLE", "Habitación doble con cama queen y baño con ducha tipo lluvia.", ["Minibar", "Secador de cabello"])
hab("201", "DOBLE", "Habitación doble con cama king y balcón hacia la calle peatonal.", ["Minibar", "Balcón", "Caja fuerte"], 25000)
hab("202", "DOBLE", "Habitación doble con dos camas semidobles y escritorio.", ["Minibar", "Escritorio de trabajo"])
hab("203", "DOBLE", "Habitación doble con cama queen, decoración de estilo republicano.", ["Minibar", "Caja fuerte"])
hab("204", "DOBLE", "Habitación doble con cama king y balcón con vista a la ciudad amurallada.", ["Minibar", "Balcón", "Vista a la ciudad amurallada"], 35000)
hab("205", "TRIPLE", "Habitación triple con cama queen y cama sencilla adicional.", ["Minibar", "Caja fuerte"])
hab("206", "TRIPLE", "Habitación triple con tres camas sencillas, ideal para grupos de amigos.", ["Minibar"])
hab("207", "FAMILIAR", "Habitación familiar de 38 m² con cama king, dos camas sencillas y sofá cama.", ["Minibar", "Sofá cama", "Cuna disponible", "Caja fuerte"])
hab("208", "FAMILIAR", "Habitación familiar con dos ambientes comunicados y dos baños.", ["Minibar", "Sofá cama", "Cuna disponible", "Dos baños"], 30000)
hab("301", "DOBLE", "Habitación doble superior con cama king y vista parcial al mar.", ["Minibar", "Caja fuerte", "Vista al mar"], 45000)
hab("302", "DOBLE", "Habitación doble superior con dos camas dobles y balcón.", ["Minibar", "Balcón", "Caja fuerte"], 30000)
hab("303", "DOBLE", "Habitación doble superior con cama queen, cafetera y amenities premium.", ["Minibar", "Cafetera", "Secador de cabello"], 20000)
hab("304", "EJECUTIVA", "Habitación ejecutiva con cama king, escritorio ergonómico e impresora bajo solicitud.", ["Minibar", "Escritorio de trabajo", "Cafetera", "Caja fuerte"])
hab("305", "EJECUTIVA", "Habitación ejecutiva con zona de trabajo, sillón de lectura y vista a la ciudad.", ["Minibar", "Escritorio de trabajo", "Cafetera", "Vista a la ciudad amurallada"])
hab("306", "EJECUTIVA", "Habitación ejecutiva esquinera con doble ventanal y sala pequeña.", ["Minibar", "Escritorio de trabajo", "Cafetera", "Sala de estar"], 25000)
hab("307", "FAMILIAR", "Habitación familiar con cama king, camarote infantil y zona de juegos.", ["Minibar", "Sofá cama", "Cuna disponible", "Camarote infantil"])
hab("308", "TRIPLE", "Habitación triple con cama king y cama sencilla, balcón con vista al mar.", ["Minibar", "Balcón", "Vista al mar"], 40000)
hab("401", "EJECUTIVA", "Habitación ejecutiva premium con vista al mar y acceso al lounge del último piso.", ["Minibar", "Escritorio de trabajo", "Cafetera", "Vista al mar", "Acceso a lounge"], 60000)
hab("402", "EJECUTIVA", "Habitación ejecutiva premium con terraza privada.", ["Minibar", "Escritorio de trabajo", "Cafetera", "Terraza privada"], 50000)
hab("403", "SUITE", "Suite junior con sala de estar, cama king y bañera.", ["Minibar", "Cafetera", "Sala de estar", "Bañera", "Caja fuerte"])
hab("404", "SUITE", "Suite con jacuzzi en terraza y vista panorámica al mar Caribe.", ["Minibar", "Cafetera", "Sala de estar", "Jacuzzi", "Vista al mar", "Terraza privada"], 170000)
hab("405", "SUITE", "Suite presidencial de 70 m² con comedor, dos baños y mayordomo bajo solicitud.", ["Minibar", "Cafetera", "Sala de estar", "Comedor", "Jacuzzi", "Vista al mar", "Dos baños"], 420000)
hab("406", "SUITE", "Suite romántica con cama king con dosel, bañera de hidromasaje y balcón.", ["Minibar", "Cafetera", "Bañera de hidromasaje", "Balcón", "Vista a la ciudad amurallada"], 90000)
for i, h in enumerate(H, start=1):
    h["id"] = i
by_num = {h["numero"]: h for h in H}

# ---------------------------------------------------------------------------
# Huéspedes
# ---------------------------------------------------------------------------
NOM_M = ["Andrés", "Carlos", "Juan Pablo", "Santiago", "Felipe", "Sebastián", "Alejandro", "Daniel", "Mauricio", "Jorge",
         "Luis Fernando", "Camilo", "Julián", "Diego", "Óscar", "Ricardo", "Hernán", "Esteban", "Nicolás", "Gustavo"]
NOM_F = ["María Fernanda", "Laura", "Valentina", "Daniela", "Catalina", "Natalia", "Paola", "Carolina", "Juliana", "Andrea",
         "Diana", "Ángela", "Luisa", "Sofía", "Mariana", "Claudia", "Adriana", "Lina", "Manuela", "Isabel"]
APE = ["Rodríguez", "Gómez", "González", "Martínez", "García", "López", "Hernández", "Sánchez", "Ramírez", "Pérez",
       "Díaz", "Muñoz", "Rojas", "Moreno", "Jiménez", "Vargas", "Castro", "Ospina", "Restrepo", "Cárdenas",
       "Montoya", "Zapata", "Arango", "Valencia", "Quintero", "Mejía", "Henao", "Salazar", "Ortiz", "Duque",
       "Acosta", "Herrera", "Gutiérrez", "Londoño", "Correa", "Marín", "Bermúdez", "Pineda", "Suárez", "Ríos"]
EXTRANJEROS = [
    ("Estados Unidos", "+1", [("Michael", "Johnson"), ("Emily", "Davis"), ("Robert", "Wilson"), ("Jessica", "Brown"), ("David", "Anderson")], lambda: str(rnd.randint(500000000, 599999999))),
    ("España", "+34", [("Javier", "Fernández Ruiz"), ("Lucía", "Navarro Gil"), ("Pablo", "Serrano Molina")], lambda: "PA" + "".join(rnd.choice("BCDFGHJKLMNPRSTVWXYZ") for _ in range(1)) + str(rnd.randint(100000, 999999))),
    ("México", "+52", [("Ana Sofía", "Treviño Garza"), ("Emiliano", "Cervantes Ruiz"), ("Ximena", "Villarreal Soto")], lambda: "G" + str(rnd.randint(10000000, 99999999))),
    ("Argentina", "+54", [("Martín", "Fernández"), ("Florencia", "Ibarra")], lambda: "AAF" + str(rnd.randint(100000, 999999))),
    ("Chile", "+56", [("Benjamín", "Soto Fuentes"), ("Josefa", "Contreras Muñoz")], lambda: "F" + str(rnd.randint(10000000, 99999999))),
    ("Ecuador", "+593", [("Mateo", "Andrade Castillo"), ("Camila", "Villacís Torres")], lambda: "A" + str(rnd.randint(1000000, 9999999))),
    ("Francia", "+33", [("Julien", "Moreau"), ("Camille", "Lefèvre")], lambda: str(rnd.randint(10, 99)) + "".join(rnd.choice("ABCDEFGHJKLMNPRSTUVWXYZ") for _ in range(2)) + str(rnd.randint(10000, 99999))),
    ("Alemania", "+49", [("Lukas", "Schneider"), ("Anna", "Becker")], lambda: "C" + "".join(rnd.choice("0123456789CFGHJKLMNPRTVWXYZ") for _ in range(8))),
    ("Canadá", "+1", [("Olivia", "Tremblay"), ("Liam", "Gagnon")], lambda: "".join(rnd.choice("ABCDEFGHJKLMNPRSTUVWXYZ") for _ in range(2)) + str(rnd.randint(100000, 999999))),
    ("Perú", "+51", [("Diego", "Quispe Huamán")], lambda: str(rnd.randint(10000000, 79999999))),
]
NOTAS = [None] * 14 + [
    "Cliente frecuente; prefiere habitaciones en piso alto.",
    "Alergia a los frutos secos; informar a cocina para el desayuno.",
    "Viaja por trabajo; solicita factura electrónica a nombre de su empresa.",
    "Solicita almohadas hipoalergénicas.",
    "Llega en vuelo nocturno; check-in tardío habitual.",
    "Celebra aniversario; ofrecer decoración de cortesía.",
    "Prefiere habitación lejos del ascensor.",
    "Viaja con mascota pequeña (política pet friendly aceptada).",
    "Requiere cuna para bebé.",
    "Vegetariano; preferencias registradas para room service.",
]
DOMINIOS = ["gmail.com", "gmail.com", "gmail.com", "hotmail.com", "outlook.com", "yahoo.com"]

def slug(s):
    t = s.lower()
    for a, b in zip("áéíóúñü", "aeiounu"):
        t = t.replace(a, b)
    return "".join(c for c in t if c.isalnum() or c == " ").split()

G = []
docs = set()
emails = set()

def add_guest(nombre, apellidos, tipo_doc, doc, tel, nac, nacim):
    while doc in docs:
        doc = doc[:-1] + str(rnd.randint(0, 9))
    docs.add(doc)
    n = slug(nombre)[0]
    a = slug(apellidos)[0]
    base = rnd.choice([f"{n}.{a}", f"{n}{a}", f"{n}_{a}", f"{n[0]}{a}"]) + rnd.choice(["", str(rnd.randint(1, 99)), str(rnd.randint(70, 99))])
    email = base + "@" + rnd.choice(DOMINIOS)
    while email in emails:
        email = base + str(rnd.randint(100, 999)) + "@" + rnd.choice(DOMINIOS)
    emails.add(email)
    G.append(dict(id=len(G) + 1, nombre=nombre, apellidos=apellidos, tipo=tipo_doc, doc=doc.upper(), email=email,
                  tel=tel, nac=nac, nacim=nacim, notas=rnd.choice(NOTAS), activo=True))

def fecha_nac():
    return f"{rnd.randint(1955, 2004)}-{rnd.randint(1, 12):02d}-{rnd.randint(1, 28):02d}"

def cel_co():
    pref = rnd.choice(["300", "301", "304", "310", "311", "312", "313", "314", "315", "316", "317", "318", "320", "321", "350"])
    return f"+57 {pref} {rnd.randint(100, 999)} {rnd.randint(1000, 9999)}"

usados = set()
while len(G) < 95:
    fem = rnd.random() < 0.5
    nombre = rnd.choice(NOM_F if fem else NOM_M)
    a1, a2 = rnd.sample(APE, 2)
    if (nombre, a1, a2) in usados:
        continue
    usados.add((nombre, a1, a2))
    cedula = str(rnd.randint(1000000000, 1199999999)) if rnd.random() < 0.6 else str(rnd.randint(10000000, 99999999))
    add_guest(nombre, f"{a1} {a2}", "CEDULA", cedula, cel_co(), "Colombia", fecha_nac())

for pais, ind, personas, gen_doc in EXTRANJEROS:
    for nombre, ape in personas:
        tel = f"{ind} {rnd.randint(200, 999)} {rnd.randint(100, 999)} {rnd.randint(1000, 9999)}"
        add_guest(nombre, ape, "PASAPORTE", gen_doc(), tel, pais, fecha_nac())

# Un huésped desactivado (registro duplicado depurado)
G[17]["activo"] = False
G[17]["notas"] = "Registro inactivo: datos depurados por solicitud del titular."
inactivo_id = G[17]["id"]
rnd.shuffle(G)
for i, g in enumerate(G, start=1):
    g["id"] = i
inactivo_id = next(g["id"] for g in G if not g["activo"])
huespedes_activos = [g["id"] for g in G if g["activo"]]

# ---------------------------------------------------------------------------
# Personal
# ---------------------------------------------------------------------------
P = [
    ("Luz Marina Cárdenas", "Supervisora de housekeeping", "Housekeeping", True),
    ("Yolanda Ospina Rivera", "Camarera de pisos", "Housekeeping", True),
    ("Sandra Milena Torres", "Camarera de pisos", "Housekeeping", True),
    ("Jhon Fredy Mosquera", "Camarero de pisos", "Housekeeping", True),
    ("Leidy Johana Pérez", "Camarera de pisos", "Housekeeping", True),
    ("Marta Lucía Banquez", "Camarera de pisos", "Housekeeping", False),
    ("Wilson Arturo Rincón", "Técnico de mantenimiento", "Mantenimiento", True),
    ("Édgar Julio Pacheco", "Técnico electricista", "Mantenimiento", True),
    ("Rafael Antonio Puello", "Jefe de mantenimiento", "Mantenimiento", True),
    ("Kelly Johanna Herrera", "Mesera de room service", "Alimentos y bebidas", True),
    ("Brayan Stiven Julio", "Mesero de room service", "Alimentos y bebidas", True),
    ("Carmen Rosa Villadiego", "Recepcionista", "Recepción", True),
    ("Andrés Felipe Zúñiga", "Botones", "Recepción", True),
    ("Mauricio Ruiz Torres", "Coordinador de eventos", "Eventos", True),
    ("Patricia Gómez Salcedo", "Contadora", "Finanzas", True),
    ("Óscar Castillo Mendoza", "Almacenista", "Almacén", True),
]
PID = {p[0]: i for i, p in enumerate(P, start=1)}
HK = [PID[n] for n in ("Yolanda Ospina Rivera", "Sandra Milena Torres", "Jhon Fredy Mosquera", "Leidy Johana Pérez", "Luz Marina Cárdenas")]
MT = [PID[n] for n in ("Wilson Arturo Rincón", "Édgar Julio Pacheco", "Rafael Antonio Puello")]

# ---------------------------------------------------------------------------
# Estado actual de cada habitación
# ---------------------------------------------------------------------------
OCUPADAS = ["104", "106", "201", "204", "207", "301", "304", "306", "308", "401", "404", "405"]
RESERVADAS = ["105", "202", "205", "303", "402", "406"]
POST_CHECKOUT_HOY = ["108", "302"]       # limpieza post check-out pendiente
MANTENIMIENTO_HOY = "305"                # mantenimiento programado hoy (ADECUACION_PROGRAMADA)
MANTENIMIENTO_FUTURO = ("203", 3)        # LIBRE, mantenimiento en 3 días
# El resto queda LIBRE

reservas = []
huespedes_en_uso = set()

def nuevo_huesped_activo():
    while True:
        g = rnd.choice(huespedes_activos)
        if g not in huespedes_en_uso:
            huespedes_en_uso.add(g)
            return g

def tarifa(num):
    return by_num[num]["precio"]

def dia_creacion(ent):
    dia = ent - rnd.randint(3, 40)
    return dia if dia < 0 else -rnd.randint(1, 20)

def add_res(num, huesped, ent, sal, estado, cin=None, cout=None, creada=None):
    noches = sal - ent
    t = tarifa(num)
    reservas.append(dict(id=len(reservas) + 1, hab=by_num[num]["id"], num=num, huesped=huesped, ent=ent, sal=sal,
                         noches=noches, estado=estado, tarifa=t, total=t * noches, cin=cin, cout=cout,
                         creada=creada or ts(dia_creacion(ent), f"{rnd.randint(8, 21):02d}:{rnd.choice(['05', '12', '27', '33', '41', '48', '56'])}")))
    return reservas[-1]

def hhmm(h1, h2):
    return f"{rnd.randint(h1, h2):02d}:{rnd.randint(0, 59):02d}"

estado_hab = {}
activa_por_hab = {}
DIAS_HISTORIA = 75

for h in H:
    num = h["numero"]
    # Límite superior de la historia (día en que la habitación queda disponible)
    if num in OCUPADAS:
        ent = -rnd.randint(0, 4)
        sal = rnd.randint(1, 5)
        g = nuevo_huesped_activo()
        cin = hace(rnd.randint(60, 300)) if ent == 0 else ts(ent, hhmm(14, 21))
        activa_por_hab[num] = add_res(num, g, ent, sal, "EN_CURSO", cin=cin)
        limite = min(ent, -1)
        estado_hab[num] = "OCUPADO"
    elif num in RESERVADAS:
        ent = rnd.choice([0, 1, 2, 4, 6, 9, 14, 21])
        sal = ent + rnd.randint(2, 6)
        g = nuevo_huesped_activo()
        activa_por_hab[num] = add_res(num, g, ent, sal, "CONFIRMADA")
        limite = -1
        estado_hab[num] = "RESERVADO"
    elif num in POST_CHECKOUT_HOY:
        limite = 0
        estado_hab[num] = "INHABILITADO_PC"
    elif num == MANTENIMIENTO_HOY:
        limite = -1
        estado_hab[num] = "INHABILITADO_MT"
    else:
        limite = -rnd.randint(1, 3)
        estado_hab[num] = "LIBRE"

    # Historia hacia atrás desde `limite`
    sal = limite
    primera = True
    while True:
        if primera and num in POST_CHECKOUT_HOY:
            gap = 0
        else:
            gap = rnd.choice([0, 0, 1, 1, 2, 3, 4, 6])
        primera = False
        sal = sal - gap
        noches = rnd.choice([1, 2, 2, 3, 3, 3, 4, 5, 7])
        ent = sal - noches
        if ent < -DIAS_HISTORIA:
            break
        g = rnd.choice(huespedes_activos + [inactivo_id])
        if rnd.random() < 0.08:
            add_res(num, g, ent, sal, "CANCELADA")
            # La habitación quedó libre: otra reserva pudo ocupar el hueco
        else:
            cin = ts(ent, hhmm(14, 22))
            if sal == 0:
                cout = hace(200)
            else:
                cout = ts(sal, hhmm(8, 12))
            add_res(num, g, ent, sal, "FINALIZADA", cin=cin, cout=cout)
        sal = ent

reservas.sort(key=lambda r: (r["ent"], r["hab"]))
for i, r in enumerate(reservas, start=1):
    r["id"] = i

# ---------------------------------------------------------------------------
# Incidencias
# ---------------------------------------------------------------------------
TAREAS = {
    "LIMPIEZA": ["Preparar materiales de limpieza", "Ingresar a la habitación", "Limpiar baño", "Cambiar ropa de cama",
                 "Limpiar superficies y piso", "Revisión final del servicio"],
    "MANTENIMIENTO": ["Revisión técnica inicial", "Ejecutar reparación o adecuación", "Verificación de funcionamiento",
                      "Cierre de mantenimiento"],
    "SERVICIO_CUARTO": ["Preparar pedido en cocina", "Entregar servicio en habitación", "Retirar vajilla y residuos",
                        "Confirmar servicio con huésped o recepción"],
    "OTRO": ["Iniciar servicio", "Ejecutar tareas del servicio", "Verificación final"],
}
TAREAS_ZC = {
    "MANTENIMIENTO": ["Revisión inicial del área", "Ejecutar reparación o adecuación", "Verificación de funcionamiento",
                      "Cierre de mantenimiento"],
    "OTRO": ["Iniciar servicio en zona común", "Ejecutar tareas del servicio", "Verificación final"],
}
INC = []
TAR = []

def add_inc(alcance, tipo, titulo, desc, estado, creada, hab=None, ubic=None, personal=None, ctx=None,
            programada=None, finalizada=None, completadas=0, tiempos=None):
    iid = len(INC) + 1
    INC.append(dict(id=iid, alcance=alcance, hab=hab, ubic=ubic, personal=personal, tipo=tipo, ctx=ctx, titulo=titulo,
                    desc=desc, estado=estado, creada=creada, fin=finalizada, prog=programada,
                    act=finalizada or creada))
    lista = (TAREAS_ZC if alcance == "ZONA_COMUN" else TAREAS)[tipo]
    for orden, t in enumerate(lista, start=1):
        hecho = orden <= completadas
        TAR.append(dict(inc=iid, desc=t, orden=orden, comp=hecho, fecha=(tiempos(orden) if hecho else None)))

# Limpiezas post check-out finalizadas de los últimos 14 días
for r in reservas:
    if r["estado"] == "FINALIZADA" and -14 <= r["sal"] < 0:
        dia = r["sal"]
        h1 = rnd.randint(10, 13)
        m1 = rnd.randint(0, 40)
        add_inc("HABITACION", "LIMPIEZA", f"Limpieza post check-out — habitación {r['num']}",
                "Limpieza obligatoria tras check-out. La habitación no se puede reservar hasta finalizarla.",
                "FINALIZADA", ts(dia, f"{h1:02d}:{m1:02d}"), hab=r["hab"], personal=rnd.choice(HK), ctx="POST_CHECKOUT",
                finalizada=ts(dia, f"{h1 + 1:02d}:{m1 + rnd.randint(5, 19):02d}"), completadas=6,
                tiempos=lambda o, dia=dia, h1=h1, m1=m1: ts(dia, f"{h1:02d}:{min(59, m1 + o * 3):02d}"))

# Limpiezas post check-out de hoy (habitaciones INHABILITADAS)
hk_ocupados = set()
pc1, pc2 = POST_CHECKOUT_HOY
add_inc("HABITACION", "LIMPIEZA", f"Limpieza post check-out — habitación {pc1}",
        "Limpieza obligatoria tras check-out. La habitación no se puede reservar hasta finalizarla.",
        "EN_PROGRESO", hace(150), hab=by_num[pc1]["id"], personal=HK[0], ctx="POST_CHECKOUT", completadas=3,
        tiempos=lambda o: hace(120 - o * 15))
hk_ocupados.add(HK[0])
add_inc("HABITACION", "LIMPIEZA", f"Limpieza post check-out — habitación {pc2}",
        "Limpieza obligatoria tras check-out. La habitación no se puede reservar hasta finalizarla.",
        "CREADA", hace(95), hab=by_num[pc2]["id"], ctx="POST_CHECKOUT")

# Mantenimiento de hoy (habitación inhabilitada)
add_inc("HABITACION", "MANTENIMIENTO", f"Mantenimiento habitación {MANTENIMIENTO_HOY}",
        "Cambio del compresor del aire acondicionado; el equipo hace ruido y no enfría por debajo de 24 °C.",
        "ASIGNADA", ts(-2, "16:20"), hab=by_num[MANTENIMIENTO_HOY]["id"], personal=MT[0], programada=ts(0, "08:00"))

# Mantenimiento programado a futuro sobre habitación libre
num_mf, dias_mf = MANTENIMIENTO_FUTURO
add_inc("HABITACION", "MANTENIMIENTO", f"Mantenimiento habitación {num_mf}",
        "Resane y pintura de paredes, cambio de sellado de la ducha.",
        "CREADA", ts(-1, "11:05"), hab=by_num[num_mf]["id"], programada=ts(dias_mf, "07:30"))

# Servicio al cuarto en curso (habitación ocupada)
add_inc("HABITACION", "SERVICIO_CUARTO", "Servicio al cuarto — habitación 404",
        "Cena para dos: posta cartagenera, arroz con coco y limonada de coco. Incluir botella de vino de cortesía por aniversario.",
        "EN_PROGRESO", hace(35), hab=by_num["404"]["id"], personal=PID["Kelly Johanna Herrera"], completadas=2,
        tiempos=lambda o: hace(30 - o * 10))

# Limpieza con huésped ausente (habitación ocupada)
add_inc("HABITACION", "LIMPIEZA", "Limpieza (huésped ausente) — habitación 207",
        "Huéspedes en tour a las Islas del Rosario; arreglo completo y reposición de toallas.",
        "ASIGNADA", hace(70), hab=by_num["207"]["id"], personal=HK[1], ctx="HUESPED_AUSENTE")
hk_ocupados.add(HK[1])

# Limpieza urgente (habitación ocupada, sin asignar)
add_inc("HABITACION", "LIMPIEZA", "Limpieza urgente — habitación 301",
        "Derrame de vino tinto sobre la alfombra y la ropa de cama; huésped en la habitación.",
        "CREADA", hace(15), hab=by_num["301"]["id"], ctx="URGENCIA")

# Zonas comunes
add_inc("ZONA_COMUN", "MANTENIMIENTO", "Mantenimiento — Piscina",
        "Bomba de recirculación con fuga; revisar sellos y limpiar prefiltro.",
        "EN_PROGRESO", hace(200), ubic="Piscina", personal=MT[1], completadas=1, tiempos=lambda o: hace(180))
add_inc("ZONA_COMUN", "OTRO", "Servicio — Salón Bocagrande",
        "Montaje de 40 sillas tipo auditorio, video beam y estación de café para evento corporativo de mañana.",
        "CREADA", hace(60), ubic="Salón Bocagrande")
add_inc("ZONA_COMUN", "MANTENIMIENTO", "Mantenimiento — Lobby",
        "Reemplazo de 6 bombillos LED fundidos en la lámpara principal del lobby.",
        "FINALIZADA", ts(-3, "09:10"), ubic="Lobby", personal=MT[1], finalizada=ts(-3, "10:25"), completadas=4,
        tiempos=lambda o: ts(-3, f"09:{20 + o * 8}"))
add_inc("ZONA_COMUN", "MANTENIMIENTO", "Mantenimiento — Ascensor",
        "Revisión preventiva mensual del ascensor por parte del contratista.",
        "FINALIZADA", ts(-9, "07:00"), ubic="Ascensor", personal=MT[2], finalizada=ts(-9, "09:40"), completadas=4,
        tiempos=lambda o: ts(-9, f"0{7 + o // 2}:{(o * 17) % 60:02d}"))
add_inc("ZONA_COMUN", "OTRO", "Servicio — Terraza bar",
        "Fumigación preventiva de la terraza y el bar.",
        "CANCELADA", ts(-6, "15:00"), ubic="Terraza bar", finalizada=ts(-5, "08:30"))

# Mantenimientos pasados en habitaciones
add_inc("HABITACION", "MANTENIMIENTO", "Mantenimiento habitación 404",
        "Revisión del motor del jacuzzi y cambio de filtros.",
        "FINALIZADA", ts(-12, "10:00"), hab=by_num["404"]["id"], personal=MT[0], programada=ts(-11, "08:00"),
        finalizada=ts(-11, "11:15"), completadas=4, tiempos=lambda o: ts(-11, f"{8 + o // 2:02d}:{(o * 13) % 60:02d}"))
add_inc("HABITACION", "MANTENIMIENTO", "Mantenimiento habitación 107",
        "Cambio de chapa electrónica de la puerta.",
        "FINALIZADA", ts(-20, "14:00"), hab=by_num["107"]["id"], personal=MT[1], programada=ts(-19, "09:00"),
        finalizada=ts(-19, "10:05"), completadas=4, tiempos=lambda o: ts(-19, f"09:{o * 12:02d}"))
add_inc("HABITACION", "SERVICIO_CUARTO", "Servicio al cuarto — habitación 304",
        "Desayuno continental a la habitación a las 7:00 a. m.",
        "FINALIZADA", ts(-1, "06:40"), hab=by_num["304"]["id"], personal=PID["Brayan Stiven Julio"],
        finalizada=ts(-1, "07:25"), completadas=4, tiempos=lambda o: ts(-1, f"07:{o * 5:02d}"))


# ---------------------------------------------------------------------------
# Inventario: (nombre, descripcion, categoria, cantidad, minima, unidad, ubicacion)
# ---------------------------------------------------------------------------
INV = [
    ("Desinfectante multiusos 3,8 L", "Desinfectante concentrado aroma lavanda para pisos y superficies", "Limpieza", 18, 8, "UNIDAD", "Almacén de housekeeping"),
    ("Limpiavidrios 1 L", "Limpiador de vidrios y espejos con atomizador", "Limpieza", 12, 6, "UNIDAD", "Almacén de housekeeping"),
    ("Hipoclorito de sodio 5 %", "Blanqueador para baños y lavandería", "Limpieza", 26, 10, "LITRO", "Almacén de housekeeping"),
    ("Trapeador de microfibra", "Trapeador con mango de aluminio y mopa de microfibra", "Limpieza", 14, 6, "UNIDAD", "Cuarto de aseo piso 1"),
    ("Escoba de cerdas suaves", "Escoba para interiores con recogedor", "Limpieza", 10, 5, "UNIDAD", "Cuarto de aseo piso 1"),
    ("Paños de microfibra", "Paños multiuso de colores para código de limpieza", "Limpieza", 9, 15, "CAJA", "Almacén de housekeeping"),
    ("Guantes de nitrilo", "Caja x100 guantes desechables talla M", "Limpieza", 4, 6, "CAJA", "Almacén de housekeeping"),
    ("Bolsas de basura negras", "Paquete x10 bolsas calibre 2 de 90x110 cm", "Limpieza", 40, 20, "CAJA", "Almacén de housekeeping"),
    ("Carro de housekeeping", "Carro de servicio con bolsa para ropa sucia y compartimentos", "Limpieza", 4, 3, "UNIDAD", "Pasillos de pisos"),
    ("Colchón doble ortopédico", "Colchón 140x190 cm de resortes pocket", "Mobiliario", 3, 1, "UNIDAD", "Bodega general"),
    ("Almohada de fibra", "Almohada hipoalergénica de fibra siliconada 50x70 cm", "Mobiliario", 46, 20, "UNIDAD", "Bodega de ropa blanca"),
    ("Silla de escritorio", "Silla ergonómica para habitaciones ejecutivas", "Mobiliario", 2, 1, "UNIDAD", "Bodega general"),
    ("Lámpara de mesa de noche", "Lámpara con base de madera y pantalla de lino", "Mobiliario", 6, 4, "UNIDAD", "Bodega general"),
    ("Cortina blackout", "Cortina blackout 2,40 x 2,20 m color arena", "Mobiliario", 5, 4, "UNIDAD", "Bodega general"),
    ("Sombrilla de piscina", "Sombrilla de 2,7 m con base de concreto", "Mobiliario", 8, 2, "UNIDAD", "Zona piscina"),
    ("Café molido 500 g", "Café colombiano tostión media para cafeteras de habitación", "Comida", 22, 10, "UNIDAD", "Despensa cocina"),
    ("Agua sin gas 600 ml", "Botella de agua de cortesía para minibar", "Comida", 210, 120, "UNIDAD", "Cuarto frío"),
    ("Arroz blanco", "Arroz de grano largo para cocina", "Comida", 35, 20, "KILOGRAMO", "Despensa cocina"),
    ("Aceite vegetal", "Aceite de cocina en garrafa", "Comida", 8, 10, "LITRO", "Despensa cocina"),
    ("Azúcar en sobres", "Caja x200 sobres de azúcar", "Comida", 6, 3, "CAJA", "Despensa cocina"),
    ("Coco rallado", "Coco fresco rallado para arroz con coco y postres", "Comida", 4, 5, "KILOGRAMO", "Cuarto frío"),
    ("Toalla de baño 70x140", "Toalla de algodón 100 % 500 g/m² blanca", "Ropa blanca", 180, 120, "UNIDAD", "Bodega de ropa blanca"),
    ("Toalla de mano", "Toalla de mano 50x90 cm blanca", "Ropa blanca", 150, 90, "UNIDAD", "Bodega de ropa blanca"),
    ("Toalla de piscina", "Toalla de piscina a rayas azul y blanca", "Ropa blanca", 45, 40, "UNIDAD", "Zona piscina"),
    ("Juego de sábanas doble", "Juego de sábanas 200 hilos para cama doble", "Ropa blanca", 64, 40, "JUEGO", "Bodega de ropa blanca"),
    ("Juego de sábanas king", "Juego de sábanas 300 hilos para cama king", "Ropa blanca", 22, 20, "JUEGO", "Bodega de ropa blanca"),
    ("Tapete de baño", "Tapete antideslizante de algodón", "Ropa blanca", 30, 35, "UNIDAD", "Bodega de ropa blanca"),
    ("Bata de baño", "Bata de toalla para suites", "Ropa blanca", 16, 10, "UNIDAD", "Bodega de ropa blanca"),
    ("Control remoto universal", "Control compatible con los televisores Smart de las habitaciones", "Tecnología", 7, 5, "UNIDAD", "Recepción"),
    ("Router WiFi de doble banda", "Access point para cobertura por piso", "Tecnología", 2, 2, "UNIDAD", "Cuarto de sistemas"),
    ("Cable HDMI 2 m", "Cable HDMI 2.0 para conexión de portátiles", "Tecnología", 11, 4, "UNIDAD", "Cuarto de sistemas"),
    ("Cargador USB-C de pared", "Cargador de préstamo para huéspedes", "Tecnología", 3, 5, "UNIDAD", "Recepción"),
    ("Teléfono de habitación", "Teléfono análogo con teclas de servicio rápido", "Tecnología", 4, 2, "UNIDAD", "Cuarto de sistemas"),
    ("Shampoo 30 ml", "Frasco de shampoo de cortesía", "Amenidades", 420, 250, "UNIDAD", "Bodega de amenidades"),
    ("Acondicionador 30 ml", "Frasco de acondicionador de cortesía", "Amenidades", 380, 250, "UNIDAD", "Bodega de amenidades"),
    ("Jabón de tocador 25 g", "Jabón en barra de cortesía con aroma a coco", "Amenidades", 190, 250, "UNIDAD", "Bodega de amenidades"),
    ("Kit dental", "Cepillo y crema dental de cortesía", "Amenidades", 160, 80, "UNIDAD", "Bodega de amenidades"),
    ("Pantuflas desechables", "Par de pantuflas para suites y habitaciones ejecutivas", "Amenidades", 70, 40, "PAR", "Bodega de amenidades"),
    ("Gorro de baño", "Gorro de baño desechable", "Amenidades", 300, 100, "UNIDAD", "Bodega de amenidades"),
    ("Bombillo LED 9 W", "Bombillo LED luz cálida E27", "Mantenimiento", 24, 20, "UNIDAD", "Taller de mantenimiento"),
    ("Pintura vinílica blanca", "Galón de pintura tipo 1 color blanco hueso", "Mantenimiento", 5, 3, "UNIDAD", "Taller de mantenimiento"),
    ("Silicona antihongos", "Tubo de silicona para sellado de duchas", "Mantenimiento", 2, 4, "UNIDAD", "Taller de mantenimiento"),
    ("Juego de destornilladores", "Juego de 8 destornilladores de precisión y estrella", "Mantenimiento", 3, 1, "JUEGO", "Taller de mantenimiento"),
    ("Taladro percutor", "Taladro inalámbrico 20 V con dos baterías", "Mantenimiento", 2, 1, "UNIDAD", "Taller de mantenimiento"),
    ("Gas refrigerante R410A", "Cilindro de refrigerante para aires acondicionados", "Mantenimiento", 1, 1, "UNIDAD", "Taller de mantenimiento"),
    ("Cloro granulado para piscina", "Cloro granulado al 70 %", "Piscina y spa", 18, 15, "KILOGRAMO", "Cuarto de máquinas piscina"),
    ("Kit medidor de pH", "Kit de reactivos para medir pH y cloro del agua", "Piscina y spa", 2, 2, "UNIDAD", "Cuarto de máquinas piscina"),
    ("Aceite esencial de eucalipto", "Frasco de 30 ml para sauna y masajes", "Piscina y spa", 3, 2, "UNIDAD", "Spa"),
]


# ===========================================================================
# MÓDULOS NUEVOS (se generan después para no alterar los datos anteriores)
# ===========================================================================
rnd2 = random.Random(20261020)

CAPACIDAD_TIPO = {"INDIVIDUAL": 1, "DOBLE": 2, "EJECUTIVA": 2, "TRIPLE": 3, "SUITE": 3, "FAMILIAR": 5}
for h in H:
    h["piso"] = int(h["numero"][0])
    h["capacidad"] = CAPACIDAD_TIPO[h["tipo"]]
by_id = {h["id"]: h for h in H}
guest_by_id = {g["id"]: g for g in G}

# --- Canal y datos de cancelación de reservas
CANALES = ["PRESENCIAL"] * 25 + ["TELEFONO"] * 10 + ["CORREO"] * 8 + ["WHATSAPP"] * 22 + ["SITIO_WEB"] * 20 + ["AGENCIA"] * 15
MOTIVOS_CANCELACION = [
    "El huésped canceló por cambio de planes de viaje",
    "Vuelo cancelado por la aerolínea",
    "Cancelación por motivos de salud del huésped",
    "No se recibió el anticipo en el plazo acordado",
    "El huésped encontró otra opción de alojamiento",
    "Cancelada por la agencia de viajes",
]
for r in reservas:
    r["canal"] = rnd2.choice(CANALES)
    r["registrada_por"] = rnd2.choice(["recepcion", "recepcion", "recepcion", "admin"])
    r["obs"] = None
    r["motivo"] = None
    r["fcanc"] = None
    if r["estado"] == "CANCELADA":
        r["motivo"] = rnd2.choice(MOTIVOS_CANCELACION)
        r["fcanc"] = ts(r["ent"] - rnd2.randint(1, 3), hhmm(9, 19))
    elif rnd2.random() < 0.12:
        r["obs"] = rnd2.choice([
            "Solicita habitación en piso alto", "Llegada estimada 10:00 p. m.", "Celebra luna de miel",
            "Requiere factura a nombre de empresa", "Pidió cuna para bebé", "Viaja con mascota pequeña",
        ])

# --- Catálogo de servicios: (nombre, categoria, precio, unidad, duracion, descripcion)
CATALOGO = [
    ("Desayuno a la habitación", "ALIMENTOS_BEBIDAS", 38000, "por persona", 30, "Desayuno típico costeño o continental servido en la habitación"),
    ("Cena a la habitación", "ALIMENTOS_BEBIDAS", 65000, "por persona", 45, "Plato fuerte del menú del día, bebida y postre"),
    ("Picada caribeña", "ALIMENTOS_BEBIDAS", 85000, "por servicio", 30, "Patacones, butifarra, queso costeño y bollo limpio para dos"),
    ("Botella de vino de la casa", "ALIMENTOS_BEBIDAS", 120000, "por botella", 15, "Vino tinto o blanco con hielera y copas"),
    ("Lavado y planchado", "LAVANDERIA", 9000, "por prenda", 1440, "Entrega al día siguiente antes de las 6:00 p. m."),
    ("Lavandería express", "LAVANDERIA", 45000, "por bolsa", 360, "Bolsa de hasta 5 kg lista el mismo día"),
    ("Masaje relajante 60 min", "SPA_BIENESTAR", 180000, "por persona", 60, "Masaje con aceites esenciales en el spa o en la habitación"),
    ("Masaje en pareja", "SPA_BIENESTAR", 320000, "por servicio", 60, "Sesión simultánea para dos personas"),
    ("Facial hidratante", "SPA_BIENESTAR", 140000, "por persona", 45, "Limpieza e hidratación facial"),
    ("Traslado aeropuerto–hotel", "TRANSPORTE", 70000, "por trayecto", 30, "Aeropuerto Rafael Núñez – hotel, hasta 4 pasajeros"),
    ("Alquiler de bicicleta", "TRANSPORTE", 50000, "por día", None, "Bicicleta urbana con casco y candado"),
    ("Tour Islas del Rosario", "TOURS", 280000, "por persona", 480, "Lancha, almuerzo típico y tiempo de playa"),
    ("City tour histórico", "TOURS", 120000, "por persona", 180, "Recorrido guiado por la ciudad amurallada y Getsemaní"),
    ("Decoración romántica", "OTRO", 150000, "por servicio", 60, "Pétalos, globos y vino espumoso de cortesía"),
    ("Late check-out", "OTRO", 90000, "por servicio", None, "Salida hasta las 3:00 p. m. sujeto a disponibilidad"),
]
SERV = [dict(id=i, nombre=n, cat=c, precio=pr, unidad=u, dur=du, desc=de) for i, (n, c, pr, u, du, de) in enumerate(CATALOGO, start=1)]
RESP_CATEGORIA = {
    "ALIMENTOS_BEBIDAS": [PID["Kelly Johanna Herrera"], PID["Brayan Stiven Julio"]],
    "LAVANDERIA": [PID["Sandra Milena Torres"], PID["Leidy Johana Pérez"]],
    "TRANSPORTE": [PID["Andrés Felipe Zúñiga"]],
    "TOURS": [PID["Carmen Rosa Villadiego"]],
    "OTRO": [PID["Andrés Felipe Zúñiga"], PID["Yolanda Ospina Rivera"]],
    "SPA_BIENESTAR": [None],
}

SOL = []
def add_sol(r, serv, cantidad, dia, hora, estado, completado=None):
    sid = len(SOL) + 1
    resp = rnd2.choice(RESP_CATEGORIA[serv["cat"]])
    SOL.append(dict(id=sid, serv=serv["id"], reserva=r["id"], hab=r["hab"], huesped=r["huesped"], cantidad=cantidad,
                    precio=serv["precio"], total=serv["precio"] * cantidad, prog=ts(dia, hora), resp=resp, estado=estado,
                    notas=None, solicitada=ts(dia, f"{max(int(hora[:2]) - 2, 6):02d}:{hora[3:]}"),
                    completado=completado, por=rnd2.choice(["recepcion", "recepcion", "admin"])))
    return SOL[-1]

def servicios_para(r):
    """1 a 3 servicios dentro de la estadía de la reserva."""
    elegidos = []
    for _ in range(rnd2.choice([1, 1, 2, 2, 3])):
        serv = rnd2.choice(SERV)
        if serv["unidad"] == "por persona":
            cant = rnd2.choice([1, 2, 2])
        elif serv["unidad"] == "por prenda":
            cant = rnd2.randint(3, 9)
        else:
            cant = 1
        elegidos.append((serv, cant))
    return elegidos

for r in reservas:
    if r["estado"] == "FINALIZADA" and rnd2.random() < 0.38:
        for serv, cant in servicios_para(r):
            dia = rnd2.randint(r["ent"], max(r["ent"], r["sal"] - 1))
            hora = hhmm(8, 20)
            add_sol(r, serv, cant, dia, hora, "COMPLETADA", completado=ts(dia, f"{min(int(hora[:2]) + 1, 23):02d}:{hora[3:]}"))
    elif r["estado"] == "EN_CURSO" and rnd2.random() < 0.6:
        for k, (serv, cant) in enumerate(servicios_para(r)):
            dia = rnd2.randint(r["ent"], 0)
            if dia < 0:
                hora = hhmm(8, 20)
                add_sol(r, serv, cant, dia, hora, "COMPLETADA", completado=ts(dia, f"{min(int(hora[:2]) + 1, 23):02d}:{hora[3:]}"))
            else:
                estado = rnd2.choice(["SOLICITADA", "EN_PROCESO", "COMPLETADA"])
                s_ = add_sol(r, serv, cant, 0, hhmm(7, 21), estado,
                             completado=hace(rnd2.randint(5, 90)) if estado == "COMPLETADA" else None)
                s_["solicitada"] = hace(rnd2.randint(100, 300))
    elif r["estado"] == "CONFIRMADA" and r["ent"] <= 2 and rnd2.random() < 0.5:
        serv = SERV[9]  # traslado aeropuerto programado para la llegada
        add_sol(r, serv, 1, r["ent"], hhmm(10, 18), "SOLICITADA")
# Una solicitud cancelada para mostrar el flujo
for s_ in SOL:
    if s_["estado"] == "SOLICITADA":
        s_["estado"] = "CANCELADA"
        s_["motivo"] = "El huésped decidió no tomar el servicio"
        break

# --- Facturación de estadías finalizadas
METODOS = ["TARJETA_CREDITO"] * 35 + ["TARJETA_DEBITO"] * 20 + ["EFECTIVO"] * 15 + ["TRANSFERENCIA"] * 15 + ["PAGO_DIGITAL"] * 15
FAC, ITEMS, PAGOS, MOVFIN = [], [], [], []

def exento(gid):
    g = guest_by_id[gid]
    return g["tipo"] == "PASAPORTE" and g["nac"] != "Colombia"

def add_movfin(tipo, categoria, concepto, valor, dia_off, metodo=None, tercero=None, referencia=None,
               factura=None, pago=None, por="admin", creado=None):
    mid = len(MOVFIN) + 1
    MOVFIN.append(dict(id=mid, tipo=tipo, cat=categoria, concepto=concepto, valor=round(valor, 2), fecha=d(dia_off),
                       metodo=metodo, tercero=tercero, ref=referencia, factura=factura, pago=pago, por=por,
                       creado=creado or ts(dia_off, hhmm(8, 18))))

CAT_NOMBRE = {"ALOJAMIENTO": "Alojamiento", "SERVICIOS": "Servicios adicionales", "EVENTOS": "Eventos"}

def emitir_factura(items, porcentaje, cliente, documento, email, fecha_expr, dia_off, reserva=None, evento=None,
                   huesped=None, pagar=1.0, metodo=None, observ=None):
    fid = len(FAC) + 1
    numero = "FV-%06d" % fid
    subtotal = sum(i["total"] for i in items)
    impuesto = round(subtotal * porcentaje / 100, 2)
    total = subtotal + impuesto
    pagado = round(total * pagar, 2)
    estado = "PAGADA" if pagar >= 1 else ("PENDIENTE" if pagar == 0 else "PAGADA_PARCIAL")
    FAC.append(dict(id=fid, numero=numero, reserva=reserva, evento=evento, huesped=huesped, cliente=cliente, doc=documento,
                    email=email, fecha=fecha_expr, subtotal=subtotal, pct=porcentaje, impuesto=impuesto, total=total,
                    pagado=pagado, estado=estado, observ=observ))
    for it in items:
        it["id"] = len(ITEMS) + 1
        it["factura"] = fid
        ITEMS.append(it)
    if pagado > 0:
        pid = len(PAGOS) + 1
        met = metodo or rnd2.choice(METODOS)
        refpago = None if met == "EFECTIVO" else "%06d" % rnd2.randint(100000, 999999)
        PAGOS.append(dict(id=pid, factura=fid, valor=pagado, metodo=met, ref=refpago, fecha=fecha_expr, por="recepcion"))
        # Reparto del pago por categoría según el peso de cada tipo de línea
        grupos = {}
        for it in items:
            cat = {"ALOJAMIENTO": "ALOJAMIENTO", "SERVICIO": "SERVICIOS", "EVENTO": "EVENTOS"}[it["origen"]]
            grupos[cat] = grupos.get(cat, 0) + it["total"]
        restante = pagado
        claves = list(grupos)
        for k, cat in enumerate(claves):
            parte = restante if k == len(claves) - 1 else round(pagado * grupos[cat] / subtotal, 2)
            restante = round(restante - parte, 2)
            add_movfin("INGRESO", cat, f"Pago {numero} — {CAT_NOMBRE[cat]} ({cliente})", parte, dia_off, met, cliente,
                       numero, fid, pid, "recepcion", fecha_expr)
    return FAC[-1]

sol_por_reserva = {}
for s_ in SOL:
    sol_por_reserva.setdefault(s_["reserva"], []).append(s_)

for r in sorted(reservas, key=lambda x: (x["sal"], x["id"])):
    if r["estado"] != "FINALIZADA":
        continue
    g = guest_by_id[r["huesped"]]
    hnum = by_id[r["hab"]]["numero"]
    items = [dict(desc=f"Alojamiento habitación {hnum} — {r['noches']} noche(s)", cantidad=r["noches"], vu=r["tarifa"],
                  total=r["total"], origen="ALOJAMIENTO", ref=r["id"])]
    for s_ in sol_por_reserva.get(r["id"], []):
        if s_["estado"] == "CANCELADA":
            continue
        serv = SERV[s_["serv"] - 1]
        items.append(dict(desc=f"Servicio: {serv['nombre']}", cantidad=s_["cantidad"], vu=s_["precio"], total=s_["total"],
                          origen="SERVICIO", ref=s_["id"]))
    pct = 0 if exento(r["huesped"]) else 19
    pagar = 1.0
    if r["sal"] == 0:
        pagar = 0          # check-out de hoy: factura emitida, pago pendiente
    elif r["sal"] == -1 and rnd2.random() < 0.5:
        pagar = 0.5        # abono parcial (saldo por cobrar)
    observ = "Exento de IVA: servicio turístico prestado a no residente (art. 481 lit. d, E.T.)" if pct == 0 else None
    metodo = "TARJETA_CREDITO" if pct == 0 and rnd2.random() < 0.7 else None
    emitir_factura(items, pct, f"{g['nombre']} {g['apellidos']}", f"{g['tipo']} {g['doc']}", g["email"], r["cout"],
                   r["sal"], reserva=r["id"], huesped=r["huesped"], pagar=pagar, metodo=metodo, observ=observ)

# --- Salones y eventos
SALONES = [
    dict(id=1, nombre="Salón Bocagrande", cap=60, desc="Salón principal con aire acondicionado, tarima y sonido", tarifa=350000),
    dict(id=2, nombre="Salón Getsemaní", cap=30, desc="Salón mediano para reuniones y capacitaciones", tarifa=220000),
    dict(id=3, nombre="Terraza Mirador", cap=80, desc="Terraza en el último piso con vista a la ciudad amurallada", tarifa=400000),
    dict(id=4, nombre="Zona de piscina", cap=50, desc="Área de piscina para cócteles y eventos sociales", tarifa=300000),
    dict(id=5, nombre="Sala de juntas Manga", cap=12, desc="Sala de juntas con pantalla de 65\" y videoconferencia", tarifa=120000),
]
NOMBRES_EXT = ["Alejandra Pombo", "Ricardo Navarro", "Silvia Castañeda", "Germán Ochoa", "Paula Andrea Rivas", "Hugo Barrios",
               "Natalia Fuentes", "Iván Cabrales", "Marcela Ortega", "Fabián Mercado", "Juliana Cuesta", "Rodrigo Pérez",
               "Tatiana Lozano", "Camilo Burgos", "Viviana Arrieta", "Jaime Tovar", "Diana Marcela Ruiz", "Esteban Rangel",
               "Sara Benavides", "Mario Ospino", "Lorena Pacheco", "Andrés Ricaurte", "Claudia Peñaranda", "Felipe Arrieta"]
EV, EVR, EVP = [], [], []
def add_evento(nombre, desc, tipo, acceso, estado, d_ini, h_ini, d_fin, h_fin, salon, capacidad, org, org_doc, org_tel,
               org_mail, valor, recursos, n_part, huespedes_part=None, notas=None, motivo=None, facturar=None):
    eid = len(EV) + 1
    EV.append(dict(id=eid, nombre=nombre, desc=desc, tipo=tipo, acceso=acceso, estado=estado, ini=ts(d_ini, h_ini),
                   fin=ts(d_fin, h_fin), salon=salon, resp=PID["Mauricio Ruiz Torres"], cap=capacidad, org=org,
                   org_doc=org_doc, org_tel=org_tel, org_mail=org_mail, valor=valor, notas=notas, motivo=motivo,
                   creado=ts(d_ini - rnd2.randint(10, 40), hhmm(9, 17))))
    for tipo_r, desc_r, cant, vu in recursos:
        EVR.append(dict(id=len(EVR) + 1, evento=eid, tipo=tipo_r, desc=desc_r, cant=cant, vu=vu, total=cant * vu))
    pasado = d_ini < 0 or (d_ini == 0 and estado in ("EN_CURSO", "FINALIZADO"))
    nombres = rnd2.sample(NOMBRES_EXT, min(n_part, len(NOMBRES_EXT)))
    for gid in (huespedes_part or []):
        g = guest_by_id[gid]
        EVP.append(dict(id=len(EVP) + 1, evento=eid, huesped=gid, nombre=f"{g['nombre']} {g['apellidos']}", doc=g["doc"],
                        email=g["email"], tel=g["tel"], conf=True, asistio=pasado and estado != "CANCELADO",
                        reg=ts(d_ini - rnd2.randint(0, 3), hhmm(9, 18))))
    for nombre_p in nombres:
        conf = rnd2.random() < 0.8
        EVP.append(dict(id=len(EVP) + 1, evento=eid, huesped=None, nombre=nombre_p, doc=str(rnd2.randint(10000000, 1199999999)),
                        email=None, tel=cel_co(), conf=conf, asistio=pasado and conf and estado == "FINALIZADO" and rnd2.random() < 0.9,
                        reg=ts(d_ini - rnd2.randint(1, 20), hhmm(9, 18))))
    if facturar:
        items = [dict(desc=f"Evento \"{nombre}\" — {SALONES[salon - 1]['nombre']}", cantidad=1, vu=valor, total=valor,
                      origen="EVENTO", ref=eid)]
        for tipo_r, desc_r, cant, vu in recursos:
            items.append(dict(desc=desc_r, cantidad=cant, vu=vu, total=cant * vu, origen="EVENTO", ref=eid))
        emitir_factura(items, 19, org, org_doc, org_mail, ts(d_fin, "18:30"), d_fin, evento=eid, pagar=facturar,
                       metodo="TRANSFERENCIA")
    return EV[-1]

en_casa = [r["huesped"] for r in reservas if r["estado"] == "EN_CURSO"]
add_evento("Lanzamiento de producto Andina Seguros", "Presentación comercial a corredores de seguros de la región Caribe",
           "CORPORATIVO", "PRIVADO", "FINALIZADO", -52, "08:00", -52, "13:00", 1, 55, "Andina Seguros S.A.", "NIT 900.123.456-7",
           "+57 605 664 2100", "eventos@andinaseguros.test", 1750000,
           [("EQUIPO_AUDIOVISUAL", "Video beam, pantalla y sonido", 1, 450000), ("CATERING", "Estación de café y refrigerio AM", 55, 28000)],
           22, facturar=1.0)
add_evento("Taller de liderazgo para mandos medios", "Capacitación de una jornada para el equipo de una empresa logística",
           "ACADEMICO", "PRIVADO", "FINALIZADO", -38, "08:00", -38, "17:00", 2, 28, "Logística Portuaria del Caribe S.A.S.",
           "NIT 901.234.567-1", "+57 605 693 4400", "talento@lpcaribe.test", 1980000,
           [("CATERING", "Almuerzo ejecutivo", 28, 42000), ("MOBILIARIO", "Montaje tipo escuela", 1, 0)], 18, facturar=1.0)
add_evento("Matrimonio Pérez–Arrieta", "Ceremonia y recepción en la terraza", "SOCIAL", "PRIVADO", "FINALIZADO",
           -24, "17:00", -24, "23:59", 3, 80, "Lorena Arrieta Gómez", "CC 1047123987", "+57 300 555 1212",
           "lorena.arrieta@correo.test", 3200000,
           [("DECORACION", "Decoración floral y centros de mesa", 10, 180000), ("CATERING", "Cena servida tres tiempos", 80, 95000),
            ("PERSONAL_APOYO", "Meseros adicionales", 6, 120000)], 24, facturar=1.0)
add_evento("Noche de cata de vinos chilenos", "Cata guiada por sommelier, exclusiva para huéspedes", "CULTURAL",
           "SOLO_HUESPEDES", "FINALIZADO", -10, "19:00", -10, "21:30", 4, 30, "Hotel Roomix", None, None, None, 0,
           [("CATERING", "Vinos y tabla de quesos", 1, 1100000)], 0,
           huespedes_part=rnd2.sample([g["id"] for g in G if g["activo"]], 9))
add_evento("Junta directiva Fundación Mar Caribe", "Reunión trimestral de junta", "CORPORATIVO", "PRIVADO", "FINALIZADO",
           -6, "09:00", -6, "12:00", 5, 12, "Fundación Mar Caribe", "NIT 806.012.345-9", "+57 605 660 9876",
           "secretaria@fmarcaribe.test", 360000, [("CATERING", "Estación de café", 10, 15000)], 9, facturar=0.5)
add_evento("Feria artesanal de San Jacinto", "Muestra de artesanías de los Montes de María abierta al público", "CULTURAL",
           "PUBLICO", "CANCELADO", -3, "10:00", -3, "18:00", 4, 50, "Asociación de Artesanos de San Jacinto",
           "NIT 900.456.789-0", "+57 311 222 3344", None, 900000, [], 6, motivo="Pronóstico de lluvias fuertes; se reprogramará")
add_evento("Cóctel de bienvenida para huéspedes", "Cóctel con música en vivo en la piscina", "SOCIAL", "SOLO_HUESPEDES",
           "PROGRAMADO", 0, "18:30", 0, "20:30", 4, 40, "Hotel Roomix", None, None, None, 0,
           [("CATERING", "Pasabocas y cocteles de bienvenida", 40, 22000), ("EQUIPO_AUDIOVISUAL", "Trío de música en vivo", 1, 650000)],
           0, huespedes_part=en_casa[:8], notas="Confirmar número final con recepción a las 4:00 p. m.")
add_evento("Capacitación Bancolombia regional Caribe", "Jornada de formación para asesores comerciales", "ACADEMICO", "PRIVADO",
           "PROGRAMADO", 1, "08:00", 1, "17:00", 1, 40, "Entidad Financiera Caribe S.A.", "NIT 890.903.938-8",
           "+57 604 510 9000", "formacion@entidadcaribe.test", 2450000,
           [("EQUIPO_AUDIOVISUAL", "Video beam, micrófonos inalámbricos", 1, 450000), ("CATERING", "Almuerzo ejecutivo", 40, 42000),
            ("CATERING", "Estación de café permanente", 40, 15000)], 34, notas="Montaje tipo auditorio desde las 7:00 a. m.")
add_evento("Cumpleaños 15 de Valentina", "Celebración familiar", "SOCIAL", "PRIVADO", "PROGRAMADO", 5, "19:00", 5, "23:59", 3, 70,
           "Mónica Herrera Salcedo", "CC 45512334", "+57 315 678 9900", "monica.herrera@correo.test", 2800000,
           [("DECORACION", "Decoración temática", 1, 1200000), ("CATERING", "Buffet", 70, 68000)], 15)
add_evento("Congreso de odontología del Caribe — día 1", "Conferencias académicas", "ACADEMICO", "PUBLICO", "PROGRAMADO",
           12, "08:00", 12, "18:00", 1, 60, "Colegio Odontológico Regional", "NIT 900.789.123-4", "+57 605 650 1122",
           "congreso@odontocaribe.test", 2100000, [("EQUIPO_AUDIOVISUAL", "Sonido y proyección", 1, 450000)], 20)

# --- Egresos operativos (relativos a hoy)
def egreso(cat, concepto, valor, dia, tercero, metodo="TRANSFERENCIA", ref=None):
    add_movfin("EGRESO", cat, concepto, valor, dia, metodo, tercero, ref, por="finanzas")

for dia in range(-74, 1, 15):
    egreso("NOMINA", "Nómina quincenal del personal", 11850000, dia, "Personal del hotel", ref="NOM-Q")
for dia, mes in [(-66, 1), (-36, 2), (-6, 3)]:
    egreso("SERVICIOS_PUBLICOS", "Energía eléctrica", rnd2.randint(6200000, 7400000), dia, "Afinia")
    egreso("SERVICIOS_PUBLICOS", "Acueducto y alcantarillado", rnd2.randint(1700000, 2100000), dia + 2, "Aguas de Cartagena")
    egreso("SERVICIOS_PUBLICOS", "Gas natural", rnd2.randint(780000, 980000), dia + 3, "Surtigas")
    egreso("SERVICIOS_PUBLICOS", "Internet y televisión", 1290000, dia + 4, "Proveedor de telecomunicaciones")
    egreso("PROVEEDORES", "Lavandería industrial de ropa blanca", rnd2.randint(2900000, 3500000), dia + 1, "Lavandería Industrial Caribe")
    egreso("MANTENIMIENTO", "Mantenimiento preventivo de aires acondicionados", rnd2.randint(950000, 1400000), dia + 5, "Frío Caribe S.A.S.")
    egreso("MANTENIMIENTO", "Mantenimiento mensual del ascensor", 850000, dia + 6, "Ascensores del Norte")
    egreso("IMPUESTOS", "Impuesto de industria y comercio (ICA)", rnd2.randint(1300000, 1700000), dia + 7, "Distrito de Cartagena")
# Comisiones de agencias (aprox. 15 % de las reservas por agencia que terminaron en cada tramo)
for d0, d1 in [(-75, -45), (-45, -15), (-15, 0)]:
    base = sum(r["total"] for r in reservas if r["canal"] == "AGENCIA" and r["estado"] == "FINALIZADA" and d0 <= r["sal"] < d1)
    if base > 0:
        egreso("MARKETING", "Comisiones de agencias de viaje y OTA", round(base * 0.15, 2), d1 - 1, "Agencias y OTA")

# --- Inventario: precio de referencia y proveedor por artículo, movimientos coherentes con el stock actual
PROVEEDOR = {
    "Limpieza": "Distribuidora Caribe Clean S.A.S.", "Mobiliario": "Muebles y Diseño del Litoral",
    "Comida": "Alimentos La Heroica", "Ropa blanca": "Textiles del Litoral S.A.S.", "Tecnología": "TecnoRed Cartagena",
    "Amenidades": "Amenities Andinas S.A.S.", "Mantenimiento": "Ferretería La Matuna",
    "Piscina y spa": "Piscinas y Químicos del Caribe",
}
PRECIO = {
    "Desinfectante multiusos 3,8 L": 32000, "Limpiavidrios 1 L": 11500, "Hipoclorito de sodio 5 %": 4200,
    "Trapeador de microfibra": 38000, "Escoba de cerdas suaves": 16500, "Paños de microfibra": 42000,
    "Guantes de nitrilo": 29000, "Bolsas de basura negras": 9800, "Carro de housekeeping": 1150000,
    "Colchón doble ortopédico": 1380000, "Almohada de fibra": 46000, "Silla de escritorio": 420000,
    "Lámpara de mesa de noche": 135000, "Cortina blackout": 210000, "Sombrilla de piscina": 390000,
    "Café molido 500 g": 24000, "Agua sin gas 600 ml": 1600, "Arroz blanco": 4300, "Aceite vegetal": 9800,
    "Azúcar en sobres": 21000, "Coco rallado": 12000, "Toalla de baño 70x140": 38000, "Toalla de mano": 19000,
    "Toalla de piscina": 42000, "Juego de sábanas doble": 98000, "Juego de sábanas king": 145000,
    "Tapete de baño": 23000, "Bata de baño": 89000, "Control remoto universal": 38000,
    "Router WiFi de doble banda": 420000, "Cable HDMI 2 m": 22000, "Cargador USB-C de pared": 48000,
    "Teléfono de habitación": 95000, "Shampoo 30 ml": 1100, "Acondicionador 30 ml": 1150, "Jabón de tocador 25 g": 750,
    "Kit dental": 2400, "Pantuflas desechables": 3600, "Gorro de baño": 380, "Bombillo LED 9 W": 7900,
    "Pintura vinílica blanca": 68000, "Silicona antihongos": 19500, "Juego de destornilladores": 58000,
    "Taladro percutor": 465000, "Gas refrigerante R410A": 520000, "Cloro granulado para piscina": 16500,
    "Kit medidor de pH": 78000, "Aceite esencial de eucalipto": 34000,
}
CONSUMIBLES = {"Limpieza", "Comida", "Amenidades", "Piscina y spa"}
USUARIO_SALIDA = {"Comida": "almacen", "Piscina y spa": "almacen"}
MOVINV = []
COMPRAS = []
for idx, (n, desc, cat, cant, mn, um, ub) in enumerate(INV, start=1):
    precio = PRECIO[n]
    proveedor = PROVEEDOR[cat]
    eventos_inv = []  # (dia, tipo, cantidad) en orden cronológico inverso
    stock = cant
    if cat in CONSUMIBLES or cat == "Ropa blanca":
        umbral_alto = max(mn * 1.8, cant * 1.6, 6)
        for semana in range(0, 12):
            dia = -semana * 7 - rnd2.randint(0, 3)
            consumo = max(1, round(max(cant, mn) * rnd2.uniform(0.06, 0.16))) if cat != "Ropa blanca" else max(1, round(cant * 0.03))
            motivo_s = {"Ropa blanca": "Baja por desgaste / reposición en habitaciones",
                        "Comida": "Despacho a cocina", "Amenidades": "Reposición de amenidades en habitaciones",
                        "Limpieza": "Entrega a housekeeping", "Piscina y spa": "Tratamiento del agua de la piscina"}[cat]
            # hacia atrás: antes de la salida había más stock
            eventos_inv.append((dia, "SALIDA", consumo, motivo_s))
            stock += consumo
            if stock > umbral_alto and rnd2.random() < 0.55:
                compra = round(stock - max(mn, cant * rnd2.uniform(0.3, 0.6)))
                if compra >= 1 and stock - compra >= 1:
                    eventos_inv.append((dia - rnd2.randint(1, 2), "ENTRADA", compra, "Compra a proveedor"))
                    stock -= compra
    else:
        if rnd2.random() < 0.4 and cant >= 2:
            eventos_inv.append((-rnd2.randint(5, 40), "SALIDA", 1, "Baja por daño irreparable"))
            stock += 1
    if rnd2.random() < 0.15:
        delta = rnd2.choice([-2, -1, 1])
        if stock - delta >= 1:
            eventos_inv.append((-rnd2.randint(3, 30), "AJUSTE", delta, "Ajuste por conteo físico"))
            stock -= delta
    inicial = max(stock, 0)
    ordenados = sorted(eventos_inv, key=lambda e: e[0])
    actual = inicial
    MOVINV.append(dict(art=idx, tipo="ENTRADA", cant=inicial, ant=0, res=inicial, costo=precio, motivo="Stock inicial",
                       prov=proveedor, por="almacen", fecha=ts(-90, "09:00")))
    for dia, tipo, q_, motivo_m in ordenados:
        if tipo == "SALIDA":
            nuevo = actual - q_
        elif tipo == "ENTRADA":
            nuevo = actual + q_
        else:
            nuevo = actual + q_
        assert nuevo >= 0, (n, actual, tipo, q_)
        por = USUARIO_SALIDA.get(cat, "housekeeping") if tipo == "SALIDA" else "almacen"
        hora = hhmm(8, 17)
        MOVINV.append(dict(art=idx, tipo=tipo, cant=q_, ant=actual, res=nuevo, costo=precio if tipo == "ENTRADA" else None,
                           motivo=motivo_m, prov=proveedor if tipo == "ENTRADA" else None, por=por, fecha=ts(dia, hora)))
        if tipo == "ENTRADA":
            COMPRAS.append((dia, n, q_, um, precio, proveedor, idx))
        actual = nuevo
    assert actual == cant, (n, actual, cant)

for dia, n, q_, um, precio, proveedor, idx in COMPRAS:
    add_movfin("EGRESO", "COMPRAS_INVENTARIO", f"Compra: {q_} {um.lower()} de {n}", q_ * precio, dia, "TRANSFERENCIA",
               proveedor, f"INV-{idx}", por="almacen")

# ---------------------------------------------------------------------------
# Salida SQL
# ---------------------------------------------------------------------------
w("""-- =============================================================================
-- Roomix — Datos iniciales de demostración (hotel boutique en Cartagena, Colombia)
--
-- Generado de forma determinística; las fechas son RELATIVAS al día de carga
-- (hora de Colombia), por lo que el hotel siempre aparece "en operación":
--   * 30 habitaciones en 4 pisos con tarifas en pesos colombianos (COP)
--   * ~120 huéspedes (colombianos y extranjeros) con historial de estadías
--   * Reservas de los últimos 75 días + estadías en curso + reservas futuras
--   * Personal de housekeeping, mantenimiento, room service y recepción
--   * Incidencias abiertas y cerradas coherentes con el estado de cada habitación
--   * Inventario de ~50 artículos con proveedor, precio e historial de movimientos
--   * Catálogo de servicios y solicitudes cargadas a la cuenta de los huéspedes
--   * Facturas y pagos de cada estadía, egresos operativos (nómina, servicios, compras)
--   * Salones y eventos (finalizados, cancelados y programados) con participantes y recursos
--
--   * Usuarios (admin + uno por rol): se crean SOLO si no existen, con la contraseña de demostración;
--     el admin debe cambiarla en su primer ingreso. Los usuarios existentes no se modifican.
--
-- ⚠️  BORRA los datos operativos existentes (habitaciones, huéspedes, reservas,
--     incidencias, personal, artículos, servicios, eventos y finanzas).
--     Se conservan: categorías de inventario, usuarios existentes y auditoría.
--     En DBeaver ejecútelo como script completo (Alt+X) en una sola conexión.
--
-- Uso:
--   a) Automático: variable APP_SEED_ENABLED=true (solo si no hay habitaciones).
--   b) Manual:     psql "$DATABASE_URL" -f seed-data.sql
--      (el backend debe haber arrancado al menos una vez para crear las tablas)
-- =============================================================================

-- El archivo está en UTF-8 (psql en Windows asume WIN1252 si no se indica)
SET client_encoding = 'UTF8';

-- Funciones auxiliares de sesión (se eliminan solas al cerrar la conexión)
CREATE OR REPLACE FUNCTION pg_temp.ahora() RETURNS timestamp
    LANGUAGE sql STABLE AS 'SELECT (now() AT TIME ZONE ''America/Bogota'')::timestamp(0)';
CREATE OR REPLACE FUNCTION pg_temp.hoy() RETURNS date
    LANGUAGE sql STABLE AS 'SELECT pg_temp.ahora()::date';
CREATE OR REPLACE FUNCTION pg_temp.hace(minutos integer) RETURNS timestamp
    LANGUAGE sql STABLE AS 'SELECT GREATEST(pg_temp.hoy()::timestamp, pg_temp.ahora() - make_interval(mins => minutos))';

TRUNCATE TABLE incidencia_tareas, incidencias, pagos, factura_items, facturas, movimientos_financieros,
               solicitudes_servicio, servicios_catalogo, evento_participantes, evento_recursos, eventos, salones,
               inventario_movimientos, habitacion_fotos, reservas, habitacion_caracteristicas,
               habitaciones, huespedes, personal, inventario_articulos
    RESTART IDENTITY CASCADE;
""")

w("-- -----------------------------------------------------------------------------")
w("-- Tarifas por tipo de habitación (COP por noche)")
w("-- -----------------------------------------------------------------------------")
w("INSERT INTO tarifas_tipo_habitacion (tipo_habitacion, precio_noche, fecha_hora_ultima_actualizacion) VALUES")
w(",\n".join(f"    ('{t}', {p}, pg_temp.ahora())" for t, p in TARIFAS.items()))
w("ON CONFLICT (tipo_habitacion) DO UPDATE SET precio_noche = EXCLUDED.precio_noche,\n"
  "    fecha_hora_ultima_actualizacion = EXCLUDED.fecha_hora_ultima_actualizacion;\n")

w("-- -----------------------------------------------------------------------------")
w("-- Categorías de inventario (las 7 predefinidas + una personalizada)")
w("-- -----------------------------------------------------------------------------")
w("""INSERT INTO inventario_categorias (nombre, descripcion, ejemplos_articulos, activo, predefinida, fecha_hora_creacion, fecha_hora_ultima_actualizacion) VALUES
    ('Limpieza', 'Productos y herramientas para la limpieza del hotel', 'Artículos de aseo, jabón líquido, desinfectante, trapeadores, escobas, paños, guantes, cubetas', TRUE, TRUE, pg_temp.ahora(), pg_temp.ahora()),
    ('Mobiliario', 'Todo lo que se encuentra dentro de una habitación o área común', 'Camas, colchones, almohadas de cama, mesas de noche, sillas, lámparas, espejos, armarios, cortinas', TRUE, TRUE, pg_temp.ahora(), pg_temp.ahora()),
    ('Comida', 'Insumos del restaurante o cocina cuando el hotel maneja alimentos', 'Ingredientes, bebidas, condimentos, empaques, utensilios de cocina, productos refrigerados', TRUE, TRUE, pg_temp.ahora(), pg_temp.ahora()),
    ('Ropa blanca', 'Textiles de uso en habitaciones y baños', 'Toallas, sábanas, fundas, cobijas, batas, tapetes de baño', TRUE, TRUE, pg_temp.ahora(), pg_temp.ahora()),
    ('Tecnología', 'Equipos electrónicos y conectividad', 'Routers, controles remotos, cables HDMI, teléfonos, cargadores, smart TV', TRUE, TRUE, pg_temp.ahora(), pg_temp.ahora()),
    ('Amenidades', 'Suministros de cortesía para el huésped', 'Jabón, shampoo, acondicionador, kit dental, café, agua embotellada', TRUE, TRUE, pg_temp.ahora(), pg_temp.ahora()),
    ('Mantenimiento', 'Herramientas y repuestos para reparaciones', 'Bombillos, llaves inglesas, pintura, selladores, fusibles, taladro', TRUE, TRUE, pg_temp.ahora(), pg_temp.ahora()),
    ('Piscina y spa', 'Insumos para el cuidado de la piscina y el área de spa', 'Cloro, kits de pH, aceites esenciales, toallas de spa, velas', TRUE, FALSE, pg_temp.ahora(), pg_temp.ahora())
ON CONFLICT (nombre) DO NOTHING;
""")

w("-- -----------------------------------------------------------------------------")
w("-- Personal")
w("-- -----------------------------------------------------------------------------")
w("INSERT INTO personal (id, nombre, rol, departamento, activo, fecha_hora_creacion, fecha_hora_ultima_actualizacion) VALUES")
w(",\n".join(f"    ({i}, {q(n)}, {q(r)}, {q(dp)}, {'TRUE' if a else 'FALSE'}, {ts(-rnd.randint(200, 900), '08:00')}, {ts(-rnd.randint(1, 60), '08:00')})"
             for i, (n, r, dp, a) in enumerate(P, start=1)) + ";\n")

w("-- -----------------------------------------------------------------------------")
w("-- Huéspedes")
w("-- -----------------------------------------------------------------------------")
w("INSERT INTO huespedes (id, nombre, apellidos, tipo_documento, numero_documento, email, telefono, nacionalidad, fecha_nacimiento, notas, activo, autoriza_tratamiento_datos, fecha_hora_autorizacion, fecha_hora_creacion, fecha_hora_ultima_actualizacion) VALUES")
rows = []
for g in G:
    alta = ts(-rnd.randint(76, 400), hhmm(8, 20))
    rows.append(f"    ({g['id']}, {q(g['nombre'])}, {q(g['apellidos'])}, '{g['tipo']}', {q(g['doc'])}, {q(g['email'])}, {q(g['tel'])}, "
                f"{q(g['nac'])}, DATE '{g['nacim']}', {q(g['notas'])}, {'TRUE' if g['activo'] else 'FALSE'}, TRUE, {alta}, {alta}, {alta})")
w(",\n".join(rows) + ";\n")

w("-- -----------------------------------------------------------------------------")
w("-- Habitaciones (estado operativo de hoy)")
w("-- -----------------------------------------------------------------------------")
w("INSERT INTO habitaciones (id, numero, tipo_habitacion, descripcion, precio_noche, piso, capacidad, estado, fecha_entrada, fecha_salida, "
  "cantidad_noches, estado_reserva, hora_real_check_in, hora_real_check_out, motivo_inhabilitacion, estado_retorno, huesped_id, "
  "fecha_hora_ultima_actualizacion) VALUES")
rows = []
for h in H:
    num = h["numero"]
    st = estado_hab[num]
    r = activa_por_hab.get(num)
    if st in ("OCUPADO", "RESERVADO"):
        rows.append(f"    ({h['id']}, '{num}', '{h['tipo']}', {q(h['desc'])}, {h['precio']}, {h['piso']}, {h['capacidad']}, '{st}', {d(r['ent'])}, {d(r['sal'])}, "
                    f"{r['noches']}, '{r['estado']}', {r['cin'] or 'NULL'}, NULL, NULL, NULL, {r['huesped']}, {r['cin'] or r['creada']})")
    elif st == "INHABILITADO_PC":
        rows.append(f"    ({h['id']}, '{num}', '{h['tipo']}', {q(h['desc'])}, {h['precio']}, {h['piso']}, {h['capacidad']}, 'INHABILITADO', NULL, NULL, NULL, NULL, NULL, NULL, "
                    f"'POST_CHECKOUT', 'LIBRE', NULL, {hace(150)})")
    elif st == "INHABILITADO_MT":
        rows.append(f"    ({h['id']}, '{num}', '{h['tipo']}', {q(h['desc'])}, {h['precio']}, {h['piso']}, {h['capacidad']}, 'INHABILITADO', NULL, NULL, NULL, NULL, NULL, NULL, "
                    f"'ADECUACION_PROGRAMADA', 'LIBRE', NULL, {ts(0, '00:00')})")
    else:
        rows.append(f"    ({h['id']}, '{num}', '{h['tipo']}', {q(h['desc'])}, {h['precio']}, {h['piso']}, {h['capacidad']}, 'LIBRE', NULL, NULL, NULL, NULL, NULL, NULL, "
                    f"NULL, NULL, NULL, {ts(-1, '12:00')})")
w(",\n".join(rows) + ";\n")

w("INSERT INTO habitacion_caracteristicas (habitacion_id, caracteristica) VALUES")
w(",\n".join(f"    ({h['id']}, {q(c)})" for h in H for c in h["car"]) + ";\n")

w("-- -----------------------------------------------------------------------------")
w(f"-- Reservas ({len(reservas)}): historial, estadías en curso y reservas futuras")
w("-- -----------------------------------------------------------------------------")
w("INSERT INTO reservas (id, habitacion_id, huesped_id, fecha_entrada, fecha_salida, cantidad_noches, estado_reserva, tarifa_noche, "
  "total_estimado, hora_real_check_in, hora_real_check_out, canal, observaciones, motivo_cancelacion, fecha_hora_cancelacion, "
  "registrada_por, fecha_hora_creacion, fecha_hora_ultima_actualizacion) VALUES")
rows = []
for r in reservas:
    act = r["cout"] or r["cin"] or r["creada"]
    rows.append(f"    ({r['id']}, {r['hab']}, {r['huesped']}, {d(r['ent'])}, {d(r['sal'])}, {r['noches']}, '{r['estado']}', {r['tarifa']}, "
                f"{r['total']}, {r['cin'] or 'NULL'}, {r['cout'] or 'NULL'}, '{r['canal']}', {q(r['obs'])}, {q(r['motivo'])}, "
                f"{r['fcanc'] or 'NULL'}, '{r['registrada_por']}', {r['creada']}, {r['fcanc'] or act})")
w(",\n".join(rows) + ";\n")

w("-- -----------------------------------------------------------------------------")
w(f"-- Incidencias ({len(INC)}) y su checklist")
w("-- -----------------------------------------------------------------------------")
w("INSERT INTO incidencias (id, alcance, habitacion_id, ubicacion, personal_id, tipo, contexto_limpieza, titulo, descripcion, estado, "
  "fecha_hora_creacion, fecha_hora_ultima_actualizacion, fecha_hora_finalizacion, fecha_hora_programada) VALUES")
rows = []
for i in INC:
    rows.append(f"    ({i['id']}, '{i['alcance']}', {i['hab'] or 'NULL'}, {q(i['ubic'])}, {i['personal'] or 'NULL'}, '{i['tipo']}', "
                f"{q(i['ctx'])}, {q(i['titulo'])}, {q(i['desc'])}, '{i['estado']}', {i['creada']}, {i['act']}, {i['fin'] or 'NULL'}, "
                f"{i['prog'] or 'NULL'})")
w(",\n".join(rows) + ";\n")

w("INSERT INTO incidencia_tareas (incidencia_id, descripcion, orden, completada, fecha_hora_completado) VALUES")
w(",\n".join(f"    ({t['inc']}, {q(t['desc'])}, {t['orden']}, {'TRUE' if t['comp'] else 'FALSE'}, {t['fecha'] or 'NULL'})" for t in TAR) + ";\n")

w("-- -----------------------------------------------------------------------------")
w(f"-- Inventario ({len(INV)} artículos)")
w("-- -----------------------------------------------------------------------------")
w("INSERT INTO inventario_articulos (id, nombre, descripcion, categoria_id, cantidad, cantidad_minima, unidad_medida, ubicacion, "
  "proveedor, precio_unitario, activo, fecha_hora_creacion, fecha_hora_ultima_actualizacion) VALUES")
rows = []
for idx, (n, desc, cat, cant, mn, um, ub) in enumerate(INV, start=1):
    rows.append(f"    ({idx}, {q(n)}, {q(desc)}, (SELECT id FROM inventario_categorias WHERE nombre = {q(cat)}), {cant}, {mn}, '{um}', "
                f"{q(ub)}, {q(PROVEEDOR[cat])}, {PRECIO[n]}, TRUE, {ts(-rnd.randint(90, 300), '09:00')}, "
                f"{ts(-rnd.randint(0, 20), hhmm(8, 18))})")
w(",\n".join(rows) + ";\n")
w("""-- Artículo dado de baja (baja lógica)
UPDATE inventario_articulos SET activo = FALSE WHERE nombre = 'Teléfono de habitación';
""")


def sql_or_null(v):
    return "NULL" if v is None else v

w("-- -----------------------------------------------------------------------------")
w(f"-- Movimientos de inventario ({len(MOVINV)}): stock inicial, salidas, compras y ajustes")
w("-- -----------------------------------------------------------------------------")
w("INSERT INTO inventario_movimientos (articulo_id, tipo, cantidad, stock_anterior, stock_resultante, costo_unitario, motivo, "
  "proveedor, registrado_por, fecha_hora) VALUES")
w(",\n".join(f"    ({m['art']}, '{m['tipo']}', {m['cant']}, {m['ant']}, {m['res']}, {sql_or_null(m['costo'])}, {q(m['motivo'])}, "
             f"{q(m['prov'])}, '{m['por']}', {m['fecha']})" for m in MOVINV) + ";\n")

w("-- -----------------------------------------------------------------------------")
w(f"-- Catálogo de servicios ({len(SERV)}) y solicitudes ({len(SOL)})")
w("-- -----------------------------------------------------------------------------")
w("INSERT INTO servicios_catalogo (id, nombre, descripcion, categoria, precio, unidad, duracion_minutos, activo, "
  "fecha_hora_creacion, fecha_hora_ultima_actualizacion) VALUES")
w(",\n".join(f"    ({x['id']}, {q(x['nombre'])}, {q(x['desc'])}, '{x['cat']}', {x['precio']}, {q(x['unidad'])}, "
             f"{sql_or_null(x['dur'])}, TRUE, {ts(-200, '09:00')}, {ts(-30, '09:00')})" for x in SERV) + ";\n")
w("INSERT INTO solicitudes_servicio (id, servicio_id, reserva_id, habitacion_id, huesped_id, cantidad, precio_unitario, total, "
  "fecha_hora_programada, responsable_id, estado, notas, motivo_cancelacion, solicitado_por, fecha_hora_solicitud, "
  "fecha_hora_completado, fecha_hora_ultima_actualizacion) VALUES")
w(",\n".join(f"    ({x['id']}, {x['serv']}, {x['reserva']}, {x['hab']}, {x['huesped']}, {x['cantidad']}, {x['precio']}, {x['total']}, "
             f"{x['prog']}, {sql_or_null(x['resp'])}, '{x['estado']}', {q(x['notas'])}, {q(x.get('motivo'))}, '{x['por']}', "
             f"{x['solicitada']}, {sql_or_null(x['completado'])}, {x['completado'] or x['solicitada']})" for x in SOL) + ";\n")

w("-- -----------------------------------------------------------------------------")
w(f"-- Salones ({len(SALONES)}) y eventos ({len(EV)})")
w("-- -----------------------------------------------------------------------------")
w("INSERT INTO salones (id, nombre, capacidad, descripcion, tarifa_hora, activo) VALUES")
w(",\n".join(f"    ({x['id']}, {q(x['nombre'])}, {x['cap']}, {q(x['desc'])}, {x['tarifa']}, TRUE)" for x in SALONES) + ";\n")
w("INSERT INTO eventos (id, nombre, descripcion, tipo, tipo_acceso, estado, fecha_hora_inicio, fecha_hora_fin, salon_id, "
  "responsable_id, capacidad, organizador_nombre, organizador_documento, organizador_telefono, organizador_email, valor_base, "
  "notas, motivo_cancelacion, creado_por, fecha_hora_creacion, fecha_hora_ultima_actualizacion) VALUES")
w(",\n".join(f"    ({x['id']}, {q(x['nombre'])}, {q(x['desc'])}, '{x['tipo']}', '{x['acceso']}', '{x['estado']}', {x['ini']}, "
             f"{x['fin']}, {x['salon']}, {x['resp']}, {x['cap']}, {q(x['org'])}, {q(x['org_doc'])}, {q(x['org_tel'])}, "
             f"{q(x['org_mail'])}, {x['valor']}, {q(x['notas'])}, {q(x['motivo'])}, 'eventos', {x['creado']}, {x['creado']})"
             for x in EV) + ";\n")
if EVR:
    w("INSERT INTO evento_recursos (id, evento_id, tipo, descripcion, cantidad, valor_unitario, total) VALUES")
    w(",\n".join(f"    ({x['id']}, {x['evento']}, '{x['tipo']}', {q(x['desc'])}, {x['cant']}, {x['vu']}, {x['total']})" for x in EVR) + ";\n")
w("INSERT INTO evento_participantes (id, evento_id, huesped_id, nombre, documento, email, telefono, confirmado, asistio, "
  "fecha_hora_registro) VALUES")
w(",\n".join(f"    ({x['id']}, {x['evento']}, {sql_or_null(x['huesped'])}, {q(x['nombre'])}, {q(x['doc'])}, {q(x['email'])}, "
             f"{q(x['tel'])}, {'TRUE' if x['conf'] else 'FALSE'}, {'TRUE' if x['asistio'] else 'FALSE'}, {x['reg']})"
             for x in EVP) + ";\n")

w("-- -----------------------------------------------------------------------------")
w(f"-- Facturas ({len(FAC)}), líneas ({len(ITEMS)}) y pagos ({len(PAGOS)})")
w("-- -----------------------------------------------------------------------------")
w("INSERT INTO facturas (id, numero, reserva_id, evento_id, huesped_id, cliente_nombre, cliente_documento, cliente_email, "
  "fecha_emision, subtotal, porcentaje_impuesto, impuesto, total, total_pagado, estado, observaciones, emitida_por, "
  "fecha_hora_creacion) VALUES")
w(",\n".join(f"    ({x['id']}, '{x['numero']}', {sql_or_null(x['reserva'])}, {sql_or_null(x['evento'])}, {sql_or_null(x['huesped'])}, "
             f"{q(x['cliente'])}, {q(x['doc'])}, {q(x['email'])}, {x['fecha']}, {x['subtotal']}, {x['pct']}, {x['impuesto']}, "
             f"{x['total']}, {x['pagado']}, '{x['estado']}', {q(x['observ'])}, 'recepcion', {x['fecha']})" for x in FAC) + ";\n")
w("INSERT INTO factura_items (id, factura_id, descripcion, cantidad, valor_unitario, total, origen, referencia_id) VALUES")
w(",\n".join(f"    ({x['id']}, {x['factura']}, {q(x['desc'])}, {x['cantidad']}, {x['vu']}, {x['total']}, '{x['origen']}', {x['ref']})"
             for x in ITEMS) + ";\n")
w("INSERT INTO pagos (id, factura_id, valor, metodo, referencia, fecha_hora, registrado_por) VALUES")
w(",\n".join(f"    ({x['id']}, {x['factura']}, {x['valor']}, '{x['metodo']}', {q(x['ref'])}, {x['fecha']}, '{x['por']}')"
             for x in PAGOS) + ";\n")

w("-- -----------------------------------------------------------------------------")
w(f"-- Movimientos financieros ({len(MOVFIN)}): ingresos por pagos y egresos operativos")
w("-- -----------------------------------------------------------------------------")
w("INSERT INTO movimientos_financieros (id, tipo, categoria, concepto, valor, fecha, metodo_pago, tercero, referencia, factura_id, "
  "pago_id, registrado_por, anulado, fecha_hora_creacion) VALUES")
w(",\n".join(f"    ({x['id']}, '{x['tipo']}', '{x['cat']}', {q(x['concepto'])}, {x['valor']}, {x['fecha']}, {q(x['metodo'])}, "
             f"{q(x['tercero'])}, {q(x['ref'])}, {sql_or_null(x['factura'])}, {sql_or_null(x['pago'])}, '{x['por']}', FALSE, "
             f"{x['creado']})" for x in MOVFIN) + ";\n")

w("-- -----------------------------------------------------------------------------")
w("-- Sincroniza las secuencias de ID con los registros insertados")
w("-- -----------------------------------------------------------------------------")
for t in ["personal", "huespedes", "habitaciones", "reservas", "incidencias", "incidencia_tareas", "inventario_articulos",
          "inventario_categorias", "inventario_movimientos", "servicios_catalogo", "solicitudes_servicio", "salones", "eventos",
          "evento_recursos", "evento_participantes", "facturas", "factura_items", "pagos", "movimientos_financieros"]:
    w(f"SELECT setval(pg_get_serial_sequence('{t}', 'id'), COALESCE((SELECT MAX(id) FROM {t}), 0) + 1, FALSE);")

# Usuarios: se crean solo si no existen (no se modifican los actuales). Contraseña inicial = la de
# demostración del backend (UsuarioSeed.PASSWORD_POR_DEFECTO), guardada como hash BCrypt; el admin debe
# cambiarla en su primer ingreso.
HASH_DEMO = "$2a$10$eJyGx3mSTIYyNS17lE/bJeKIhlvTG81c5bT19bZyr6FcWlM18cMcK"
USUARIOS = [
    ("admin", "Administrador Roomix", "admin@roomix.test", "ADMIN", True),
    ("recepcion", "Carmen Rosa Villadiego", "recepcion@roomix.test", "RECEPCION", False),
    ("housekeeping", "Luz Marina Cárdenas", "housekeeping@roomix.test", "HOUSEKEEPING", False),
    ("finanzas", "Patricia Gómez Salcedo", "finanzas@roomix.test", "FINANZAS", False),
    ("eventos", "Mauricio Ruiz Torres", "eventos@roomix.test", "EVENTOS", False),
    ("almacen", "Óscar Castillo Mendoza", "almacen@roomix.test", "ALMACEN", False),
]
w("")
w("-- -----------------------------------------------------------------------------")
w("-- Usuarios del sistema (uno por rol). Solo se insertan si no existen; los existentes no se tocan.")
w("-- Contraseña inicial: la de demostración del backend. El admin debe cambiarla al primer ingreso.")
w("-- -----------------------------------------------------------------------------")
w("INSERT INTO usuarios (username, nombre_completo, email, password_hash, rol, activo, debe_cambiar_password,")
w("                      intentos_fallidos, fecha_hora_creacion, fecha_hora_ultima_actualizacion) VALUES")
w(",\n".join(
    f"  ({q(u)}, {q(n)}, {q(e)}, '{HASH_DEMO}', '{r}', TRUE, {'TRUE' if cambiar else 'FALSE'}, 0, pg_temp.ahora(), pg_temp.ahora())"
    for u, n, e, r, cambiar in USUARIOS))
w("ON CONFLICT DO NOTHING;")
w("SELECT setval(pg_get_serial_sequence('usuarios', 'id'), COALESCE((SELECT MAX(id) FROM usuarios), 0) + 1, FALSE);")

with open(OUT, "w", encoding="utf-8", newline="\n") as f:
    f.write("\n".join(out) + "\n")

print(f"habitaciones={len(H)} huespedes={len(G)} reservas={len(reservas)} incidencias={len(INC)} tareas={len(TAR)} inventario={len(INV)}")
print(f"servicios={len(SERV)} solicitudes={len(SOL)} eventos={len(EV)} participantes={len(EVP)} facturas={len(FAC)} pagos={len(PAGOS)} "
      f"movfin={len(MOVFIN)} movinv={len(MOVINV)}")
from collections import Counter
print(Counter(r['estado'] for r in reservas))
print(Counter(estado_hab.values()))
