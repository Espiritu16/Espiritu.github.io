package com.marcosdeDesarrollo.demo.EstilosPE.domain.repository;

import com.marcosdeDesarrollo.demo.EstilosPE.persistence.entity.Clientes;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientesRepository extends JpaRepository<Clientes, Integer> {
}
