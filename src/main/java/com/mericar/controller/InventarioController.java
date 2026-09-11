package com.mericar.controller;

import com.mericar.dto.MovimientoStockRequest;
import com.mericar.entity.Producto;
import com.mericar.service.InventarioService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.mericar.dto.MovimientoStockHistorialDTO;
import java.util.List;
@RestController
@RequestMapping("/api/inventario")
@CrossOrigin("*")
public class InventarioController {

    private final InventarioService inventarioService;

    public InventarioController(
            InventarioService inventarioService
    ) {
        this.inventarioService = inventarioService;
    }

    @PostMapping("/movimientos")
    public ResponseEntity<?> registrarMovimiento(
            @RequestBody MovimientoStockRequest request
    ) {

        try {

            Producto producto =
                    inventarioService.registrarMovimiento(
                            request
                    );

            return ResponseEntity.ok(producto);

        } catch (RuntimeException e) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            java.util.Map.of(
                                    "mensaje",
                                    e.getMessage()
                            )
                    );
        }
    }
    // ==========================================
// HISTORIAL DE MOVIMIENTOS POR PRODUCTO
// ==========================================

@GetMapping("/productos/{idProducto}/movimientos")
public ResponseEntity<?> obtenerMovimientosPorProducto(
        @PathVariable Long idProducto
) {

    try {

        List<MovimientoStockHistorialDTO> movimientos =
                inventarioService
                        .obtenerMovimientosPorProducto(
                                idProducto
                        );

        return ResponseEntity.ok(
                java.util.Map.of(
                        "success", true,
                        "movimientos", movimientos
                )
        );

    } catch (RuntimeException e) {

        return ResponseEntity
                .badRequest()
                .body(
                        java.util.Map.of(
                                "success", false,
                                "mensaje", e.getMessage()
                        )
                );
    }
}
}