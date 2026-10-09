# 🖥️ Guía para Usar CardMaster en tu PC Ubuntu (Paso a Paso para No Programadores)

Esta guía explica **todo lo que necesitas saber** para usar CardMaster en tu computadora con Linux Ubuntu, explicada paso a paso y de forma muy sencilla.

---

## 📍 ¿En dónde se guardó el script para agregar la app a tu escritorio?

El script instalador se encuentra guardado en esta ruta exacta de tu equipo:

```text
/home/personal/Web-Personal/CreditManager/desktop-ubuntu/instalar-en-ubuntu.sh
```

### ¿Qué hace exactamente ese script?
Cuando ejecutas ese script, hace tres cosas de forma automática:
1. Toma el archivo lanzador **`CardMaster.desktop`** y le pone la ruta completa de tu computadora y el icono oficial de alta resolución.
2. Copia ese acceso directo a tu **menú de aplicaciones de Ubuntu** (`~/.local/share/applications/CardMaster.desktop`).
3. Copia el icono directamente a tu **Escritorio** (`~/Escritorio/CardMaster.desktop` o `~/Desktop/CardMaster.desktop`) para que puedas abrirlo con solo hacer doble clic.

---

## 🚀 Cómo Instalar el Icono en tu Escritorio (Solo se hace una vez)

Si aún no tienes el icono en tu pantalla o si quieres volver a crearlo, solo abre tu terminal y escribe:

```bash
cd /home/personal/Web-Personal/CreditManager/desktop-ubuntu
./instalar-en-ubuntu.sh
```

*¡Listo! Verás el icono de **CardMaster** en tu Escritorio y en la lista de aplicaciones de Ubuntu.*

---

## 🖱️ Cómo Abrir y Usar CardMaster Todos los Días

Tienes 3 formas muy sencillas de abrir tu aplicación:

### Forma 1: Con Doble Clic en tu Escritorio (La más fácil)
1. Busca el icono de **CardMaster** en tu Escritorio de Ubuntu.
2. Haz **doble clic** sobre él.
3. Se abrirá una ventana limpia y elegante, **100% de escritorio**, sin pestañas de navegador ni barras de internet.

### Forma 2: Buscando en tus Aplicaciones de Ubuntu
1. Presiona la tecla **Super / Windows** en tu teclado (o haz clic en "Actividades" arriba a la izquierda).
2. Escribe **CardMaster** en el buscador.
3. Haz clic en el icono. *(Si quieres tenerlo siempre a la mano en tu barra lateral izquierda, haz clic derecho en el icono y elige "Añadir a favoritos")*.

### Forma 3: Desde la Terminal
Si prefieres abrirlo con un comando:
```bash
cd /home/personal/Web-Personal/CreditManager/desktop-ubuntu
./iniciar-ubuntu.sh
```

---

## 🛑 Cómo Cerrar la Aplicación

* **Manera normal:** Solo haz clic en la **"X"** de la ventana (arriba a la derecha).  
  *La aplicación está programada de forma inteligente: al cerrar la ventana, el motor de la base de datos se apaga automáticamente en segundo plano sin dejar basura en tu memoria ni ocupar puertos.*
* **Manera manual (por si acaso):**  
  Si alguna vez necesitas asegurar que todo esté cerrado, ejecuta:
  ```bash
  cd /home/personal/Web-Personal/CreditManager/desktop-ubuntu
  ./detener-ubuntu.sh
  ```

---

## 💾 ¿En dónde se guardan tus datos financieros en la PC?

Toda tu información (tus tarjetas, tus gastos, el dinero apartado y tus deudas) se guarda físicamente en un solo archivo dentro de tu computadora:

```text
/home/personal/Web-Personal/CreditManager/desktop-ubuntu/tarjetas.db
```

### 🔒 Tu Privacidad es 100% Real:
* Este archivo vive en tu disco duro. **No se envía a ningún servidor de internet ni a ninguna nube**.
* **Cómo hacer un respaldo en una memoria USB:** Solo copia ese archivo `tarjetas.db` a tu memoria USB o disco externo. Si cambias de computadora, solo pegas ese archivo en la nueva máquina y tendrás todas tus finanzas listas.

---

## 🔑 ¿Qué hacer si olvidas tu contraseña maestra?

Por seguridad, la contraseña maestra está cifrada con un candado matemático muy fuerte (PBKDF2 con 100,000 rondas). Si se te llega a olvidar:

1. Ve a la terminal y ejecuta este script de rescate:
   ```bash
   cd /home/personal/Web-Personal/CreditManager/desktop-ubuntu
   ./resetear-contrasena.sh
   ```
2. Este script borra **únicamente el candado de la contraseña olvidada**, pero **NO borra tus tarjetas ni tus gastos**.
3. Abre CardMaster de nuevo y te pedirá inventar una nueva contraseña. ¡Todos tus movimientos seguirán ahí intactos!

---

## 💳 Nuevas Funciones Fáciles de Usar en la PC

* **Mover tarjetas con el ratón:** Puedes hacer clic sobre cualquier tarjeta de crédito, arrastrarla y soltarla donde quieras para acomodarlas a tu gusto.
* **Corregir un gasto (✏️ Editar):** Si te equivocaste en el precio o en el nombre de una compra, en la lista de Movimientos haz clic en el lápiz **✏️** para corregirlo en un segundo sin tener que borrarlo.
* **Separar miles con comas automáticamente:** Cuando escribas montos de dinero, por ejemplo `15000`, la aplicación pondrá automáticamente comas y puntos (`$15,000.00`) para que nunca te confundas de ceros.
* **Fondos Apartados (Dinero en Mano):** Puedes registrar cuándo tienes dinero en efectivo o en tarjeta de débito listo para pagar el corte, para no gastártelo en otras cosas.
* **El Día Dorado en el Calendario:** En la pestaña Calendario, las estrellas doradas te indican los días del mes donde todas tus tarjetas ya cortaron y ninguna ha vencido, para que puedas pagarlas todas juntas en una sola ida al banco.
