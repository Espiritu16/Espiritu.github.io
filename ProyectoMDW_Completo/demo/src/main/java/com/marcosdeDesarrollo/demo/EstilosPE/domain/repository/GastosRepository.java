package com.marcosdeDesarrollo.demo.EstilosPE.domain.repository;

import com.marcosdeDesarrollo.demo.EstilosPE.persistence.entity.Gastos;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GastosRepository extends JpaRepository<Gastos, Integer> {

    List<Gastos> findAllByOrderByFechaDesc();
}
