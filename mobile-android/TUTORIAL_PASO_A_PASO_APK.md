# 📘 Tutorial Definitivo: Cómo Usar CardMaster y su APK (Para No Programadores)

> Si nunca has usado una terminal o te sientes confundido con tantos términos, **no te preocupes**.  
> Esta guía te explica todo como si fuera una receta de cocina, paso a paso y sin palabras raras.

---

## 💡 Antes de empezar: La gran confusión aclarada

Mucha gente cree que tiene que *"abrir el APK dentro de Termux"*. **¡Esto NO es así!**

Piensa en CardMaster como un coche:
* ⚙️ **Termux es el MOTOR del coche:** Trabaja debajo del capó (en segundo plano).
* 📱 **El APK (CardMaster) es la PANTALLA y el VOLANTE:** Es la app bonita con botones, tarjetas y colores donde tú registras tus gastos.
* **¿Qué pasa si intentas manejar el coche con el motor apagado?** La pantalla te dice *"Error al conectar con el servidor"*, porque el motor aún no está encendido.
* **¿Cómo se abre el APK?** Con tu dedo en la pantalla de tu celular, **exactamente igual que como abres WhatsApp, YouTube o Facebook**.

---

## 🟢 PARTE 1: La Preparación (Solo se hace una sola vez)

Si ya tienes instalado el APK de **CardMaster** en tu pantalla de inicio y tienes la app **Termux**, realiza esta configuración una sola vez:

### Paso 1: Dale permiso a Termux para ver archivos
1. Abre la aplicación **Termux** en tu teléfono (la pantalla negra).
2. Escribe lo siguiente y presiona la tecla **Enter** (la flecha azul en tu teclado):
   ```bash
   termux-setup-storage
   ```
3. En tu teléfono saldrá un mensaje que dice: *"¿Permitir a Termux acceder a fotos y archivos?"*. Presiona **Permitir**.

---

### Paso 2: Preparar la carpeta dentro de Termux
Copia y pega este comando en Termux y presiona **Enter**:
```bash
cp -r "/storage/emulated/0/Web personal/CreditManager-main" ~/
```
*(Esto copia la carpeta a la memoria interna de Termux para que Android no bloquee permisos).*

Ahora entra a la carpeta e instala los paquetes necesarios (esto tarda 1 minuto):
```bash
cd ~/CreditManager-main/mobile-android
npm install
```

---

### Paso 3: Crear el botón de encendido mágico (El comando `iniciar`)
Para que nunca más tengas que escribir comandos largos, copia y pega esto una sola vez:
```bash
echo "alias iniciar='cd ~/CreditManager-main/mobile-android && node server.js'" >> ~/.bashrc && source ~/.bashrc
```
¡Listo! La preparación ha terminado para siempre.

---

## 🚀 PARTE 2: Tu Rutina Diaria (Cómo usar la app todos los días en 3 toques)

A partir de hoy, cada vez que quieras registrar un gasto o ver tus tarjetas en tu celular, haz estos **3 toques sencillos**:

```text
[ Toque 1: Abre Termux y escribe "iniciar" ] 
                   ⬇️
[ Toque 2: Presiona el botón HOME para minimizar ] 
                   ⬇️
[ Toque 3: Abre el icono de CardMaster en tu pantalla ]
```

### Toque 1: Encender el motor
1. Abre **Termux**.
2. Escribe solamente la palabra:
   ```bash
   iniciar
   ```
   ...y presiona **Enter**.
3. Verás salir unas letras en la pantalla diciendo:  
   `💳 SERVIDOR CARDMASTER INICIADO CON ÉXITO en http://localhost:3000`

### Toque 2: Minimizar (No cerrar)
Presiona el botón del medio de tu teléfono (botón **Home** o desliza con el dedo hacia arriba) para salir al menú de tu teléfono.  
*(Termux se quedará trabajando silenciosamente en segundo plano).*

### Toque 3: Abrir tu aplicación
Toca el icono de **CardMaster** en la pantalla de tu celular con tu dedo.  
Verás la pantalla de inicio con la luz en **🟢 Verde (Conectado)** pidiéndote tu contraseña. ¡Eso es todo!

---

## 🛑 PARTE 3: Cómo apagarlo cuando termines tu día

Cuando ya no vayas a usar la app y quieras apagar el motor para ahorrar batería:

1. Abre **Termux**.
2. En la barra de botones que está justo arriba de tu teclado, toca el botón que dice **`Ctrl`** y luego presiona la letra **`c`** en tu teclado (`Ctrl + C`).
3. La pantalla volverá a mostrar el símbolo `$`. Ya está apagado.

---

## 💻 PARTE 4: ¿Y si NO quiero usar Termux para nada? (Opción con tu PC)

Si prefieres no usar Termux en tu teléfono y quieres que tu computadora sea el motor:

1. **En tu computadora (Ubuntu):**
   Abre la terminal y ejecuta:
   ```bash
   cd /home/personal/Web-Personal/CreditManager/mobile-android
   ./iniciar-android.sh
   ```
2. **En tu celular (Asegúrate de estar en el mismo Wi-Fi de tu casa):**
   - Abre la app **CardMaster**.
   - En la parte superior de la pantalla toca el botón:  
     👉 **⚙️ Servidor: Desconectado**.
   - Selecciona la opción **"💻 En tu PC por Wi-Fi"**.
   - Escribe la dirección IP de tu computadora:  
     `http://192.168.10.122:3000`
   - Pulsa **"Guardar y Probar"**.

¡Listo! La app se conectará automáticamente a tu computadora por la red de tu casa sin necesidad de abrir Termux en tu teléfono.
