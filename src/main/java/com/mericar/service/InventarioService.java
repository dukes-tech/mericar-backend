package com.mericar.service;

import com.mericar.dto.MovimientoStockRequest;
import com.mericar.entity.DetalleRegistroStock;
import com.mericar.entity.Producto;
import com.mericar.entity.RegistroStock;
import com.mericar.repository.DetalleRegistroStockRepository;
import com.mericar.repository.ProductoRepository;
import com.mericar.repository.RegistroStockRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import com.mericar.dto.MovimientoStockHistorialDTO;

import java.util.List;
import java.util.stream.Collectors;
import com.mericar.entity.Usuario;
import com.mericar.repository.UsuarioRepository;
@Service
public class InventarioService {

    private final ProductoRepository productoRepository;
    private final RegistroStockRepository registroStockRepository;
    private final DetalleRegistroStockRepository detalleRegistroStockRepository;
    private final UsuarioRepository usuarioRepository;



    public InventarioService(
        ProductoRepository productoRepository,
        RegistroStockRepository registroStockRepository,
        DetalleRegistroStockRepository detalleRegistroStockRepository,
        UsuarioRepository usuarioRepository
) {
    this.productoRepository = productoRepository;
    this.registroStockRepository = registroStockRepository;
    this.detalleRegistroStockRepository = detalleRegistroStockRepository;
    this.usuarioRepository = usuarioRepository;
}

    // ==========================================
    // REGISTRAR MOVIMIENTO
    // ==========================================

    @Transactional
    public Producto registrarMovimiento(
            MovimientoStockRequest request
    ) {

        // ======================================
        // VALIDACIONES
        // ======================================

        if (request.getIdProducto() == null) {
            throw new RuntimeException(
                    "Debe seleccionar un producto"
            );
        }

        if (
                request.getCantidad() == null ||
                request.getCantidad() <= 0
        ) {
            throw new RuntimeException(
                    "La cantidad debe ser mayor a cero"
            );
        }

        if (
                request.getTipoMovimiento() == null ||
                request.getTipoMovimiento().isBlank()
        ) {
            throw new RuntimeException(
                    "Debe indicar el tipo de movimiento"
            );
        }

        if (request.getIdUsuario() == null) {
            throw new RuntimeException(
                    "Debe indicar el usuario"
            );
        }

        // ======================================
        // BUSCAR PRODUCTO
        // ======================================

        Producto producto = productoRepository
                .findById(request.getIdProducto())
                .orElseThrow(() ->
                        new RuntimeException(
                                "Producto no encontrado"
                        )
                );

        if (!Boolean.TRUE.equals(producto.getActivo())) {
            throw new RuntimeException(
                    "No se pueden realizar movimientos sobre un producto inactivo"
            );
        }

        int stockAnterior =
                producto.getStockActual() != null
                        ? producto.getStockActual()
                        : 0;

        int stockNuevo;

        String tipo = request
                .getTipoMovimiento()
                .trim()
                .toUpperCase();

        // ======================================
        // CALCULAR STOCK
        // ======================================

        switch (tipo) {

            case "ENTRADA":

                stockNuevo =
                        stockAnterior +
                        request.getCantidad();

                break;

            case "SALIDA":

                if (request.getCantidad() > stockAnterior) {
                    throw new RuntimeException(
                            "Stock insuficiente. Disponible: "
                                    + stockAnterior
                    );
                }

                stockNuevo =
                        stockAnterior -
                        request.getCantidad();

                break;

            case "AJUSTE":

                // En AJUSTE, cantidad representa
                // el stock físico real contado.

                stockNuevo = request.getCantidad();

                break;

            default:

                throw new RuntimeException(
                        "Tipo de movimiento inválido"
                );
        }

        // ======================================
        // CREAR CABECERA
        // ======================================

        RegistroStock registro =
                new RegistroStock();

        registro.setFecha(LocalDate.now());

        registro.setIdUsuario(
                request.getIdUsuario()
        );

        registro.setObservacion(
                request.getObservacion()
        );

        registro.setFechaCreacion(
                LocalDateTime.now()
        );

        registro =
                registroStockRepository.save(registro);

        // ======================================
        // CREAR DETALLE
        // ======================================

        DetalleRegistroStock detalle =
                new DetalleRegistroStock();

        detalle.setIdRegistroStock(
                registro.getIdRegistroStock()
        );

        detalle.setIdProducto(
                producto.getIdProducto()
        );

        detalle.setCantidad(
                request.getCantidad()
        );

        detalle.setTipoMovimiento(tipo);

        detalle.setStockAnterior(
                stockAnterior
        );

        detalle.setStockNuevo(
                stockNuevo
        );

        detalleRegistroStockRepository.save(
                detalle
        );

        // ======================================
        // ACTUALIZAR PRODUCTO
        // ======================================

        producto.setStockActual(stockNuevo);

        producto.setFechaActualizacion(
                LocalDateTime.now()
        );

        return productoRepository.save(producto);
    }
    // ==========================================
// HISTORIAL DE MOVIMIENTOS POR PRODUCTO
// ==========================================
@Transactional
public void registrarMovimientoSinActualizarStock(
        Long idProducto,
        Integer cantidad,
        String tipoMovimiento,
        Long idUsuario,
        String observacion,
        Integer stockAnterior,
        Integer stockNuevo
) {

    RegistroStock registro = new RegistroStock();

    registro.setFecha(LocalDate.now());
    registro.setIdUsuario(idUsuario);
    registro.setObservacion(observacion);
    registro.setFechaCreacion(LocalDateTime.now());

    registro =
            registroStockRepository.save(registro);


    DetalleRegistroStock detalle =
            new DetalleRegistroStock();

    detalle.setIdRegistroStock(
            registro.getIdRegistroStock()
    );

    detalle.setIdProducto(idProducto);
    detalle.setCantidad(cantidad);
    detalle.setTipoMovimiento(tipoMovimiento);
    detalle.setStockAnterior(stockAnterior);
    detalle.setStockNuevo(stockNuevo);

    detalleRegistroStockRepository.save(detalle);
}
public List<MovimientoStockHistorialDTO>
obtenerMovimientosPorProducto(Long idProducto) {

    // Verificar que el producto exista
    productoRepository
            .findById(idProducto)
            .orElseThrow(() ->
                    new RuntimeException(
                            "Producto no encontrado"
                    )
            );

    // Obtener movimientos
    List<DetalleRegistroStock> detalles =
            detalleRegistroStockRepository
                    .findByIdProductoOrderByIdDetalleStockDesc(
                            idProducto
                    );

    return detalles.stream()
            .map(detalle -> {

                RegistroStock registro =
                        registroStockRepository
                                .findById(
                                        detalle.getIdRegistroStock()
                                )
                                .orElse(null);

                MovimientoStockHistorialDTO dto =
                        new MovimientoStockHistorialDTO();

                dto.setIdDetalleStock(
                        detalle.getIdDetalleStock()
                );

                dto.setIdRegistroStock(
                        detalle.getIdRegistroStock()
                );

                dto.setIdProducto(
                        detalle.getIdProducto()
                );

                dto.setCantidad(
                        detalle.getCantidad()
                );

                dto.setTipoMovimiento(
                        detalle.getTipoMovimiento()
                );

                dto.setStockAnterior(
                        detalle.getStockAnterior()
                );

                dto.setStockNuevo(
                        detalle.getStockNuevo()
                );

                if (registro != null) {

                    dto.setFecha(
                            registro.getFecha()
                    );

                    dto.setFechaCreacion(
                            registro.getFechaCreacion()
                    );

                    dto.setIdUsuario(
                                registro.getIdUsuario()
                        );

                        Usuario usuario = usuarioRepository
                                .findById(registro.getIdUsuario())
                                .orElse(null);

                        if (usuario != null) {

                        String nombreCompleto =
                                ((usuario.getNombres() != null
                                        ? usuario.getNombres()
                                        : "")
                                + " "
                                + (usuario.getApellidos() != null
                                        ? usuario.getApellidos()
                                        : ""))
                                .trim();

                        dto.setNombreUsuario(
                                nombreCompleto
                        );
                        }

                        dto.setObservacion(
                                registro.getObservacion()
                        );

                    dto.setObservacion(
                            registro.getObservacion()
                    );
                }

                return dto;
            })
            .collect(Collectors.toList());
}
}