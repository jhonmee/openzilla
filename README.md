# OpenZilla

Aplicación Android de código abierto para llevar el control de hábitos y adicciones que
quieres dejar (fumar, alcohol, redes sociales, compras impulsivas, etc.), inspirada en apps
tipo "Quit That" pero pensada desde cero para ser **100% local y privada**.

## Por qué existe

Es una alternativa gratuita y auditable a apps similares que cobran por funciones básicas o
que envían datos a servidores externos. OpenZilla no lo hace: no tiene cuentas, no tiene
compras dentro de la app, no tiene anuncios y no puede conectarse a internet aunque quisiera
(el permiso `INTERNET` ni siquiera está declarado en el `AndroidManifest.xml`).

## Funciones

- Registra cualquier hábito o adicción que quieras dejar, con icono, categoría y tipo de
  coste (dinero / tiempo / evento).
- Contador de tiempo sin recaídas en tiempo real, con un indicador circular de progreso
  hacia una meta configurable (24h por defecto, como en la app de referencia).
- Calendario mensual que marca los días cubiertos por tu racha actual.
- Frase motivacional del día y lista de motivos personales para dejar cada hábito.
- Estimación de dinero ahorrado / tiempo recuperado según el tipo de hábito.
- Estadísticas: racha actual, mejor racha histórica, número de recaídas, historial en
  gráfico de barras simple.
- Sala de trofeos con logros por duración (24 horas, 3 días, 1 semana… hasta 2 años).
- Tema claro/oscuro (o seguir al sistema), con color de acento personalizable por separado
  para cada modo.
- Bloqueo opcional de la app con PIN (nunca se guarda en texto plano; se guarda con hash
  PBKDF2 + salt dentro de un almacén cifrado con Android Keystore).
- Exportar/importar todos tus datos a un archivo `.json` que tú eliges dónde guardar —no
  hay backup automático a ninguna nube.
- Sin publicidad, sin compras, sin "versión premium": todo está disponible siempre.

## Privacidad y seguridad, en concreto

- **Sin permiso de red**: no se declara `INTERNET` en el manifest, así que el sistema
  operativo bloquea cualquier intento de conexión aunque el código lo intentara.
- **`android:allowBackup="false"`**: tus datos tampoco viajan a través del backup
  automático de Android/Google.
- **Base de datos local (Room/SQLite)** como única fuente de datos persistentes.
- **PIN con hash salteado (PBKDF2-HMAC-SHA256, 120 000 iteraciones)** guardado en
  `EncryptedSharedPreferences`, respaldado por Android Keystore.
- **Exportación/importación explícitas** mediante el selector de archivos del sistema
  (Storage Access Framework): la app nunca elige la ubicación por ti ni sube nada.

## Diseño pensado para no tener fugas de memoria ni tareas fantasma en segundo plano

- No hay ningún `Service` en primer plano ni bucles infinitos fuera de la UI visible.
- Los contadores en vivo (el de la pantalla de resumen, por ejemplo) usan
  `LaunchedEffect`, que Compose cancela automáticamente en cuanto la pantalla deja de
  estar visible — no queda nada corriendo de fondo.
- Las notificaciones (frase del día y avisos de logros) se programan con `WorkManager`,
  que el sistema operativo agrupa y respeta Doze/ahorro de batería; no hay temporizador
  propio ni `AlarmManager` despertando el dispositivo.
- Todas las escrituras a la base de datos van envueltas en `Result<T>` y, cuando implican
  varios pasos (por ejemplo, "reiniciar racha" = guardar historial + actualizar contador),
  se ejecutan dentro de una única transacción de Room: o se completan ambas, o ninguna.
- Cualquier acción irreversible (eliminar un hábito, reiniciar una racha, borrar todos los
  datos) pasa por un cuadro de diálogo de confirmación explícito.

## Compilar el proyecto

Requisitos: Android Studio (Koala o más reciente) o Gradle + Android SDK por línea de
comandos, JDK 17.

```bash
./gradlew assembleDebug
```

El APK de depuración queda en `app/build/outputs/apk/debug/`. Para instalarlo directamente
en un dispositivo conectado:

```bash
./gradlew installDebug
```

Este repositorio no incluye el *wrapper* de Gradle (`gradlew`/`gradle-wrapper.jar`) para
mantenerlo ligero; ábrelo con Android Studio y deja que genere el wrapper automáticamente,
o ejecuta `gradle wrapper` una vez si usas Gradle instalado en tu sistema.

## Estructura del proyecto

```
app/src/main/java/com/openzilla/app/
  data/         Entidades Room, DAOs, repositorio, DataStore de ajustes, PIN, exportar/importar
  ui/theme/     Tema Compose (claro/oscuro, color de acento personalizable)
  ui/home/      Lista de hábitos
  ui/addhabit/  Asistente para crear/editar un hábito
  ui/detail/    Pantalla de detalle con las 4 pestañas (Resumen, Motivación, Progreso, Trofeos)
  ui/settings/  Ajustes, bloqueo por PIN, exportar/importar, borrar datos
  ui/components/Piezas reutilizables (medidor circular, calendario, diálogo de confirmación)
  notification/ Notificaciones locales vía WorkManager
  util/         Categorías, formato de tiempo, trofeos, frases motivacionales
```

## Licencia

MIT — ver `LICENSE`. Úsala, modifícala y redistribúyela libremente.
