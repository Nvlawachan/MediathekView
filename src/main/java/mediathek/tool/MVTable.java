/*    
 *    MediathekView
 *    Copyright (C) 2008   W. Xaver
 *    W.Xaver[at]googlemail.com
 *    http://zdfmediathk.sourceforge.net/
 *    
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package mediathek.tool;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DragSource;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;

import javax.activation.DataHandler;
import javax.swing.DropMode;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.RowSorter.SortKey;
import javax.swing.SortOrder;
import javax.swing.TransferHandler;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import de.mediathekview.mlib.tool.Listener;
import de.mediathekview.mlib.tool.Log;
import mediathek.config.Daten;
import mediathek.config.MVConfig;
import mediathek.daten.*;

import static mediathek.daten.FilmColumns.*;

@SuppressWarnings("serial")
public final class MVTable extends JTable {
    private Collection<Column> columns;
    private List<Column> invisibleColumns;

    public enum TableType {
        STANDARD, FILME, DOWNLOADS, ABOS, PSET, PROG, MEDIA_DB
    }
    //public static final String TABELLEN = "Tabellen";
    public static final String FELDTRENNER = "|";
    public static final String SORT_ASCENDING = "ASCENDING";
    public static final String SORT_DESCENDING = "DESCENDING";
    public boolean iconAnzeigen = false;
    public boolean iconKlein = false;
    private final Map<Column,Integer> breiten;
    private final int[] reihe;
    private MVConfig.Configs nrDatenSystem = null;
    private TableType tabelle;
    private int maxSpalten;
    private List<? extends RowSorter.SortKey> listeSortKeys = null;
    private int indexSpalte = 0;
    private int[] selRows;
    private int[] selIndexes = null;
    private int selRow = -1;
    private MVConfig.Configs iconAnzeigenStr = null;
    private MVConfig.Configs iconKleinStr = null;
    public boolean lineBreak = true;

    private final Daten daten;


    /**
     * Return the type of this MVTable.
     *
     * @return
     */
    public TableType getTableType() {
        return tabelle;
    }

    public MVTable(TableType tabelle) {
        this.tabelle = tabelle;
        daten = Daten.getInstance();
        setAutoCreateRowSorter(true);
        setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        invisibleColumns = new ArrayList<>();
        breiten = new HashMap<>();

        String[] spaltenTitel;
        switch (tabelle) {
            case FILME:
                //spaltenTitel = DatenFilm.COLUMN_NAMES;
                maxSpalten = FilmColumns.values().length;
                invisibleColumns = ColumnManagerFactory.getInstance().getInvisibleFilmColumns();
                columns = ColumnManagerFactory.getInstance().getFilmColumns();
                indexSpalte = FilmColumns.NR.getId();
                nrDatenSystem = MVConfig.Configs.SYSTEM_EIGENSCHAFTEN_TABELLE_FILME;
                iconAnzeigenStr = MVConfig.Configs.SYSTEM_TAB_FILME_ICON_ANZEIGEN;
                iconKleinStr = MVConfig.Configs.SYSTEM_TAB_FILME_ICON_KLEIN;
                //this.setModel(new TModelFilm(new Object[][]{}, spaltenTitel));
                this.setModel(new TModelFilm(new Object[][]{}, ColumnManagerFactory.getInstance().getFilmColumnAllNames()));
                this.getTableHeader().addMouseListener(new WidthAdjuster(this));
                break;
            case DOWNLOADS:
                //spaltenTitel = .COLUMN_NAMES;
                maxSpalten = DownloadColumns.values().length;
                invisibleColumns = ColumnManagerFactory.getInstance().getInvisibleDownloadColumns();
                columns = ColumnManagerFactory.getInstance().getDownloadColumns();
                indexSpalte = DownloadColumns.NR.getId();
                nrDatenSystem = MVConfig.Configs.SYSTEM_EIGENSCHAFTEN_TABELLE_DOWNLOADS;
                iconAnzeigenStr = MVConfig.Configs.SYSTEM_TAB_DOWNLOAD_ICON_ANZEIGEN;
                iconKleinStr = MVConfig.Configs.SYSTEM_TAB_DOWNLOAD_ICON_KLEIN;
                setDragEnabled(true);
                setDropMode(DropMode.INSERT_ROWS);
                setTransferHandler(new TableRowTransferHandlerDownload(this));
                setModel(new TModelDownload(new Object[][]{}, ColumnManagerFactory.getInstance().getDownloadColumnAllNames()));
                this.getTableHeader().addMouseListener(new WidthAdjuster(this));
                break;
            case ABOS:
                //spaltenTitel = DatenAbo.COLUMN_NAMES;
                maxSpalten = AboColumns.values().length;
                columns = ColumnManagerFactory.getInstance().getAboColumns();
                invisibleColumns = ColumnManagerFactory.getInstance().getInvisibleAboColumns();
                indexSpalte = AboColumns.NR.getId();
                nrDatenSystem = MVConfig.Configs.SYSTEM_EIGENSCHAFTEN_TABELLE_ABOS;
                iconAnzeigenStr = MVConfig.Configs.SYSTEM_TAB_ABO_ICON_ANZEIGEN;
                iconKleinStr = MVConfig.Configs.SYSTEM_TAB_ABO_ICON_KLEIN;
                this.setModel(new TModelAbo(new Object[][]{}, ColumnManagerFactory.getInstance().getAboColumnAllNames()));
                this.getTableHeader().addMouseListener(new WidthAdjuster(this));
                break;
            case PSET:
                spaltenTitel = DatenPset.COLUMN_NAMES;
                maxSpalten = DatenPset.MAX_ELEM;
                columns = ColumnManagerFactory.getInstance().getPSetColumns();
                invisibleColumns = ColumnManagerFactory.getInstance().getInvisiblePsetColumns();
                indexSpalte = 0;
                nrDatenSystem = null;
                this.setModel(new TModel(new Object[][]{}, spaltenTitel));
                this.setRowSorter(null);
                this.setAutoCreateRowSorter(false); // Reihenfolge ist die Anzeige der Button!
                this.getTableHeader().addMouseListener(new WidthAdjuster(this));
                break;
            case PROG:
                spaltenTitel = DatenProg.COLUMN_NAMES;
                maxSpalten = DatenProg.MAX_ELEM;
                columns = ColumnManagerFactory.getInstance().getProgColumns();
                invisibleColumns = ColumnManagerFactory.getInstance().getInvisibleProgColumns();
                indexSpalte = 0;
                nrDatenSystem = null;
                this.setModel(new TModel(new Object[][]{}, spaltenTitel));
                this.getTableHeader().addMouseListener(new WidthAdjuster(this));
                break;
            case MEDIA_DB:
                spaltenTitel = DatenMediaDB.COLUMN_NAMES;
                maxSpalten = DatenMediaDB.MAX_ELEM;
                columns = ColumnManagerFactory.getInstance().getMediaDbColumns();
                invisibleColumns = ColumnManagerFactory.getInstance().getInvisibleMediaDbColumns();
                indexSpalte = 0;
                nrDatenSystem = MVConfig.Configs.SYSTEM_EIGENSCHAFTEN_TABELLE_MEDIA_DB;
                this.setModel(new TModel(new Object[][]{}, spaltenTitel));
                this.getTableHeader().addMouseListener(new WidthAdjuster(this));
                break;
        }
        reihe = getArray(maxSpalten);
        if (iconAnzeigenStr != null) {
            iconAnzeigen = Boolean.parseBoolean(MVConfig.get(iconAnzeigenStr));
        }
        if (iconKleinStr != null) {
            iconKlein = Boolean.parseBoolean(MVConfig.get(iconKleinStr));
        }
        setHeight();
        Listener.addListener(new Listener(Listener.EREIGNIS_FONT, MVTable.class.getSimpleName()) {
            @Override
            public void ping() {
                setHeight();
            }
        });
    }

    public void setLineBreak(boolean lb) {
        lineBreak = lb;
    }

    public void invertSelection() {
        ListSelectionModel mdl = getSelectionModel();
        int[] selected = getSelectedRows();
        mdl.setValueIsAdjusting(true);
        mdl.setSelectionInterval(0, getRowCount() - 1);
        for (int i : selected) {
            mdl.removeSelectionInterval(i, i);
        }
        mdl.setValueIsAdjusting(false);
    }

    @SuppressWarnings("fallthrough")
    public void setHeight() {
        int sizeArea = 0;
        int size;
        switch (tabelle) {
            case FILME:
                if (!invisibleColumns.contains(FilmColumns.BESCHREIBUNG)) {
                    sizeArea = MVFont.fontSize * 5;
                } else if (lineBreak) {
                    sizeArea = MVFont.fontSize * 4;
                }
            case DOWNLOADS:
            case ABOS:
                if (lineBreak) {
                    sizeArea = MVFont.fontSize * 4;
                }
        }
        if (!iconAnzeigen) {
            if (MVFont.fontSize < 15) {
                size = 18;
            } else {
                size = MVFont.fontSize + MVFont.fontSize / 3;
            }
        } else if (iconKlein) {
            if (MVFont.fontSize < 18) {
                size = 20;
            } else {
                size = MVFont.fontSize + MVFont.fontSize / 3;
            }
        } else if (MVFont.fontSize < 30) {
            size = 36;
        } else {
            size = MVFont.fontSize + MVFont.fontSize / 3;
        }
        setRowHeight(size > sizeArea ? size : sizeArea);
    }

    public void reorder(int index, int[] rowFrom) {
        getSelected();
        TModel tModel = (TModelDownload) getModel();
        // listeDownloads neu nach der Reihenfolge in der Tabelle erstellen
        for (int i = 0; i < this.getRowCount(); ++i) {
            DatenDownload d = ((DatenDownload) tModel.getValueAt(this.convertRowIndexToModel(i), DatenDownload.DOWNLOAD_REF));
            if (d != null) {
                daten.getListeDownloads().remove(d);
                daten.getListeDownloads().add(d);
            }
        }
        // Downloads zum Verschieben suchen
        LinkedList<DatenDownload> liste = new LinkedList<>();
        for (int row : rowFrom) {
            if (index > row) {
                --index;
            }
            DatenDownload d = ((DatenDownload) tModel.getValueAt(this.convertRowIndexToModel(row), DatenDownload.DOWNLOAD_REF));
            liste.add(d);
            daten.getListeDownloads().remove(d);
        }
        // an der richtigen Stellei einfügen
        daten.getListeDownloads().addAll(index, liste);
        // die Tabellensortierung löschen, die wird jetzt mit der Liste wieder gefüllt
        this.getRowSorter().setSortKeys(null);
        this.setRowSorter(null);
        this.setAutoCreateRowSorter(true);
        setSelected();
        Listener.notify(Listener.EREIGNIS_REIHENFOLGE_DOWNLOAD, MVTable.class.getSimpleName());
    }

    public void initTabelle() {
        // Tabelle das erste Mal initialisieren,
        // mit den gespeicherten Daten oder
        // den Standardwerten
        // erst die Breite, dann die Reihenfolge
        try {
            if (nrDatenSystem == null) {
                // wird nur für eingerichtete Tabellen gemacht
                return;
            }
            String b = "", r = "", s = "", upDown = "";
            boolean ok = false;
            if (!MVConfig.get(nrDatenSystem).isEmpty()) {
                ok = true;
                int f1, f2, f3;
                //String d = Daten.system[nrDatenSystem];
                if ((f1 = MVConfig.get(nrDatenSystem).indexOf(FELDTRENNER)) != -1) {
                    b = MVConfig.get(nrDatenSystem).substring(0, f1);
                    if ((f2 = MVConfig.get(nrDatenSystem).indexOf(FELDTRENNER, f1 + 1)) != -1) {
                        r = MVConfig.get(nrDatenSystem).substring(f1 + 1, f2);
                    }
                    if ((f3 = MVConfig.get(nrDatenSystem).indexOf(FELDTRENNER, f2 + 1)) != -1) {
                        s = MVConfig.get(nrDatenSystem).substring(f2 + 1, f3);
                        upDown = MVConfig.get(nrDatenSystem).substring(f3 + 1);
                    }
                }
                if (!arrLesen(r, reihe)) {
                    ok = false;
                }
                SortKey sk = sortKeyLesen(s, upDown);
                if (sk != null) {
                    LinkedList<SortKey> listSortKeys_ = new LinkedList<>();
                    listSortKeys_.add(sk);
                    this.getRowSorter().setSortKeys(listSortKeys_);
                }
            }
            if (ok) {
                setSpalten();
                setHeight();
            } else {
                resetTabelle();
            }
        } catch (Exception ex) {
            //vorsichtshalber
        }
    }


    public void fireTableDataChanged(boolean setSpalten) {
        if (setSpalten) {
            getSelected();
        }
        ((TModel) this.getModel()).fireTableDataChanged();
        if (setSpalten) {
            setSelected();
        }
    }

    public void requestFocusSelelct(JScrollPane jScrollPane) {
        requestFocus();
        if (getRowCount() > 0) {
            // sonst ist schon eine Zeile markiert
            if (getSelectedRow() == -1) {
                setRowSelectionInterval(0, 0);
            }
            int firstSelectedRow = getSelectedRow();
            Rectangle cellLocation = getCellRect(firstSelectedRow, 0, false);
            jScrollPane.getVerticalScrollBar().setValue(cellLocation.y);
        }
    }

    public void requestFocusSelelct(JScrollPane jScrollPane, int zeile) {
        requestFocus();
        if (getRowCount() > 0) {
            // sonst ist schon eine Zeile markiert
            setRowSelectionInterval(zeile, zeile);
            int firstSelectedRow = getSelectedRow();
            Rectangle cellLocation = getCellRect(firstSelectedRow, 0, false);
            jScrollPane.getVerticalScrollBar().setValue(cellLocation.y);
        }
    }

    public void setSelRow(int i) {
        // Sel auf zeile i in der TABELLE! (nicht Modell) setzten oder erste-letzt Zeile
        if (getRowCount() > 0) {
            if (i < 0) {
                i = 0;
            }
            if (i >= getRowCount()) {
                i = getRowCount() - 1;
            }
            setRowSelectionInterval(i, i);
            scrollToSelection(i);
        }
    }

    public void scrollToSelection() {
        if (getRowCount() > 0) {
            int i = getSelectedRow();
            if (i < 0) {
                i = 0;
                setRowSelectionInterval(i, i);
            }
            if (i >= getRowCount()) {
                i = getRowCount() - 1;
            }
            scrollToSelection(i);
        }
    }

    private void scrollToSelection(int rowIndex) {
        if (!(getParent() instanceof JViewport)) {
            return;
        }
        JViewport viewport = (JViewport) getParent();
        Rectangle rect = getCellRect(rowIndex, 0, true);
        Rectangle viewRect = viewport.getViewRect();
        rect.setLocation(rect.x - viewRect.x, rect.y - viewRect.y);

        int centerX = (viewRect.width - rect.width) / 2;
        int centerY = (viewRect.height - rect.height) / 2;
        if (rect.x < centerX) {
            centerX = -centerX;
        }
        if (rect.y < centerY) {
            centerY = -centerY;
        }
        rect.translate(centerX, centerY);
        viewport.scrollRectToVisible(rect);
    }

    public void getSelected() {
        // Einstellungen der Tabelle merken
        selRow = this.getSelectedRow();
        selRows = this.getSelectedRows();
        switch (tabelle) {
            case DOWNLOADS:
            case FILME:
            case ABOS:
                int selIndex = -1;
                if (selRow >= 0) {
                    selIndex = (Integer) this.getModel().getValueAt(this.convertRowIndexToModel(selRow), indexSpalte);
                } else {
                    selIndex = -1;
                }
                if (selIndex >= 0) {
                    selIndexes = new int[selRows.length];
                    int k = 0;
                    for (int i : selRows) {
                        selIndexes[k++] = (Integer) this.getModel().getValueAt(this.convertRowIndexToModel(i), indexSpalte);
                    }
                } else {
                    selIndexes = null;
                }
                break;
            case MEDIA_DB:
                break;
            default:
                if (selRows != null) {
                    if (selRows.length > 0) {
                        String[] indexWertSelection = new String[selRows.length];
                        for (int i = 0; i < selRows.length; ++i) {
                            indexWertSelection[i] = this.getModel().getValueAt(this.convertRowIndexToModel(selRows[i]), indexSpalte).toString();
                        }
                    }
                }
                break;
        }
    }

    private void setSelected() {
        // gemerkte Einstellungen der Tabelle wieder setzten
        boolean found = false;
        switch (tabelle) {
            case DOWNLOADS:
            case FILME:
            case ABOS:
                if (selIndexes != null) {
                    int r;
                    this.selectionModel.setValueIsAdjusting(true);
                    TModel tModel = (TModel) this.getModel();
                    for (int i : selIndexes) {
                        r = tModel.getIdxRow(i);
                        if (r >= 0) {
                            // ansonsten gibts die Zeile nicht mehr
                            r = this.convertRowIndexToView(r);
                            this.addRowSelectionInterval(r, r);
                            found = true;
                        }
                    }
                    if (!found && selRow >= 0 && this.getRowCount() > selRow) {
                        // große Frage was da besser ist???
                        for (int i = selRow; i >= 0; --i) {
                            this.setRowSelectionInterval(i, i);
                            break;
                        }
                    } else if (!found && selRow >= 0 && this.getRowCount() > 0) {
                        this.setRowSelectionInterval(tModel.getRowCount() - 1, tModel.getRowCount() - 1);
                    }
                    this.selectionModel.setValueIsAdjusting(false);
                }
                selIndexes = null;
                break;
            case MEDIA_DB:
                break;
            default:
                if (selRows != null) {
                    if (selRows.length > 0) {
                        this.selectionModel.setValueIsAdjusting(true);
                        for (int selectedRow : selRows) {
                            if (selectedRow < this.getRowCount()) {
                                this.addRowSelectionInterval(selectedRow, selectedRow);
                            }
                        }
                        this.selectionModel.setValueIsAdjusting(false);
                    }
                }
                break;
        }
    }

    public void spaltenEinAus() {
        getSpalten(); // die aktuelle Breite holen
        setSpaltenBreiten();
        this.validate();
    }

    private void setSpaltenBreiten()
    {
        int i = 0;
        for (Column col:columns) {

            if (invisibleColumns.contains(col)) {
                // geänderte Ansicht der Spalten abfragen
                breiten.put(col,0);
            } else if (breiten.get(col) == 0) {
                breiten.put(col,100);
            }
            if (breiten.get(col) == 0) {
                this.getColumnModel().getColumn(this.convertColumnIndexToView(i)).setMinWidth(0);
                this.getColumnModel().getColumn(this.convertColumnIndexToView(i)).setPreferredWidth(0);
                this.getColumnModel().getColumn(this.convertColumnIndexToView(i)).setMaxWidth(0);
            } else {
                this.getColumnModel().getColumn(this.convertColumnIndexToView(i)).setMinWidth(10);
                this.getColumnModel().getColumn(this.convertColumnIndexToView(i)).setMaxWidth(3000);
                this.getColumnModel().getColumn(this.convertColumnIndexToView(i)).setPreferredWidth(breiten.get(col));
            }
            i++;
        }
    }

    public void getSpalten() {
        // Einstellungen der Tabelle merken
        getSelected();
        for (int i = 0; i < reihe.length && i < this.getModel().getColumnCount(); ++i) {
            reihe[i] = this.convertColumnIndexToModel(i);
        }

        int a = 0;
        for (Column col : columns) {
            breiten.put(col,getColumnModel().getColumn(
                    this.convertColumnIndexToView(a)).getWidth());
            a++;
        }

        if (this.getRowSorter() != null) {
            listeSortKeys = this.getRowSorter().getSortKeys();
        } else {
            listeSortKeys = null;
        }
    }

    public void setSpalten() {
        // gemerkte Einstellungen der Tabelle wieder setzten
//        setSelected();
        try {
            setSpaltenBreiten();
            for (int i = 0; i < reihe.length && i < this.getColumnCount(); ++i) {
                this.getColumnModel().moveColumn(this.convertColumnIndexToView(reihe[i]), i);
            }
            if (listeSortKeys != null && !listeSortKeys.isEmpty()) {
            	this.getRowSorter().setSortKeys(listeSortKeys);
            }
            setSelected();
            this.validate();
        } catch (Exception ex) {
            Log.errorLog(965001463, ex);
        }
    }

    private void resetFilmeTab(int i) {
        reihe[i] = i;
        FilmColumns col = ColumnManagerFactory.getInstance().getFilmColumnById(i);
        switch (col) {
            case NR:
                breiten.put(col,50);
                break;
            case TITEL:
                breiten.put(col,300);
                break;
            case BESCHREIBUNG:
                breiten.put(col,400);
                break;
            case DATUM:
            case ZEIT:
            case SENDER:
            case GROESSE:
            case DAUER:
            case GEO:
                breiten.put(col,100);
                break;
            case URL:
                breiten.put(col,500);
                break;
            case FILM_ABSPIELEN:
            case FILM_AUFZEICHNEN:
            case HD:
            case UT:
                breiten.put(col,50);
                break;
            default:
                breiten.put(col,200);
                break;
        }
    }

    private void resetDownloadsTab(int i) {
        reihe[i] = i;
        DownloadColumns col = ColumnManagerFactory.getInstance().getDownloadColumnById(i);
        switch (col) {
            case NR:
            case FILMNR:
                breiten.put(col,75);
                break;
            case BUTTON_START:
            case BUTTON_DEL:
            case RESTART:
            case REMOTE_DOWNLOAD:
            case PAUSE:
            case SPOTLIGHT:
            case UNTERTITEL:
            case INFODATEI:
            case HD:
            case UT:
                breiten.put(col,50);
                break;
            case TITEL:
                breiten.put(col,250);
                break;
            case ABO:
            case THEMA:
                breiten.put(col,150);
                break;
            case DATUM:
            case ZEIT:
            case GROESSE:
            case GESCHWINDIGKEIT:
            case SENDER:
            case FORTSCHRITT:
            case RESTZEIT:
            case DAUER:
            case GEO:
                breiten.put(col,100);
                break;
            default:
                breiten.put(col,200);
                break;
        }
    }

    private void resetAbosTab(int i) {
        reihe[i] = i;
        AboColumns col = ColumnManagerFactory.getInstance().getAboColumnById(i);
        switch (col)
        {
            case NR:
            case AKTIV:
            case MIN_MAX:
                breiten.put(col,75);
                break;
            case LETZTES_ABO:
            case SENDER:
                breiten.put(col,100);
                break;
                default:
                    breiten.put(col,200);
                    break;
        }
    }

    public void resetTabelle() {
        // Standardwerte wetzen
        for (int i = 0; i < maxSpalten; ++i) {
            switch (tabelle) {
                case FILME:
                    resetFilmeTab(i);
                    break;
                case DOWNLOADS:
                    resetDownloadsTab(i);
                    break;
                case ABOS:
                    resetAbosTab(i);
                    break;
                case MEDIA_DB:
                    reihe[i] = i;
                    MediaDbColumns col = ColumnManagerFactory.getInstance().getMediaDbColumnById(i);
                    breiten.put(col,200);
                    break;
            }
        }
        listeSortKeys = null;
        this.getRowSorter().setSortKeys(null);
        this.setRowSorter(null);
        this.setAutoCreateRowSorter(true);
        setSpalten();
        setHeight();
    }

    public void tabelleNachDatenSchreiben() {
        if (tabelle == TableType.STANDARD) {
            // wird nur für eingerichtet Tabellen gemacht
            return;
        }
        // Tabellendaten ind die Daten.system schreiben
        // erst die Breite, dann die Reihenfolge
        String b, r, s = "", upDown = "";
        int reihe_[] = new int[maxSpalten];
        int breite_[] = new int[maxSpalten];
        for (int i = 0; i < reihe_.length && i < this.getModel().getColumnCount(); ++i) {
            reihe_[i] = this.convertColumnIndexToModel(i);
        }
        for (int i = 0; i < breite_.length && i < this.getModel().getColumnCount(); ++i) {
            breite_[i] = this.getColumnModel().getColumn(this.convertColumnIndexToView(i)).getWidth();
        }
        b = Integer.toString(breite_[0]);
        r = Integer.toString(reihe_[0]);
        for (int i = 1; i < breiten.size(); i++) {
            b = b + ',' + Integer.toString(breite_[i]);
            r = r + ',' + Integer.toString(reihe_[i]);
        }
        listeSortKeys = this.getRowSorter().getSortKeys();
        if (listeSortKeys != null) {
            if (!listeSortKeys.isEmpty()) {
                SortKey sk = listeSortKeys.get(0);
                s = String.valueOf(sk.getColumn());
                upDown = sk.getSortOrder().equals(SortOrder.ASCENDING) ? SORT_ASCENDING : SORT_DESCENDING;
            }
        }
        MVConfig.add(nrDatenSystem, b + FELDTRENNER + r + FELDTRENNER + s + FELDTRENNER + upDown);
        if (iconAnzeigenStr != null) {
            MVConfig.add(iconAnzeigenStr, String.valueOf(iconAnzeigen));
        }
        if (iconKleinStr != null) {
            MVConfig.add(iconKleinStr, String.valueOf(iconKlein));
        }
    }

    private int[] getArray(int anzahl) {
        int[] arr = new int[anzahl];
        for (int i = 0; i < arr.length; ++i) {
            arr[i] = -1;
        }
        return arr;
    }

    private boolean arrLesen(String s, int[] arr) {
        String sub;
        if (maxSpalten != countString(s)) {
            // dann hat sich die Anzahl der Spalten der Tabelle geändert: Versionswechsel
            return false;
        } else {
            for (int i = 0; i < maxSpalten; i++) {
                if (!s.isEmpty()) {
                    if (s.contains(",")) {
                        sub = s.substring(0, s.indexOf(','));
                        s = s.replaceFirst(sub + ',', "");
                    } else {
                        sub = s;
                        s = "";
                    }
                    try {
                        arr[i] = Integer.parseInt(sub);
                    } catch (Exception ex) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static SortKey sortKeyLesen(String s, String upDown) {
        SortKey sk;
        int sp;
        try {
            sp = Integer.parseInt(s);
            if (upDown.equals(SORT_DESCENDING)) {
                sk = new SortKey(sp, SortOrder.DESCENDING);
            } else {
                sk = new SortKey(sp, SortOrder.ASCENDING);
            }
        } catch (Exception ex) {
            return null;
        }
        return sk;
    }

    private int countString(String s) {
        int ret = 0;
        for (int i = 0; i < s.length(); ++i) {
            if (s.charAt(i) == ',') {
                ++ret;
            }
        }
        return ++ret;
    }

    private class TableRowTransferHandlerDownload extends TransferHandler {
        //private final DataFlavor localObjectFlavor = new ActivationDataFlavor(Integer.class, DataFlavor.javaJVMLocalObjectMimeType, "Integer Row Index");
        private final DataFlavor localObjectFlavor = new DataFlavor(Integer.class, "Integer Row Index");
        private JTable table = null;
        private int[] transferedRows = null;

        public TableRowTransferHandlerDownload(JTable table) {
            this.table = table;
        }

        @Override
        protected Transferable createTransferable(JComponent c) {
            assert (c.equals(table));
            transferedRows = table.getSelectedRows();
            return new DataHandler(table.getSelectedRow(), localObjectFlavor.getMimeType());
        }

        @Override
        public boolean canImport(TransferHandler.TransferSupport info) {
            try {
                boolean b = info.getComponent() == table && info.isDrop() && info.isDataFlavorSupported(localObjectFlavor);
                table.setCursor(b ? DragSource.DefaultMoveDrop : DragSource.DefaultMoveNoDrop);
                return b;
                // here's the problem
                // canImport is called during drags AND before drop is accepted
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        }

        @Override
        public int getSourceActions(JComponent c) {
            return TransferHandler.COPY_OR_MOVE;
        }

        @Override
        public boolean importData(TransferHandler.TransferSupport info) {
            try {
                JTable target = (JTable) info.getComponent();
                JTable.DropLocation dl = (JTable.DropLocation) info.getDropLocation();
                int index = dl.getRow();
                TModel tModel = (TModel) table.getModel();
                int max = tModel.getRowCount();
                if (index < 0 || index > max) {
                    index = max;
                }
                target.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                if (transferedRows != null) {
                    reorder(index, transferedRows);
                    transferedRows = null;
                    return true;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return false;
        }

        @Override
        protected void exportDone(JComponent c, Transferable t, int act) {
            if (act == TransferHandler.MOVE) {
                table.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }
        }
    }

    public class WidthAdjuster extends MouseAdapter {

        private List<? extends RowSorter.SortKey> listeSortKeys = null;

        private final JTable table;
        private static final int EPSILON = 5;   //boundary sensitivity
        private final Cursor EAST = Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR);
        private final Cursor WEST = Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR);

        public WidthAdjuster(final JTable table) {
            this.table = table;
        }

        @Override
        public void mousePressed(final MouseEvent evt) {
            //int c = getLeftColumn(evt.getPoint());
            if (evt.getClickCount() == 1) {
                if (table.getRowSorter() != null) {
                    listeSortKeys = table.getRowSorter().getSortKeys();
                } else {
                    listeSortKeys = null;
                }
            }
            if (evt.getClickCount() > 1 && usingResizeCursor()) {
                resize(getLeftColumn(evt.getPoint()));
            }
        }

        private JTableHeader getTableHeader() {
            return table.getTableHeader();
        }

        private boolean usingResizeCursor() {
            Cursor cursor = getTableHeader().getCursor();
            return cursor.equals(EAST) || cursor.equals(WEST);
        }

        //if near the boundary, will choose left column
        private int getLeftColumn(final Point pt) {
            pt.x -= EPSILON;
            return getTableHeader().columnAtPoint(pt);
        }

        private void resize(final int col) {

            TableColumnModel tcm = table.getColumnModel();
            TableColumn tc = tcm.getColumn(col);
            TableCellRenderer tcr = tc.getHeaderRenderer();
            if (tcr == null) {
                tcr = table.getTableHeader().getDefaultRenderer();
            }
            Object obj = tc.getHeaderValue();
            Component comp = tcr.getTableCellRendererComponent(table, obj, false, false, 0, 0);
            int maxWidth = comp.getPreferredSize().width;

            for (int i = 0, ub = table.getRowCount(); i != ub; ++i) {
                tcr = table.getCellRenderer(i, col);
                obj = table.getValueAt(i, col);
                comp = tcr.getTableCellRendererComponent(table, obj, false, false, i, col);
                int w = comp.getPreferredSize().width;
                if (w > maxWidth) {
                    maxWidth = w;
                }
            }
            maxWidth += 10; //and room to grow...
            tc.setPreferredWidth(maxWidth); //remembers the value
            tc.setWidth(maxWidth);          //forces layout, repaint

            if (listeSortKeys != null) {
                if (!listeSortKeys.isEmpty()) {
                    table.getRowSorter().setSortKeys(listeSortKeys);
                }
            }
        }

    }
}
