package com.marcosdeDesarrollo.demo.EstilosPE.domain.dto;

import com.marcosdeDesarrollo.demo.EstilosPE.persistence.entity.Estado;

public class ActualizarEstadoProductoRequest {

    private Estado estado;

    public Estado getEstado() {
        return estado;
    }

    public void setEstado(Estado estado) {
        this.estado = estado;
    }
}
