package dacite.intellij.visualisation;

import javax.swing.table.AbstractTableModel;
import java.util.*;

public class DefUseTableModel extends AbstractTableModel {
    private List<String> columnNames = Arrays.asList("Variable", "Def Location", "Use Location", "Highlight");
    private List<RowData> data = new ArrayList<>();

    public int getColumnCount() {
        return columnNames.size();
    }

    public int getRowCount() {
        return data.size();
    }

    public String getColumnName(int col) {
        return columnNames.get(col);
    }

    public Object getValueAt(int row, int col) {
        return data.get(row).getValueForCol(col);
    }

    public void addColumn(String name) {
        columnNames.add(name);
        fireTableStructureChanged();
    }

    public void addRow(String var, String def, String use) {
        data.add(new RowData(var, def, use));
        fireTableRowsInserted(data.size(), data.size());
    }

    public void removeRow(int index){
        data.remove(index);
    }

    public RowData getRow(int row) {
        return data.get(row);
    }

    @Override
    public Class getColumnClass(int column) {
        switch (column) {
            case 0: case 1: case 2:
                return String.class;
            default:
                return Boolean.class;
        }
    }

    /*public Class getColumnClass(int c) {
        return getValueAt(0, c).getClass();
    }*/

    /*
     * Don't need to implement this method unless your table's
     * editable.
     */
    public boolean isCellEditable(int row, int col) {
            return true;
    }

    /*
     * Don't need to implement this method unless your table's
     * data can change.
     */
    public void setValueAt(Object value, int row, int col) {
        data.get(row).setValueForCol(value, col);
        fireTableCellUpdated(row, col);
    }

    class RowData{

        private Map<Integer, Object> values = new HashMap<>();

        public RowData(String var, String defPath, String usePath){
            values.put(0, var);
            values.put(1, defPath);
            values.put(2,usePath);
            values.put(3, false);
        }

        public Object getValueForCol(int columnIndex) {
            if(values.containsKey(columnIndex)){
                return values.get(columnIndex);
            }
            return "";
        }

        public void setValueForCol(Object aValue, int columnIndex) {
            values.put(columnIndex, aValue);
        }

    }
}