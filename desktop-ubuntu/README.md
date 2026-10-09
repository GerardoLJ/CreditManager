# 🖥️ CardMaster — Edición Escritorio Nativo para Linux Ubuntu

Esta carpeta contiene la **aplicación nativa 100% de escritorio para Linux Ubuntu**, diseñada bajo la filosofía **Local-First Software**. Funciona en su propia ventana dedicada (GTK3 / WebKitGTK) con acceso directo en el escritorio y sin barras de navegador.

> **Independencia Total:** Esta carpeta es 100% autónoma. Puedes moverla a cualquier otra computadora con Ubuntu o eliminar cualquier otra carpeta sin que esta aplicación deje de funcionar.

---

## 🚀 Inicio Rápido (En 1 Clic)

### Opción A: Instalar Acceso Directo en el Escritorio (Recomendado)
Ejecuta el asistente de instalación:
```bash
./instalar-en-ubuntu.sh
```
*Esto creará el icono de **CardMaster** en tu Escritorio y en el menú de aplicaciones de Ubuntu.*  
A partir de ahí, solo haz **doble clic en el icono de CardMaster**.

### Opción B: Iniciar desde la Terminal
```bash
./iniciar-ubuntu.sh
```

### Para Detener la Aplicación
Al cerrar la ventana pulsando la **X**, el servicio se apaga automáticamente. Si deseas detenerlo manualmente:
```bash
./detener-ubuntu.sh
```

---

## 📁 Estructura Interna de esta Carpeta

```text
desktop-ubuntu/
├── 📄 desktop-app.py         # Ventana nativa GTK3 + WebKitGTK 4.1 con gestión de ciclo de vida
├── 📄 iniciar-ubuntu.sh      # Lanzador en un solo paso
├── 📄 detener-ubuntu.sh      # Detención limpia de procesos
├── 📄 CardMaster.desktop     # Acceso directo para escritorio y menú GNOME
├── 📄 instalar-en-ubuntu.sh  # Asistente de instalación
├── 📄 resetear-contrasena.sh # Utilidad para recuperar acceso a la bóveda
├── 📄 server.js              # Backend local Node.js + Express + SQLite
├── 📄 tarjetas.db            # Base de datos física local de este equipo
├── 📁 public/                # Interfaz de usuario (HTML, CSS, JS, Iconos)
├── 📁 node_modules/          # Dependencias locales (Express, SQLite3, CORS)
├── 📄 package.json           # Manifiesto de paquetes npm
└── 📄 README.md              # Este manual
```

---

## 🔒 Seguridad y Privacidad
* **Bóveda Criptográfica:** PBKDF2 con SHA-256 (100,000 iteraciones) y salt aleatorio único.
* **Soberanía de Datos:** Toda tu información se guarda en [`tarjetas.db`](file:///home/personal/Web-Personal/CreditManager/desktop-ubuntu/tarjetas.db). Cero servidores externos, cero telemetría.
* **Recuperación de Contraseña:** Si olvidas tu clave maestra, ejecuta `./resetear-contrasena.sh` para crear una nueva contraseña sin perder tus tarjetas ni movimientos.

---

## 🔄 Transferir Datos a tu Celular
Si deseas pasar tus datos al celular de forma voluntaria:
1. Abre CardMaster y ve a la pestaña **Ajustes**.
2. Pulsa **"Exportar Sincronización (JSON)"** o descarga una copia de `tarjetas.db`.
3. Pasa ese archivo a tu teléfono e impórtalo en la aplicación de Android.

