const currencyFormatter = new Intl.NumberFormat('es-PE', {
  style: 'currency',
  currency: 'PEN',
  minimumFractionDigits: 2
});

const numberFormatter = new Intl.NumberFormat('es-PE');

document.addEventListener('DOMContentLoaded', () => {
  const API_BASE = '/api';
  const API_VENTAS = `${API_BASE}/ventas`;
  const API_KPIS = `${API_VENTAS}/kpis`;
  const API_REPORTE_MENSUAL = `${API_VENTAS}/reporte-mensual`;
  const API_CATEGORIAS = `${API_BASE}/categorias`;
  const API_PRODUCTOS = `${API_BASE}/productos`;

  const elements = {
    tablaBody: document.querySelector('#tablaVentas tbody'),
    resumenVentas: document.getElementById('ventasResumen'),
    kpiVentasHoy: document.getElementById('ventasHoy'),
    kpiIngresosMes: document.getElementById('ingresosMes'),
    kpiProductosVendidos: document.getElementById('productosVendidos'),
    kpiTicketPromedio: document.getElementById('ticketPromedio'),
    filtroEstado: document.getElementById('filtroEstadoVenta'),
    filtroMetodo: document.getElementById('filtroMetodoPago'),
    filtroFechaInicio: document.getElementById('fechaInicio'),
    filtroFechaFin: document.getElementById('fechaFin'),
    filtroBuscar: document.getElementById('buscarVenta'),
    btnLimpiarFiltros: document.getElementById('clearVentaFilters'),
    btnReporteMensual: document.getElementById('btnReporteMensual'),
    btnNuevaVenta: document.getElementById('btnNuevaVenta'),
    modalElement: document.getElementById('ventaModal'),
    modalTitle: document.getElementById('ventaModalLabel'),
    form: document.getElementById('formVenta'),
    submitButton: document.querySelector('#formVenta button[type="submit"]'),
    btnAgregarDetalle: document.getElementById('agregarProductoVenta'),
    ventaIdInput: document.getElementById('ventaId'),
    clienteInput: document.getElementById('cliente'),
    tipoComprobanteSelect: document.getElementById('tipoComprobante'),
    metodoPagoSelect: document.getElementById('metodoPago'),
    referenciaInput: document.getElementById('referencia'),
    dniInput: document.getElementById('dni'),
    rucInput: document.getElementById('ruc'),
    dniGroup: document.getElementById('dniGroup'),
    rucGroup: document.getElementById('rucGroup'),
    fechaVisual: document.getElementById('fechaVisual'),
    categoriaSelect: document.getElementById('categoriaSeleccionada'),
    productoSelect: document.getElementById('productoSeleccionado'),
    cantidadInput: document.getElementById('cantidadProducto'),
    precioInput: document.getElementById('precioProducto'),
    detalleBody: document.getElementById('detalleVentaBody'),
    detalleEmptyRow: document.getElementById('detalleVentaEmpty'),
    subtotalSpan: document.getElementById('ventaSubtotal'),
    totalPreviewSpan: document.getElementById('ventaTotalPreview'),
    totalHidden: document.getElementById('total')
  };

  const state = {
    detalles: [],
    modoLectura: false,
    productosCache: new Map(),
    clienteId: null,
    searchTimeout: null
  };

  const modal = new bootstrap.Modal(elements.modalElement);

  const headers = () => ({
    'Content-Type': 'application/json',
    ...(sessionStorage.getItem('jwtToken')
      ? { Authorization: `Bearer ${sessionStorage.getItem('jwtToken')}` }
      : {})
  });

  const handleApiError = (response) => {
    if (response.status === 401 || response.status === 403) {
      window.location.href = '/html/login.html';
    }
  };

  /* -------------------------------------------------- */
  /* Filtros y listado                                  */
  /* -------------------------------------------------- */

  const obtenerFiltros = () => ({
    estado: elements.filtroEstado?.value?.trim() || '',
    metodoPago: elements.filtroMetodo?.value?.trim() || '',
    fechaInicio: elements.filtroFechaInicio?.value || '',
    fechaFin: elements.filtroFechaFin?.value || '',
    search: elements.filtroBuscar?.value?.trim() || ''
  });

  const construirUrlVentas = () => {
    const filtros = obtenerFiltros();
    const url = new URL(API_VENTAS, window.location.origin);

    if (filtros.estado) url.searchParams.append('estado', filtros.estado);
    if (filtros.metodoPago) url.searchParams.append('metodoPago', filtros.metodoPago);
    if (filtros.fechaInicio) url.searchParams.append('fechaInicio', filtros.fechaInicio);
    if (filtros.fechaFin) url.searchParams.append('fechaFin', filtros.fechaFin);
    if (filtros.search) url.searchParams.append('search', filtros.search);

    return url;
  };

  const cargarVentas = async () => {
    try {
      const response = await fetch(construirUrlVentas(), { headers: headers() });
      if (!response.ok) {
        handleApiError(response);
        throw new Error('No se pudo obtener el listado de ventas');
      }
      const ventas = await response.json();
      renderTablaVentas(ventas);
    } catch (error) {
      console.error(error);
      renderTablaVentas([]);
    }
  };

  const renderTablaVentas = (ventas) => {
    elements.tablaBody.innerHTML = '';

    if (!ventas.length) {
      const fila = document.createElement('tr');
      fila.innerHTML = '<td colspan="10" class="text-center text-muted py-3">No se encontraron ventas con los filtros seleccionados</td>';
      elements.tablaBody.appendChild(fila);
      elements.resumenVentas.textContent = 'Sin registros para mostrar';
      return;
    }

    ventas.forEach((venta) => {
      const fila = document.createElement('tr');
      fila.innerHTML = `
        <td>${venta.id}</td>
        <td>${venta.cliente ?? 'Venta mostrador'}</td>
        <td>${venta.usuario ?? '-'}</td>
        <td>${venta.fecha ?? '-'}</td>
        <td class="text-end">${venta.total != null ? currencyFormatter.format(Number(venta.total)) : 'S/ 0.00'}</td>
        <td>${venta.metodoPago ?? '-'}</td>
        <td>${venta.tipoComprobante ?? '-'}</td>
        <td>${venta.referencia ?? '-'}</td>
        <td><span class="badge ${badgeEstado(venta.estado)}">${venta.estado ?? '-'}</span></td>
        <td class="text-nowrap">
          <div class="btn-group btn-group-sm" role="group">
            <button class="btn btn-outline-primary" data-action="ver" data-id="${venta.id}" title="Ver detalle">
              <i class="bi bi-eye"></i>
            </button>
            <button class="btn btn-outline-secondary" data-action="editar" data-id="${venta.id}" title="Editar">
              <i class="bi bi-pencil-square"></i>
            </button>
            <button class="btn btn-outline-danger" data-action="eliminar" data-id="${venta.id}" title="Eliminar">
              <i class="bi bi-trash"></i>
            </button>
            <button class="btn btn-outline-success" data-action="boleta" data-id="${venta.id}" title="Generar boleta">
              <i class="bi bi-receipt-cutoff"></i>
            </button>
          </div>
        </td>
      `;
      elements.tablaBody.appendChild(fila);
    });

    elements.resumenVentas.textContent = `${ventas.length} venta(s) encontradas`;
  };

  const badgeEstado = (estado) => {
    switch (estado) {
      case 'Pagada':
        return 'bg-success';
      case 'Pendiente':
        return 'bg-warning text-dark';
      case 'Cancelada':
        return 'bg-danger';
      default:
        return 'bg-secondary';
    }
  };

  /* -------------------------------------------------- */
  /* KPIs y reporte mensual                             */
  /* -------------------------------------------------- */

  const cargarKpis = async () => {
    try {
      const response = await fetch(API_KPIS, { headers: headers() });
      if (!response.ok) {
        handleApiError(response);
        throw new Error('No se pudo obtener los KPI de ventas');
      }
      const kpis = await response.json();
      elements.kpiVentasHoy.textContent = numberFormatter.format(Number(kpis.ventasHoy ?? 0));
      elements.kpiIngresosMes.textContent = currencyFormatter.format(Number(kpis.ingresosMes ?? 0));
      elements.kpiProductosVendidos.textContent = numberFormatter.format(Number(kpis.productosVendidosMes ?? 0));
      elements.kpiTicketPromedio.textContent = currencyFormatter.format(Number(kpis.ticketPromedioMes ?? 0));
    } catch (error) {
      console.error(error);
      elements.kpiVentasHoy.textContent = '0';
      elements.kpiIngresosMes.textContent = 'S/ 0.00';
      elements.kpiProductosVendidos.textContent = '0';
      elements.kpiTicketPromedio.textContent = 'S/ 0.00';
    }
  };

  //generar reporte mensual, con fecha actual de la exportacion 
  const generarCsvReporte = (ventas) => {
    const encabezados = ['ID', 'Fecha', 'Cliente', 'Método', 'Comprobante', 'Referencia', 'Total'];
    const filas = ventas.map((venta) => [
      venta.id,
      venta.fecha ?? '',
      (venta.cliente ?? '').replace(/,/g, ' '),
      venta.metodoPago ?? '',
      venta.tipoComprobante ?? '',
      venta.referencia ?? '',
      Number(venta.total ?? 0).toFixed(2)
    ]);
    const lineas = [encabezados, ...filas].map((fila) => fila.join(','));
    return lineas.join('\n');
  };

  const descargarReporteMensual = async () => {
    try {
      const response = await fetch(API_REPORTE_MENSUAL, { headers: headers() });
      if (!response.ok) {
        handleApiError(response);
        throw new Error('No se pudo generar el reporte');
      }
      const ventas = await response.json();
      if (!ventas.length) {
        window.alert('No hay ventas registradas en el mes actual.');
        return;
      }
      const csv = generarCsvReporte(ventas);
      const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
      const url = URL.createObjectURL(blob);
      const enlace = document.createElement('a');
      enlace.href = url;
      enlace.download = `reporte_ventas_${new Date().toISOString().slice(0, 10)}.csv`;
      enlace.click();
      URL.revokeObjectURL(url);
    } catch (error) {
      console.error(error);
      window.alert('No se pudo descargar el reporte mensual.');
    }
  };

  /* -------------------------------------------------- */
  /* Formulario y detalles                             */
  /* -------------------------------------------------- */

  const generarReferenciaLocal = () => {
    const ahora = new Date();
    const fecha = ahora.toISOString().replace(/[-:TZ.]/g, '').slice(0, 14);
    return `VEN-${fecha}`;
  };

  const setModoLectura = (lectura) => {
    state.modoLectura = lectura;
    const controlesBasicos = [
      elements.clienteInput,
      elements.tipoComprobanteSelect,
      elements.metodoPagoSelect,
      elements.dniInput,
      elements.rucInput,
      elements.categoriaSelect,
      elements.productoSelect,
      elements.cantidadInput,
      elements.precioInput
    ];

    controlesBasicos.forEach((control) => {
      if (!control) return;
      control.disabled = lectura;
    });

    elements.btnAgregarDetalle.classList.toggle('d-none', lectura);
    elements.submitButton.classList.toggle('d-none', lectura);
  };

  const limpiarDetalle = () => {
    state.detalles = [];
    renderDetalle();
  };

  const renderDetalle = () => {
    elements.detalleBody.innerHTML = '';

    if (!state.detalles.length) {
      elements.detalleBody.appendChild(elements.detalleEmptyRow);
      actualizarResumen();
      return;
    }

    state.detalles.forEach((item) => {
      const fila = document.createElement('tr');
      const subtotal = item.precioUnitario * item.cantidad;
      fila.innerHTML = `
        <td>${item.nombre} <span class="text-muted">${item.sku ? `(${item.sku})` : ''}</span></td>
        <td class="text-center">${item.cantidad}</td>
        <td class="text-end">${currencyFormatter.format(item.precioUnitario)}</td>
        <td class="text-end">${currencyFormatter.format(subtotal)}</td>
        <td class="text-end">
          ${state.modoLectura
            ? '<span class="text-muted">—</span>'
            : `<button class="btn btn-link text-danger p-0" data-id="${item.productoId}" title="Quitar">
                 <i class="bi bi-x-circle"></i>
               </button>`}
        </td>
      `;
      elements.detalleBody.appendChild(fila);
    });

    actualizarResumen();
  };

  const actualizarResumen = () => {
    const subtotal = state.detalles.reduce((acc, item) => acc + item.precioUnitario * item.cantidad, 0);
    elements.subtotalSpan.textContent = currencyFormatter.format(subtotal);
    elements.totalPreviewSpan.textContent = currencyFormatter.format(subtotal);
    elements.totalHidden.value = subtotal.toFixed(2);
  };

  const agregarDetalle = () => {
    if (state.modoLectura) return;

    const productoId = elements.productoSelect.value;
    if (!productoId) {
      window.alert('Selecciona un producto para agregar a la venta.');
      return;
    }

    const producto = state.productosCache.get(productoId);
    if (!producto) {
      window.alert('El producto seleccionado no está disponible.');
      return;
    }

    const cantidad = Number(elements.cantidadInput.value);
    if (!cantidad || cantidad <= 0) {
      window.alert('Ingresa una cantidad válida.');
      return;
    }

    const precio = Number(elements.precioInput.value);
    if (!precio || precio <= 0) {
      window.alert('Ingresa un precio válido.');
      return;
    }

    const existente = state.detalles.find((detalle) => detalle.productoId === Number(productoId));
    if (existente) {
      existente.cantidad += cantidad;
      existente.precioUnitario = precio;
    } else {
      state.detalles.push({
        productoId: Number(productoId),
        nombre: producto.nombreProducto,
        sku: producto.sku,
        cantidad,
        precioUnitario: precio
      });
    }

    renderDetalle();
    elements.productoSelect.value = '';
    elements.precioInput.value = '';
    elements.cantidadInput.value = 1;
  };

  const removerDetalle = (productoId) => {
    if (state.modoLectura) return;
    state.detalles = state.detalles.filter((detalle) => detalle.productoId !== productoId);
    renderDetalle();
  };

  const aplicarTipoComprobante = () => {
    const tipo = elements.tipoComprobanteSelect.value || 'BoletaSimple';

    const configurarCampo = (input, group, habilitar, placeholder) => {
      input.disabled = state.modoLectura || !habilitar;
      input.required = habilitar && !state.modoLectura;
      input.value = habilitar ? input.value : '';
      input.placeholder = placeholder;
      group.classList.toggle('opacity-50', !habilitar);
    };

    switch (tipo) {
      case 'Boleta':
        configurarCampo(elements.dniInput, elements.dniGroup, true, 'Ej. 12345678');
        configurarCampo(elements.rucInput, elements.rucGroup, false, 'No requerido');
        break;
      case 'Factura':
        configurarCampo(elements.dniInput, elements.dniGroup, false, 'No requerido');
        configurarCampo(elements.rucInput, elements.rucGroup, true, 'Ej. 12345678901');
        break;
      default:
        configurarCampo(elements.dniInput, elements.dniGroup, false, 'No requerido');
        configurarCampo(elements.rucInput, elements.rucGroup, false, 'No requerido');
        break;
    }
  };

  const prepararFormularioNuevaVenta = () => {
    elements.form.reset();
    elements.modalTitle.textContent = 'Nueva venta';
    elements.submitButton.textContent = 'Guardar venta';
    elements.referenciaInput.value = generarReferenciaLocal();
    elements.referenciaInput.readOnly = true;
    elements.ventaIdInput.value = '';
    state.productosCache = new Map();
    state.clienteId = null;
    setModoLectura(false);
    elements.tipoComprobanteSelect.value = 'BoletaSimple';
    aplicarTipoComprobante();
    if (elements.fechaVisual) {
      elements.fechaVisual.value = new Date().toLocaleDateString('es-PE');
      elements.fechaVisual.disabled = true;
    }
    elements.categoriaSelect.value = '';
    elements.productoSelect.innerHTML = '<option value="">Seleccionar producto</option>';
    elements.cantidadInput.value = 1;
    elements.precioInput.value = '';
    limpiarDetalle();
  };

  const popularFormularioConVenta = (venta) => {
    elements.ventaIdInput.value = venta.id;
    elements.modalTitle.textContent = `Editar venta #${venta.id}`;
    elements.submitButton.textContent = 'Actualizar venta';
    elements.clienteInput.value = venta.cliente ?? '';
    state.clienteId = venta.clienteId ?? null;
    elements.tipoComprobanteSelect.value = venta.tipoComprobante ?? 'BoletaSimple';
    elements.metodoPagoSelect.value = venta.metodoPago ?? '';
    elements.referenciaInput.value = venta.referencia ?? generarReferenciaLocal();
    elements.referenciaInput.readOnly = true;
    elements.dniInput.value = venta.dni ?? '';
    elements.rucInput.value = venta.ruc ?? '';
    if (elements.fechaVisual) {
      elements.fechaVisual.value = venta.fecha ?? new Date().toLocaleDateString('es-PE');
      elements.fechaVisual.disabled = true;
    }
    aplicarTipoComprobante();

    state.detalles = (venta.detalles ?? []).map((detalle) => ({
      productoId: detalle.productoId,
      nombre: detalle.nombreProducto ?? `Producto #${detalle.productoId}`,
      sku: detalle.sku ?? '',
      cantidad: detalle.cantidad,
      precioUnitario: Number(detalle.precioUnitario ?? 0)
    }));

    renderDetalle();
  };

  const popularFormularioSoloLectura = (venta) => {
    elements.modalTitle.textContent = `Detalle de venta #${venta.id}`;
    elements.submitButton.textContent = 'Guardar venta';
    elements.referenciaInput.readOnly = true;
    elements.clienteInput.value = venta.cliente ?? '';
    state.clienteId = venta.clienteId ?? null;
    elements.tipoComprobanteSelect.value = venta.tipoComprobante ?? 'BoletaSimple';
    elements.metodoPagoSelect.value = venta.metodoPago ?? '';
    elements.referenciaInput.value = venta.referencia ?? '';
    elements.dniInput.value = venta.dni ?? '';
    elements.rucInput.value = venta.ruc ?? '';
    if (elements.fechaVisual) {
      elements.fechaVisual.value = venta.fecha ?? new Date().toLocaleDateString('es-PE');
      elements.fechaVisual.disabled = true;
    }
    aplicarTipoComprobante();

    state.detalles = (venta.detalles ?? []).map((detalle) => ({
      productoId: detalle.productoId,
      nombre: detalle.nombreProducto ?? `Producto #${detalle.productoId}`,
      sku: detalle.sku ?? '',
      cantidad: detalle.cantidad,
      precioUnitario: Number(detalle.precioUnitario ?? 0)
    }));

    renderDetalle();
  };

  const obtenerVentaPorId = async (id) => {
    const response = await fetch(`${API_VENTAS}/${id}`, { headers: headers() });
    if (!response.ok) {
      handleApiError(response);
      throw new Error('No se pudo obtener la venta seleccionada');
    }
    return response.json();
  };

  const verVenta = async (id) => {
    try {
      const venta = await obtenerVentaPorId(id);
      setModoLectura(true);
      elements.referenciaInput.readOnly = true;
      popularFormularioSoloLectura(venta);
      elements.submitButton.classList.add('d-none');
      modal.show();
    } catch (error) {
      console.error(error);
      window.alert('No se pudo cargar el detalle de la venta.');
    }
  };

  const editarVenta = async (id) => {
    try {
      const venta = await obtenerVentaPorId(id);
      setModoLectura(false);
      popularFormularioConVenta(venta);
      aplicarTipoComprobante();
      modal.show();
    } catch (error) {
      console.error(error);
      window.alert('No se pudo cargar la venta para edición.');
    }
  };

  const eliminarVenta = async (id) => {
    if (!window.confirm(`¿Estás seguro de eliminar la venta #${id}?`)) {
      return;
    }
    try {
      const response = await fetch(`${API_VENTAS}/${id}`, {
        method: 'DELETE',
        headers: headers()
      });
      if (!response.ok) {
        handleApiError(response);
        const error = await response.json().catch(() => ({}));
        throw new Error(error.error ?? 'No se pudo eliminar la venta');
      }
      window.alert('Venta eliminada correctamente');
      await cargarVentas();
      await cargarKpis();
    } catch (error) {
      console.error(error);
      window.alert(error.message);
    }
  };

  const generarBoleta = (id) => {
    window.alert(`Funcionalidad pendiente: generar comprobante para la venta #${id}`);
  };

  const guardarVenta = async () => {
    if (state.modoLectura) {
      return;
    }

    if (!state.detalles.length) {
      window.alert('Agrega al menos un producto a la venta.');
      return;
    }

    if (!elements.metodoPagoSelect.value) {
      window.alert('Selecciona un método de pago.');
      return;
    }

    const tipoComprobante = elements.tipoComprobanteSelect.value || 'BoletaSimple';
    const dni = elements.dniInput.disabled ? null : elements.dniInput.value.trim();
    const ruc = elements.rucInput.disabled ? null : elements.rucInput.value.trim();

    if (tipoComprobante === 'Boleta' && (!dni || dni.length !== 8)) {
      window.alert('El DNI debe contener 8 dígitos.');
      return;
    }

    if (tipoComprobante === 'Factura' && (!ruc || ruc.length !== 11)) {
      window.alert('El RUC debe contener 11 dígitos.');
      return;
    }

    const payload = {
      clienteId: state.clienteId,
      clienteNombre: elements.clienteInput.value.trim(),
      tipoComprobante,
      metodoPago: elements.metodoPagoSelect.value,
      referencia: elements.referenciaInput.value,
      dni,
      ruc,
      detalles: state.detalles.map((detalle) => ({
        productoId: detalle.productoId,
        cantidad: detalle.cantidad,
        precioUnitario: detalle.precioUnitario
      }))
    };

    const ventaId = elements.ventaIdInput.value;
    const url = ventaId ? `${API_VENTAS}/${ventaId}` : API_VENTAS;
    const method = ventaId ? 'PUT' : 'POST';

    try {
      const response = await fetch(url, {
        method,
        headers: headers(),
        body: JSON.stringify(payload)
      });

      if (!response.ok) {
        handleApiError(response);
        const error = await response.json().catch(() => ({}));
        throw new Error(error.error ?? 'No se pudo guardar la venta');
      }

      window.alert(ventaId ? 'Venta actualizada correctamente' : 'Venta registrada correctamente');
      modal.hide();
      await Promise.all([cargarVentas(), cargarKpis()]);
      prepararFormularioNuevaVenta();
    } catch (error) {
      console.error(error);
      window.alert(error.message);
    }
  };

  /* -------------------------------------------------- */
  /* Carga de catálogos                                  */
  /* -------------------------------------------------- */

  const cargarCategorias = async () => {
    try {
      const response = await fetch(API_CATEGORIAS, { headers: headers() });
      if (!response.ok) {
        handleApiError(response);
        throw new Error('No se pudieron cargar las categorías');
      }
      const categorias = await response.json();
      elements.categoriaSelect.innerHTML = '<option value="">Seleccionar categoría</option>';
      categorias.forEach((categoria) => {
        const option = document.createElement('option');
        option.value = categoria.idCategoria;
        option.textContent = categoria.nombreCategoria;
        elements.categoriaSelect.appendChild(option);
      });
    } catch (error) {
      console.error(error);
    }
  };

  const cargarProductosPorCategoria = async (categoriaId) => {
    elements.productoSelect.innerHTML = '<option value="">Seleccionar producto</option>';
    elements.precioInput.value = '';
    state.productosCache = new Map();
    if (!categoriaId) return;

    try {
      const url = new URL(API_PRODUCTOS, window.location.origin);
      url.searchParams.append('categoriaId', categoriaId);
      url.searchParams.append('estado', 'Activo');

      const response = await fetch(url, { headers: headers() });
      if (!response.ok) {
        handleApiError(response);
        throw new Error('No se pudieron cargar los productos');
      }

      const productos = await response.json();
      productos.forEach((producto) => {
        state.productosCache.set(String(producto.idProducto), producto);
        const option = document.createElement('option');
        option.value = producto.idProducto;
        option.textContent = `${producto.nombreProducto} (${producto.sku})`;
        elements.productoSelect.appendChild(option);
      });
    } catch (error) {
      console.error(error);
    }
  };

  const actualizarPrecioSugerido = () => {
    const producto = state.productosCache.get(elements.productoSelect.value);
    elements.precioInput.value = producto?.precio ? Number(producto.precio).toFixed(2) : '';
  };

  /* -------------------------------------------------- */
  /* Eventos                                            */
  /* -------------------------------------------------- */

  elements.tablaBody.addEventListener('click', (event) => {
    const button = event.target.closest('button[data-action]');
    if (!button) return;

    const id = Number(button.dataset.id);
    switch (button.dataset.action) {
      case 'ver':
        verVenta(id);
        break;
      case 'editar':
        editarVenta(id);
        break;
      case 'eliminar':
        eliminarVenta(id);
        break;
      case 'boleta':
        generarBoleta(id);
        break;
      default:
        break;
    }
  });

  elements.btnAgregarDetalle.addEventListener('click', agregarDetalle);

  elements.detalleBody.addEventListener('click', (event) => {
    const button = event.target.closest('button[data-id]');
    if (!button) return;
    removerDetalle(Number(button.dataset.id));
  });

  elements.btnNuevaVenta.addEventListener('click', () => {
    prepararFormularioNuevaVenta();
    modal.show();
  });

  elements.form.addEventListener('submit', (event) => {
    event.preventDefault();
    guardarVenta();
  });

  elements.modalElement.addEventListener('hidden.bs.modal', () => {
    prepararFormularioNuevaVenta();
  });

  elements.tipoComprobanteSelect.addEventListener('change', aplicarTipoComprobante);
  elements.categoriaSelect.addEventListener('change', (event) => cargarProductosPorCategoria(event.target.value));
  elements.productoSelect.addEventListener('change', actualizarPrecioSugerido);

  if (elements.btnLimpiarFiltros) {
    elements.btnLimpiarFiltros.addEventListener('click', () => {
      if (elements.filtroEstado) elements.filtroEstado.value = '';
      if (elements.filtroMetodo) elements.filtroMetodo.value = '';
      if (elements.filtroFechaInicio) elements.filtroFechaInicio.value = '';
      if (elements.filtroFechaFin) elements.filtroFechaFin.value = '';
      if (elements.filtroBuscar) elements.filtroBuscar.value = '';
      cargarVentas();
    });
  }

  if (elements.btnReporteMensual) {
    elements.btnReporteMensual.addEventListener('click', descargarReporteMensual);
  }

  [elements.filtroEstado, elements.filtroMetodo, elements.filtroFechaInicio, elements.filtroFechaFin]
    .filter(Boolean)
    .forEach((input) => input.addEventListener('change', cargarVentas));

  if (elements.filtroBuscar) {
    elements.filtroBuscar.addEventListener('input', () => {
      clearTimeout(state.searchTimeout);
      state.searchTimeout = setTimeout(cargarVentas, 250);
    });
  }

  cargarCategorias();
  prepararFormularioNuevaVenta();
  cargarVentas();
  cargarKpis();
});
