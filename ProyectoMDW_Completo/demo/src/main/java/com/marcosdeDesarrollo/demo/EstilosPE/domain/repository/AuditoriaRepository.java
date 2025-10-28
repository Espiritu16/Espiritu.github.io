package com.marcosdeDesarrollo.demo.EstilosPE.domain.repository;

import com.marcosdeDesarrollo.demo.EstilosPE.persistence.entity.Auditoria;
import com.marcosdeDesarrollo.demo.EstilosPE.persistence.entity.TipoOperacion;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditoriaRepository extends JpaRepository<Auditoria, Integer> {

    Optional<Auditoria> findTopByTablaAfectadaAndIdRegistroAndTipoOperacionOrderByFechaDesc(
            String tablaAfectada,
            Integer idRegistro,
            TipoOperacion tipoOperacion);

    Optional<Auditoria> findTopByTablaAfectadaAndIdRegistroOrderByFechaDesc(String tablaAfectada, Integer idRegistro);
}
