/**
 * gastos.js - Lógica para el Módulo de Gastos Generales
 * Versión 3.0 - Conectado al backend Spring Boot.
 */
document.addEventListener('DOMContentLoaded', () => {
    const tablaBody = document.querySelector('#tablaGastos tbody');
    const resumenElement = document.getElementById('resumenGastos');
    const buscarInput = document.getElementById('buscarGasto');
    const filtroEstadoSelect = document.getElementById('filtroEstadoGasto');
    const filtroTipoSelect = document.getElementById('filtroTipoGasto');
    const fechaInicioInput = document.getElementById('filtroFechaInicio');
    const fechaFinInput = document.getElementById('filtroFechaFin');
    const limpiarFiltrosBtn = document.getElementById('limpiarFiltrosGastos');
    const btnReporteGastos = document.getElementById('btnReporteGastos');
    const modal = new bootstrap.Modal(document.getElementById('gastoModal'));
    const form = document.getElementById('formGasto');
    const gastoIdInput = document.getElementById('gastoId');
    const modalTitle = document.getElementById('modalLabel');
    const btnNuevoGasto = document.getElementById('btnNuevoGasto');
    const descripcionInput = document.getElementById('descripcion');
    const tipoSelect = document.getElementById('tipo');
    const fechaInput = document.getElementById('fecha');
    const montoInput = document.getElementById('monto');
    const rolGroup = document.getElementById('rolPersonalGroup');
    const usuarioGroup = document.getElementById('usuarioPersonalGroup');
    const rolSelect = document.getElementById('rolPersonal');
    const usuarioSelect = document.getElementById('usuarioPersonal');
    const servicioGroup = document.getElementById('servicioGroup');
    const servicioSelect = document.getElementById('tipoServicio');

    const ROLES_API = '/api/roles';
    const ROLES_VALIDOS = ['ADMINISTRADOR', 'VENDEDOR', 'CONTADOR'];
    const rolesUsuarios = new Map();
    let rolesCargados = false;
    let gastosCache = [];

    // ================== CAMBIO CLAVE ==================
    // Usamos la URL completa del backend para que el frontend sepa dónde hacer las peticiones.
    const API_URL = '/api/gastos';

    // --- SECCIÓN 1: FUNCIONES DE AYUDA Y RENDERIZADO ---

    /**
     * Obtiene el token JWT del sessionStorage.
     * @returns {string|null} El token JWT o null si no se encuentra.
     */
    function getToken() {
        return sessionStorage.getItem('jwtToken');
    }

    /**
     * Maneja los errores de respuesta de la API, especialmente los de autenticación.
     * @param {Response} response - La respuesta del fetch.
     */
    function handleApiError(response) {
        if (response.status === 401 || response.status === 403) {
            alert('Su sesión ha expirado o no tiene permisos. Por favor, inicie sesión de nuevo.');
            window.location.href = '/html/login.html'; // Redirigir al login
        } else {
            console.error(`Error de API: ${response.status} ${response.statusText}`);
        }
    }

    /**
     * Devuelve el nombre del rol formateado para mostrarlo en UI.
     * @param {string} nombreRol
     */
    function formatearNombreRol(nombreRol) {
        if (!nombreRol) return '';
        const lower = nombreRol.toLowerCase();
        return lower.charAt(0).toUpperCase() + lower.slice(1);
    }

    function obtenerTextoSeleccion(selectElement) {
        if (!selectElement) return '';
        const option = selectElement.options[selectElement.selectedIndex];
        return option ? option.textContent.trim() : '';
    }

    /**
     * Devuelve la fecha actual en formato ISO (yyyy-mm-dd) para inputs type="date".
     */
    function obtenerFechaActualIso() {
        const hoy = new Date();
        const offset = hoy.getTimezoneOffset();
        const ajustada = new Date(hoy.getTime() - offset * 60 * 1000);
        return ajustada.toISOString().split('T')[0];
    }

    /**
     * Muestra u oculta los campos exclusivos para gastos de personal.
     * @param {boolean} visible
     */
    function mostrarCamposPersonal(visible) {
        if (!rolGroup || !usuarioGroup || !rolSelect || !usuarioSelect) {
            return;
        }
        rolGroup.classList.toggle('d-none', !visible);
        usuarioGroup.classList.toggle('d-none', !visible);
        rolSelect.required = visible;
        usuarioSelect.required = visible;
        rolSelect.disabled = !visible || rolSelect.options.length <= 1;
        usuarioSelect.disabled = !visible;

        if (!visible) {
            rolSelect.value = '';
            usuarioSelect.innerHTML = '<option value="">Seleccionar usuario</option>';
            usuarioSelect.disabled = true;
        }
    }

    /**
     * Muestra u oculta los campos para detallar el tipo de servicio.
     * @param {boolean} visible
     */
    function mostrarCamposServicio(visible) {
        if (!servicioGroup || !servicioSelect) {
            return;
        }
        servicioGroup.classList.toggle('d-none', !visible);
        servicioSelect.required = visible;
        servicioSelect.disabled = !visible;
        if (!visible) {
            servicioSelect.value = '';
        }
    }

    /**
     * Ajusta automáticamente la descripción según el tipo y los datos seleccionados.
     */
    function actualizarDescripcion() {
        if (!descripcionInput) return;

        const tipo = tipoSelect ? tipoSelect.value : '';
        let descripcion = '';

        if (!tipo) {
            descripcionInput.value = '';
            return;
        }

        if (tipo === 'Personal') {
            const usuario = usuarioSelect ? usuarioSelect.value : '';
            descripcion = usuario ? `Pago de personal - ${usuario}` : 'Pago de personal';
        } else if (tipo === 'Servicios') {
            const servicio = servicioSelect ? servicioSelect.value : '';
            const servicioTexto = servicio ? obtenerTextoSeleccion(servicioSelect) : '';
            descripcion = servicio ? `Pago de servicio-${servicio}` : (servicioTexto ? `Pago de servicio - ${servicioTexto.toLowerCase()}` : 'Pago de servicio');
        } else if (tipo === 'Alquiler e Infraestructura') {
            descripcion = 'Gasto de alquiler e infraestructura';
        } else if (tipo === 'Suministros') {
            descripcion = 'Gasto de suministros operativos';
        } else if (tipo === 'Transporte y Logística') {
            descripcion = 'Gasto de transporte y logística';
        } else if (tipo === 'Otros') {
            descripcion = 'Otros gastos operativos';
        } else {
            descripcion = `Gasto de ${tipo.toLowerCase()}`;
        }

        descripcionInput.value = descripcion;
    }

    /**
     * Llena el select de usuarios según el rol seleccionado.
     * @param {string} rolNombre
     */
    function poblarUsuariosPorRol(rolNombre) {
        if (!usuarioSelect) return;
        usuarioSelect.innerHTML = '<option value="">Seleccionar usuario</option>';
        const usuarios = rolesUsuarios.get(rolNombre) || [];

        usuarios.forEach((usuario) => {
            const option = document.createElement('option');
            option.value = usuario;
            option.textContent = usuario;
            usuarioSelect.appendChild(option);
        });

        usuarioSelect.disabled = usuarios.length === 0;
        if (usuarios.length === 0) {
            usuarioSelect.value = '';
        }

        actualizarDescripcion();
    }

    /**
     * Carga los roles disponibles para gastos de personal, evitando múltiples peticiones.
     */
    async function cargarRolesPersonal() {
        if (rolesCargados || !rolSelect) {
            return;
        }
        const token = getToken();
        if (!token) return;

        try {
            const response = await fetch(ROLES_API, {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            if (!response.ok) {
                handleApiError(response);
                return;
            }

            const roles = await response.json();
            rolSelect.innerHTML = '<option value="">Seleccionar rol</option>';
            rolesUsuarios.clear();

            roles
                .filter((rol) => ROLES_VALIDOS.includes((rol.nombre || '').toUpperCase()))
                .sort((a, b) => a.nombre.localeCompare(b.nombre))
                .forEach((rol) => {
                    const nombre = (rol.nombre || '').toUpperCase();
                    rolesUsuarios.set(nombre, rol.usuarios || []);
                    const option = document.createElement('option');
                    option.value = nombre;
                    option.textContent = formatearNombreRol(nombre);
                    rolSelect.appendChild(option);
                });

            rolesCargados = true;
            rolSelect.disabled = rolSelect.options.length <= 1;
        } catch (error) {
            console.error('Error al cargar roles para gastos de personal:', error);
            alert('No se pudo obtener la lista de roles para el gasto de personal.');
        }
    }

    /**
     * Aplica los filtros activos y devuelve los gastos resultantes.
     */
    function aplicarFiltros() {
        if (!Array.isArray(gastosCache)) return [];

        const termino = buscarInput ? buscarInput.value.trim().toLowerCase() : '';
        const estadoSeleccionado = filtroEstadoSelect ? filtroEstadoSelect.value : '';
        const tipoSeleccionado = filtroTipoSelect ? filtroTipoSelect.value : '';
        const fechaInicio = fechaInicioInput && fechaInicioInput.value ? new Date(fechaInicioInput.value) : null;
        const fechaFin = fechaFinInput && fechaFinInput.value ? new Date(fechaFinInput.value) : null;

        return gastosCache.filter((gasto) => {
            const descripcion = (gasto.descripcion || '').toLowerCase();
            const tipo = (gasto.tipo || '').toLowerCase();
            const estado = (gasto.estado || '').toLowerCase();
            const fecha = gasto.fecha ? new Date(gasto.fecha) : null;

            const coincideBusqueda = !termino || descripcion.includes(termino) || String(gasto.id).includes(termino);
            const coincideEstado = !estadoSeleccionado || estado === estadoSeleccionado.toLowerCase();
            const coincideTipo = !tipoSeleccionado || tipo === tipoSeleccionado.toLowerCase();
            const coincideFechaInicio = !fechaInicio || (fecha && fecha >= fechaInicio);
            const coincideFechaFin = !fechaFin || (fecha && fecha <= fechaFin);

            return coincideBusqueda && coincideEstado && coincideTipo && coincideFechaInicio && coincideFechaFin;
        });
    }

    function formatearMonto(valor) {
        const numero = typeof valor === 'number' ? valor : parseFloat(valor);
        if (Number.isFinite(numero)) {
            return numero.toFixed(2);
        }
        return '0.00';
    }

    function actualizarResumen(filtrados) {
        if (!resumenElement) return;
        const totalRegistros = Array.isArray(gastosCache) ? gastosCache.length : 0;
        const visibles = filtrados.length;
        if (totalRegistros === 0) {
            resumenElement.textContent = 'Sin registros para mostrar';
        } else if (visibles === totalRegistros) {
            resumenElement.textContent = `Mostrando ${visibles} registro${visibles === 1 ? '' : 's'} en total`;
        } else {
            resumenElement.textContent = `Mostrando ${visibles} de ${totalRegistros} registro${totalRegistros === 1 ? '' : 's'}`;
        }
    }

    function actualizarKpis(gastos) {
        const kpis = {
            'personal': document.getElementById('gastoPersonal'),
            'alquiler e infraestructura': document.getElementById('gastoAlquilerInfraestructura'),
            'servicios': document.getElementById('gastoServicios'),
            'suministros': document.getElementById('gastoSuministros'),
            'transporte y logística': document.getElementById('gastoTransporte'),
            'otros': document.getElementById('gastoOtros')
        };

        const acumulados = {
            'personal': 0,
            'alquiler e infraestructura': 0,
            'servicios': 0,
            'suministros': 0,
            'transporte y logística': 0,
            'otros': 0
        };

        gastos.forEach((gasto) => {
            const tipo = (gasto.tipo || '').toLowerCase();
            const monto = typeof gasto.monto === 'number' ? gasto.monto : parseFloat(gasto.monto);
            const valor = Number.isFinite(monto) ? monto : 0;

            if (tipo in acumulados) {
                acumulados[tipo] += valor;
            } else {
                acumulados.otros += valor;
            }
        });

        Object.entries(acumulados).forEach(([clave, total]) => {
            const elemento = kpis[clave];
            if (elemento) {
                elemento.textContent = `S/ ${total.toFixed(2)}`;
            }
        });
    }

    function pintarTabla() {
        if (!tablaBody) return;
        tablaBody.innerHTML = '';

        const filtrados = aplicarFiltros();
        actualizarResumen(filtrados);

        if (!filtrados.length) {
            const fila = document.createElement('tr');
            const celda = document.createElement('td');
            celda.colSpan = 7;
            celda.className = 'text-center text-muted py-3';
            celda.textContent = 'No se encontraron gastos con los criterios seleccionados.';
            fila.appendChild(celda);
            tablaBody.appendChild(fila);
            return;
        }

        filtrados.forEach((gasto) => {
            const row = document.createElement('tr');
            const estadoClase = obtenerClaseEstado(gasto.estado);
            const montoFormateado = formatearMonto(gasto.monto);

            row.innerHTML = `
                <td>${gasto.id}</td>
                <td>${gasto.descripcion || '—'}</td>
                <td>${gasto.tipo || '—'}</td>
                <td>${gasto.fecha || '—'}</td>
                <td class="text-end">S/ ${montoFormateado}</td>
                <td><span class="badge ${estadoClase}">${gasto.estado || 'Pendiente'}</span></td>
                <td class="col-acciones">
                    <div class="btn-group btn-group-sm" role="group">
                        <button class="btn btn-primary btn-editar" data-gasto-id="${gasto.id}" title="Editar"><i class="bi bi-pencil"></i></button>
                        <button class="btn btn-info btn-estado" data-gasto-id="${gasto.id}" title="Cambiar estado"><i class="bi bi-arrow-repeat"></i></button>
                        <button class="btn btn-danger btn-eliminar" data-gasto-id="${gasto.id}" title="Eliminar"><i class="bi bi-trash"></i></button>
                    </div>
                </td>
            `;
            tablaBody.appendChild(row);
        });
    }

    /**
     * Obtiene todos los gastos desde la API y actualiza la vista.
     */
    async function renderizarTabla() {
        const token = getToken();
        if (!token) return;

        try {
            const response = await fetch(API_URL, {
                headers: { 'Authorization': `Bearer ${token}` }
            });

            if (!response.ok) {
                handleApiError(response);
                return;
            }

            const gastos = await response.json();
            gastosCache = Array.isArray(gastos) ? gastos : [];
            actualizarKpis(gastosCache);
            pintarTabla();
        } catch (error) {
            console.error('Error al renderizar la tabla de gastos:', error);
            alert('No se pudieron cargar los datos de gastos.');
        }
    }

    /**
     * Devuelve la clase de Bootstrap para el badge de estado.
     */
    function obtenerClaseEstado(estado) {
        switch (estado) {
            case 'Completado': return 'bg-success';
            case 'Pendiente': return 'bg-warning text-dark';
            default: return 'bg-secondary';
        }
    }

    // --- SECCIÓN 2: MANEJO DE EVENTOS ---

    // Delegación de eventos para los botones de acción en la tabla
    tablaBody.addEventListener('click', async (event) => {
        const target = event.target.closest('button');
        if (!target) return;

        const gastoId = target.dataset.gastoId;

        if (target.classList.contains('btn-editar')) {
            await editarGasto(gastoId);
        } else if (target.classList.contains('btn-estado')) {
            await cambiarEstado(gastoId);
        } else if (target.classList.contains('btn-eliminar')) {
            await eliminarGasto(gastoId);
        }
    });

    if (tipoSelect) {
        tipoSelect.addEventListener('change', async () => {
            const valor = tipoSelect.value;
            const esPersonal = valor === 'Personal';
            const esServicio = valor === 'Servicios';

            if (esPersonal) {
                await cargarRolesPersonal();
            }

            mostrarCamposPersonal(esPersonal);
            mostrarCamposServicio(esServicio);

            if (!esPersonal && usuarioSelect) {
                usuarioSelect.value = '';
            }

            if (!esServicio && servicioSelect) {
                servicioSelect.value = '';
            }

            actualizarDescripcion();
        });
    }

    if (rolSelect) {
        rolSelect.addEventListener('change', () => {
            poblarUsuariosPorRol(rolSelect.value);
        });
    }

    if (usuarioSelect) {
        usuarioSelect.addEventListener('change', actualizarDescripcion);
    }

    if (servicioSelect) {
        servicioSelect.addEventListener('change', () => {
            actualizarDescripcion();
        });
    }

    if (buscarInput) {
        buscarInput.addEventListener('input', () => {
            pintarTabla();
        });
    }

    if (filtroEstadoSelect) {
        filtroEstadoSelect.addEventListener('change', () => {
            pintarTabla();
        });
    }

    if (filtroTipoSelect) {
        filtroTipoSelect.addEventListener('change', () => {
            pintarTabla();
        });
    }

    if (fechaInicioInput) {
        fechaInicioInput.addEventListener('change', () => {
            pintarTabla();
        });
    }

    if (fechaFinInput) {
        fechaFinInput.addEventListener('change', () => {
            pintarTabla();
        });
    }

    if (limpiarFiltrosBtn) {
        limpiarFiltrosBtn.addEventListener('click', () => {
            if (buscarInput) buscarInput.value = '';
            if (filtroEstadoSelect) filtroEstadoSelect.value = '';
            if (filtroTipoSelect) filtroTipoSelect.value = '';
            if (fechaInicioInput) fechaInicioInput.value = '';
            if (fechaFinInput) fechaFinInput.value = '';
            pintarTabla();
        });
    }

    function generarCsv(datos) {
        const encabezados = ['ID', 'Descripción', 'Tipo', 'Fecha', 'Monto', 'Estado', 'Registrado por'];
        const filas = datos.map(gasto => ([
            gasto.id,
            `"${(gasto.descripcion || '').replace(/"/g, '""')}"`,
            gasto.tipo || '',
            gasto.fecha || '',
            formatearMonto(gasto.monto),
            gasto.estado || '',
            gasto.registradoPor || ''
        ]));
        const contenido = [encabezados.join(','), ...filas.map(fila => fila.join(','))].join('\n');
        return new Blob([contenido], { type: 'text/csv;charset=utf-8;' });
    }

    if (btnReporteGastos) {
        btnReporteGastos.addEventListener('click', () => {
            const datos = aplicarFiltros();
            if (!datos.length) {
                alert('No hay datos filtrados para generar el reporte.');
                return;
            }
            const blob = generarCsv(datos);
            const url = URL.createObjectURL(blob);
            const enlace = document.createElement('a');
            enlace.href = url;
            const timestamp = new Date().toISOString().split('T')[0];
            enlace.download = `reporte_gastos_${timestamp}.csv`;
            document.body.appendChild(enlace);
            enlace.click();
            document.body.removeChild(enlace);
            URL.revokeObjectURL(url);
        });
    }

    mostrarCamposPersonal(false);
    mostrarCamposServicio(false);
    actualizarDescripcion();

    // Evento para el formulario de creación de gastos
    form.addEventListener('submit', async (event) => {
        event.preventDefault();
        const token = getToken();

        if (!form.checkValidity()) {
            form.reportValidity();
            return;
        }

        actualizarDescripcion();

        const descripcion = descripcionInput ? descripcionInput.value.trim() : '';
        const tipoSeleccionado = tipoSelect ? tipoSelect.value : '';
        const fecha = fechaInput ? fechaInput.value : '';
        const montoValor = parseFloat(montoInput ? montoInput.value : '0');
        const esGastoPersonal = tipoSeleccionado === 'Personal';
        const esServicio = tipoSeleccionado === 'Servicios';

        if (Number.isNaN(montoValor)) {
            alert('Ingrese un monto válido para el gasto.');
            return;
        }

        if (esGastoPersonal) {
            if (!rolSelect || !usuarioSelect) {
                alert('No se pudo cargar la información de usuarios para gastos de personal.');
                return;
            }
            if (!rolSelect.value || !usuarioSelect.value) {
                alert('Seleccione el rol y el usuario al que se aplicará el gasto de personal.');
                return;
            }
        }

        if (esServicio) {
            if (!servicioSelect || !servicioSelect.value) {
                alert('Seleccione el tipo de servicio pagado.');
                return;
            }
        }

        const esEdicion = Boolean(gastoIdInput && gastoIdInput.value);

        const payloadCrear = {
            descripcion,
            tipo: tipoSeleccionado,
            fecha,
            monto: montoValor,
            estado: 'Pendiente'
        };

        if (esGastoPersonal) {
            payloadCrear.rolDestino = rolSelect.value;
            payloadCrear.usuarioDestino = usuarioSelect.value;
        }

        if (esServicio && servicioSelect) {
            payloadCrear.servicio = servicioSelect.value;
        }

        let url = API_URL;
        let method = 'POST';
        let payload = payloadCrear;

        if (esEdicion) {
            url = `${API_URL}/${gastoIdInput.value}`;
            method = 'PUT';
            payload = {
                descripcion,
                tipo: tipoSeleccionado,
                fecha,
                monto: montoValor
            };
        }

        try {
            const response = await fetch(url, {
                method,
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify(payload)
            });

            if (!response.ok) {
                if (response.status === 401 || response.status === 403) {
                    handleApiError(response);
                    return;
                }
                let mensaje = esEdicion ? 'No se pudo actualizar el gasto.' : 'No se pudo registrar el gasto.';
                try {
                    const data = await response.json();
                    if (data && data.error) {
                        mensaje = data.error;
                    }
                } catch (err) {
                    console.error('No se pudo leer la respuesta de error al crear gasto:', err);
                }
                throw new Error(mensaje);
            }

            alert(esEdicion ? 'Gasto actualizado con éxito.' : 'Nuevo gasto registrado con éxito.');
            modal.hide();
            if (gastoIdInput) {
                gastoIdInput.value = '';
            }
            await renderizarTabla();
        } catch (error) {
            console.error('Error al registrar o actualizar el gasto:', error);
            alert(error.message || (esEdicion ? 'No se pudo actualizar el gasto.' : 'No se pudo registrar el gasto.'));
        }
    });

    // Evento para el botón "Nuevo Gasto" que resetea el modal
    btnNuevoGasto.addEventListener('click', () => {
        form.reset();
        modalTitle.textContent = 'Registrar Nuevo Gasto';
        mostrarCamposPersonal(false);
        mostrarCamposServicio(false);
        if (gastoIdInput) {
            gastoIdInput.value = '';
        }
        if (rolSelect) {
            rolSelect.disabled = rolSelect.options.length <= 1;
        }
        if (usuarioSelect) {
            usuarioSelect.disabled = true;
        }
        if (servicioSelect) {
            servicioSelect.disabled = true;
        }
        if (fechaInput) {
            fechaInput.value = obtenerFechaActualIso();
        }
        actualizarDescripcion();
    });

    // --- SECCIÓN 3: FUNCIONES DE ACCIÓN ---

    async function editarGasto(id) {
        const token = getToken();
        if (!token) {
            alert('Su sesión ha caducado. Inicie sesión nuevamente para editar gastos.');
            return;
        }

        try {
            const response = await fetch(`${API_URL}/${id}`, {
                headers: { 'Authorization': `Bearer ${token}` }
            });

            if (!response.ok) {
                handleApiError(response);
                throw new Error('No se pudo cargar la información del gasto seleccionado.');
            }

            const gasto = await response.json();

            form.reset();
            if (gastoIdInput) {
                gastoIdInput.value = gasto.id ?? '';
            }

            modalTitle.textContent = `Editar Gasto #${gasto.id ?? id}`;

            if (tipoSelect) {
                tipoSelect.value = gasto.tipo || '';
            }

            const esPersonal = gasto.tipo === 'Personal';
            const esServicio = gasto.tipo === 'Servicios';

            if (esPersonal) {
                await cargarRolesPersonal();
            }

            mostrarCamposPersonal(esPersonal);
            mostrarCamposServicio(esServicio);

            if (rolSelect) {
                rolSelect.disabled = rolSelect.options.length <= 1;
            }
            if (usuarioSelect) {
                usuarioSelect.disabled = esPersonal ? false : true;
            }
            if (servicioSelect) {
                servicioSelect.disabled = !esServicio;
            }

            if (fechaInput) {
                fechaInput.value = gasto.fecha || obtenerFechaActualIso();
            }

            if (montoInput) {
                const valor = gasto.monto != null ? Number(gasto.monto) : null;
                montoInput.value = Number.isFinite(valor) ? valor.toFixed(2) : '';
            }

            if (descripcionInput) {
                descripcionInput.value = gasto.descripcion || '';
            }

            actualizarDescripcion();
            if (descripcionInput && gasto.descripcion) {
                descripcionInput.value = gasto.descripcion;
            }

            modal.show();
        } catch (error) {
            console.error(`Error al cargar el gasto #${id} para edición:`, error);
            alert(error.message || 'No se pudo cargar el gasto seleccionado.');
        }
    }

    async function cambiarEstado(id) {
        const token = getToken();
        if (!confirm('¿Desea cambiar el estado de este gasto?')) return;

        try {
            // Primero, obtenemos el gasto actual para saber su estado
            let response = await fetch(`${API_URL}/${id}`, {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            if (!response.ok) { handleApiError(response); return; }
            const gasto = await response.json();

            // Cambiamos el estado
            const nuevoEstado = gasto.estado === 'Pendiente' ? 'Completado' : 'Pendiente';

            response = await fetch(`${API_URL}/${id}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify({ estado: nuevoEstado })
            });

            if (!response.ok) {
                if (response.status === 401 || response.status === 403) {
                    handleApiError(response);
                    return;
                }
                let mensaje = 'No se pudo actualizar el estado del gasto.';
                try {
                    const data = await response.json();
                    if (data && data.error) {
                        mensaje = data.error;
                    }
                } catch (err) {
                    console.error('No se pudo leer la respuesta de error al actualizar estado:', err);
                }
                throw new Error(mensaje);
            }

            alert('El estado del gasto ha sido actualizado.');
            await renderizarTabla();
        } catch (error) {
            console.error(`Error al cambiar estado del gasto #${id}:`, error);
            alert(error.message || 'No se pudo actualizar el estado del gasto.');
        }
    }

    async function eliminarGasto(id) {
        if (!confirm(`¿Estás seguro de que deseas eliminar el Gasto #${id}?`)) return;

        const token = getToken();
        try {
            const response = await fetch(`${API_URL}/${id}`, {
                method: 'DELETE',
                headers: { 'Authorization': `Bearer ${token}` }
            });

            if (!response.ok) {
                if (response.status === 401 || response.status === 403) {
                    handleApiError(response);
                    return;
                }
                let mensaje = 'No se pudo eliminar el gasto.';
                try {
                    const data = await response.json();
                    if (data && data.error) {
                        mensaje = data.error;
                    }
                } catch (err) {
                    console.error('No se pudo leer la respuesta de error al eliminar gasto:', err);
                }
                throw new Error(mensaje);
            }

            alert(`Gasto #${id} eliminado con éxito.`);
            await renderizarTabla();
        } catch (error) {
            console.error(`Error al eliminar el gasto #${id}:`, error);
            alert(error.message || 'No se pudo eliminar el gasto.');
        }
    }

    // --- INICIALIZACIÓN ---
    renderizarTabla();
});
