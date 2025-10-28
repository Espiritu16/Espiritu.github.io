package com.marcosdeDesarrollo.demo.EstilosPE.domain.repository;

import com.marcosdeDesarrollo.demo.EstilosPE.persistence.entity.Tipos_Gasto;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TiposGastoRepository extends JpaRepository<Tipos_Gasto, Integer> {

    Optional<Tipos_Gasto> findByNombreTipoIgnoreCase(String nombreTipo);
}
