# Mi Rutina Gym (Android)

App de rutina de hipertrofia (ciclo de 8 días) con animaciones de técnica, temporizadores de descanso,
registro diario y gráficas de progreso. Todo funciona sin internet; los datos quedan en el teléfono.

## Obtener el APK

### Opción A – Sin instalar nada (GitHub, gratis)
1. Crea un repositorio nuevo en github.com y sube el contenido de esta carpeta (incluida `.github`).
2. Ve a la pestaña **Actions** → "Compilar APK" se ejecuta solo (~5 min).
3. Abre la ejecución terminada → sección **Artifacts** → descarga `MiRutinaGym-apk` (zip con `app-debug.apk`).
4. Pásalo al teléfono, ábrelo y permite "Instalar apps de origen desconocido".

### Opción B – Android Studio
1. File → Open → selecciona esta carpeta.
2. Espera la sincronización de Gradle → Build → Build APK(s).

## Modificar la app
Toda la interfaz está en `app/src/main/assets/index.html` (HTML + JS, un solo archivo).
Respaldo: Más → Exportar abre el menú Compartir para enviarte el respaldo (WhatsApp, Drive, correo).

## Sonidos y segundo plano (v1.1)
- Suena una alerta corta al completar cada serie y una alerta fuerte (3 repeticiones) al terminar cada descanso o isométrico.
- Los sonidos van por el canal de ALARMA y, con "Volumen fuerte" activo (Más → Ajustes), suben el volumen de alarma al máximo solo mientras suenan; luego lo restauran.
- Durante el entrenamiento hay una notificación fija con la cuenta regresiva. La alerta suena aunque estés en otra app o con la pantalla apagada.
- Primera vez: acepta el permiso de notificaciones y toca Más → "Permitir funcionar en segundo plano".
- En Xiaomi, Samsung, Huawei u Oppo: Ajustes → Apps → Mi Rutina Gym → Batería → "Sin restricciones" (y en Xiaomi activa también "Inicio automático").

## v1.2
- Reordenar ejercicios arrastrando (☰) o con flechas: en Hoy, en Rutina y durante el entrenamiento.
- Editar, reemplazar, agregar o quitar ejercicios antes de iniciar (se guarda en la rutina) o en plena sesión (eliges si aplica solo a hoy o también a la rutina).
- Vista del entrenamiento: "Por ejercicio" o "Todos en una página" (se cambia arriba en la sesión o en Más → Ajustes).

## v1.3
- Biblioteca de 80 ejercicios animados agrupados por músculo (Pecho, Espalda, Hombros, Bíceps, Tríceps, Piernas, Glúteos, Core, Cardio).
- Tipos de registro: peso + reps, reps sin peso, duración (seg) y cardio (minutos + km + kcal opcionales).

## v1.4
- CORRECCIÓN: la alerta de fin de descanso ahora usa AlarmManager (alarma del sistema). Suena aunque estés en
  WhatsApp, Instagram, X o con la pantalla apagada, incluso si el sistema congela la app.
- Nuevo en Más → "Alertas en segundo plano": estado de permisos + botón "Probar alerta en 10 s".
- Modo claro / oscuro / automático (Más → Apariencia, o el ícono ☀️/🌙 en Hoy).
- Si publicas la versión web en GitHub Pages, reemplaza también ese index.html por app/src/main/assets/index.html.

## v1.5
- CORRECCIÓN: vibración al terminar el descanso. Ahora vibra con uso "alarma" (Android bloquea las vibraciones
  normales de apps en segundo plano) y con intensidad máxima. Botón "📳 Vibración" en Más para probarla.

## v1.6
- Animaciones rediseñadas en estilo ilustración anatómica: figura con volumen, fondo claro, máquinas grises
  con acolchado negro y músculos trabajados resaltados (rojo = principal, naranja = secundarios).
- Encuadre automático por ejercicio y animación optimizada (solo se animan las visibles).

## v1.7
- Minimizar el entrenamiento en curso (botón ⌄ arriba a la izquierda o el botón Atrás del teléfono).
  Queda una barra flotante con tiempo, progreso y cuenta regresiva del descanso; tócala para volver.

## Ejercicios animados (GIF / MP4 / PNG)
- Carpeta `ejercicios/`: 80 GIF, MP4 e imagen fija de cada ejercicio de la biblioteca, generados con el mismo motor de animación de la app.
- `ejercicios/ejercicios.json`: catálogo con `exerciseId` (`mrg_<clave>`), `animKey`, músculos, equipo, instrucciones, consejos, variaciones, relacionados y valores por defecto.
- `ejercicios/index.html`: galería con buscador. En la versión web queda en `<tu-página>/ejercicios/`.
- No van dentro del APK (no está en `assets`), así que la app no aumenta de tamaño.

## v1.8
- Tiempo del entrenamiento en formato horas: pasada la hora se muestra "1 h 20 min 05 s".
- Rachas diarias en Hoy: racha actual, mejor racha, entrenos del mes y los últimos 14 días. Un día de descanso marcado como completado mantiene la racha.
- Calorías estimadas al finalizar (fórmula MET según tu peso, series y repeticiones marcadas); se guardan en el historial.
- Aviso de fin de descanso más pequeño ("¡Vamos de nuevo!"): ocupa una franja chica, dura ~2 s y se quita al tocarlo.

## v1.8.1 (respaldo)
- Exportar ahora comparte un ARCHIVO .json (sin límite de tamaño). Guárdalo en Drive, correo o envíalo por WhatsApp como documento.
- "Seleccionar archivo" en Importar ya funciona dentro de la APK.
- Importar acepta texto copiado de WhatsApp en varias partes (con fecha y nombre) y avisa si el respaldo está cortado. Los entrenamientos actuales se conservan (se combinan).
