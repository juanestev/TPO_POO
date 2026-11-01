# Revision de seguridad y datos personales (tarjeta 6.3)

Revision hecha sobre el codigo entregado. Para cada punto se indica **donde esta** y **como se prueba**,
y al final se listan, con sinceridad, los limites que quedan.

## Lo que se cumple

| Tema | Que se hizo | Donde | Prueba |
|---|---|---|---|
| Contrasenas | Nunca se guardan: solo hash PBKDF2-SHA256 (120.000 vueltas) con sal aleatoria por usuario. Comparacion en tiempo constante. | `seguridad/ServicioClaves` | `ServicioAutenticacionTest` |
| Login | Mismo mensaje ante usuario inexistente, clave mala o usuario inactivo (no revela que usuarios existen). Se calcula un hash igual si el usuario no existe. | `seguridad/ServicioAutenticacion` | `claveMalaUsuarioInexistenteOInactivoDanElMismoError` |
| Fuerza bruta | Tras 5 intentos fallidos la pantalla se bloquea 30 segundos. Las claves nuevas piden 8+ caracteres con letras y numeros. | `vista/VentanaLogin`, `ServicioClaves` | `rechazaClavesDebilesYNombresRepetidos` |
| Permisos por rol | La matriz de la Actividad 02 esta en codigo (`Permisos`). Se aplica en **dos lugares**: la ventana solo muestra las pestanas permitidas y los **controladores** vuelven a exigir el permiso. | `seguridad/Permisos`, `seguridad/Sesion`, `controlador/*` | `PermisosControladoresTest`, `PermisosTest` |
| Administracion de usuarios | No se puede borrar ni desactivar el propio usuario, ni dejar al sistema sin administrador activo. | `controlador/ControladorUsuarios` | `ControladorUsuariosTest` |
| Inyeccion SQL | Todas las consultas usan `PreparedStatement` con parametros; las unicas concatenaciones son de texto fijo. | `dao/*` | revision del codigo |
| Datos sensibles | DNI, telefono, mail y disponibilidad son atributos privados y solo cambian por setters que validan. | `modelo/Persona`, `modelo/Voluntario` | `PersonaTest`, `VoluntarioTest` |
| Registros (logs) | No se imprimen datos personales, codigos QR ni claves. El notificador simulado solo escribe "Mail simulado registrado". | `servicios/NotificadorMailConsola` | revision del codigo |
| Credenciales de correo | El SMTP real se configura por variables de entorno; no hay claves escritas en el codigo ni en el repositorio. | `Main.crearNotificador` | revision del codigo |
| Base de datos | `voluntariado.db` esta en `.gitignore`: los datos reales no se suben al repositorio. Las claves foraneas estan activadas. | `.gitignore`, `dao/ConexionBD` | `AsignacionDAOTest` |
| Datos de prueba | Todos ficticios; los mails usan el dominio `ejemplo.com` para no escribirle a nadie real. | `dao/DatosDePrueba` | — |

## Ley 25.326 (proteccion de datos personales)

| Principio | Estado |
|---|---|
| Finalidad y calidad de los datos | Solo se guardan los datos necesarios para organizar el voluntariado (identificacion, contacto, habilidades y disponibilidad). Se validan al ingresar. |
| Seguridad de los datos | Acceso por usuario y rol, claves con hash, datos privados en el modelo, base fuera del repositorio. |
| Derecho de rectificacion y supresion | Un administrador u operador puede **modificar** y **dar de baja** voluntarios desde la pantalla Voluntarios. Un voluntario con asignacion confirmada no se puede dar de baja hasta liberarlo (se conserva el historial). |
| Confidencialidad | Cada rol ve solo lo que su permiso habilita. |

## Limites conocidos (no resueltos)

Son decisiones de alcance de un trabajo universitario; conviene decirlas si se pregunta:

1. **La base SQLite no esta cifrada.** Quien tenga acceso al archivo `voluntariado.db` puede leer los datos de los voluntarios (las claves de usuario si estan protegidas por el hash).
2. **No hay pantalla de consentimiento** ni registro de quien acepto el tratamiento de sus datos.
3. **No hay registro de auditoria** (quien modifico que y cuando).
4. **El bloqueo por intentos fallidos esta solo en la pantalla**, no en el servicio de autenticacion ni persistido.
5. **Las claves de prueba** (`admin`, `coordinador`, `operador`) estan documentadas en el README y deben cambiarse en un uso real.
6. **El envio por SMTP real no fue probado contra un servidor** (la integracion esta implementada y se activa por variables de entorno; el resto del sistema se probo con el notificador simulado).
7. **La baja de un voluntario es fisica** (se borra de la base), no un borrado logico.
