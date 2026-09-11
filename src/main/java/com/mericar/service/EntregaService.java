package com.mericar.service;

import com.mericar.dto.CuentaCobrarDTO;
import com.mericar.dto.DetalleEntregaHistorialDTO;
import com.mericar.dto.DetalleEntregaRequest;
import com.mericar.dto.EntregaHistorialDTO;
import com.mericar.dto.EntregaRequest;
import com.mericar.dto.ReporteClienteDTO;

import com.mericar.entity.Cliente;
import com.mericar.entity.DetalleEntrega;
import com.mericar.entity.Entrega;
import com.mericar.entity.MetodoPago;
import com.mericar.entity.Producto;

import com.mericar.repository.ClienteRepository;
import com.mericar.repository.EntregaRepository;
import com.mericar.repository.ProductoRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.mericar.dto.ReporteProductoDTO;
import com.mericar.dto.ReporteInventarioDTO;
import com.mericar.dto.MovimientoStockRequest;
@Service
public class EntregaService {

    @Autowired
    private EntregaRepository entregaRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

@Autowired
private InventarioService inventarioService;
    // ==========================================
    // GUARDAR ENTREGA
    // ==========================================

    @Transactional
    public Entrega guardar(EntregaRequest request) {

        if (
            request.getProductos() == null ||
            request.getProductos().isEmpty()
        ) {
            throw new RuntimeException(
                "Debe seleccionar al menos un producto"
            );
        }


        // ==========================================
        // CREAR ENTREGA
        // ==========================================

        Entrega entrega = new Entrega();

        entrega.setIdCliente(
            request.getIdCliente()
        );

        entrega.setNombreClienteOcasional(
            request.getNombreClienteOcasional()
        );

        entrega.setFecha(
            LocalDate.now()
        );

        entrega.setObservacion(
            request.getObservacion()
        );

        entrega.setIdUsuario(
            request.getIdUsuario()
        );

        entrega.setTipo(
            request.getTipo()
        );

        entrega.setActivo(true);

        entrega.setFechaCreacion(
            LocalDateTime.now()
        );

        entrega.setFechaActualizacion(
            LocalDateTime.now()
        );


        // ==========================================
        // MÉTODO DE PAGO
        // ==========================================

        MetodoPago metodoPago;

        BigDecimal efectivo =
            request.getPagoEfectivo() != null
                ? request.getPagoEfectivo()
                : BigDecimal.ZERO;

        BigDecimal transferencia =
            request.getPagoTransferencia() != null
                ? request.getPagoTransferencia()
                : BigDecimal.ZERO;


        if (
            efectivo.compareTo(BigDecimal.ZERO) > 0 &&
            transferencia.compareTo(BigDecimal.ZERO) > 0
        ) {

            metodoPago = MetodoPago.MIXTO;

        } else if (
            transferencia.compareTo(BigDecimal.ZERO) > 0
        ) {

            metodoPago = MetodoPago.TRANSFERENCIA;

        } else {

            metodoPago = MetodoPago.EFECTIVO;
        }


        entrega.setMetodoPago(
            metodoPago
        );

        entrega.setPagoEfectivo(
            efectivo
        );

        entrega.setPagoTransferencia(
            transferencia
        );


        // ==========================================
        // DETALLES Y TOTAL
        // ==========================================

        BigDecimal total =
            BigDecimal.ZERO;


        for (
            DetalleEntregaRequest producto :
            request.getProductos()
        ) {

            if (
                producto.getCantidad() == null ||
                producto.getCantidad() <= 0
            ) {
                continue;
            }


            if (
                producto.getPrecio() == null ||
                producto.getPrecio()
                    .compareTo(BigDecimal.ZERO) <= 0
            ) {
                throw new RuntimeException(
                    "El precio del producto no es válido"
                );
            }


            BigDecimal subtotal =
                producto.getPrecio().multiply(
                    BigDecimal.valueOf(
                        producto.getCantidad()
                    )
                );


            DetalleEntrega detalle =
                new DetalleEntrega();

            detalle.setEntrega(
                entrega
            );

            detalle.setIdProducto(
                producto.getIdProducto()
            );

            detalle.setCantidad(
                producto.getCantidad()
            );

            detalle.setPrecioUnitario(
                producto.getPrecio()
            );

            detalle.setSubtotal(
                subtotal
            );


            entrega.getDetalles().add(
                detalle
            );


            total =
                total.add(subtotal);
        }


        // ==========================================
        // VALIDAR QUE EXISTAN PRODUCTOS VÁLIDOS
        // ==========================================

        if (entrega.getDetalles().isEmpty()) {

            throw new RuntimeException(
                "Debe seleccionar al menos un producto válido"
            );
        }


        // ==========================================
        // ABONO
        // ==========================================

        BigDecimal abono =
            efectivo.add(
                transferencia
            );


        if (
            abono.compareTo(total) > 0
        ) {
            throw new RuntimeException(
                "El abono no puede ser mayor al total"
            );
        }


        // ==========================================
        // TOTALES
        // ==========================================

        entrega.setTotal(
            total
        );

        entrega.setAbono(
            abono
        );

        entrega.setSaldoPendiente(
            total.subtract(abono)
        );


Entrega entregaGuardada =
        entregaRepository.saveAndFlush(entrega);

for (DetalleEntrega detalle : entregaGuardada.getDetalles()) {

    MovimientoStockRequest movimiento =
            new MovimientoStockRequest();

    movimiento.setIdProducto(
            detalle.getIdProducto()
    );

    movimiento.setCantidad(
            detalle.getCantidad()
    );

    movimiento.setTipoMovimiento(
            "SALIDA"
    );

    movimiento.setIdUsuario(
            request.getIdUsuario()
    );

    movimiento.setObservacion(
            "Salida automática por entrega #"
                    + entregaGuardada.getIdEntrega()
    );

    inventarioService.registrarMovimiento(
            movimiento
    );
}

return entregaGuardada;
    }


    // ==========================================
    // HISTORIAL DE ENTREGAS POR CLIENTE
    // ==========================================

    @Transactional(readOnly = true)
    public List<EntregaHistorialDTO> obtenerHistorialCliente(
        Long idCliente
    ) {

        List<Entrega> entregas =
            entregaRepository
                .findByIdClienteAndActivoTrueOrderByFechaDescIdEntregaDesc(
                    idCliente
                );


        List<EntregaHistorialDTO> resultado =
            new ArrayList<>();


        for (Entrega entrega : entregas) {

            EntregaHistorialDTO dto =
                new EntregaHistorialDTO();


            dto.setIdEntrega(
                entrega.getIdEntrega()
            );

            dto.setFecha(
                entrega.getFecha()
            );

            dto.setTotal(
                entrega.getTotal()
            );

            dto.setAbono(
                entrega.getAbono()
            );

            dto.setSaldoPendiente(
                entrega.getSaldoPendiente()
            );

            dto.setMetodoPago(
                entrega.getMetodoPago() != null
                    ? entrega.getMetodoPago().name()
                    : null
            );

            dto.setPagoEfectivo(
                entrega.getPagoEfectivo()
            );

            dto.setPagoTransferencia(
                entrega.getPagoTransferencia()
            );

            dto.setObservacion(
                entrega.getObservacion()
            );


            // ==========================================
            // PRODUCTOS
            // ==========================================

            List<DetalleEntregaHistorialDTO> productos =
                new ArrayList<>();


          

            dto.setProductos(
                productos
            );


            resultado.add(
                dto
            );
        }


        return resultado;
    }


    // ==========================================
    // CUENTAS POR COBRAR
    // ==========================================

    @Transactional(readOnly = true)
    public List<CuentaCobrarDTO> obtenerCuentasPorCobrar() {

        List<Entrega> entregas =
            entregaRepository
                .findByActivoTrueAndSaldoPendienteGreaterThanOrderByFechaAsc(
                    BigDecimal.ZERO
                );


        List<CuentaCobrarDTO> resultado =
            new ArrayList<>();


        for (Entrega entrega : entregas) {

            if (entrega.getIdCliente() == null) {
                continue;
            }


            Cliente cliente =
                clienteRepository
                    .findById(
                        entrega.getIdCliente()
                    )
                    .orElse(null);


            CuentaCobrarDTO dto =
                new CuentaCobrarDTO();


            dto.setIdEntrega(
                entrega.getIdEntrega()
            );

            dto.setIdCliente(
                entrega.getIdCliente()
            );

            dto.setFecha(
                entrega.getFecha()
            );

            dto.setTotal(
                entrega.getTotal()
            );

            dto.setAbono(
                entrega.getAbono()
            );

            dto.setSaldoPendiente(
                entrega.getSaldoPendiente()
            );


            if (cliente != null) {

                String nombreCompleto =
                    (
                        (cliente.getNombres() != null
                            ? cliente.getNombres()
                            : "")
                        +
                        " "
                        +
                        (cliente.getApellidos() != null
                            ? cliente.getApellidos()
                            : "")
                    ).trim();


                dto.setCliente(
                    nombreCompleto
                );

                dto.setTelefono(
                    cliente.getTelefono()
                );

            } else {

                dto.setCliente(
                    "Cliente no encontrado"
                );

                dto.setTelefono(
                    ""
                );
            }


            resultado.add(
                dto
            );
        }


        return resultado;
    }


    // ==========================================
    // REPORTE ENTREGAS POR CLIENTE
    // ==========================================

    @Transactional(readOnly = true)
    public List<ReporteClienteDTO> obtenerReportePorCliente() {

        List<Object[]> resultados =
            entregaRepository
                .obtenerReportePorCliente();


        List<ReporteClienteDTO> reporte =
            new ArrayList<>();


        for (Object[] fila : resultados) {

            Long idCliente =
                ((Number) fila[0])
                    .longValue();


            Cliente cliente =
                clienteRepository
                    .findById(
                        idCliente
                    )
                    .orElse(null);


            ReporteClienteDTO dto =
                new ReporteClienteDTO();


            dto.setIdCliente(
                idCliente
            );

            dto.setCantidadEntregas(
                ((Number) fila[1])
                    .longValue()
            );

            dto.setTotalEntregado(
                (BigDecimal) fila[2]
            );

            dto.setTotalAbonado(
                (BigDecimal) fila[3]
            );

            dto.setSaldoPendiente(
                (BigDecimal) fila[4]
            );


            if (cliente != null) {

                String nombreCompleto =
                    (
                        (cliente.getNombres() != null
                            ? cliente.getNombres()
                            : "")
                        +
                        " "
                        +
                        (cliente.getApellidos() != null
                            ? cliente.getApellidos()
                            : "")
                    ).trim();


                dto.setCliente(
                    nombreCompleto
                );

                dto.setTelefono(
                    cliente.getTelefono()
                );

            } else {

                dto.setCliente(
                    "Cliente no encontrado"
                );

                dto.setTelefono(
                    ""
                );
            }


            reporte.add(
                dto
            );
        }


        return reporte;
    }
    // ==========================================
// REPORTE PRODUCTOS VENDIDOS
// ==========================================

@Transactional(readOnly = true)
public List<ReporteProductoDTO> obtenerReporteProductos() {

    List<Object[]> resultados =
        entregaRepository.obtenerReporteProductos();

    List<ReporteProductoDTO> reporte =
        new ArrayList<>();


    for (Object[] fila : resultados) {

        Long idProducto =
            ((Number) fila[0]).longValue();


        Producto producto =
            productoRepository
                .findById(idProducto)
                .orElse(null);


        ReporteProductoDTO dto =
            new ReporteProductoDTO();


        dto.setIdProducto(
            idProducto
        );


        dto.setCantidadVendida(
            ((Number) fila[1]).longValue()
        );


        dto.setTotalVendido(
            (BigDecimal) fila[2]
        );


        if (producto != null) {

            dto.setProducto(
                producto.getNombre()
            );

        } else {

            dto.setProducto(
                "Producto no encontrado"
            );
        }


        reporte.add(dto);
    }


    return reporte;
}
// ==========================================
// REPORTE INVENTARIO
// ==========================================

@Transactional(readOnly = true)
public List<ReporteInventarioDTO> obtenerReporteInventario() {

    List<Producto> productos =
        productoRepository
            .findAllByOrderByNombreAsc();


    List<ReporteInventarioDTO> reporte =
        new ArrayList<>();


    for (Producto producto : productos) {

        ReporteInventarioDTO dto =
            new ReporteInventarioDTO();


        dto.setIdProducto(
            producto.getIdProducto()
        );

        dto.setProducto(
            producto.getNombre()
        );

        dto.setPrecio(
            producto.getPrecio()
        );

        dto.setStockActual(
            producto.getStockActual()
        );

        dto.setActivo(
            producto.getActivo()
        );


        // ==========================================
        // VALOR DEL INVENTARIO
        // ==========================================

        BigDecimal precio =
            producto.getPrecio() != null
                ? producto.getPrecio()
                : BigDecimal.ZERO;


        Integer stock =
            producto.getStockActual() != null
                ? producto.getStockActual()
                : 0;


        BigDecimal valorInventario =
            precio.multiply(
                BigDecimal.valueOf(stock)
            );


        dto.setValorInventario(
            valorInventario
        );


        reporte.add(dto);
    }


    return reporte;
}
}