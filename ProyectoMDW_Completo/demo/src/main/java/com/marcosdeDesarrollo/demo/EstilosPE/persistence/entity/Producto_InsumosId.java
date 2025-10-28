package com.marcosdeDesarrollo.demo.EstilosPE.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class Producto_InsumosId implements Serializable {

    @Column(name = "id_producto")
    private Long idProducto;

    @Column(name = "id_insumo")
    private Long idInsumo;

    public Producto_InsumosId() {
    }

    public Producto_InsumosId(Long idProducto, Long idInsumo) {
        this.idProducto = idProducto;
        this.idInsumo = idInsumo;
    }

    public Long getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(Long idProducto) {
        this.idProducto = idProducto;
    }

    public Long getIdInsumo() {
        return idInsumo;
    }

    public void setIdInsumo(Long idInsumo) {
        this.idInsumo = idInsumo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Producto_InsumosId)) {
            return false;
        }
        Producto_InsumosId that = (Producto_InsumosId) o;
        return Objects.equals(idProducto, that.idProducto)
                && Objects.equals(idInsumo, that.idInsumo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idProducto, idInsumo);
    }
}
