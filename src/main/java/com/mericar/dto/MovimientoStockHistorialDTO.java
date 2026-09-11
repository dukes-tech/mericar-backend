package com.mericar.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class MovimientoStockHistorialDTO {

    private Long idDetalleStock;

    private Long idRegistroStock;

    private Long idProducto;

    private Integer cantidad;

    private String tipoMovimiento;

    private Integer stockAnterior;

    private Integer stockNuevo;

    private LocalDate fecha;

    private LocalDateTime fechaCreacion;

    private Long idUsuario;

    private String nombreUsuario;
    
    private String observacion;
}