package com.marcosdeDesarrollo.demo.DTO;

import java.time.LocalDateTime;

public class RolResponseDto {
    private Integer id;
    private String nombre;
    private String descripcion;
    private long usuariosAsignados;
    private LocalDateTime fechaCreacion;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public long getUsuariosAsignados() {
        return usuariosAsignados;
    }

    public void setUsuariosAsignados(long usuariosAsignados) {
        this.usuariosAsignados = usuariosAsignados;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
}
