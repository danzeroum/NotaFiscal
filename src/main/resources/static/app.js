const state = {
  page: 0,
  size: 5,
  sort: 'receivedAt,desc',
  loading: false,
};

const statusLabels = {
  RECEIVED: 'Recebido',
  PROCESSING: 'Processando',
  DONE: 'Concluído',
  FAILED: 'Falha',
};

const statusClasses = {
  RECEIVED: 'info',
  PROCESSING: 'warning',
  DONE: 'success',
  FAILED: 'danger',
};

const uploadForm = document.getElementById('uploadForm');
const uploadFeedback = document.getElementById('uploadFeedback');
const refreshButton = document.getElementById('refreshButton');
const batchesContainer = document.getElementById('batchesContainer');
const pagination = document.getElementById('pagination');
const batchTemplate = document.getElementById('batchTemplate');

function setFeedback(message, type = 'info') {
  uploadFeedback.textContent = message;
  uploadFeedback.className = `feedback ${type}`;
}

async function uploadBatch(event) {
  event.preventDefault();
  if (state.loading) return;

  const formData = new FormData(uploadForm);
  const file = formData.get('file');
  if (!file || file.size === 0) {
    setFeedback('Selecione um arquivo ZIP para enviar.', 'error');
    return;
  }

  state.loading = true;
  setFeedback('Enviando lote…', 'info');

  try {
    const response = await fetch('/batches', {
      method: 'POST',
      body: formData,
    });

    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(errorText || 'Falha ao enviar o lote.');
    }

    const created = await response.json();
    setFeedback(`Lote ${created.id} enviado com sucesso!`, 'success');
    uploadForm.reset();
    state.page = 0;
    await loadBatches();
  } catch (error) {
    console.error(error);
    setFeedback('Não foi possível enviar o lote. Verifique o arquivo e tente novamente.', 'error');
  } finally {
    state.loading = false;
  }
}

function formatDate(value) {
  if (!value) return '—';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return '—';
  }
  return date.toLocaleString('pt-BR');
}

function formatIssue(issue) {
  return `${issue.severity}: ${issue.detail}`;
}

function renderBatch(batch) {
  const fragment = batchTemplate.content.cloneNode(true);
  fragment.querySelector('.batch-id').textContent = batch.id;
  fragment
    .querySelector('.batch-dates')
    .textContent = `Recebido em ${formatDate(batch.receivedAt)} · Finalizado em ${formatDate(batch.completedAt)}`;
  const badge = fragment.querySelector('.badge');
  badge.dataset.status = batch.status;
  badge.classList.add(statusClasses[batch.status] || 'info');
  badge.textContent = statusLabels[batch.status] || batch.status;

  fragment.querySelector('.stat-total').textContent = batch.stats?.invoicesTotal ?? 0;
  fragment.querySelector('.stat-ok').textContent = batch.stats?.invoicesOk ?? 0;
  fragment.querySelector('.stat-issues').textContent = batch.stats?.invoicesWithIssues ?? 0;
  fragment.querySelector('.stat-p95').textContent = batch.stats?.processingMsP95 ?? 0;

  const issues = batch.issuesSummary && batch.issuesSummary.length > 0
    ? batch.issuesSummary.map(formatIssue)
    : ['Nenhum alerta disponível.'];

  const issueList = fragment.querySelector('.issue-list');
  issues.forEach((issue) => {
    const item = document.createElement('li');
    item.textContent = issue;
    issueList.appendChild(item);
  });

  return fragment;
}

function renderEmptyState() {
  const empty = document.createElement('p');
  empty.className = 'empty';
  empty.textContent = 'Nenhum lote encontrado. Faça o upload do primeiro arquivo ZIP para começar.';
  batchesContainer.innerHTML = '';
  batchesContainer.appendChild(empty);
}

function renderBatches(data) {
  batchesContainer.innerHTML = '';
  if (!data.content || data.content.length === 0) {
    renderEmptyState();
    return;
  }

  const fragment = document.createDocumentFragment();
  data.content.forEach((batch) => {
    fragment.appendChild(renderBatch(batch));
  });
  batchesContainer.appendChild(fragment);
}

function renderPagination(data) {
  pagination.innerHTML = '';
  if (data.totalPages <= 1) {
    return;
  }

  const prev = document.createElement('button');
  prev.textContent = 'Anterior';
  prev.className = 'secondary';
  prev.disabled = data.first;
  prev.addEventListener('click', () => {
    if (data.first) return;
    state.page = data.number - 1;
    loadBatches();
  });

  const next = document.createElement('button');
  next.textContent = 'Próxima';
  next.className = 'secondary';
  next.disabled = data.last;
  next.addEventListener('click', () => {
    if (data.last) return;
    state.page = data.number + 1;
    loadBatches();
  });

  const info = document.createElement('span');
  info.className = 'page-info';
  info.textContent = `Página ${data.number + 1} de ${data.totalPages}`;

  pagination.appendChild(prev);
  pagination.appendChild(info);
  pagination.appendChild(next);
}

async function loadBatches() {
  try {
    const params = new URLSearchParams({
      page: state.page.toString(),
      size: state.size.toString(),
      sort: state.sort,
    });
    const response = await fetch(`/batches?${params.toString()}`);
    if (!response.ok) {
      throw new Error('Erro ao buscar lotes.');
    }
    const data = await response.json();
    state.page = data.number ?? 0;
    renderBatches(data);
    renderPagination(data);
  } catch (error) {
    console.error(error);
    batchesContainer.innerHTML = '';
    const errorMessage = document.createElement('p');
    errorMessage.className = 'empty error';
    errorMessage.textContent = 'Não foi possível carregar os lotes. Atualize a página ou tente novamente mais tarde.';
    batchesContainer.appendChild(errorMessage);
  }
}

uploadForm.addEventListener('submit', uploadBatch);
refreshButton.addEventListener('click', () => {
  state.page = 0;
  loadBatches();
});

document.addEventListener('DOMContentLoaded', () => {
  loadBatches();
});
