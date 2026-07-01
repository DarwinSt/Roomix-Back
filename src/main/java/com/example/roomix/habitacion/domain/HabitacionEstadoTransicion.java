package com.example.roomix.habitacion.domain;



import com.example.roomix.habitacion.exception.TransicionEstadoInvalidaException;



import java.util.Map;

import java.util.Set;



/**

 * Transiciones operativas manuales. INHABILITADO lo gestiona el sistema

 * (mantenimiento del día o limpieza post check-out), salvo reserva validada.

 */

public final class HabitacionEstadoTransicion {



    private static final Map<EstadoHabitacion, Set<EstadoHabitacion>> TRANSICIONES = Map.of(

            EstadoHabitacion.LIBRE, Set.of(EstadoHabitacion.RESERVADO),

            EstadoHabitacion.RESERVADO, Set.of(

                    EstadoHabitacion.OCUPADO,

                    EstadoHabitacion.LIBRE

            ),

            EstadoHabitacion.OCUPADO, Set.of(EstadoHabitacion.LIBRE)

    );



    private HabitacionEstadoTransicion() {

    }



    public static void validar(

            EstadoHabitacion estadoActual,

            EstadoHabitacion estadoNuevo,

            MotivoInhabilitacion motivoInhabilitacion

    ) {

        validar(estadoActual, estadoNuevo, motivoInhabilitacion, false);

    }



    public static void validar(

            EstadoHabitacion estadoActual,

            EstadoHabitacion estadoNuevo,

            MotivoInhabilitacion motivoInhabilitacion,

            boolean permitirReservaDesdeInhabilitado

    ) {

        if (estadoActual == estadoNuevo) {

            return;

        }



        if (estadoNuevo == EstadoHabitacion.INHABILITADO) {

            throw new TransicionEstadoInvalidaException(

                    estadoActual,

                    estadoNuevo,

                    "INHABILITADO se aplica automáticamente (mantenimiento hoy o limpieza tras check-out)"

            );

        }



        if (estadoActual == EstadoHabitacion.INHABILITADO) {

            if (permitirReservaDesdeInhabilitado && estadoNuevo == EstadoHabitacion.RESERVADO) {

                return;

            }

            throw new TransicionEstadoInvalidaException(

                    estadoActual,

                    estadoNuevo,

                    "La habitación está inhabilitada hoy. Finalice el mantenimiento o la limpieza, "

                            + "o reserve fechas que no incluyan el día bloqueado"

            );

        }



        Set<EstadoHabitacion> permitidos = TRANSICIONES.getOrDefault(estadoActual, Set.of());

        if (!permitidos.contains(estadoNuevo)) {

            throw new TransicionEstadoInvalidaException(estadoActual, estadoNuevo);

        }

    }

}


