package com.example.finanzas;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;


public class DbHelper extends SQLiteOpenHelper {

    //datos de la base de datos.
    private static final String NOMBRE_BD = "finanzas.db";
    private static final int VERSION_BD = 1;


    public static final String TABLA = "transacciones";
    public static final String COL_ID = "id";
    public static final String COL_CONCEPTO = "concepto";
    public static final String COL_MONTO = "monto";
    public static final String COL_TIPO = "tipo";

    public DbHelper(Context context) {
        super(context, NOMBRE_BD, null, VERSION_BD);
    }

    //se ejecuta una sola vez, cuando la base de datos se crea por primera vez.
    @Override
    public void onCreate(SQLiteDatabase db) {
        String crearTabla = "CREATE TABLE " + TABLA + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_CONCEPTO + " TEXT NOT NULL, " +
                COL_MONTO + " REAL NOT NULL, " +
                COL_TIPO + " TEXT NOT NULL)";
        db.execSQL(crearTabla);
    }

    //se ejecuta si cambia la version de la BD. Para este proyecto basta recrearla.
    @Override
    public void onUpgrade(SQLiteDatabase db, int versionAnterior, int versionNueva) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLA);
        onCreate(db);
    }

    //insertar
    public long insertar(Transaccion t) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues valores = new ContentValues();
        valores.put(COL_CONCEPTO, t.getConcepto());
        valores.put(COL_MONTO, t.getMonto());
        valores.put(COL_TIPO, t.getTipo());
        long id = db.insert(TABLA, null, valores);
        db.close();
        return id;
    }

    //leer
    public List<Transaccion> obtenerTodas() {
        List<Transaccion> lista = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLA, null, null, null, null, null, COL_ID + " DESC");
        if (cursor.moveToFirst()) {
            do {
                lista.add(leerFila(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return lista;
    }
    public Transaccion obtenerPorId(long id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLA, null,
                COL_ID + " = ?", new String[]{String.valueOf(id)},
                null, null, null);

        Transaccion t = null;
        if (cursor.moveToFirst()) {
            t = leerFila(cursor);
        }
        cursor.close();
        db.close();
        return t;
    }

    //actualizar
    public int actualizar(Transaccion t) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues valores = new ContentValues();
        valores.put(COL_CONCEPTO, t.getConcepto());
        valores.put(COL_MONTO, t.getMonto());
        valores.put(COL_TIPO, t.getTipo());
        int filas = db.update(TABLA, valores,
                COL_ID + " = ?", new String[]{String.valueOf(t.getId())});
        db.close();
        return filas; // numero de filas afectadas
    }

    //eliminar
    public int eliminar(long id) {
        SQLiteDatabase db = getWritableDatabase();
        int filas = db.delete(TABLA,
                COL_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
        return filas;
    }

    //metodo privado de ayuda: convierte la fila actual del cursor en un objeto Transaccion.
    private Transaccion leerFila(Cursor cursor) {
        long id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID));
        String concepto = cursor.getString(cursor.getColumnIndexOrThrow(COL_CONCEPTO));
        double monto = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_MONTO));
        String tipo = cursor.getString(cursor.getColumnIndexOrThrow(COL_TIPO));
        return new Transaccion(id, concepto, monto, tipo);
    }
}
