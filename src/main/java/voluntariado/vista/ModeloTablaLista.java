package voluntariado.vista;

import java.util.List;
import java.util.function.BiFunction;
import javax.swing.table.AbstractTableModel;

// Modelo de tabla reutilizable (como el de UVA 6, pero para cualquier tipo de fila):
// recibe los titulos de las columnas y una funcion que dice que mostrar en cada celda.
public class ModeloTablaLista<T> extends AbstractTableModel {

    private final String[] columnas;
    private final BiFunction<T, Integer, Object> valorDeCelda;
    private List<T> filas = List.of();

    public ModeloTablaLista(String[] columnas, BiFunction<T, Integer, Object> valorDeCelda) {
        this.columnas = columnas.clone();
        this.valorDeCelda = valorDeCelda;
    }

    public void setFilas(List<T> filas) {
        this.filas = List.copyOf(filas);
        fireTableDataChanged();
    }

    public T getFila(int indice) {
        return filas.get(indice);
    }

    public int indiceDe(T fila) {
        return filas.indexOf(fila);
    }

    @Override
    public int getRowCount() {
        return filas.size();
    }

    @Override
    public int getColumnCount() {
        return columnas.length;
    }

    @Override
    public String getColumnName(int columna) {
        return columnas[columna];
    }

    @Override
    public Object getValueAt(int fila, int columna) {
        return valorDeCelda.apply(filas.get(fila), columna);
    }
}
