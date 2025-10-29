package com.marcosdeDesarrollo.demo.EstilosPE.domain.repository;
import com.marcosdeDesarrollo.demo.EstilosPE.persistence.entity.Estado;
import com.marcosdeDesarrollo.demo.EstilosPE.persistence.entity.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long>, JpaSpecificationExecutor<Producto> {
    Producto findBySkuIgnoreCase(String sku);

    long countByEstado(Estado estado);

    boolean existsBySkuIgnoreCase(String sku);

    boolean existsBySkuIgnoreCaseAndIdProductoNot(String sku, Long idProducto);

    long countByStockActualLessThan(int stock);

}
