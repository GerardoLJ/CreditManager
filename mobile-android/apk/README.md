# 📦 CardMaster APK para Android (100% Nativo)

En esta carpeta se encuentra el instalador directo **`CardMaster.apk`** para tu celular Android.

> [!IMPORTANT]
> **🌟 100% Nativo de Android (Cero WebViews, Cero Servidores Web, Cero Luces Rojas):**  
> Esta aplicación fue desarrollada en código nativo para Android (Java + SQLite + Material Components).
> - **Sin servidores:** No requiere Node.js, ni conexiones HTTP, ni puertos localhost, eliminando cualquier indicador de error o luz roja.
> - **Instalación limpia:** Al abrirla por primera vez tras instalarla, inicia 100% libre de contraseñas preconfiguradas, permitiéndote crear tu propia Contraseña Maestra o restaurar un respaldo previo.
> - **Base de datos segura y fija:** Los datos residen en la base de datos interna SQLite (`/data/data/com.cardmaster.finance/databases/cardmaster.db`).
> - **Respaldos sin alterar la base de datos:** Puedes exportar tu respaldo `.json` a **cualquier carpeta de tu teléfono** (Descargas, Google Drive, Tarjeta SD) utilizando el selector de archivos oficial de Android (SAF), **sin que se mueva ni cambie jamás la ubicación de tu base de datos interna**.
> - **Interoperabilidad total:** El archivo JSON generado es 100% compatible para transferirse entre este APK de Android y la Web-App de PC (Windows, macOS o Linux) y viceversa.

---

## 📲 ¿Cómo instalar este archivo en tu celular?

1. **Pásalo a tu celular:**
   - **Opción A (Cable USB):** Conecta tu celular a la PC y copia el archivo `CardMaster.apk` a la carpeta **Descargas** de tu teléfono.
   - **Opción B (Nube o Mensajería):** Sube el archivo `CardMaster.apk` a tu **Google Drive**, envíatelo por **Telegram** o correo electrónico y descárgalo en tu teléfono.

2. **Instalación con 1 toque:**
   - Abre la app **Archivos** o **Descargas** en tu teléfono.
   - Toca el archivo **`CardMaster.apk`**.
   - Si tu teléfono muestra un aviso de seguridad que dice *"Por motivos de seguridad, tu teléfono no tiene permitido instalar apps desconocidas de esta fuente"*:
     - Toca en **Configuración / Ajustes**.
     - Activa la opción **"Permitir desde esta fuente"**.
     - Vuelve atrás y toca **Instalar**.

3. **¡Listo para usar!**  
   - Al tocar el icono de **CardMaster**, verás la pantalla de configuración inicial de la bóveda para crear tu contraseña maestra y comenzar a registrar tus tarjetas, gastos, apartados y presupuestos de inmediato.

---

## 🛠️ ¿Te aparece el mensaje "No se instaló la app" en Android? (Soluciones Rápidas)

Si al tocar el instalador tu teléfono muestra **"No se instaló la app"**, se debe casi siempre a una de las siguientes causas del sistema de Android:

1. **Conflicto con una versión previa (Causa principal):**
   - Si tenías instalada una versión anterior de CardMaster en el celular (compilada anteriormente o instalada como prueba), Android rechaza la instalación por conflicto de firmas de seguridad (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`).
   - **Solución:** Ve a los Ajustes de tu teléfono > **Aplicaciones** > busca **CardMaster** y pulsa **Desinstalar** (asegúrate de que no quede en la papelera de apps). Luego abre de nuevo `CardMaster.apk` e instálalo normalmente.

2. **Bloqueo de Google Play Protect:**
   - En Android 13, 14 y 15, Play Protect examina las apps descargadas fuera de la tienda. Si muestra una ventana que dice *"App no reconocida"* o bloquea la instalación:
   - **Solución:** Toca en **"Más detalles"** y luego pulsa **"Instalar de todas formas"**. (O entra a Google Play Store > foto de perfil > *Play Protect* > icono de engrane arriba a la derecha y desactiva temporalmente el análisis mientras instalas).

3. **Permisos de Orígenes Desconocidos:**
   - Si intentas instalar desde WhatsApp, Telegram o Drive, a veces el instalador se interrumpe.
   - **Solución:** Abre la app oficial **Archivos / Mis Archivos / Files** de tu celular, ve a la carpeta **Descargas**, busca `CardMaster.apk` y ábrelo desde ahí.

4. **Espacio en memoria:**
   - Verifica tener al menos 100 MB libres en el almacenamiento interno de tu teléfono.
