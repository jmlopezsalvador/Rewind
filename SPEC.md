# SPEC — App "Rewind" (simulador de reloj de pantalla de bloqueo para magia)

> Documento funcional para Claude Code. Nombre provisional: **Rewind** (no usar el nombre, marca ni créditos de la app comercial de referencia).
> Basado en la observación de la app de referencia (menús y comportamiento grabados en vídeo) y en dos descripciones extensas, ya depuradas.

---

## 1. Qué es y cómo funciona el efecto

Una app de magia que muestra, a pantalla completa, una **réplica exacta de la pantalla de bloqueo del propio móvil** con un reloj falso que el mago controla. Hay que **simular**: la app no toca el reloj del sistema ni nada fuera de su propia pantalla.

**Principio del efecto (el requisito más importante):**

1. Los espectadores dejan sus móviles boca abajo (no pueden comparar horas).
2. La pantalla falsa muestra `hora_real + N` minutos (modo Atrás) y avanza en sincronía con el reloj real, de modo que el cambio de minuto se produce en el mismo segundo que el real.
3. El mago dispara de forma invisible → tras un retardo, el reloj **rebobina N minutos** con una animación hasta quedar en la **hora real**.
4. Los espectadores miran sus móviles: marcan la misma hora → "el tiempo ha retrocedido para todos".

En modo Adelante es simétrico: la pantalla parte de `hora_real − N` y avanza hasta la hora real.

---

## 2. Alcance

**MVP (fase 1):** pantalla falsa calibrable, reloj con offset, disparo por Tap / Tap con vibración / Proximidad, animación con retardo y velocidad, gestos ocultos y persistencia local.
**Fase 2:** cuadrícula invisible para introducir N en directo, modo entrenamiento, alerta de minuto, simulación de pantalla apagada (Always On + Auto Wake) y botones de volumen.

**Fuera de alcance:** modificar la hora del sistema o de otros dispositivos; actuación compartida entre varios móviles (Host / Join / app Receptor); mandos Bluetooth; efecto Liquid Glass; backend, cuentas, analítica o cualquier comunicación de red.

**Plataforma:** Android primero (Kotlin + Jetpack Compose), con la lógica de dominio aislada (idealmente en Kotlin Multiplatform) para portarla a iOS. CI en GitHub Actions.

---

## 3. Pantallas y navegación

| Pantalla | Acceso | Contenido |
|---|---|---|
| **Performance Screen** (principal) | Arranque de la app | Pantalla de bloqueo falsa a pantalla completa (modo inmersivo, sin barras del sistema, pantalla siempre encendida). |
| **Settings** | Deslizar con 2 dedos hacia abajo sobre la Performance Screen | Instructions · Configuration (Display Settings, Performance Settings) · Credits · botón **Save**. |
| **Display Settings** | Settings | Set Screenshot · Set Dragged Screenshot · Configure Display · Blend Images · Time Format. |
| **Configure Display** | Display Settings | Editor WYSIWYG sobre la captura (ver §4.2). |
| **Blend Images** | Display Settings | Brightness · Contrast · Saturation · Overall, con vista previa. Texto: "Ajusta los deslizadores hasta que las dos imágenes se fundan sin costuras". |
| **Performance Settings** | Settings | Ver §5. |

Deslizar con 2 dedos hacia arriba: **reset** al estado de espera (se recalcula el offset y se cancela cualquier animación).

---

## 4. Configuración visual (Display)

### 4.1 Imágenes de entrada (galería del dispositivo)
- **Screenshot (A):** captura real de la pantalla de bloqueo con reloj y fecha. Es la referencia y el fondo base.
- **Dragged Screenshot (B):** el mismo fondo **sin reloj** (captura con la pantalla de bloqueo desplazada o el wallpaper original). Sirve para tapar el reloj real de A.
- Composición por capas: `A` (base) → parche de `B` recortado a la zona del reloj y la fecha → texto dinámico (hora y fecha).

### 4.2 Configure Display
- Dos cajas arrastrables y redimensionables sobre la captura: **Fecha** (marco azul discontinuo, p. ej. "Saturday 15 April") y **Hora** (marco rojo discontinuo, vista previa "88:88").
- Deslizadores y toggles: Size, Weight, Width (espaciado o ancho de dígitos), Opacity, Gloss, Text Colour.
- Fuente: la del sistema por defecto, con opción de elegir otra (fina o negrita).
- Ajustes independientes para fecha y hora. La fecha también es dinámica (si cambia el día durante el rebobinado, cambia).

### 4.3 Blend Images
Brightness, Contrast, Saturation y Overall se aplican al parche B (y opcionalmente al texto) para que encaje con A, compensando el oscurecimiento o desenfoque que el SO aplica al fondo de bloqueo.

### 4.4 Time Format
`12 h` · `12 h (2 dígitos)` · `24 h` · `24 h (2 dígitos)`. En 12 h, medianoche y mediodía se muestran como **12**, nunca como 0 o 00.

---

## 5. Configuración de la actuación (Performance Settings)

| Ajuste | Tipo / valores | Por defecto | Efecto |
|---|---|---|---|
| Grid Input Screen | Off / Visible (entrenamiento) / Invisible | Off | Si está activa, N se introduce en directo tocando una cuadrícula 3×3 (+0) sobre la pantalla falsa. |
| Grid Includes Zero | bool | on | Añade la casilla 0 (permite N de dos cifras, p. ej. 1-0). Sin 0, las zonas 1–9 son más grandes. |
| Time Travel Direction | Backwards / Forwards | Backwards | Sentido del rebobinado. |
| Add Extra Minutes | int 1–999 (teclado numérico) | 3 | N fijo cuando no se usa la cuadrícula. Debe admitir más de 100 minutos (cruza horas). |
| Animate | bool | on | Off = salto instantáneo al valor final en un solo frame. |
| Animation Delay | 1–10 s (deslizador) | 4 s | Espera entre el disparo y el inicio de la animación. |
| Trigger Mechanism | Tap / Tap (Vibrate) / Proximity (+ Volume en fase 2) | Tap | Cómo se dispara. "Vibrate" = vibración háptica corta de confirmación al mago. |
| Animation Speed | Slow / Regular / Fast | Regular | Tiempo por paso de minuto (proponer 1 s / 0,5 s / 0,2 s; con N grande, acelerar o saltar decenas). |
| Approaching Minute Alert | bool | off | Brillo sutil en el borde inferior cuando faltan ≤ X s para el cambio de minuto real. Color adaptado a modo claro u oscuro. |
| Peek Activation Time | bool | off | *(Texto original: "Display a peek of the time to begin the animation when the real time will match the initial time displayed").* Ver pregunta abierta Q1. |
| Auto Wake | bool | off | Con Always On activo, simula el encendido de la pantalla en el momento de la activación. |
| Always On Display | Off / On (+ opciones) | Off | Simula el móvil bloqueado o con pantalla apagada (negro o AOD atenuado) hasta la activación. |

Todos los ajustes e imágenes se guardan **solo en local** (DataStore / UserDefaults + almacenamiento interno). Se persisten al pulsar **Save**.

---

## 6. Lógica temporal

```
offset = (direction == BACKWARDS ? +N : -N)
displayTime(t) = trunc_minuto(now(t)) + offset        // en espera, avanza con el reloj real
```

- **Sincronía de minuto:** el cambio de minuto en pantalla ocurre en el segundo :00 real (programar el tick con el reloj del sistema; nunca con un temporizador acumulativo que derive).
- **Animación:** desde `displayTime` paso a paso (−1 o +1 min por paso, con rollover de horas, 12/24 h y fecha) hasta `trunc_minuto(now)` **calculado al final**. Si el minuto real cambia durante la animación, el destino se recalcula para terminar siempre en la hora real vigente.
- **Instantáneo (Animate = off):** tras el retardo, un único cambio de frame a la hora real.
- **Tras terminar:** `SYNCED_HOLD`. La pantalla sigue mostrando la hora real y avanzando con ella (offset = 0) hasta un reset.

### Máquina de estados
```
IDLE (muestra hora+offset) ──[grid activa: toques]──▶ GRID_INPUT ──[N confirmado]──▶ ARMED
IDLE ──[grid off]──▶ ARMED
ARMED ──[trigger]──▶ DELAY (Animation Delay) ──▶ ANIMATING ──▶ SYNCED_HOLD
cualquier estado ──[2 dedos ↑]──▶ IDLE (reset)     ──[2 dedos ↓]──▶ SETTINGS
```
Con Always On Display, IDLE/ARMED muestran la pantalla "apagada". Auto Wake la "enciende" al pasar a DELAY.

---

## 7. Disparadores

- **Tap:** toque en cualquier punto (o en una zona configurable). No debe confundirse con la cuadrícula: el disparo solo cuenta en el estado ARMED.
- **Tap (Vibrate):** igual que Tap, más una vibración corta.
- **Proximity:** tapar el sensor de proximidad (pasar la mano). Android: `Sensor.TYPE_PROXIMITY` con listener solo mientras está ARMED. iOS: `proximityMonitoring` apaga la pantalla mientras está tapado, así que hay que tenerlo en cuenta en el flujo.
- **Volumen (fase 2):** interceptar las teclas de volumen sin mostrar el HUD del sistema (Android `onKeyDown`; iOS requiere un workaround).
- Toda la lectura de sensores va fuera del hilo de UI y solo está activa en ARMED (batería).

---

## 8. Requisitos no funcionales

- Arranque < 1 s hasta la pantalla falsa. Animación a 60/120 Hz sin parpadeos ni saltos visibles al pasar de espera a animación.
- Pantalla siempre encendida mientras la app está en primer plano. Modo inmersivo total (ni status bar ni gestos visibles del sistema). Android: valorar "screen pinning".
- La barra de estado de la captura A está congelada (batería, cobertura): permitir taparla con el parche B, o documentarlo como limitación.
- Sin red ni permisos innecesarios (solo galería y vibración).
- Lógica temporal 100 % testeable con un reloj inyectable (tests unitarios de rollover de hora y día, 12 h "12", N > 100 y cambio de minuto a mitad de la animación).

---

## 9. Casos de uso

**CU-01 · Preparar la pantalla falsa**
Actor: mago. Pre: tiene las capturas A y B en la galería.
Flujo: Settings → Display → Set Screenshot (A) → Set Dragged Screenshot (B) → Save.
Post: la Performance Screen muestra A con el reloj real tapado.
Aceptación: sin capturas, la app muestra instrucciones y no entra en modo actuación.

**CU-02 · Calibrar reloj y fecha**
Flujo: Configure Display → arrastrar o redimensionar las cajas de hora y fecha → ajustar Size, Weight, Width, Opacity, Gloss y Colour → Time Format → Save.
Aceptación: superpuesto sobre A, el texto generado coincide en posición, tamaño y grosor con el original (comprobable alternando A y la render).

**CU-03 · Fundir imágenes**
Flujo: Blend Images → ajustar Brightness, Contrast, Saturation y Overall hasta que el parche B no se note sobre A.
Aceptación: vista previa en tiempo real y valores persistidos.

**CU-04 · Configurar la actuación**
Flujo: Performance Settings → dirección, N (Add Extra Minutes), Animate, Delay, Trigger, Speed y opciones → Save.
Aceptación: N acepta 1–999. El delay se muestra en la etiqueta ("Animation Delay (4s)").

**CU-05 · Actuación estándar (N fijo, Atrás)** *(flujo principal)*
Pre: CU-01 a CU-04 hechos, N = 5, Trigger = Proximity.
1. El mago pide a los espectadores que pongan los móviles boca abajo.
2. Abre la app: la pantalla muestra hora real + 5 (p. ej. 20:01 cuando son las 19:56) y avanza con normalidad.
3. Pasa la mano por encima → tras 4 s el reloj rebobina 20:01 → 19:56.
4. Los espectadores miran sus móviles: 19:56.
Alternativos: Tap o Tap (Vibrate) en lugar de proximidad. Animate = off → salto instantáneo.
Aceptación: la hora final coincide con la real al segundo. Si el minuto real cambia durante la animación, termina en el minuto nuevo.
*Contextos de uso (no requieren funciones nuevas): efecto solo; justificar una restauración (cartas que vuelven a su orden, cerillas intactas, objeto roto y recompuesto); combinar con una predicción.*

**CU-06 · N elegido libremente por un espectador (cuadrícula invisible)**
Pre: Grid Input = Invisible, Always On Display recomendado.
1. Un espectador dice un número (p. ej. 7).
2. El mago toca discretamente la zona del 7 de la cuadrícula invisible (vibración de confirmación opcional) y confirma (proponer: doble toque o pausa de 1,5 s).
3. La pantalla pasa a mostrar hora real + 7 (estando "apagada" con Always On, o antes de enseñarla).
4. Continúa como en CU-05 desde el paso 3.
Aceptación: con Grid Includes Zero se pueden introducir números de dos cifras. Ningún elemento de la cuadrícula es visible.

**CU-07 · Modo Adelante**
Igual que CU-05 con Direction = Forwards: la pantalla parte de hora real − N y avanza hasta la hora real.

**CU-08 · Evitar el cambio de minuto**
Pre: Approaching Minute Alert = on.
Flujo: cuando faltan ≤ 10 s (configurable) para el cambio de minuto real, aparece un brillo sutil en el borde inferior. El mago espera a que pase antes de disparar.
Aceptación: el brillo no es reconocible para un espectador y se adapta a modo claro u oscuro.

**CU-09 · Simular móvil bloqueado y despertar**
Pre: Always On Display = on, Auto Wake = on.
Flujo: la pantalla parece apagada o AOD → disparo → al empezar DELAY la pantalla "se enciende" con una transición tipo wake del SO → animación.
Aceptación: la transición imita la del sistema (fade y escalado leve).

**CU-10 · Entrenar la cuadrícula**
Pre: Grid Input = Visible.
Flujo: la cuadrícula se dibuja con números y bordes. El mago practica la memoria muscular y después cambia a Invisible.

**CU-11 · Controles ocultos**
- 2 dedos ↓ → Settings (desde cualquier estado).
- 2 dedos ↑ → reset a IDLE con el offset recalculado.
Aceptación: los gestos no disparan el trigger ni la cuadrícula. Opción "Swap" para intercambiar gestos y botones.

---

## 10. Preguntas abiertas (validar con la app de referencia)

- **Q1 · Peek Activation Time:** ¿se muestra brevemente la hora al activar y la animación empieza cuando la hora real coincide con la inicial mostrada? Propuesta provisional: con Peek activo, en lugar de un retardo fijo, la animación se programa para terminar exactamente en un cambio de minuto real.
- **Q2 · Dragged Screenshot:** ¿cómo se obtiene exactamente en iOS y en Android la captura sin reloj?
- **Q3 · Confirmación de N en la cuadrícula:** ¿doble toque, timeout o un segundo toque en una zona fija?
- **Q4 · Velocidades reales** de Slow, Regular y Fast, y comportamiento con N > 60.
