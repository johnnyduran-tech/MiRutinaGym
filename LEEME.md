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
