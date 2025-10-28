package com.marcosdeDesarrollo.demo.EstilosPE.domain.dto;

public class DashboardCategoriaDto {

    private String categoria;
    private long unidadesVendidas;

    public DashboardCategoriaDto() {
    }

    public DashboardCategoriaDto(String categoria, long unidadesVendidas) {
        this.categoria = categoria;
        this.unidadesVendidas = unidadesVendidas;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public long getUnidadesVendidas() {
        return unidadesVendidas;
    }

    public void setUnidadesVendidas(long unidadesVendidas) {
        this.unidadesVendidas = unidadesVendidas;
    }
}
