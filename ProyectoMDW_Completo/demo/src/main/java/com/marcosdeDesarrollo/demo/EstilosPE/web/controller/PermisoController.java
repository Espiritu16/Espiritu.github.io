package com.marcosdeDesarrollo.demo.EstilosPE.web.controller;

import com.marcosdeDesarrollo.demo.EstilosPE.domain.dto.PermisoResponseDto;
import com.marcosdeDesarrollo.demo.EstilosPE.domain.service.PermisoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/permisos")
@CrossOrigin(origins = "*")
@Tag(name = "Permisos", description = "Endpoints para consulta de permisos del sistema") // 👈 NUEVO
public class PermisoController {

    private final PermisoService permisoService;

    public PermisoController(PermisoService permisoService) {
        this.permisoService = permisoService;
    }

    @Operation(
        summary = "Listar todos los permisos", 
        description = "Obtiene la lista completa de permisos disponibles en el sistema para asignación de roles"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de permisos obtenida exitosamente",
                    content = @Content(schema = @Schema(implementation = PermisoResponseDto.class))),
        @ApiResponse(responseCode = "401", description = "No autorizado"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @GetMapping
    public List<PermisoResponseDto> listar() {
        return permisoService.listar();
    }
}
