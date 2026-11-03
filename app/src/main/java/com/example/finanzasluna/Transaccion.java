package com.example.finanzasluna;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class Transaccion {

    public static final String TIPO_INGRESO = "Ingreso";
    public static final String TIPO_GASTO = "Gasto";

    // Document ID de Firestore. No se guarda como campo dentro del documento.
    private String id;
    private String concepto;
    private double monto;
    private String tipo;

    // Autor del registro (para auditoria y para el InfoWindow del mapa).
    private String userId;
    private String userEmail;

    // Evidencia fotografica (Firebase Storage) y geolocalizacion (FusedLocationProviderClient + Geocoder).
    private String fotoUrl;
    private Double latitud;
    private Double longitud;
    private String direccion;

    @ServerTimestamp
    private Date creadoEn;

    // Constructor vacio requerido por Firestore para el mapeo automatico (POJO).
    public Transaccion() {
    }

    public Transaccion(String concepto, double monto, String tipo) {
        this.concepto = concepto;
        this.monto = monto;
        this.tipo = tipo;
    }

    @Exclude
    public String getId() {
        return id;
    }

    @Exclude
    public void setId(String id) {
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

    public Date getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(Date creadoEn) {
        this.creadoEn = creadoEn;
    }

    @Exclude
    public boolean esIngreso() {
        return TIPO_INGRESO.equals(tipo);
    }

    @Exclude
    public boolean tieneUbicacion() {
        return latitud != null && longitud != null;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getFotoUrl() {
        return fotoUrl;
    }

    public void setFotoUrl(String fotoUrl) {
        this.fotoUrl = fotoUrl;
    }

    public Double getLatitud() {
        return latitud;
    }

    public void setLatitud(Double latitud) {
        this.latitud = latitud;
    }

    public Double getLongitud() {
        return longitud;
    }

    public void setLongitud(Double longitud) {
        this.longitud = longitud;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }
}
