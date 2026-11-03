# Mis Finanzas — Gestor de Gastos Personales

Aplicación nativa de Android (Java) para registrar **ingresos y gastos** diarios,
adjuntando **evidencia fotográfica** y la **ubicación** donde ocurrió cada
transacción. Los datos viven en la nube (**Firebase Firestore** + **Firebase
Storage**) y se sincronizan en tiempo real entre dispositivos. El acceso está
protegido con **FirebaseAuth** y control de roles (RBAC): un **Administrador**
puede eliminar cualquier transacción, mientras que un **Operario** solo puede
crear y consultar.

## Estudiante

**Luna Gutierrez Diaz**

## Evolución del proyecto

| Entrega | Persistencia | Alcance |
|---------|--------------|---------|
| Momento 1 | SQLite local | Los datos vivían solo en el teléfono donde se instalaba la app. |
| Momento 2 | Firebase Firestore (NoSQL, en la nube) | Varios teléfonos ven el mismo inventario de transacciones en tiempo real. |
| Momento 3 — **actual** | Firestore + Storage + Auth + Maps + Notificaciones | Producto completo: login con roles, fotos de evidencia, geolocalización con mapa interactivo y notificaciones. |

## Credenciales de prueba

Cuentas ya creadas en el proyecto de Firebase (`finanzas-bb4f4`), listas para usar
en la sustentación:

| Rol | Correo | Contraseña |
|-----|--------|------------|
| Administrador | `admin@finanzasluna.com` | `Admin123!` |
| Operario | `operario@finanzasluna.com` | `Operario123!` |

Al registrar cada cuenta, elige el rol correspondiente en el `RadioGroup` de la
pantalla de Registro.

## Los 5 pilares del Momento 3

### A. Seguridad y control de acceso (Auth + RBAC + Sesión)
- `LoginActivity` / `RegisterActivity`: registro e inicio de sesión con
  `FirebaseAuth` (correo/contraseña), validado en vivo con `TextInputLayout.setError`.
- `SessionManager` persiste la sesión en `SharedPreferences` → **Auto-Login**: si
  ya hay sesión guardada y FirebaseAuth la reconoce, `LoginActivity` salta directo
  a `MainActivity`.
- **Cerrar sesión** (menú superior de `MainActivity`): `FirebaseAuth.signOut()` +
  `SessionManager.cerrarSesion()` (limpia la caché local) + regreso a `LoginActivity`.
- **RBAC**: el rol (`admin`/`operario`) se guarda en `usuarios/{uid}` y se elige al
  registrarse. Solo `admin` ve y puede usar el botón de eliminar en cada tarjeta
  (`TransaccionAdapter`); `operario` puede crear y ver todo, pero no borrar nada.

### B. Datos y multimedia (Firestore + Storage + Glide)
- CRUD en tiempo real sobre `transacciones` (ya existente desde Momento 2, con
  `addSnapshotListener`).
- `FormActivity` permite adjuntar una foto por **Cámara**
  (`ActivityResultContracts.TakePicturePreview`, con permiso `CAMERA` en runtime) o
  **Galería** (`ActivityResultContracts.PickVisualMedia`, el selector moderno de
  fotos que no necesita permisos de almacenamiento).
- `StorageRepository` sube el JPEG a `evidencias/{uid}/{uuid}.jpg` en Firebase
  Storage, reporta el progreso (`ProgressBar`) y obtiene el `Download URL`.
- `TransaccionAdapter` pinta la miniatura con **Glide**
  (`placeholder`/`error`/`centerCrop`), lo que evita el bug de reciclaje del
  `RecyclerView` (una fila nunca muestra momentáneamente la foto de otra).

### C. Geolocalización y mapas (GPS + Google Maps SDK)
- `LocationHelper` usa `FusedLocationProviderClient.getCurrentLocation(...)` (con
  permiso `ACCESS_FINE_LOCATION`/`ACCESS_COARSE_LOCATION` pedido en runtime) y
  convierte las coordenadas a una dirección legible con `Geocoder`
  (geocodificación inversa, corrida en un hilo de fondo).
- `MapaActivity` muestra un `SupportMapFragment` con un marcador por cada
  transacción que tenga ubicación (verde = ingreso, rojo = gasto), `InfoWindow` al
  tocar el pin (concepto, monto y dirección), zoom habilitado y un menú para
  alternar entre capa **Normal** y **Satélite**.
- La Maps API Key vive en `AndroidManifest.xml`
  (`com.google.android.geo.API_KEY`), ligada al proyecto GCP `finanzas-bb4f4`.
  Si necesitas regenerarla: Google Cloud Console → selecciona el proyecto → *APIs
  & Services → Library* → habilita **Maps SDK for Android** → *Credentials →
  Create API Key*.

### D. Notificaciones (locales y push)
- `NotificationHelper.crearCanal()` registra el `NotificationChannel` requerido
  desde Android 8.0, y pide el permiso `POST_NOTIFICATIONS` en Android 13+
  (`MainActivity`, vía `ActivityResultLauncher`).
- Cada vez que `FormActivity` guarda una transacción con éxito, se emite una
  notificación local con `PendingIntent` que reabre la app en `MainActivity`.
- `MyFirebaseMessagingService` deja la infraestructura de **FCM** lista para
  recibir mensajes push enviados desde la consola de Firebase (*Cloud Messaging →
  Send your first message*), guardando el token de cada dispositivo en
  `usuarios/{uid}.fcmToken`.

### E. Diseño, responsividad y buenas prácticas
- Todas las pantallas con formularios (`Login`, `Registro`, `Form`) usan
  `ScrollView` + `fillViewport`, por lo que nada se corta ni se superpone al rotar
  a horizontal.
- Validaciones en tiempo real con `TextInputLayout.setError()` en todos los
  formularios (concepto, monto, correo, contraseñas, nombre).

## Evidencia visual (Momento 2)

Grabaciones/capturas de la entrega anterior (CRUD en tiempo real sobre Firestore):

- [Diseño de cada ítem de la lista (tarjeta con ícono, color y monto)](https://drive.google.com/file/d/1Nt4brXhPBzt0C-2GOD9ZHlRiShcoaETZ/view?usp=drivesdk)
- [Validaciones en tiempo real y sincronización vía SnapshotListener](https://drive.google.com/file/d/17QP-v_YQU2Se4uk8AYuzbvM9a-RH-vf7/view?usp=drivesdk)
- [Reglas de Firestore en la consola de Firebase](https://drive.google.com/file/d/1iWZJ9VMi08USw9S_3mP969LoCFssqaGX/view?usp=drivesdk)

## Reglas de seguridad

Las reglas reales ya están publicadas en el proyecto `finanzas-bb4f4` (verificado:
una lectura sin autenticar devuelve `403 PERMISSION_DENIED` tanto en Firestore
como en Storage, y un `operario` no puede escribir el perfil de otro usuario).
El contenido exacto que quedó publicado vive en `firestore.rules` y
`storage.rules` en la raíz de este repo (admin-only delete en `transacciones`,
cada usuario solo escribe su propio perfil/sus propias fotos en `evidencias/{uid}/...`).
Si alguna vez se necesita republicar, se pega ese mismo contenido en:
- Firebase Console → Firestore Database → Reglas → Publicar.
- Firebase Console → Storage → Reglas → Publicar.

## Configuración de Firebase

1. Proyecto de Firebase: `finanzas-bb4f4`, `applicationId`
   **`com.example.finanzasluna`**.
2. `app/google-services.json` conecta la app con ese proyecto.
3. Colecciones de Firestore: `transacciones` (ingresos/gastos, con foto y
   ubicación opcionales) y `usuarios` (perfil + rol de cada cuenta).
4. Firebase Storage: carpeta `evidencias/{uid}/...` para las fotos.

## Estructura del código

```
app/src/main/java/com/example/finanzasluna/
├── Transaccion.java              # Modelo (POJO) de cada transaccion
├── Usuario.java                  # Modelo (POJO) del perfil + rol
├── FirestoreRepository.java      # CRUD + SnapshotListener de "transacciones"
├── AuthRepository.java           # Registro / login / logout (FirebaseAuth + Firestore)
├── StorageRepository.java        # Subida de fotos a Firebase Storage
├── LocationHelper.java           # FusedLocationProviderClient + Geocoder
├── NotificationHelper.java       # NotificationChannel + notificaciones locales
├── MyFirebaseMessagingService.java # Infraestructura FCM (push)
├── SessionManager.java           # Sesion persistida en SharedPreferences
├── TransaccionAdapter.java       # Adapter del RecyclerView (Glide + RBAC)
├── LoginActivity.java            # Launcher: login + Auto-Login
├── RegisterActivity.java         # Registro (incluye seleccion de rol)
├── MainActivity.java             # Lista en tiempo real + saldo + menu (mapa/logout)
├── FormActivity.java             # Crear/editar: foto + ubicacion + notificacion
└── MapaActivity.java             # Mapa con pines por transaccion

app/src/main/res/layout/
├── activity_login.xml / activity_register.xml
├── activity_main.xml / activity_form.xml / activity_mapa.xml
└── item_transaccion.xml

firestore.rules / storage.rules   # Reglas de seguridad de referencia (ver arriba)
```

## Acciones

| Acción | Cómo se hace |
|--------|--------------|
| Registrarse / iniciar sesión | `LoginActivity` → `RegisterActivity` |
| Crear transacción (+foto +ubicación) | Botón flotante **+** dentro de `FormActivity` |
| Editar | Toca una transacción de la lista |
| Eliminar (solo Administrador) | Ícono de basura en la tarjeta (pide confirmación) |
| Ver mapa de transacciones | Menú superior → "Ver mapa" |
| Cerrar sesión | Menú superior → "Cerrar sesion" |
