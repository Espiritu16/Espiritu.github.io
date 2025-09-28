package com.marcosdeDesarrollo.demo.Controller;

import com.marcosdeDesarrollo.demo.DTO.PermisoResponseDto;
import com.marcosdeDesarrollo.demo.Entity.Permiso;
import com.marcosdeDesarrollo.demo.Repository.PermisoRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/permisos")
@CrossOrigin(origins = "*")
public class PermisoController {

    private final PermisoRepository permisoRepository;

    public PermisoController(PermisoRepository permisoRepository) {
        this.permisoRepository = permisoRepository;
    }

    @GetMapping
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
