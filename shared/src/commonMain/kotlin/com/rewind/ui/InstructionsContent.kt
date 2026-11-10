package com.rewind.ui

/** Una sección de la página de instrucciones: un título y sus párrafos (los que empiezan por "• " son viñetas). */
data class InstructionSection(val title: String, val paragraphs: List<String>)

/**
 * Documentación para quien usa la app, en el orden en que se aprende. Al añadir o cambiar una función de la app,
 * actualizar aquí la sección que corresponda (o añadir una nueva en su sitio). Texto de cara al usuario: claro y sin jerga.
 *
 * Se adapta al sistema: [volumeKeys] indica si la plataforma permite usar las teclas de volumen como disparador
 * (hoy solo Android); en las demás, esa opción no se ofrece ni se explica.
 */
fun instructions(volumeKeys: Boolean): List<InstructionSection> = listOf(
    InstructionSection(
        "El efecto",
        listOf(
            "Rewind muestra a pantalla completa una réplica de la pantalla de bloqueo de tu móvil, con un reloj falso. " +
                "El reloj parte de la hora real más N minutos (o menos, si eliges el modo Adelante) y avanza con normalidad.",
            "Cuando tú lo decides, el reloj retrocede esos N minutos con una animación hasta marcar la hora real. " +
                "Los espectadores miran sus propios móviles y marcan la misma hora: el tiempo ha retrocedido para todos.",
            "La app solo cambia lo que se ve en su propia pantalla. No toca la hora de tu móvil ni la de nadie.",
            "Antes de empezar, pide a los espectadores que dejen sus móviles boca abajo para que no puedan comparar horas.",
        ),
    ),
    InstructionSection(
        "1. Prepara tu pantalla de bloqueo",
        listOf(
            "Necesitas una captura de pantalla de la pantalla de bloqueo real de tu móvil, con el reloj y la fecha visibles. " +
                "Es la captura A. Bloquea el móvil, enciéndelo y haz la captura con los botones de tu móvil.",
            "Abre Ajustes y pulsa «Elegir captura A». Sin esta captura la app no se puede usar para actuar: " +
                "solo muestra un aviso con instrucciones.",
            "Opcional: la captura B, «Elegir sin reloj B». Es la misma pantalla de bloqueo pero sin el reloj ni la fecha. " +
                "Con ella la app tapa el reloj original y dibuja el suyo encima. Sin ella, el reloj nuevo se dibuja sobre el " +
                "original, y si no coinciden del todo puede notarse.",
            "Las dos capturas deben ser del mismo móvil y del mismo tamaño de pantalla.",
            "«Quitar imágenes» borra las dos capturas.",
        ),
    ),
    InstructionSection(
        "2. Ajusta el reloj y la fecha",
        listOf(
            "En Ajustes, pulsa «Editar hora, fecha y mezcla». Verás tu captura con dos cajas: la roja es la hora y la azul es la fecha.",
            "• Arrastra una caja para moverla. Arrastra el cuadrado de su esquina inferior derecha para cambiar su tamaño.",
            "• En la pestaña Hora (y en Fecha) ajusta tamaño, grosor, espaciado, opacidad, alineación y color hasta que se parezca al reloj de tu móvil.",
            "• «Reloj apilado» pone las horas encima de los minutos, como en algunos móviles Pixel.",
            "• En Fecha puedes elegir el formato (por ejemplo «Tue, Nov 10» o «martes 10 de noviembre»).",
            "• El interruptor «Ver A» muestra solo tu captura original. Alterna para comparar con el reloj dibujado: el objetivo es que no se note la diferencia.",
            "• La pestaña Mezcla solo sirve si has elegido la captura B. Ajusta brillo, contraste, saturación y opacidad del recorte " +
                "hasta que no se vean los bordes sobre la captura A.",
            "Pulsa Guardar para conservar los cambios.",
        ),
    ),
    InstructionSection(
        "3. Configura la actuación",
        listOf(
            "• Dirección. Atrás: el reloj va por delante de la hora real y retrocede hasta ella. Adelante: parte por detrás y avanza hasta la real.",
            "• Minutos extra (de 1 a 999): cuántos minutos de diferencia tiene el reloj falso respecto a la hora real al empezar.",
            "• Formato de hora: 12 o 24 horas, con o sin cero delante.",
            "• Animar el rebobinado: si lo desactivas, el reloj salta de golpe a la hora real en vez de retroceder minuto a minuto.",
            "• Retardo antes de animar (de 1 a 10 s): lo que espera la app entre tu disparo y el inicio de la animación.",
            "• Velocidad: lenta, normal o rápida. Con muchos minutos extra la animación se acelera sola para no durar más de 20 s.",
            "Pulsa Guardar en Ajustes para aplicar los cambios.",
        ),
    ),
    InstructionSection(
        "4. Elige cómo disparar el efecto",
        buildList {
            add("• Toque: tocas la pantalla en cualquier sitio.")
            add("• Toque + vibración: igual, y el móvil vibra un instante para confirmarte que se ha disparado.")
            add(
                "• Proximidad: pasas la mano sobre el sensor de proximidad (suele estar arriba, junto al altavoz). " +
                    "El sensor solo está activo mientras esperas el disparo. Si el móvil arranca con el sensor tapado, no se dispara solo: " +
                    "primero tiene que verlo destapado. En iPhone, mientras el sensor está tapado el sistema apaga la pantalla.",
            )
            if (volumeKeys) {
                add(
                    "• Volumen: pulsas subir o bajar volumen. El efecto se dispara y el móvil no cambia el volumen ni muestra " +
                        "el aviso del sistema. Mientras esta opción esté elegida y la pantalla principal abierta, las teclas de volumen " +
                        "solo sirven para disparar.",
                )
            } else {
                add("La tecla de volumen como disparador no está disponible en este sistema.")
            }
            add("El disparo solo cuenta cuando la app está esperando. Si ya se ha disparado, un segundo toque no hace nada.")
        },
    ),
    InstructionSection(
        "5. Simular el móvil apagado o bloqueado",
        listOf(
            "Activa «Simular pantalla apagada (Always On)» en Ajustes. Hasta que disparas, la pantalla no se ve normal. " +
                "Eliges el aspecto con «Aspecto de la pantalla apagada»:",
            "• Negra: la pantalla se ve negra, como un móvil apagado.",
            "• Atenuada y desenfocada: se ve tu pantalla de bloqueo más oscura y con el fondo desenfocado, como un móvil bloqueado " +
                "con la pantalla en reposo. Si tocas la pantalla, se enfoca y recupera su aspecto normal (el toque solo la despierta, " +
                "no dispara el efecto). Pasados unos segundos sin tocarla, vuelve a atenuarse. " +
                "Mientras el efecto está en marcha (retardo y animación) la pantalla no se atenúa; cuando termina, vuelve a atenuarse " +
                "pasados esos segundos sin tocarla, y un toque la despierta de nuevo. Al reiniciar tampoco se atenúa de golpe: " +
                "espera esos mismos segundos sin que la toques. " +
                "Puedes ajustar cuánto se atenúa (brillo), cuánto se desenfoca el fondo y cuántos segundos se queda despierta. " +
                "El desenfoque necesita Android 12 o posterior; en versiones anteriores solo se atenúa.",
            "Con «Encender sola al disparar (Auto Wake)» la pantalla se enciende con un fundido suave en el momento del disparo. " +
                "Sin Auto Wake, se enciende cuando empieza la animación.",
            "Los controles ocultos (apartado 8) funcionan igual con la pantalla apagada o atenuada.",
        ),
    ),
    InstructionSection(
        "6. Alerta de cambio de minuto",
        listOf(
            "El reloj falso cambia de minuto en el mismo segundo que el real. Si disparas justo antes de un cambio de minuto, " +
                "el efecto puede resultar menos limpio.",
            "Con la alerta activada, aparece un brillo suave en el borde inferior de la pantalla cuando faltan pocos segundos para el cambio de minuto " +
                "(tú eliges cuántos, de 3 a 30). Espera a que pase y entonces dispara. El brillo se adapta al color del borde de tu captura.",
        ),
    ),
    InstructionSection(
        "7. Registrar un número en directo",
        listOf(
            "Sirve para que un espectador elija el número de minutos y tú lo introduzcas sin que se note.",
            "Actívalo en Ajustes con «Registrar número en directo» y pulsa Guardar. Ahí mismo eliges si el teclado es " +
                "visible o invisible y, si es visible, cuánta transparencia tiene.",
            "• Entrar: mantén pulsada la casilla inferior izquierda de la pantalla (apartado 8). El móvil vibra: el modo de registro está activo.",
            "• Teclear: la mitad inferior de la pantalla se convierte en un teclado de 3 columnas por 4 filas (la mitad de arriba no responde). " +
                "Las tres primeras filas son los números 1 2 3 / 4 5 6 / 7 8 9 y la fila de abajo, entera, es el 0. " +
                "Se teclean siempre dos cifras: para el 7, toca 0 y luego 7; para el 17, toca 1 y luego 7.",
            "• Terminar: al teclear la segunda cifra el modo se cierra solo, el número queda registrado y el móvil vuelve a vibrar. " +
                "El reloj pasa a mostrar la hora real más esos minutos.",
            "• Con el teclado invisible la pantalla no cambia: la cuadrícula existe pero no se ve, así que conviene ensayar dónde está cada zona " +
                "(con «Ver controles de pantalla» o con el teclado visible). Con el teclado visible se dibuja encima, con la transparencia elegida, " +
                "y arriba se ve lo tecleado.",
            "Mientras el modo de registro está activo, el resto de la pantalla no responde (ni toques, ni disparo, ni Ajustes, ni reiniciar), " +
                "para evitar pulsaciones por error. Si entras sin querer y no tecleas nada, el modo se cierra solo a los 10 segundos.",
            "Si tecleas 00, el modo se cierra sin cambiar nada. El número registrado vale hasta que reinicies; al reiniciar se vuelve a los minutos de Ajustes. " +
                "Solo se puede registrar mientras la app espera el disparo.",
        ),
    ),
    InstructionSection(
        "8. Controles ocultos de la pantalla",
        listOf(
            "La pantalla principal se divide mentalmente en 9 casillas (3 columnas por 3 filas). Estos son los controles:",
            "• Pulsación larga (algo más de medio segundo) en la casilla de arriba a la derecha: abre Ajustes.",
            "• Pulsación larga en la casilla de arriba a la izquierda: reinicia. La pantalla vuelve a la espera con la hora más N y el efecto se puede repetir.",
            "• Pulsación larga en la casilla de abajo a la izquierda: entra en el registro de número (apartado 7), si has activado esa opción.",
            "• Dos dedos deslizando hacia abajo: abre Ajustes. Dos dedos hacia arriba: reinicia.",
            "• Un toque corto en cualquier otro sitio: dispara el efecto (con el disparador Toque).",
            "Si no recuerdas dónde está cada casilla, activa en Ajustes «Ver controles de pantalla»: dibuja la cuadrícula encima y rotula qué hace cada casilla. " +
                "Es para ensayar: apágalo antes de actuar.",
        ),
    ),
    InstructionSection(
        "9. Cómo se hace paso a paso",
        listOf(
            "1. Ensaya con el móvil en la mano y los controles a la vista hasta tener los gestos de memoria.",
            "2. Apaga «Ver controles de pantalla» y comprueba que Ajustes está guardado.",
            "3. Pide a los espectadores que dejen sus móviles boca abajo.",
            "4. Abre Rewind. Verás la pantalla de bloqueo con la hora real más N minutos (o negra, si usas Always On).",
            "5. Si quieres que un espectador elija el número, regístralo ahora con una pulsación larga abajo a la izquierda y sus dos cifras.",
            "6. Muestra la pantalla si hace falta y, cuando quieras, dispara el efecto.",
            "7. Tras el retardo, el reloj retrocede hasta la hora real. Pide que miren sus móviles.",
            "8. Para repetirlo, reinicia con la pulsación larga arriba a la izquierda.",
        ),
    ),
    InstructionSection(
        "Consejos y límites",
        listOf(
            "• La hora final siempre es la hora real del momento: si el minuto real cambia durante la animación, el reloj termina en el minuto nuevo.",
            "• La barra de estado de tu captura (batería, cobertura, hora) está congelada, porque es una imagen. " +
                "Si te importa, haz la captura con la barra de estado lo más neutra posible o tápala con la captura B.",
            "• La pantalla se mantiene encendida y sin barras mientras la app está abierta (en iPhone, con el indicador de inicio oculto).",
            "• Todo se guarda solo en tu móvil. La app no usa internet ni cuentas.",
            "• Ensaya siempre antes en las mismas condiciones en las que vayas a actuar.",
        ),
    ),
)
