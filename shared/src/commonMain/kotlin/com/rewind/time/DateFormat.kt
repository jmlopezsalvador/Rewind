package com.rewind.time

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month

/** Formatos de la fecha de la pantalla de bloqueo (idioma incluido; el de cada móvil varía). */
enum class DateFormat(val label: String) {
    EN_SHORT("Tue, Nov 10"),
    EN_LONG("Tuesday 10 November"),
    ES_SHORT("mar, 10 nov"),
    ES_LONG("martes 10 de noviembre"),
}

private val EN_DAYS = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
private val ES_DAYS = listOf("lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo")
private val EN_MONTHS = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December",
)
private val ES_MONTHS = listOf(
    "enero", "febrero", "marzo", "abril", "mayo", "junio",
    "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
)
private val ES_SHORT_DAYS = listOf("lun", "mar", "mié", "jue", "vie", "sáb", "dom")
private val ES_SHORT_MONTHS = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")

fun formatDate(t: LocalDateTime, format: DateFormat): String {
    val d = t.dayOfWeek.ordinal   // lunes = 0
    val m = t.monthNumber - 1
    val day = t.dayOfMonth
    return when (format) {
        DateFormat.EN_SHORT -> "${EN_DAYS[d].take(3)}, ${EN_MONTHS[m].take(3)} $day"
        DateFormat.EN_LONG -> "${EN_DAYS[d]} $day ${EN_MONTHS[m]}"
        DateFormat.ES_SHORT -> "${ES_SHORT_DAYS[d]}, $day ${ES_SHORT_MONTHS[m]}"
        DateFormat.ES_LONG -> "${ES_DAYS[d]} $day de ${ES_MONTHS[m]}"
    }
}
