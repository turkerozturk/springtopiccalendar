(() => {
    const table = document.getElementById("pivot-table");
    const controls = document.getElementById("tracker-view-controls");
    if (!table || !controls) return;

    const tbody = table.tBodies[0];
    const rows = Array.from(tbody.rows);
    const sortButtons = Array.from(table.querySelectorAll("[data-tracker-sort]"));
    const sortField = document.getElementById("tracker-sort-field");
    const sortDirection = document.getElementById("tracker-sort-direction");
    const groupPinned = document.getElementById("tracker-group-pinned");
    const headingToggles = Array.from(controls.querySelectorAll("[data-tracker-heading]"));
    const storageKey = "dtt.tracker.view";
    const dayColumns = {
        "last-done": ".right-frozen-last-done",
        "first-warn": ".right-frozen-first-warn",
        upcoming: ".right-frozen-upcoming"
    };
    let collator;
    try {
        collator = new Intl.Collator(table.dataset.trackerLocale, { sensitivity: "base" });
    } catch {
        collator = new Intl.Collator(undefined, { sensitivity: "base" });
    }
    let descending = false;

    let preferences = {};
    try {
        preferences = JSON.parse(localStorage.getItem(storageKey)) ?? {};
    } catch {
        // Storage may be unavailable. The controls still work on this page.
    }

    const savedHeadings = Array.isArray(preferences.compactHeadings) ? preferences.compactHeadings : [];
    groupPinned.checked = preferences.groupPinned !== false;
    headingToggles.forEach(toggle => {
        toggle.checked = !savedHeadings.includes(toggle.dataset.trackerHeading);
    });

    function applyHeadings() {
        const compactHeadings = headingToggles.filter(toggle => !toggle.checked)
            .map(toggle => toggle.dataset.trackerHeading);
        table.dataset.compactHeadings = compactHeadings.join(" ");
        sortButtons.forEach(button => {
            const compact = compactHeadings.includes(button.dataset.trackerSort);
            button.classList.toggle("tracker-heading-compact", compact);
            button.querySelector(".tracker-heading")?.classList.toggle("visually-hidden", compact);
        });
        return compactHeadings;
    }

    function savePreferences() {
        const compactHeadings = applyHeadings();
        try {
            localStorage.setItem(storageKey, JSON.stringify({ groupPinned: groupPinned.checked, compactHeadings }));
        } catch {
            // Keep the current choice for this page when storage is blocked.
        }
    }

    function updateDoneCount(row) {
        const count = row.querySelector("[data-tracker-done-count]");
        const done = row.querySelectorAll(".pivot-table-entry-cell.status-1").length;
        count.textContent = done;
    }

    function sortValue(row, field) {
        switch (field) {
            case "topic":
                return row.dataset.trackerTopic ?? "";
            case "category":
                return row.querySelector(".left-frozen-category")?.textContent.trim();
            case "done":
                return Number(row.querySelector("[data-tracker-done-count]").textContent);
            case "prediction": {
                const prediction = row.querySelector(".right-frozen-prediction").dataset.trackerPrediction;
                // Undefined predictions sort between today (0) and the next in-range day (1).
                return prediction === undefined ? 0.5 : Number(prediction);
            }
            default: {
                const value = row.querySelector(dayColumns[field]).dataset.trackerDay;
                return value === undefined ? undefined : Number(value);
            }
        }
    }

    function applySort() {
        const field = sortField.value;
        const direction = descending ? -1 : 1;
        const activeField = field || (groupPinned.checked ? "" : "topic");
        const sorted = rows.map(row => ({
            row,
            name: row.dataset.trackerTopic ?? "",
            id: Number(row.dataset.trackerTopicId),
            pinned: row.dataset.trackerPinned === "true",
            value: activeField ? sortValue(row, activeField) : undefined
        }));

        sorted.sort((a, b) => {
            if (groupPinned.checked && a.pinned !== b.pinned) return a.pinned ? -1 : 1;
            if (!activeField) return 0;
            if (a.value === undefined && b.value !== undefined) return 1;
            if (b.value === undefined && a.value !== undefined) return -1;
            const difference = a.value === b.value ? 0 :
                typeof a.value === "string" ? collator.compare(a.value, b.value) : a.value - b.value;
            return direction * difference || collator.compare(a.name, b.name) || a.id - b.id;
        });

        // Move existing rows so entry buttons, modal triggers and filtered rows retain their state.
        let nextRow = tbody.firstElementChild;
        sorted.forEach(({ row }) => {
            if (row === nextRow) {
                nextRow = nextRow.nextElementSibling;
            } else {
                tbody.insertBefore(row, nextRow);
            }
        });

        sortButtons.forEach(button => {
            const header = button.closest("th");
            if (button.dataset.trackerSort === field) {
                header.setAttribute("aria-sort", descending ? "descending" : "ascending");
            } else {
                header.removeAttribute("aria-sort");
            }
        });
        sortDirection.disabled = !field;
        sortDirection.textContent = descending ? "↓ Descending" : "↑ Ascending";
    }

    sortButtons.forEach(button => button.addEventListener("click", () => {
        const field = button.dataset.trackerSort;
        descending = sortField.value === field && !descending;
        sortField.value = field;
        applySort();
    }));
    sortField.addEventListener("change", () => {
        descending = false;
        applySort();
    });
    sortDirection.addEventListener("click", () => {
        descending = !descending;
        applySort();
    });
    groupPinned.addEventListener("change", () => {
        savePreferences();
        applySort();
    });
    headingToggles.forEach(toggle => toggle.addEventListener("change", savePreferences));

    table.addEventListener("tracker-status-updated", event => {
        updateDoneCount(event.target.closest("tr"));
        if (sortField.value === "done") applySort();
    });

    rows.forEach(updateDoneCount);
    applyHeadings();
    applySort();
    controls.hidden = false;
})();
