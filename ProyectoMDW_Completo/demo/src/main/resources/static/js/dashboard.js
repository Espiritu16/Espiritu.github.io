const currencyFormatter = new Intl.NumberFormat('es-PE', {
  style: 'currency',
  currency: 'PEN',
  minimumFractionDigits: 2
});

const numberFormatter = new Intl.NumberFormat('es-PE');
const MESES = ['Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio', 'Julio', 'Agosto', 'Setiembre', 'Octubre', 'Noviembre', 'Diciembre'];

document.addEventListener('DOMContentLoaded', () => {
  if (!document.querySelector('.dashboard')) {
    return;
  }

  const API_BASE = '/api/dashboard';
  const charts = {};

  const elements = {
    resumenIngresosDia: document.getElementById('resumenIngresosDia'),
    resumenVentasPendientes: document.getElementById('resumenVentasPendientes'),
    kpiVentasDia: document.getElementById('kpiVentasDia'),
    kpiIngresosDia: document.getElementById('kpiIngresosDia'),
    kpiVentasPendientes: document.getElementById('kpiVentasPendientes'),
    kpiIngresosMes: document.getElementById('kpiIngresosMes'),
    kpiTicketPromedioMes: document.getElementById('kpiTicketPromedioMes'),
    kpiProductosVendidosMes: document.getElementById('kpiProductosVendidosMes'),
    ventasRecientesBody: document.getElementById('ventasRecientesBody')
  };

  const selects = {
    ventas: document.getElementById('selVentasMes'),
    gastos: document.getElementById('selGastosMes')
  };

  const ahora = new Date();
  const anioActual = ahora.getFullYear();
  const mesActual = ahora.getMonth();

  configurarSelect(selects.ventas, mesActual, actualizarSerieVentas);
  configurarSelect(selects.gastos, mesActual, actualizarSerieGastos);

  initDashboard();

  async function initDashboard() {
    await Promise.allSettled([
      cargarResumen(),
      cargarVentasRecientes(),
      actualizarSerieVentas(),
      actualizarSerieGastos(),
      cargarCategorias(),
      cargarMetodosPago()
    ]);
  }

  function configurarSelect(select, valor, handler) {
    if (!select) return;
    if ([...select.options].some(opt => opt.value === String(valor))) {
      select.value = String(valor);
    }
    select.addEventListener('change', () => handler());
  }

  async function cargarResumen() {
    const data = await fetchJson(`${API_BASE}/resumen`);
    if (!data) return;

    updateText(elements.resumenIngresosDia, currencyFormatter.format(toNumber(data.ingresosDia)));
    updateText(elements.resumenVentasPendientes, numberFormatter.format(toNumber(data.ventasPendientes)));
    updateText(elements.kpiVentasDia, numberFormatter.format(toNumber(data.ventasDia)));
    updateText(elements.kpiIngresosDia, currencyFormatter.format(toNumber(data.ingresosDia)));
    updateText(elements.kpiVentasPendientes, numberFormatter.format(toNumber(data.ventasPendientes)));
    updateText(elements.kpiIngresosMes, currencyFormatter.format(toNumber(data.ingresosMes)));
    updateText(elements.kpiTicketPromedioMes, currencyFormatter.format(toNumber(data.ticketPromedioMes)));
    updateText(elements.kpiProductosVendidosMes, numberFormatter.format(toNumber(data.productosVendidosMes)));
  }

  async function cargarVentasRecientes() {
    const tbody = elements.ventasRecientesBody;
    if (!tbody) return;

    const ventas = await fetchJson(`${API_BASE}/ventas-recientes`);
    tbody.innerHTML = '';

    if (!ventas || !ventas.length) {
      const fila = document.createElement('tr');
      const celda = document.createElement('td');
      celda.colSpan = 7;
      celda.className = 'text-center text-muted py-3';
      celda.textContent = 'Sin ventas registradas hoy.';
      fila.appendChild(celda);
      tbody.appendChild(fila);
      return;
    }

    ventas.forEach((venta) => {
      const fila = document.createElement('tr');
      fila.innerHTML = `
        <td>${venta.id}</td>
        <td>${venta.hora || '--:--'}</td>
        <td>${escapeHtml(venta.cliente || '—')}</td>
        <td>${escapeHtml(venta.vendedor || '—')}</td>
        <td><span class="badge bg-light text-dark">${escapeHtml(venta.metodoPago || 'Sin definir')}</span></td>
        <td class="text-end">${currencyFormatter.format(toNumber(venta.total))}</td>
        <td><span class="badge ${badgeEstado(venta.estado)}">${escapeHtml(venta.estado || '—')}</span></td>
      `;
      tbody.appendChild(fila);
    });
  }

  async function actualizarSerieVentas() {
    if (!document.getElementById('chartVentasMes')) {
      return;
    }
    if (!selects.ventas) {
      return cargarSerieVentasMensuales();
    }
    const mesSeleccionado = parseInt(selects.ventas.value, 10);
    if (Number.isNaN(mesSeleccionado)) {
      return;
    }
    const anio = anioParaMes(mesSeleccionado);
    const data = await fetchJson(`${API_BASE}/ventas-por-dia?year=${anio}&month=${mesSeleccionado + 1}`);
    const labels = data ? data.map(item => item.etiqueta) : [];
    const valores = data ? data.map(item => toNumber(item.total)) : [];

    renderChart('chartVentasMes', {
      type: 'bar',
      data: {
        labels,
        datasets: [{
          label: `Ventas ${nombreMes(mesSeleccionado)} ${anio}`,
          data: valores,
          backgroundColor: '#2563eb',
          borderRadius: 6
        }]
      },
      options: defaultChartOptions('Soles (S/)')
    });
  }

  async function actualizarSerieGastos() {
    if (!document.getElementById('chartGastosMes')) {
      return;
    }
    if (!selects.gastos) {
      return cargarSerieGastosMensuales();
    }
    const mesSeleccionado = parseInt(selects.gastos.value, 10);
    if (Number.isNaN(mesSeleccionado)) {
      return;
    }
    const anio = anioParaMes(mesSeleccionado);
    const data = await fetchJson(`${API_BASE}/gastos-por-dia?year=${anio}&month=${mesSeleccionado + 1}`);
    const labels = data ? data.map(item => item.etiqueta) : [];
    const valores = data ? data.map(item => toNumber(item.total)) : [];

    renderChart('chartGastosMes', {
      type: 'bar',
      data: {
        labels,
        datasets: [{
          label: `Gastos ${nombreMes(mesSeleccionado)} ${anio}`,
          data: valores,
          backgroundColor: '#f97316',
          borderRadius: 6
        }]
      },
      options: defaultChartOptions('Soles (S/)')
    });
  }

  async function cargarSerieVentasMensuales() {
    const ventas = await fetchJson(`${API_BASE}/ventas-mensuales`);
    const labels = ventas ? ventas.map(item => item.etiqueta) : [];
    const valores = ventas ? ventas.map(item => toNumber(item.total)) : [];

    renderChart('chartVentasMes', {
      type: 'line',
      data: {
        labels,
        datasets: [{
          label: 'Ventas (últimos meses)',
          data: valores,
          fill: true,
          borderColor: '#2563eb',
          backgroundColor: 'rgba(37, 99, 235, 0.15)',
          tension: 0.3,
          pointRadius: 4,
          pointBackgroundColor: '#2563eb'
        }]
      },
      options: defaultChartOptions('Soles (S/)')
    });
  }

  async function cargarSerieGastosMensuales() {
    const gastos = await fetchJson(`${API_BASE}/gastos-mensuales`);
    const labels = gastos ? gastos.map(item => item.etiqueta) : [];
    const valores = gastos ? gastos.map(item => toNumber(item.total)) : [];

    renderChart('chartGastosMes', {
      type: 'bar',
      data: {
        labels,
        datasets: [{
          label: 'Gastos (últimos meses)',
          data: valores,
          backgroundColor: '#f97316',
          borderRadius: 6
        }]
      },
      options: defaultChartOptions('Soles (S/)')
    });
  }

  async function cargarCategorias() {
    const categorias = await fetchJson(`${API_BASE}/top-categorias`);
    const labels = categorias && categorias.length
      ? categorias.map(item => item.categoria)
      : ['Sin datos'];
    const data = categorias && categorias.length
      ? categorias.map(item => toNumber(item.unidadesVendidas))
      : [0];

    renderChart('chartCategorias', {
      type: 'bar',
      data: {
        labels,
        datasets: [{
          label: 'Unidades vendidas',
          data,
          backgroundColor: labels.map((_, idx) => palette(idx)),
          borderRadius: 6
        }]
      },
      options: defaultChartOptions('Unidades')
    });
  }

  async function cargarMetodosPago() {
    const metodos = await fetchJson(`${API_BASE}/metodos-pago`);
    if (!document.getElementById('chartPagos')) {
      return;
    }

    const labels = metodos && metodos.length
      ? metodos.map(item => item.metodo)
      : [];
    const data = metodos && metodos.length
      ? metodos.map(item => toNumber(item.cantidad))
      : [];

    const total = data.reduce((acc, value) => acc + value, 0);

    if (!labels.length || total <= 0) {
      renderChart('chartPagos', {
        type: 'doughnut',
        data: {
          labels: ['Sin datos'],
          datasets: [{
            data: [1],
            backgroundColor: ['#e5e7eb']
          }]
        },
        options: {
          plugins: {
            legend: { display: false },
            tooltip: { enabled: false },
            title: { display: true, text: 'Sin datos disponibles' }
          },
          responsive: true,
          maintainAspectRatio: false,
          cutout: '58%'
        }
      });
      return;
    }

    renderChart('chartPagos', {
      type: 'doughnut',
      data: {
        labels,
        datasets: [{
          label: 'Ventas',
          data,
          backgroundColor: labels.map((_, idx) => palette(idx)),
          borderWidth: 0
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        cutout: '58%',
        plugins: {
          legend: {
            position: 'bottom'
          }
        }
      }
    });
  }

  async function fetchJson(url) {
    try {
      const response = await fetch(url, { headers: buildHeaders() });
      if (!response.ok) {
        console.error(`Error al consultar ${url}: ${response.status}`);
        return null;
      }
      return await response.json();
    } catch (error) {
      console.error(`Error al consultar ${url}:`, error);
      return null;
    }
  }

  function buildHeaders() {
    const headers = { 'Content-Type': 'application/json' };
    const token = sessionStorage.getItem('jwtToken');
    if (token) {
      headers.Authorization = `Bearer ${token}`;
    }
    return headers;
  }

  function renderChart(id, config) {
    const canvas = document.getElementById(id);
    if (!canvas || typeof Chart === 'undefined') {
      return;
    }
    if (charts[id]) {
      charts[id].destroy();
    }
    config.options = config.options || {};
    charts[id] = new Chart(canvas, config);
  }

  function defaultChartOptions(yLabel) {
    return {
      responsive: true,
      maintainAspectRatio: false,
      scales: {
        y: {
          beginAtZero: true,
          ticks: {
            callback: (value) => value >= 1000 ? `${(value / 1000).toFixed(1)}k` : value
          },
          title: yLabel ? { display: true, text: yLabel } : undefined
        }
      },
      plugins: {
        legend: { display: false }
      }
    };
  }

  function updateText(element, value) {
    if (element) {
      element.textContent = value;
    }
  }

  function toNumber(value) {
    if (value == null) return 0;
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : 0;
  }

  function escapeHtml(value) {
    if (value == null) return '';
    return value
      .toString()
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }

  function badgeEstado(estado = '') {
    const normalized = estado.toLowerCase();
    if (normalized.includes('pagada') || normalized.includes('completado')) {
      return 'bg-success-subtle text-success';
    }
    if (normalized.includes('pend') || normalized.includes('espera')) {
      return 'bg-warning-subtle text-warning';
    }
    if (normalized.includes('cancel')) {
      return 'bg-danger-subtle text-danger';
    }
    return 'bg-secondary-subtle text-secondary';
  }

  function palette(index) {
    const colors = [
      '#2563eb',
      '#22c55e',
      '#f97316',
      '#8b5cf6',
      '#0ea5e9',
      '#f43f5e'
    ];
    return colors[index % colors.length];
  }

  function nombreMes(indice) {
    return MESES[indice] || '';
  }

  function anioParaMes(mesSeleccionado) {
    if (mesSeleccionado > mesActual) {
      return anioActual - 1;
    }
    return anioActual;
  }
});
