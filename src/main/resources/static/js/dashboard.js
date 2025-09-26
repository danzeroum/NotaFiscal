(function () {
    'use strict';

    const state = {
        currentPage: 0,
        totalPages: 0,
        selectedFile: null,
        traceId: generateTraceId()
    };

    const elements = {
        mockIndicator: document.getElementById('mockIndicator'),
        traceIdSpan: document.getElementById('traceId'),
        uploadArea: document.getElementById('uploadArea'),
        fileInput: document.getElementById('fileInput'),
        uploadBtn: document.getElementById('uploadBtn'),
        batchesList: document.getElementById('batchesList'),
        prevPage: document.getElementById('prevPage'),
        nextPage: document.getElementById('nextPage'),
        pageInfo: document.getElementById('pageInfo'),
        totalProcessed: document.getElementById('totalProcessed'),
        successRate: document.getElementById('successRate'),
        p95Latency: document.getElementById('p95Latency'),
        healthStatus: document.getElementById('healthStatus')
    };

    function init() {
        elements.traceIdSpan.textContent = state.traceId;
        setupListeners();
        loadBatches();
        loadMetrics();
        checkHealth();
        setInterval(loadMetrics, 30000);
    }

    function setupListeners() {
        elements.uploadArea.addEventListener('click', () => elements.fileInput.click());
        elements.uploadArea.addEventListener('dragover', handleDragOver);
        elements.uploadArea.addEventListener('dragleave', handleDragLeave);
        elements.uploadArea.addEventListener('drop', handleDrop);

        elements.fileInput.addEventListener('change', handleFileSelect);
        elements.uploadBtn.addEventListener('click', uploadFile);

        elements.prevPage.addEventListener('click', () => changePage(-1));
        elements.nextPage.addEventListener('click', () => changePage(1));
    }

    function handleDragOver(event) {
        event.preventDefault();
        event.stopPropagation();
        elements.uploadArea.classList.add('dragover');
    }

    function handleDragLeave(event) {
        event.preventDefault();
        event.stopPropagation();
        elements.uploadArea.classList.remove('dragover');
    }

    function handleDrop(event) {
        event.preventDefault();
        event.stopPropagation();
        elements.uploadArea.classList.remove('dragover');

        const files = event.dataTransfer.files;
        if (files.length > 0) {
            handleFile(files[0]);
        }
    }

    function handleFileSelect(event) {
        const files = event.target.files;
        if (files.length > 0) {
            handleFile(files[0]);
        }
    }

    function handleFile(file) {
        if (!file.name.endsWith('.zip')) {
            alert('Por favor, selecione um arquivo ZIP');
            return;
        }

        state.selectedFile = file;
        elements.uploadArea.innerHTML = `
            <p>Arquivo selecionado:</p>
            <strong>${file.name}</strong>
            <p>${formatFileSize(file.size)}</p>
        `;
        elements.uploadBtn.disabled = false;
    }

    async function uploadFile() {
        if (!state.selectedFile) {
            return;
        }

        const formData = new FormData();
        formData.append('file', state.selectedFile);

        elements.uploadBtn.disabled = true;
        elements.uploadBtn.textContent = 'Enviando...';

        try {
            const response = await fetch('/api/v1/batches', {
                method: 'POST',
                headers: { 'X-Trace-Id': state.traceId },
                body: formData
            });

            if (response.headers.get('X-BTF-Mock') === 'true') {
                elements.mockIndicator.style.display = 'block';
            }

            if (response.ok) {
                await response.json();
                showSuccess('Lote criado com sucesso!');
                resetUpload();
                state.currentPage = 0;
                loadBatches();
                loadMetrics();
            } else {
                const error = await response.json().catch(() => null);
                showError(error?.detail || 'Erro ao processar arquivo');
            }
        } catch (error) {
            showError('Erro de conexão');
        } finally {
            elements.uploadBtn.disabled = false;
            elements.uploadBtn.textContent = 'Enviar';
        }
    }

    async function loadBatches() {
        try {
            const response = await fetch(`/api/v1/batches?page=${state.currentPage}&size=10`, {
                headers: { 'X-Trace-Id': state.traceId }
            });
            if (!response.ok) {
                return;
            }
            if (response.headers.get('X-BTF-Mock') === 'true') {
                elements.mockIndicator.style.display = 'block';
            }
            const data = await response.json();
            renderBatches(data);
            updatePagination(data);
        } catch (error) {
            console.error('Erro ao carregar lotes', error);
        }
    }

    function renderBatches(data) {
        if (!data.content || data.content.length === 0) {
            elements.batchesList.innerHTML = '<p>Nenhum lote encontrado</p>';
            return;
        }

        elements.batchesList.innerHTML = data.content.map(batch => `
            <div class="batch-item">
                <div>
                    <strong>${batch.id}</strong><br>
                    <small>${formatDate(batch.receivedAt)}</small>
                </div>
                <div>
                    <span class="batch-status ${batch.status?.toLowerCase() || ''}">
                        ${batch.status}
                    </span>
                </div>
                <div>
                    Total: ${batch.stats?.invoicesTotal ?? 0}<br>
                    OK: ${batch.stats?.invoicesOk ?? 0}
                </div>
                <div>
                    <button type="button" onclick="downloadExcel('${batch.id}')">📥 Excel</button>
                </div>
            </div>
        `).join('');
    }

    function updatePagination(data) {
        state.currentPage = data.number ?? 0;
        state.totalPages = data.totalPages ?? 1;

        elements.pageInfo.textContent = `Página ${state.currentPage + 1} de ${state.totalPages}`;
        elements.prevPage.disabled = state.currentPage === 0;
        elements.nextPage.disabled = state.currentPage >= state.totalPages - 1;
    }

    function changePage(delta) {
        const next = state.currentPage + delta;
        if (next < 0 || next >= state.totalPages) {
            return;
        }
        state.currentPage = next;
        loadBatches();
    }

    async function loadMetrics() {
        try {
            const response = await fetch('/actuator/prometheus', {
                headers: { 'X-Trace-Id': state.traceId }
            });
            if (!response.ok) {
                return;
            }
            const text = await response.text();
            parseMetrics(text);
        } catch (error) {
            console.error('Erro ao carregar métricas', error);
        }
    }

    function parseMetrics(text) {
        const lines = text.split('\n');
        let success = 0;
        let errors = 0;
        let p95 = 0;

        for (const line of lines) {
            if (line.includes('nfe_processed_total{type="success"}')) {
                const match = line.match(/(\d+(?:\.\d+)?)/);
                if (match) {
                    success = Number(match[1]);
                }
            }
            if (line.includes('nfe_processed_total{type="error"}')) {
                const match = line.match(/(\d+(?:\.\d+)?)/);
                if (match) {
                    errors = Number(match[1]);
                }
            }
            if (line.includes('nfe_processing_time_bucket') && line.includes('0.95')) {
                const match = line.match(/(\d+(?:\.\d+)?)/g);
                if (match) {
                    p95 = Number(match.at(-1) ?? 0) * 1000;
                }
            }
        }

        const total = success + errors;
        elements.totalProcessed.textContent = total.toFixed(0);
        elements.successRate.textContent = total > 0
                ? `${((success / total) * 100).toFixed(1)}%`
                : '0%';
        elements.p95Latency.textContent = `${p95.toFixed(0)}ms`;
    }

    async function checkHealth() {
        try {
            const response = await fetch('/actuator/health', {
                headers: { 'X-Trace-Id': state.traceId }
            });
            if (!response.ok) {
                elements.healthStatus.textContent = '🔴 Down';
                return;
            }
            const data = await response.json();
            elements.healthStatus.textContent = data.status === 'UP' ? '🟢 OK' : '🔴 Down';
        } catch (error) {
            elements.healthStatus.textContent = '🔴 Down';
        }
    }

    function generateTraceId() {
        return 'xxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, c => {
            const r = Math.random() * 16 | 0;
            const v = c === 'x' ? r : (r & 0x3 | 0x8);
            return v.toString(16);
        });
    }

    function formatFileSize(bytes) {
        if (!bytes) {
            return '0 Bytes';
        }
        const units = ['Bytes', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(1024));
        return `${(bytes / Math.pow(1024, i)).toFixed(2)} ${units[i]}`;
    }

    function formatDate(value) {
        if (!value) {
            return '-';
        }
        return new Date(value).toLocaleString('pt-BR');
    }

    function resetUpload() {
        state.selectedFile = null;
        elements.fileInput.value = '';
        elements.uploadArea.innerHTML = '<p>Arraste arquivo ZIP aqui ou clique para selecionar</p>';
        elements.uploadBtn.disabled = true;
    }

    function showSuccess(message) {
        alert(`✅ ${message}`);
    }

    function showError(message) {
        alert(`❌ ${message}`);
    }

    window.downloadExcel = function (batchId) {
        window.location.href = `/api/v1/batches/${batchId}/export.xlsx`;
    };

    document.addEventListener('DOMContentLoaded', init);
})();
