package com.marcosdeDesarrollo.demo.EstilosPE.domain.dto;

public class DashboardPaymentMethodDto {

    private String metodo;
    private long cantidad;

    public DashboardPaymentMethodDto() {
    }

    public DashboardPaymentMethodDto(String metodo, long cantidad) {
        this.metodo = metodo;
        this.cantidad = cantidad;
    }

    public String getMetodo() {
        return metodo;
    }

    public void setMetodo(String metodo) {
        this.metodo = metodo;
    }

    public long getCantidad() {
        return cantidad;
    }

    public void setCantidad(long cantidad) {
        this.cantidad = cantidad;
    }
}
