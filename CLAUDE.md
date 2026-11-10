# TimeTravel

App móvil para **Android y iOS** con un único código en Kotlin Multiplatform + Compose Multiplatform.
El usuario habla español: responde y comenta el código en español.

> **Estado:** esqueleto KMP creado y compilando en Android (`com.rewind`, nombre provisional **Rewind**). La fuente de
> verdad funcional es **`SPEC.md`**. iOS aún no se ha compilado nunca.

## Reglas para Claude
- **Ignorar por completo la carpeta `ignore/`** (vídeos y material suelto del usuario): no leerla, no listarla, no
  buscar dentro, no usarla como referencia. Excluirla también de búsquedas (Glob/Grep) y del control de versiones.
- La carpeta `data/` es para material de referencia de la app (capturas, recursos, especificaciones) que el usuario irá aportando.
- La base técnica y de arquitectura es **`ARQUITECTURA.md`** (sigue ese documento para montar el proyecto; es la fuente de
  verdad de versiones, estructura, Gradle, CI de iOS y reglas de código). Si algo choca con él, consultar al usuario.
- Sustituir `com.miapp` / `MiApp` de `ARQUITECTURA.md` por el paquete y nombre reales cuando se decidan (pendiente: preguntar).
- **Antes de lanzar el workflow de iOS en GitHub Actions, preguntar siempre al usuario** (cuesta cuota de macOS; se acumulan mejoras).
- No inventar funcionalidad: esperar a las especificaciones del usuario antes de diseñar pantallas o lógica.

## Qué hace la app
Simulador de reloj de pantalla de bloqueo para magia (detalle en `SPEC.md`). Muestra a pantalla completa una réplica de la
pantalla de bloqueo del móvil con un reloj falso (`hora real ± N min`, sincronizado con el cambio de minuto real). Con un
disparo oculto (Tap / Tap+vibración / Proximidad) y tras un retardo, el reloj rebobina N minutos con animación hasta la hora
real. No toca el reloj del sistema. Gestos ocultos: 2 dedos ↓ = Settings, 2 dedos ↑ = reset. Sin red ni cuentas; ajustes solo en local.
- Fase 1 (MVP): capturas A/B de la galería, editor de hora/fecha, Blend Images, offset, disparadores, animación, persistencia.
- Fase 2: cuadrícula invisible para N en directo, entrenamiento, alerta de minuto, Always On/Auto Wake, botones de volumen.
- Máquina de estados: IDLE → (GRID_INPUT) → ARMED → DELAY → ANIMATING → SYNCED_HOLD (SPEC §6).
- Lógica temporal 100 % testeable con reloj inyectable; código en `shared/.../time/` (`TimeLogic.kt`, `Performance.kt` = máquina de estados ARMED→DELAY→ANIMATING→SYNCED_HOLD como función pura de (estado, ahora)). Con N grande la animación se acelera (máx. 20 s, provisional, Q4).
- UI actual: `PerformanceScreen` (toque = disparo; 2 dedos ↓ = Settings, 2 dedos ↑ = reset; `Gestures.kt`) y `SettingsScreen` (dirección, N, formato, animar, retardo, velocidad). Android: inmersivo + pantalla siempre encendida. Display Settings (capturas, editor, blend) pendiente.
- Preguntas abiertas Q1–Q4 en `SPEC.md` §10.

## Arquitectura (resumen de `ARQUITECTURA.md`)
- **Un solo código** en `shared/commonMain` (lógica pura + interfaz Compose). Solo lo que depende del sistema va con
  `expect`/`actual` (`androidMain` / `iosMain`). Regla de oro: si no es estrictamente del sistema, va en común.
- `shared/`: módulo multiplataforma con TODO el código.
  - `commonMain/.../<dominio>/`: lógica pura, sin UI ni Android.
  - `commonMain/.../ui/`: pantallas, ajustes, `Platform.kt` (`expect`), `SettingsStore` + interfaz `KeyValueStore`.
  - `commonMain/.../App.kt`: raíz Compose común.
  - `androidMain` / `iosMain`: las `actual` (SharedPreferences / NSUserDefaults, barras del sistema) y `MainViewController` (iOS).
  - Tests en `commonTest` con `kotlin.test` (se ejecutan en JVM; opcionalmente en el simulador de iOS).
- `app/`: solo la envoltura Android (`MainActivity`, manifiesto, recursos).
- `iosApp/`: envoltura iOS en SwiftUI + `project.yml` (XcodeGen genera el `.xcodeproj` en CI; no se versiona).
- `.github/workflows/ios.yml`: en un Mac de GitHub (`macos-15`) compila y publica un `.ipa` sin firmar para firmarlo con
  Sideloadly/AltStore. Solo se lanza a mano (`workflow_dispatch`).
- En Windows los destinos de iOS se ignoran (`kotlin.native.ignoreDisabledTargets=true`); Android compila y testea igual.

## Reglas de código para que compile en iOS
- En `commonMain`: nada de `java.*` ni `android.*`. Usar `bignum` (`com.ionspin.kotlin:bignum:0.3.10`),
  `kotlinx-datetime` (0.6.1), `kotlin.math.*`. Sin `String.format`, `Locale`, `SimpleDateFormat`, `Thread`, `synchronized`.
- Sin recursos de Android (`R.string`) en común; textos en el código.
- Iconos dibujados con `Canvas`/`Path` de Compose (no `material-icons-extended`).
- Lógica de dominio pura y sin UI, con tests en `commonTest`.
- Lo del sistema (sensores, vibración, portapapeles, red…): interfaz en común + implementación por plataforma inyectada desde el punto de entrada.
- El fallo de iOS solo se ve en Actions (~20 min): revisar con cuidado antes de lanzarlo.

## Compilar y probar (Windows)
- Kotlin 2.0.21, Compose Multiplatform 1.7.1, AGP 8.7.3, Gradle 8.9, JDK 17, minSdk 26, target/compileSdk 35, iOS mínimo 15.0.
- `JAVA_HOME` apunta al JDK 17: `C:\Program Files\Microsoft\jdk-17.0.21.21-hotspot`.
- SDK de Android en `C:\Users\jmlop\AppData\Local\Android\Sdk` (`local.properties`, ignorado por git).
- `./gradlew.bat :shared:testDebugUnitTest assembleDebug` → APK en `app/build/outputs/apk/debug/app-debug.apk` (se instala a mano).
- Emulador AVD `calcu` (Android 15), reutilizable. Notas: poner la zona horaria `Europe/Madrid` (arranca en GMT); el teclado
  Gboard del emulador puede salir en negro (es del emulador, no de la app).
- `gh` en Windows: `C:\Program Files\GitHub CLI\gh.exe` (hace falta `gh auth login` una vez).

## Flujo de trabajo
1. Desarrollar y probar en Android/JVM en local.
2. Commit y subida a la rama de iOS (aparte de `main`).
3. **Preguntar antes de lanzar Actions** y acumular mejoras: `gh workflow run ios.yml --ref <rama>`.
4. Descargar el `.ipa` e instalarlo con Sideloadly (Apple ID gratis; caduca a los 7 días; activar Modo desarrollador en el iPhone).

## Pendiente
- Recibir las especificaciones de la app y rellenar "Qué hace la app".
- Decidir nombre definitivo (hoy `Rewind` / `com.rewind`) y rama de iOS; crear repo git.
- Falta el icono de la app (Android e iOS). Comprobar en iOS cuando el usuario lo autorice.
- Firmar el `.ipa` (pendiente de decidir cómo).
