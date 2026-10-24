package com.example.roomix.evento.domain;

/**
 * Enumeraciones del módulo de eventos.
 */
public final class EventoEnums {

    private EventoEnums() {
    }

    /** Accesibilidad del evento (documento de tesis, Tabla 22). */
    public enum TipoAcceso {
        PUBLICO("Público"),
        PRIVADO("Privado"),
        SOLO_HUESPEDES("Exclusivo para huéspedes");

        private final String nombre;

        TipoAcceso(String nombre) {
            this.nombre = nombre;
        }

        public String getNombre() {
            return nombre;
        }
    }

    public enum EstadoEvento {
        PROGRAMADO,
        EN_CURSO,
        FINALIZADO,
        CANCELADO
    }

    public enum TipoEvento {
        CORPORATIVO("Corporativo"),
        SOCIAL("Social (boda, cumpleaños, aniversario)"),
        ACADEMICO("Académico / capacitación"),
        CULTURAL("Cultural"),
        OTRO("Otro");

        private final String nombre;

        TipoEvento(String nombre) {
            this.nombre = nombre;
        }

        public String getNombre() {
            return nombre;
        }
    }

    public enum TipoRecurso {
        EQUIPO_AUDIOVISUAL("Equipo audiovisual"),
        MOBILIARIO("Mobiliario"),
        CATERING("Catering"),
        DECORACION("Decoración"),
        PERSONAL_APOYO("Personal de apoyo"),
        OTRO("Otro");

        private final String nombre;

        TipoRecurso(String nombre) {
            this.nombre = nombre;
        }

        public String getNombre() {
            return nombre;
        }
    }
}
