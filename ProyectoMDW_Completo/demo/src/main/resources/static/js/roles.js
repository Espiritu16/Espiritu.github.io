const ROLES_API = '/api/roles';
const TOKEN_KEY = 'jwtToken';

let roles = [];
let rolEnEdicion = null;
let filtroRol = 'all';

const tablaBody = document.querySelector('#tablaRoles tbody');
const infoRegistros = document.getElementById('infoRegistros');
const formRol = document.getElementById('formRol');
const rolModalElement = document.getElementById('rolModal');
const rolModal = rolModalElement ? new bootstrap.Modal(rolModalElement) : null;
const modalTitle = document.getElementById('modalLabel');
const inputNombre = document.getElementById('nombreRol');
const inputDescripcion = document.getElementById('descripcionRol');
const inputRolId = document.getElementById('rolId');

function obtenerToken() {
  return sessionStorage.getItem(TOKEN_KEY);
}

async function fetchConToken(url, options = {}) {
  const headers = Object.assign({}, options.headers || {});
  headers['Content-Type'] = 'application/json';
  const token = obtenerToken();
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  return fetch(url, { ...options, headers });
}

function formatoFecha(fechaIso) {
  if (!fechaIso) {
    return '—';
  }
  try {
    const fecha = new Date(fechaIso);
    return fecha.toLocaleDateString();
  } catch (e) {
    return fechaIso;
  }
}

function mostrarMensaje(mensaje, tipo = 'info') {
  const toast = document.createElement('div');
  toast.className = `alert alert-${tipo}`;
  toast.textContent = mensaje;
  document.body.appendChild(toast);
  setTimeout(() => toast.remove(), 2500);
}

function renderRoles() {
  tablaBody.innerHTML = '';
  if (!roles.length) {
    const row = document.createElement('tr');
    const cell = document.createElement('td');
    cell.colSpan = 5;
    cell.className = 'text-center text-secondary py-3';
    cell.textContent = 'No hay roles registrados todavía.';
    row.appendChild(cell);
    tablaBody.appendChild(row);
    infoRegistros.textContent = 'Sin registros';
    return;
  }

  const filtrados = roles.filter((rol) => coincideConFiltro(rol.nombre));

  if (!filtrados.length) {
    const row = document.createElement('tr');
    const cell = document.createElement('td');
    cell.colSpan = 5;
    cell.className = 'text-center text-secondary py-3';
    cell.textContent = 'No hay roles que coincidan con este filtro.';
    row.appendChild(cell);
    tablaBody.appendChild(row);
    infoRegistros.textContent = `Sin registros para la pestaña seleccionada (Total: ${roles.length})`;
    return;
  }

  filtrados.forEach((rol) => {
    const row = document.createElement('tr');
    row.innerHTML = `
      <td class="fw-semibold">${rol.nombre}</td>
      <td>${rol.descripcion ? rol.descripcion : '—'}</td>
      <td class="text-center">${rol.usuariosAsignados}</td>
      <td>${formatoFecha(rol.fechaCreacion)}</td>
      <td class="d-flex gap-2">
        <button type="button" class="btn btn-sm btn-outline-secondary" data-accion="editar">Editar</button>
        <button type="button" class="btn btn-sm btn-outline-info" data-accion="permisos">Permisos</button>
      </td>
    `;
    row.querySelector('[data-accion="editar"]').addEventListener('click', () => abrirModalEdicion(rol));
    row.querySelector('[data-accion="permisos"]').addEventListener('click', () => mostrarMensaje('La gestión de permisos estará disponible próximamente.'));
    tablaBody.appendChild(row);
  });

  infoRegistros.textContent = `Mostrando ${filtrados.length} rol${filtrados.length === 1 ? '' : 'es'} (de ${roles.length})`;
}

async function cargarRoles() {
  try {
    const response = await fetchConToken(ROLES_API, { method: 'GET' });
    if (!response.ok) {
      throw new Error('No se pudieron cargar los roles');
    }
    roles = await response.json();
    renderRoles();
  } catch (error) {
    console.error('❌ Error al cargar roles:', error);
    mostrarMensaje(error.message, 'danger');
  }
}

function abrirModalCreacion() {
  rolEnEdicion = null;
  formRol.reset();
  inputRolId.value = '';
  modalTitle.textContent = 'Crear rol';
  if (rolModal) {
    rolModal.show();
  }
}

function abrirModalEdicion(rol) {
  rolEnEdicion = rol;
  inputRolId.value = rol.id;
  inputNombre.value = rol.nombre;
  inputDescripcion.value = rol.descripcion || '';
  modalTitle.textContent = 'Editar rol';
  if (rolModal) {
    rolModal.show();
  }
}

async function guardarRol(event) {
  event.preventDefault();

  const payload = {
    nombre: inputNombre.value,
    descripcion: inputDescripcion.value || null,
  };

  const rolId = inputRolId.value;
  const url = rolId ? `${ROLES_API}/${rolId}` : ROLES_API;
  const method = rolId ? 'PUT' : 'POST';

  try {
    const response = await fetchConToken(url, {
      method,
      body: JSON.stringify(payload),
    });

    if (!response.ok) {
      const errorBody = await response.json().catch(() => ({}));
      throw new Error(errorBody.error || 'No se pudo guardar el rol');
    }

    const mensaje = rolId ? 'Rol actualizado correctamente.' : 'Rol creado correctamente.';
    mostrarMensaje(mensaje, 'success');

    if (rolModal) {
      rolModal.hide();
    }
    formRol.reset();
    rolEnEdicion = null;
    await cargarRoles();
  } catch (error) {
    console.error('❌ Error al guardar rol:', error);
    mostrarMensaje(error.message, 'danger');
  }
}

function inicializarEventos() {
  const btnAgregar = document.getElementById('btnAgregarRol');
  if (btnAgregar) {
    btnAgregar.addEventListener('click', abrirModalCreacion);
  }

  if (formRol) {
    formRol.addEventListener('submit', guardarRol);
  }

  const btnExportar = document.getElementById('btnExportar');
  if (btnExportar) {
    btnExportar.addEventListener('click', () => mostrarMensaje('Función de exportación en desarrollo.'));
  }

  configurarTabsRol();
}

document.addEventListener('DOMContentLoaded', () => {
  inicializarEventos();
  cargarRoles();
});

function configurarTabsRol() {
  const tabs = document.querySelectorAll('#rolesTabs .nav-link');
  if (!tabs.length) {
    return;
  }

  tabs.forEach((tab) => {
    tab.addEventListener('click', () => {
      const rolFiltro = tab.dataset.rolTab || 'all';
      if (rolFiltro === filtroRol) {
        return;
      }
      filtroRol = rolFiltro;
      actualizarTabsRolUI();
      renderRoles();
    });
  });

  actualizarTabsRolUI();
}

function actualizarTabsRolUI() {
  const tabs = document.querySelectorAll('#rolesTabs .nav-link');
  tabs.forEach((tab) => {
    const rolFiltro = tab.dataset.rolTab || 'all';
    if (rolFiltro === filtroRol) {
      tab.classList.add('active');
    } else {
      tab.classList.remove('active');
    }
  });
}

function coincideConFiltro(nombreRol) {
  if (filtroRol === 'all') {
    return true;
  }

  const nombreNormalizado = (nombreRol || '').trim().toLowerCase();

  switch (filtroRol) {
    case 'administrador':
      return nombreNormalizado.includes('admin');
    case 'vendedor':
      return nombreNormalizado.includes('vendedor');
    case 'contador':
      return nombreNormalizado.includes('contador');
    default:
      return true;
  }
}
