package com.marcosdeDesarrollo.demo.EstilosPE.domain.service;

import com.marcosdeDesarrollo.demo.EstilosPE.domain.dto.PermisoResponseDto;
import com.marcosdeDesarrollo.demo.EstilosPE.domain.repository.PermisoRepository;
import com.marcosdeDesarrollo.demo.EstilosPE.persistence.entity.Permiso;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PermisoService {

    private final PermisoRepository permisoRepository;

    public PermisoService(PermisoRepository permisoRepository) {
        this.permisoRepository = permisoRepository;
    }

    public List<PermisoResponseDto> listar() {
        return permisoRepository.findAll()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private PermisoResponseDto mapToDto(Permiso permiso) {
        PermisoResponseDto dto = new PermisoResponseDto();
        dto.setId(permiso.getId());
        dto.setNombre(permiso.getNombre());
        dto.setDescripcion(permiso.getDescripcion());
        return dto;
    }
}
