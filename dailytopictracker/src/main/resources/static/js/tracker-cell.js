(() => {
    const table = document.getElementById("pivot-table");
    if (!table) return;

    const dialog = document.getElementById("statusModal");
    const modal = bootstrap.Modal.getOrCreateInstance(dialog);
    const title = document.getElementById("statusModalTitle");
    const form = document.getElementById("tracker-cell-note-form");
    const editor = document.getElementById("tracker-cell-note");
    const save = document.getElementById("tracker-cell-note-save");
    const newline = document.getElementById("tracker-cell-note-newline");
    const remove = document.getElementById("tracker-cell-delete");
    const message = document.getElementById("tracker-cell-message");
    const edit = dialog.querySelector(".edit-btn");
    const preview = dialog.querySelector(".note-read-btn");
    const statusButtons = dialog.querySelectorAll(".status-btn");
    const csrfName = document.querySelector('meta[name="_csrf_header"]').content;
    const csrfToken = document.querySelector('meta[name="_csrf"]').content;
    let active;

    function updateEditor() {
        const lines = editor.value.split("\n").length;
        editor.rows = Math.min(lines, 5);
        editor.wrap = lines === 1 ? "off" : "soft";
    }

    function showMessage(text, isError = false) {
        message.textContent = text;
        message.classList.toggle("text-danger", isError);
        message.hidden = !text;
    }

    function syncControls() {
        const locked = active.pending || active.closing;
        editor.disabled = !active.noteReady || locked;
        newline.disabled = editor.disabled;
        save.disabled = editor.disabled
            || (active.entryId ? editor.value === active.originalNote : !editor.value.trim());
        remove.hidden = !active.entryId;
        remove.disabled = locked;
        preview.disabled = !active.entryId || locked;
        edit.disabled = locked;
        statusButtons.forEach(button => { button.disabled = locked; });
    }

    function fillEditor(text) {
        editor.value = text;
        updateEditor();
        const end = editor.value.length;
        editor.setSelectionRange(end, end);
        editor.scrollTop = editor.scrollHeight;
        editor.scrollLeft = editor.scrollWidth;
    }

    function request(url, method = "GET", data) {
        return $.ajax({
            url,
            method,
            dataType: "json",
            cache: false,
            headers: { [csrfName]: csrfToken },
            contentType: "application/json",
            data: data === undefined ? undefined : JSON.stringify(data),
            timeout: 15000
        });
    }

    function entryUrl(path, params) {
        return path + "?" + new URLSearchParams({ ...params, returnPage: "pivottable" });
    }

    function paintCell(cell, entry, predicted = false) {
        const link = document.createElement("span");
        link.className = "fake-link";
        const { topicId, categoryId, dateLocal } = cell.dataset;
        cell.classList.remove("status-0", "status-1", "status-2", "status-3");

        if (entry) {
            cell.dataset.entryId = entry.id;
            cell.dataset.oldStatus = entry.status;
            cell.classList.add("status-" + entry.status);
            link.dataset.href = entryUrl(dialog.dataset.entryEditUrl + "/" + entry.id, { categoryId });
            const content = document.createElement("span");
            content.title = "Entry ID: " + entry.id;
            if (entry.hasNote) {
                content.className = "entry-with-content";
                content.innerHTML = '<i class="fa-brands fa-readme" style="color: blue;"></i>';
            } else {
                content.textContent = "\u00a0";
            }
            link.append(content);
            cell.replaceChildren(link, document.createElement("br"));
        } else {
            delete cell.dataset.entryId;
            delete cell.dataset.oldStatus;
            if (predicted) {
                cell.dataset.oldStatus = "3";
                cell.classList.add("status-3");
            }
            link.dataset.href = entryUrl(dialog.dataset.entryNewUrl, { topicId, dateYmd: dateLocal, categoryId });
            link.title = "Add New Entry";
            link.classList.add("add-new-entry");
            link.textContent = "+";
            cell.replaceChildren(link);
        }
        cell.dispatchEvent(new Event("tracker-status-updated", { bubbles: true }));
    }

    function cellData() {
        const { topicId, categoryId, dateLocal } = active.cell.dataset;
        return { entryId: active.entryId, topicId, categoryId, localDate: dateLocal };
    }

    function closeDialog() {
        active.closing = true;
        syncControls();
        if (active.shown) modal.hide();
    }

    async function mutate(action, errorText) {
        active.pending = true;
        syncControls();
        showMessage("");
        try {
            await action();
        } catch (error) {
            const detail = error.status === 409 ? "This cell has changed. Refresh the Tracker and try again." : errorText;
            showMessage(detail, true);
            return;
        } finally {
            active.pending = false;
            syncControls();
        }
        closeDialog();
    }

    table.addEventListener("click", async event => {
        const cell = event.target.closest(".pivot-table-entry-cell");
        if (!cell || active) return;

        const entryId = cell.dataset.entryId ?? null;
        const opened = { cell, entryId, noteReady: !entryId, originalNote: "",
            pending: false, shown: false, closing: false, focusAtEnd: false };
        active = opened;
        title.textContent = cell.closest("tr").dataset.trackerTopic || "Entry";
        title.title = title.textContent;
        fillEditor("");
        showMessage(entryId ? "Loading note…" : "");
        syncControls();
        modal.show();

        if (!entryId) return;
        try {
            const entry = await request(dialog.dataset.cellUrl + "/" + entryId);
            if (active !== opened || opened.closing) return;
            opened.noteReady = true;
            fillEditor(entry.content);
            opened.originalNote = editor.value;
            opened.focusAtEnd = Boolean(editor.value);
            showMessage("");
        } catch {
            if (active !== opened || opened.closing) return;
            showMessage("Could not load the note. Reopen the cell to try again.", true);
        }
        syncControls();
    });

    dialog.addEventListener("shown.bs.modal", () => {
        active.shown = true;
        if (active.closing) modal.hide();
    });
    dialog.addEventListener("hide.bs.modal", event => {
        if (active?.pending) {
            event.preventDefault();
        } else if (active) {
            active.closing = true;
        }
    });
    dialog.addEventListener("hidden.bs.modal", () => { active = null; });

    editor.addEventListener("focus", () => {
        if (!active?.focusAtEnd) return;
        const opened = active;
        opened.focusAtEnd = false;
        requestAnimationFrame(() => {
            if (active !== opened || document.activeElement !== editor) return;
            const end = editor.value.length;
            editor.setSelectionRange(end, end);
        });
    });
    editor.addEventListener("input", () => {
        updateEditor();
        syncControls();
    });
    newline.addEventListener("click", () => {
        editor.setRangeText("\n", editor.selectionStart, editor.selectionEnd, "end");
        updateEditor();
        syncControls();
        editor.focus();
    });

    form.addEventListener("submit", event => {
        event.preventDefault();
        if (!active || save.disabled) return;
        const { cell } = active;
        const payload = { ...cellData(), content: editor.value };
        mutate(async () => {
            const entry = await request(dialog.dataset.cellUrl, "POST", payload);
            paintCell(cell, entry);
        }, "Could not save the note. Your text is still here.");
    });

    remove.addEventListener("click", () => {
        if (!active?.entryId || !window.confirm("Delete this entry and its note?")) return;
        const { cell } = active;
        const { entryId, topicId, localDate } = cellData();
        const params = new URLSearchParams({ topicId, localDate });
        mutate(async () => {
            const result = await request(dialog.dataset.cellUrl + "/" + entryId + "?" + params, "DELETE");
            paintCell(cell, null, result.predictionDate === cell.dataset.dateLocal);
        }, "Could not delete the entry.");
    });

    statusButtons.forEach(button => button.addEventListener("click", () => {
        const { cell } = active;
        const hasNote = Boolean(cell.querySelector(".entry-with-content"));
        const payload = { ...cellData(), status: button.dataset.status, oldStatus: cell.dataset.oldStatus };
        mutate(async () => {
            const entry = await request(dialog.dataset.statusUrl, "POST", payload);
            paintCell(cell, { ...entry, hasNote });
        }, "Could not update the status.");
    }));

    edit.addEventListener("click", () => {
        const link = active?.cell.querySelector("[data-href]");
        if (link) window.location.href = link.dataset.href;
    });
    preview.addEventListener("click", () => {
        if (!active?.entryId) return;
        const noteUrl = dialog.dataset.entryUrl + "/" + active.entryId + "/note-full";
        dialog.addEventListener("hidden.bs.modal", () => {
            htmx.ajax("GET", noteUrl, { target: "#noteModal .modal-body", swap: "innerHTML" });
        }, { once: true });
        closeDialog();
    });
})();
