# 📘 Tutorial Definitivo: Cómo Usar CardMaster en tu Celular (Versión Local-First IndexedDB)

> 🎉 **¡NUEVA VERSIÓN LOCAL-FIRST: 100% INDEPENDIENTE Y SIN ERRORES!**  
> - **Persistencia Nativa en IndexedDB:** Almacenamiento seguro, transaccional y sin restricciones de Scoped Storage ni bloqueos binarios.
> - **Cero Dependencia de SQLite Binario:** Sin riesgo de corrupción de archivos entre diferentes CPUs o sistemas operativos.
> - **Respaldos Universales en JSON Plano:** Exporta e importa tus datos en formato `.json` compatible al 100% entre Linux, Windows, macOS y Android.
> - **Integración con Web Share API:** Guarda y comparte tu archivo de respaldo directamente en Google Drive, WhatsApp o Descargas sin necesidad de permisos especiales.

---

## 🌟 Paso 1: Instala el archivo APK en tu celular
1. Copia o descarga el archivo **`CardMaster.apk`** (ubicado en `mobile-android/apk/CardMaster.apk`) en tu teléfono.
2. Abre el explorador de archivos de tu teléfono y toca el archivo **`CardMaster.apk`**.
3. Si Android te pide confirmación o permiso de instalación de apps desconocidas:
   - Toca **Ajustes / Configuración**.
   - Activa el interruptor **"Permitir desde esta fuente"**.
   - Vuelve atrás y pulsa **Instalar** (o **Actualizar**).
4. **¿Aparece el mensaje "No se instaló la app"?**
   - **Desinstala la versión anterior:** Si ya tenías CardMaster instalado antes, ve a *Ajustes > Aplicaciones > CardMaster > Desinstalar* y vuelve a intentar instalar el archivo APK. Android bloquea actualizaciones si detecta diferencias de clave entre versiones.
   - **Google Play Protect:** Si Play Protect bloquea la instalación, toca *"Más detalles"* > *"Instalar de todas formas"*.

---

## 📥 Paso 2: Primer Inicio y Carga de tu Respaldo

### Caso A: Si ya tienes un archivo de respaldo JSON con tus datos
1. Abre **CardMaster** desde el icono en tu pantalla de inicio.
2. Al detectar que la base de datos interna está recién instalada y vacía, la app te mostrará automáticamente el asistente:
   > **¡Bienvenido a CardMaster!**  
   > *La base de datos local se ha inicializado en tu dispositivo de forma 100% privada y fuera de línea.*
3. Toca el botón azul grande: **📥 Importar Respaldo JSON Existente**.
4. Selecciona tu archivo `cardmaster_backup.json` (desde *Descargas*, *Documentos* o *Google Drive*).
5. **Listo:** La aplicación validará el esquema de datos y cargará todas tus tarjetas, personas y movimientos.
6. Ahora solo ingresa tu contraseña maestra y desbloquea la app.

### Caso B: Si vas a iniciar completamente desde cero
1. En el asistente de bienvenida, toca **✨ Iniciar desde Cero (Nueva Bóveda)**.
2. Define tu nueva contraseña maestra y pulsa **Crear Bóveda**.

---

## 💾 ¿Cómo Hacer Respaldos de tus Datos en Cualquier Momento?

Para respaldar tu información de forma segura:
1. Abre el menú lateral y entra a la pestaña **⚙️ Ajustes**.
2. Desliza hasta la sección **Base de Datos en este Dispositivo**.
3. Toca el botón azul: **💾 Exportar Respaldo Completo (.json)**.
4. En Android, se abrirá la hoja para compartir oficial donde puedes guardarlo en Google Drive, enviártelo por mensaje o guardarlo en tu almacenamiento local.
5. ¡Listo! Tu información queda respaldada en un archivo `.json` plano y seguro.

---

## 📂 ¿Cómo Restaurar un Respaldo en Cualquier Momento?

Si en el futuro deseas restaurar una copia previa de tu base de datos:
- **Desde la pantalla de bloqueo:** Toca el enlace **📥 Importar Respaldo (.json)**, selecciona tu archivo y quedará restaurado al instante.
- **Desde Ajustes:** En la sección de base de datos, toca **📥 Importar y Restaurar Respaldo (.json)** o arrastra tu archivo al recuadro.
