package com.marcosdeDesarrollo.demo.EstilosPE.domain.repository;

import com.marcosdeDesarrollo.demo.EstilosPE.persistence.entity.EstadoVenta;
import com.marcosdeDesarrollo.demo.EstilosPE.persistence.entity.Venta;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface VentaRepository extends JpaRepository<Venta, Integer>, JpaSpecificationExecutor<Venta> {

    long countByFecha(LocalDate fecha);

    long countByEstado(EstadoVenta estado);

    List<Venta> findByFechaOrderByFechaCreacionDesc(LocalDate fecha);
}
