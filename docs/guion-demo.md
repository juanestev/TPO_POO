# Guion de la demo (tarjeta 6.8)

Duracion sugerida: **8 a 10 minutos**. Conviene ensayarlo una vez completo antes de presentar.

## Antes de empezar

1. JDK 21 o superior (en esta PC: el JDK 25). Verificar con `java -version`.
2. Borrar `voluntariado.db` si queda de una prueba anterior, para arrancar con los datos de prueba limpios.
3. Abrir la app (`./mvnw exec:java` o `java -jar target/voluntariado.jar`).
4. Tener una carpeta lista donde guardar una imagen QR.

## Orden de la presentacion

| Min | Que mostrar | Que decir |
|---|---|---|
| 0:00 | Slide / voz: el problema | Visita del Papa: hay que organizar voluntarios en puntos de control, avisarles y saber en tiempo real si cada punto tiene gente suficiente. |
| 1:00 | Arquitectura (ver `docs/arquitectura-y-patrones.md`) | Capas con MVC; la vista nunca toca la base. Patrones: Strategy, Observer, Adapter, DAO. |
| 2:00 | **Login como `admin`** | Cada rol ve solo lo suyo: el admin ve Voluntarios, Puntos, Cobertura y Usuarios. (Cerrar sesion.) |
| 2:30 | **Login como `coordinador`** → pestaña **Cobertura** | Todos los puntos estan en rojo (deficit): nadie asignado ni presente. |
| 3:30 | Pestaña **Asignacion** → punto P2, habilidad *Control accesos*, turno *Sabado 08:00 a 14:00* → *Sugerir* → *Confirmar* | **Strategy**: el motor filtra por habilidad y disponibilidad sin repetir voluntarios. El coordinador confirma. Se guardan en SQLite y se arman los mails con QR. |
| 5:00 | Pestaña **Check-in** → elegir una fila → *Guardar QR...* → *Entrada con imagen QR...* con ese archivo | **Adapter** + ZXing: el QR del mail se lee y se registra la entrada. Mostrar que la fila pasa a "Presente". |
| 6:00 | Registrar la entrada de los demas con el codigo (manual) → volver a **Cobertura** | **Observer**: el punto P2 pasa de rojo a verde **sin tocar nada** en el panel. Registrar una salida: vuelve a rojo. |
| 7:30 | **Emergencia**: dejar a un punto cercano con sobrante y otro en deficit → *Sugerir reasignacion (emergencia)* → *Confirmar* | Reutiliza el motor de asignacion: mueve a un voluntario presente de una zona cercana, le da un QR nuevo y le avisa del cambio. |
| 8:30 | Cerrar y volver a abrir la app | **DAO**: los datos siguen ahi. |
| 9:00 | Mostrar los tests (`./mvnw test`) | Pasan todos, incluidas las pruebas integrales de los dos flujos y las de permisos por rol. |
| 9:30 | Limites y cierre | Ver "Que decir si preguntan por lo que falta". |

## Como preparar la emergencia (hacerlo antes de la demo, o ensayarlo)

La emergencia necesita un punto con **sobrante** y otro cercano con **deficit**. Con los datos de prueba, P1 (Plaza de Mayo, dotacion 6) y P2 (Catedral, dotacion 4) son vecinos. Receta:

1. Como `admin`: en *Puntos de control* subir la dotacion de **P2 a 5** y guardar.
2. Como `coordinador`: en *Asignacion*, punto P2, habilidad *Control accesos*, turno *Sabado 08:00 a 14:00*, *Sugerir* y *Confirmar* (son 5).
3. En *Check-in*, registrar la entrada de los 5 (elegir cada fila y *Registrar entrada*).
4. Como `admin`: volver a poner la dotacion de **P2 en 4**. Ahora P2 tiene **sobrante** y P1 sigue en **deficit**.
5. Como `coordinador`: *Asignacion*, punto **P1**, misma habilidad y turno, *Sugerir reasignacion (emergencia)*: propone mover a 1 voluntario de P2. *Confirmar*.
6. En *Cobertura*: P2 vuelve a verde. El voluntario movido recibio un aviso de cambio con un QR nuevo.

Esta misma secuencia esta automatizada en `FlujoIntegralTest.flujoB_emergenciaMueveGenteDeUnaZonaCercanaConSobrante`.

## Si algo falla en vivo

- **Tener a mano la demo de consola** (`DemoFlujo`): recorre todo el flujo sin interfaz y sin tocar la base.
- Si la ventana no abre: revisar que `JAVA_HOME` apunte a un JDK 21 o superior.
- Si no hay sugerencias: probar otro punto o habilidad; los datos de prueba tienen mas gente de *Control accesos* los sabados de 08:00 a 14:00.

## Que decir si preguntan por lo que falta

- El **envio por SMTP real** esta implementado pero no se probo contra un servidor; en la demo los mails se simulan.
- El **QR se lee desde una imagen** o con el codigo escrito a mano; no hay lectura por webcam.
- La base **no esta cifrada** y no hay registro de auditoria (detalle en `docs/seguridad.md`).

## Preguntas probables

| Pregunta | Respuesta corta |
|---|---|
| ¿Por que un HashMap? | Para buscar un voluntario por DNI sin recorrer toda la lista. |
| ¿Por que Strategy? | Para cambiar el criterio de asignacion sin tocar el motor; la reasignacion reutiliza el mismo. |
| ¿Por que Adapter en el mail? | Para no depender del proveedor y probar sin red con el notificador simulado. |
| ¿Por que el modelo no imprime nada? | Para que la logica sea independiente de la interfaz y se pueda testear. |
| ¿Como se protegen las claves? | Hash PBKDF2 con sal por usuario; nunca se guardan en texto plano. |
| ¿Que pasa si falla la base al guardar? | Los controladores deshacen el cambio en memoria para que memoria y base no queden distintas. |
| ¿Que pasa si no hay internet al avisar? | El aviso queda en una cola guardada en la base y se reintenta. |
