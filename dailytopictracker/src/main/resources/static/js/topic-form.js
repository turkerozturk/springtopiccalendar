(() => {
    const fold = text => text.toLowerCase().normalize('NFD').replace(/\p{M}/gu, '').replace(/ı/g, 'i');

    function matches(text, query) {
        let from = 0;
        for (const character of query) {
            if (/\s/u.test(character)) continue;
            const position = text.indexOf(character, from);
            if (position === -1) return false;
            from = position + character.length;
        }
        return true;
    }

    function filterOptions(input, select) {
        const options = [...select.options].map(option => ({ option, name: fold(option.textContent) }));
        input.addEventListener('input', () => {
            const query = fold(input.value.trim());
            const selected = select.value;
            const visible = options.filter(({ option, name }) =>
                !option.value || option.value === selected || matches(name, query));
            select.replaceChildren(...visible.map(({ option }) => option));
            select.value = selected;
        });
    }

    filterOptions(document.getElementById('categoryFilter'), document.getElementById('category'));
    const image = document.getElementById('imageFileName');
    if (image) {
        filterOptions(document.getElementById('imageFilter'), image);
        image.addEventListener('change', () => {
            const preview = document.getElementById('imagePreview');
            preview.src = image.selectedOptions[0]?.dataset.previewUrl || preview.dataset.defaultUrl;
        });
    }

    for (const type of ['base', 'end']) {
        const picker = document.getElementById(type + 'DatePicker');
        const stored = document.getElementById(type + 'DateMillisYmd');
        picker.addEventListener('change', () => {
            const timestamp = new Date(picker.value + 'T00:00:00').getTime();
            stored.value = Number.isFinite(timestamp) ? String(timestamp) : '';
        });
        document.querySelector(`[data-clear-date="${type}"]`).addEventListener('click', () => {
            picker.value = '';
            stored.value = '';
        });
        document.querySelector(`[data-use-date="${type}"]`)?.addEventListener('click', event => {
            const timestamp = event.currentTarget.dataset.dateMillis;
            const date = new Date(Number(timestamp));
            const year = String(date.getFullYear()).padStart(4, '0');
            const month = String(date.getMonth() + 1).padStart(2, '0');
            const day = String(date.getDate()).padStart(2, '0');
            picker.value = `${year}-${month}-${day}`;
            stored.value = timestamp;
        });
    }

    if (typeof EasyMDE !== 'function') return;

    function insertLink(editor, imageLink) {
        const cm = editor.codemirror;
        const selection = cm.getSelection();
        if (/^https?:\/\/\S+$/i.test(selection)) {
            cm.replaceSelection(imageLink ? `![](${selection})` : `<${selection}>`);
        } else {
            const start = cm.indexFromPos(cm.getCursor('from'));
            const label = selection.replace(/[\\[\]]/g, '\\$&');
            const prefix = `${imageLink ? '!' : ''}[${label}](`;
            cm.replaceSelection(prefix + 'https://)');
            cm.setSelection(cm.posFromIndex(start + prefix.length), cm.posFromIndex(start + prefix.length + 8));
        }
        cm.focus();
    }

    const editor = new EasyMDE({
        element: document.getElementById('description'),
        autoDownloadFontAwesome: false,
        spellChecker: false,
        forceSync: true,
        minHeight: '15rem',
        renderingConfig: { singleLineBreaks: false, markedOptions: { sanitize: true } },
        toolbar: [
            'bold', 'italic', 'heading', '|', 'quote', 'unordered-list', 'ordered-list', '|',
            { name: 'link', action: editor => insertLink(editor, false), className: 'fa fa-link', title: 'Insert link' },
            { name: 'image', action: editor => insertLink(editor, true), className: 'fa fa-image', title: 'Insert image' },
            'preview', 'fullscreen', '|', 'guide'
        ]
    });
    const cm = editor.codemirror;
    const input = cm.getInputField();
    input.setAttribute('aria-label', 'Description (Markdown)');
    const shortcuts = cm.getOption('extraKeys');
    cm.setOption('extraKeys', { ...shortcuts, Tab: false, 'Shift-Tab': false });
})();
