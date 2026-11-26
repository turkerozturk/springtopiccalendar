(() => {
    const list = document.getElementById('settings-list');
    const rows = [...list.querySelectorAll('.setting-row')].map(node => ({
        node,
        id: node.dataset.settingId,
        key: node.querySelector('.setting-key').textContent,
        value: node.querySelector('.setting-value').textContent
    }));
    const search = document.getElementById('setting-search');
    const sort = document.getElementById('setting-sort');
    const empty = document.getElementById('settings-empty');
    const count = document.getElementById('setting-count');
    const feedback = document.getElementById('settings-feedback');
    const collator = new Intl.Collator(document.documentElement.lang, { sensitivity: 'base', numeric: true });
    const feedbackKey = 'dtt-settings-feedback:' + location.pathname;
    const fold = value => value.toLowerCase().normalize('NFD').replace(/\p{M}/gu, '').replace(/ı/g, 'i');

    function refreshList(updateUrl = true) {
        const query = fold(search.value.trim());
        const order = {
            key: (a, b) => collator.compare(a.key, b.key),
            'key-desc': (a, b) => collator.compare(b.key, a.key),
            value: (a, b) => collator.compare(a.value, b.value) || collator.compare(a.key, b.key),
            'id-desc': (a, b) => Number(b.id) - Number(a.id)
        }[sort.value];
        rows.sort(order);

        let matches = 0;
        for (const row of rows) {
            const visible = fold(row.key).includes(query) || fold(row.value).includes(query);
            row.node.hidden = !visible;
            matches += Number(visible);
            list.append(row.node);
        }
        count.textContent = query ? `${matches} of ${rows.length} settings` : `${rows.length} settings`;
        empty.hidden = matches !== 0;
        empty.textContent = query ? 'No settings match this filter.' : 'No saved settings.';

        if (updateUrl) {
            const url = new URL(location.href);
            url.searchParams.delete('q');
            url.searchParams.delete('sort');
            if (search.value.trim()) url.searchParams.set('q', search.value);
            if (sort.value !== 'key') url.searchParams.set('sort', sort.value);
            history.replaceState(history.state, '', url);
        }
    }

    const params = new URLSearchParams(location.search);
    search.value = params.get('q') ?? '';
    if ([...sort.options].some(option => option.value === params.get('sort'))) {
        sort.value = params.get('sort');
    }
    refreshList(false);
    search.addEventListener('input', () => refreshList());
    sort.addEventListener('change', () => refreshList());
    document.getElementById('clear-setting-search').addEventListener('click', () => {
        search.value = '';
        refreshList();
        search.focus();
    });

    function showFeedback(message, variant) {
        feedback.textContent = message;
        feedback.className = `alert alert-${variant}`;
        feedback.hidden = false;
    }

    try {
        const message = sessionStorage.getItem(feedbackKey);
        if (message) {
            sessionStorage.removeItem(feedbackKey);
            showFeedback(message, 'success');
        }
    } catch {
        // Search and editing also work when storage is unavailable.
    }

    const modalElement = document.getElementById('setting-modal');
    const modal = new bootstrap.Modal(modalElement);
    const form = document.getElementById('setting-form');
    const id = document.getElementById('setting-id');
    const key = document.getElementById('setting-key');
    const value = document.getElementById('setting-value');
    const error = document.getElementById('setting-form-error');
    const saveButton = document.getElementById('save-setting');

    let editorTrigger;
    function openEditor(trigger, row, presetKey = '') {
        editorTrigger = trigger;
        feedback.hidden = true;
        id.value = row?.id ?? '';
        key.value = row?.key ?? presetKey;
        value.value = row?.value ?? '';
        error.hidden = true;
        document.getElementById('setting-modal-title').textContent = row ? 'Edit setting' : 'Add setting';
        modal.show();
    }

    async function post(url, fields) {
        const response = await fetch(url, { method: 'POST', body: fields });
        if (response.redirected) throw new Error('Sign in again before changing settings.');
        if (!response.ok) {
            const isPlainText = response.headers.get('Content-Type')?.startsWith('text/plain');
            throw new Error((isPlainText ? await response.text() : '') || 'Could not update the settings.');
        }
    }

    function reloadWithFeedback(message) {
        try {
            sessionStorage.setItem(feedbackKey, message);
        } catch {
            // Reload still displays the saved settings.
        }
        location.reload();
    }

    modalElement.addEventListener('shown.bs.modal', () => (key.value ? value : key).focus());
    modalElement.addEventListener('hidden.bs.modal', () => editorTrigger?.focus());
    document.getElementById('add-setting').addEventListener('click', event => openEditor(event.currentTarget));
    document.querySelectorAll('[data-setting-key]').forEach(button => {
        button.addEventListener('click', () => {
            const preset = button.dataset.settingKey;
            openEditor(button, rows.find(row => row.key === preset), preset);
        });
    });

    form.addEventListener('submit', async event => {
        event.preventDefault();
        error.hidden = true;
        saveButton.disabled = true;
        try {
            await post(form.action, new URLSearchParams(new FormData(form)));
            reloadWithFeedback('Setting saved.');
        } catch (failure) {
            error.textContent = failure.message;
            error.hidden = false;
            saveButton.disabled = false;
        }
    });

    list.addEventListener('click', async event => {
        const row = rows.find(item => item.node === event.target.closest('.setting-row'));
        if (!row) return;
        const editButton = event.target.closest('[data-edit-setting]');
        if (editButton) {
            openEditor(editButton, row);
            return;
        }
        const button = event.target.closest('[data-delete-setting]');
        if (!button || !confirm(`Delete the setting “${row.key}”?`)) return;

        const fields = new URLSearchParams(new FormData(form));
        for (const field of ['id', 'key', 'value']) fields.delete(field);
        feedback.hidden = true;
        button.disabled = true;
        try {
            await post(button.dataset.deleteSetting, fields);
            reloadWithFeedback('Setting deleted.');
        } catch (failure) {
            showFeedback(failure.message, 'danger');
            button.disabled = false;
        }
    });
})();
