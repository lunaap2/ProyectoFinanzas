# Mis Finanzas — Gestor de Gastos Personales

Aplicación nativa de Android (Java) para registrar **ingresos y gastos** diarios.
Permite crear, ver, editar y eliminar transacciones, y guarda toda la información
localmente en una base de datos **SQLite**, por lo que los datos permanecen aunque
se cierre la aplicación.

## 👤 Estudiante

**[Escribe aquí tu nombre completo]**

## 📱 Descripción

La app funciona como un registro de finanzas personales:

- **Pantalla principal:** lista todas las transacciones en un `RecyclerView` y
  muestra el **saldo total** (ingresos menos gastos). Incluye un botón flotante
  (`FloatingActionButton`) para agregar una nueva transacción.
- **Pantalla de creación/edición:** formulario con `EditText` para el concepto y
  el monto, y un `RadioGroup` para elegir entre **Ingreso** o **Gasto**.
- **Diseño de cada ítem:** muestra el concepto, el tipo y el monto, con un
  indicador visual por color (verde `+` para ingresos, rojo `-` para gastos).

### Acciones

| Acción | Cómo se hace |
|--------|--------------|
| Crear  | Botón flotante **+** |
| Editar | Toca una transacción de la lista |
| Eliminar | Mantén presionada una transacción (pide confirmación) |

## 🛠️ Requerimientos técnicos cumplidos

1. **Interfaz (Layouts):** pantalla principal con RecyclerView + FAB, pantalla de
   formulario y layout personalizado para cada ítem.
2. **Persistencia SQLite (CRUD):** tabla `transacciones` (`id`, `concepto`,
   `monto`, `tipo`) con los métodos **Insertar, Leer (todas y por id), Actualizar
   y Eliminar**.
3. **RecyclerView:** `Adapter` personalizado en Java con patrón `ViewHolder`. La
   lista se actualiza en tiempo real al agregar, editar o eliminar.

## 🗂️ Estructura del código (separación de responsabilidades)

```
app/src/main/java/com/example/finanzas/
├── Transaccion.java          # Modelo de datos (POJO)
├── DbHelper.java             # Base de datos SQLite y operaciones CRUD
├── TransaccionAdapter.java   # Adapter + ViewHolder del RecyclerView
├── MainActivity.java         # Pantalla principal (lista + saldo + FAB)
└── FormActivity.java         # Formulario de crear / editar

app/src/main/res/layout/
├── activity_main.xml         # Pantalla principal
├── activity_form.xml         # Formulario
└── item_transaccion.xml      # Diseño de cada fila
```

## 📸 Capturas de pantalla

> Reemplaza estas imágenes por capturas reales tomadas desde el emulador o tu
> celular (menú de Android Studio: **View > Tool Windows > Running Devices**, o el
> botón de cámara del emulador). Guárdalas en la carpeta `screenshots/`.

| Lista de transacciones | Formulario de registro |
|------------------------|------------------------|
| ![Lista](screenshots/lista.png) | ![Formulario](screenshots/formulario.png) |

## ▶️ Cómo ejecutar

1. Abrir el proyecto en **Android Studio**.
2. Esperar a que Gradle sincronice.
3. Ejecutar en un emulador o dispositivo (botón ▶ *Run*).

- Lenguaje: **Java**
- `minSdk`: 24 · `targetSdk`: 36
