# 🌐 CardMaster - Versión Web Universal

Bienvenido a la versión **Web Universal** de CardMaster, diseñada para funcionar en **cualquier dispositivo y sistema operativo** (Windows, macOS, iPhone/iPad iOS, tablets y cualquier navegador moderno) **sin necesidad de conocimientos técnicos ni comandos complejos**.

---

## 🚀 ¿Cómo usarlo en tu sistema operativo?

### 🪟 En Windows (10 u 11)
> **¡Solo 1 clic, sin tocar la terminal!**

1. Entra a esta carpeta `web-app`.
2. Haz doble clic sobre el archivo:  
   👉 **`iniciar-windows.bat`**
3. Se abrirá automáticamente tu navegador (Chrome, Edge, etc.) con **CardMaster listo para usarse** en: `http://localhost:3000`.
4. Cuando termines, simplemente cierra la ventana o haz doble clic en **`detener-windows.bat`**.

---

### 🍎 En Mac (macOS)
> **Doble clic directo desde Finder:**

1. Abre esta carpeta en Finder.
2. Haz doble clic en el archivo:  
   👉 **`iniciar-mac.command`**
3. Se abrirá automáticamente en tu navegador Safari/Chrome.
4. Para detener la app cuando termines, haz doble clic en **`detener-mac.command`**.

---

### 📱 En iPhone / iPad (iOS) o Tablets
> **Puedes usarla como una App nativa (PWA) sin instalar nada desde la App Store:**

1. **Abre CardMaster en tu computadora** (Windows, Mac o Ubuntu) estando conectado a la misma red Wi-Fi de tu casa/oficina.
2. La ventana de tu computadora te mostrará una dirección para celulares (ejemplo: `http://192.168.1.50:3000`).
3. En tu **iPhone o iPad**, abre **Safari** y escribe esa dirección.
4. **Instálala como App en tu pantalla de inicio:**
   - Toca el botón **Compartir** (el cuadrito con la flecha hacia arriba ⬆️ en la parte inferior de Safari).
   - Elige la opción **"Añadir a pantalla de inicio"** (o *"Add to Home Screen"*).
   - ¡Listo! Tendrás el icono de **CardMaster** en tu pantalla de inicio como cualquier otra aplicación.

---

### 🐧 En otras distribuciones Linux (Fedora, Debian, Arch, etc.)
1. Haz doble clic o ejecuta el script:  
   👉 **`./iniciar-linux.sh`**
2. Se abrirá automáticamente tu navegador predeterminado.

---

## 💾 ¿Cómo sincronizar tus datos con tu archivo `tarjetas.db`?

Si ya tienes tus tarjetas y compras guardadas en tu archivo `tarjetas.db`:

1. Abre la aplicación web en cualquier dispositivo.
2. Si te aparece la pantalla de bienvenida con la contraseña, puedes hacer clic en:  
   **`📂 Cargar / Reemplazar tarjetas.db`**  
   y seleccionar tu archivo.
3. O una vez adentro, ve a **Ajustes** ➔ **Base de Datos SQLite** ➔ toca en **"Arrastra tu tarjetas.db o haz clic para elegir"** y selecciona tu archivo.
4. Todos tus datos se cargarán y sincronizarán en 1 segundo.
