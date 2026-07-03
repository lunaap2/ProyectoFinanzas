package com.example.finanzas;

public class Transaccion {


    public static final String TIPO_INGRESO = "Ingreso";
    public static final String TIPO_GASTO = "Gasto";

    private long id;
    private String concepto;
    private double monto;
    private String tipo;

    public Transaccion(String concepto, double monto, String tipo) {
        this.concepto = concepto;
        this.monto = monto;
        this.tipo = tipo;
    }

    public Transaccion(long id, String concepto, double monto, String tipo) {
        this.id = id;
        this.concepto = concepto;
        this.monto = monto;
        this.tipo = tipo;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getConcepto() {
        return concepto;
    }

    public void setConcepto(String concepto) {
        this.concepto = concepto;
    }

    public double getMonto() {
        return monto;
    }

    public void setMonto(double monto) {
        this.monto = monto;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public boolean esIngreso() {
        return TIPO_INGRESO.equals(tipo);
    }
}
