package voluntariado.dao;

import java.util.List;
import voluntariado.servicios.MensajeMail;

// Patron DAO: guarda los avisos que no se pudieron enviar para no perderlos si se cierra la app.
public interface ColaMailsDAO {

    // En el mismo orden en que quedaron en la cola.
    List<MensajeMail> listar();

    // Deja guardada exactamente esta lista (borra lo anterior). Todo o nada.
    void reemplazarTodos(List<MensajeMail> mensajes);
}
