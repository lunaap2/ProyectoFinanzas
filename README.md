# Mis Finanzas — Gestor de Gastos Personales

Aplicación nativa de Android (Java) para registrar **ingresos y gastos** diarios.
Permite crear, ver, editar y eliminar transacciones, con los datos sincronizados
**en la nube y en tiempo real** mediante **Firebase Firestore**: cualquier cambio
hecho desde un teléfono se refleja al instante en todos los demás dispositivos
conectados a la misma cuenta.

##  Estudiante

**Luna Gutierrez Diaz**

##  Evolución del proyecto

| Entrega | Persistencia | Alcance |
|---------|--------------|---------|
| Primer Seguimiento (Momento 1) | SQLite local | Los datos vivían solo en el teléfono donde se instalaba la app. |
| Segundo Seguimiento (Momento 2) — **actual** | Firebase Firestore (NoSQL, en la nube) | Varios teléfonos ven el mismo inventario de transacciones en tiempo real, sin recargar. |

##  Descripción

La app funciona como un registro de finanzas personales conectado a la nube:

- **Pantalla principal:** lista todas las transacciones en un `RecyclerView` con
  tarjetas (`CardView`) con sombra, esquinas redondeadas e íconos, y muestra el
  **saldo total** (ingresos menos gastos). Incluye un botón flotante
  (`FloatingActionButton`) para agregar una nueva transacción. La lista está
  conectada a Firestore mediante un `SnapshotListener`, por lo que se actualiza
  sola en tiempo real ante cualquier cambio (propio o de otro dispositivo).
- **Pantalla de creación/edición:** formulario con `TextInputLayout` de Material
  Design para el concepto y el monto, y un `RadioGroup` para elegir entre
  **Ingreso** o **Gasto**. Incluye validaciones de negocio en tiempo real
  (`TextWatcher`) y deshabilita el botón "Guardar" mientras se escribe en
  Firestore, para evitar el doble envío.
- **Diseño de cada ítem:** tarjeta con ícono circular (flecha arriba/abajo según
  el tipo), concepto, tipo y monto, con indicador visual por color (verde `+`
  para ingresos, rojo `-` para gastos).

### Acciones

| Acción | Cómo se hace |
|--------|--------------|
| Crear  | Botón flotante **+** |
| Editar | Toca una transacción de la lista |
| Eliminar | Mantén presionada una transacción, o el ícono de basura (pide confirmación) |

##  Requerimientos técnicos cumplidos (Segundo Seguimiento)

1. **Sincronización en tiempo real:** el `RecyclerView` ya no lee de SQLite; está
   conectado a la colección `transacciones` de Firestore mediante un
   `SnapshotListener` (`FirestoreRepository.escucharCambios`).
2. **CRUD completo en NoSQL:**
   - *Create:* el formulario agrega documentos nuevos con `coleccion.add(t)`.
   - *Read:* la lista mapea automáticamente cada documento a la clase POJO
     `Transaccion` (`DocumentSnapshot.toObject`).
   - *Update:* la edición sobrescribe el documento apuntando a su `Document ID`
     (`coleccion.document(id).set(t)`).
   - *Delete:* el borrado apunta también al `Document ID`
     (`coleccion.document(id).delete()`).
3. **Diseño de interfaz (UI/UX):** listado con `CardView` (sombra, bordes
   redondeados, íconos vectoriales) y formularios con `TextInputLayout` de
   Material Design.
4. **Validaciones y rendimiento:**
   - Ningún campo crítico (concepto, monto) se envía vacío a Firestore.
   - Validaciones de negocio en tiempo real con `TextWatcher` (avisa si el
     concepto es muy corto o si el monto es ≤ $0 mientras el usuario escribe).
   - El botón "Guardar" se deshabilita mientras se guarda, previniendo el doble
     envío.
   - El `ListenerRegistration` de Firestore se libera con `.remove()` en el
     `onDestroy()` de `MainActivity` para no dejar listeners activos en memoria.

##  Configuración de Firebase

1. Proyecto de Firebase: `finanzas-bb4f4`, con la app Android registrada bajo el
   `applicationId` **`com.example.finanzasluna`**.
2. El archivo `app/google-services.json` conecta la app con ese proyecto (no se
   versiona con credenciales reales de producción compartidas públicamente).
3. Base de datos: **Firestore Database**, colección `transacciones`, con reglas
   abiertas de solo lectura/escritura para efectos de desarrollo y evaluación
   académica (`allow read, write: if true;`).

##  Estructura del código (separación de responsabilidades)

```
app/src/main/java/com/example/finanzasluna/
├── Transaccion.java          # Modelo de datos (POJO mapeado por Firestore)
├── FirestoreRepository.java  # Acceso a Firestore: CRUD + SnapshotListener
├── TransaccionAdapter.java   # Adapter + ViewHolder del RecyclerView (CardView)
├── MainActivity.java         # Pantalla principal (lista en tiempo real + saldo + FAB)
└── FormActivity.java         # Formulario de crear / editar (TextInputLayout + validaciones)

app/src/main/res/layout/
├── activity_main.xml         # Pantalla principal
├── activity_form.xml         # Formulario (TextInputLayout)
└── item_transaccion.xml      # Diseño de cada fila (CardView + ícono)

app/src/main/res/drawable/
├── ic_arrow_upward.xml       # Ícono de ingreso
├── ic_arrow_downward.xml     # Ícono de gasto
├── ic_delete.xml             # Ícono de eliminar
└── ic_add.xml                # Ícono del FAB
```

