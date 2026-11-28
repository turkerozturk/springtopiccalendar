(() => {
    const list = document.getElementById("day-tracker-list");
    const controls = document.getElementById("day-tracker-controls");
    const filter = document.getElementById("day-tracker-filter");
    const { date, q, sort } = filter.elements;
    const empty = document.getElementById("day-tracker-empty");
    const more = document.getElementById("day-tracker-more");
    const message = document.getElementById("day-tracker-message");
    const preferenceKey = "dtt.day-tracker.filters";
    let next = list.firstElementChild.dataset.next;
    let pending;
    let debounce;
    let replacing = false;
    let failedUrl;

    try {
        controls.open = localStorage.getItem(preferenceKey) !== "collapsed";
    } catch {
        // The filter panel still works when storage is unavailable.
    }
    controls.addEventListener("toggle", () => {
        try {
            localStorage.setItem(preferenceKey, controls.open ? "expanded" : "collapsed");
        } catch {
            // Keep the choice for the current page.
        }
    });

    function filters() {
        return { date: date.value, q: q.value.trim(), sort: sort.value };
    }

    function updateMore() {
        more.hidden = !(next || failedUrl);
        more.disabled = Boolean(pending) || Boolean(debounce);
        more.textContent = failedUrl ? "Retry" : "Load more";
    }

    function announce(text) {
        message.textContent = text;
        message.hidden = !text;
    }

    async function load(url, replace = false) {
        pending?.abort();
        const request = new AbortController();
        pending = request;
        failedUrl = null;
        updateMore();
        list.setAttribute("aria-busy", "true");
        announce("Loading topics…");
        try {
            const response = await fetch(url, { signal: request.signal, cache: "no-store" });
            if (pending !== request) return;
            if (response.status === 409) {
                next = null;
                announce("The active profile changed. Reload this page to see its topics.");
                return;
            }
            if (!response.ok) throw new Error("Could not load topics");
            const html = new DOMParser().parseFromString(await response.text(), "text/html");
            if (pending !== request) return;

            const chunk = html.querySelector("[data-day-chunk]");
            if (!chunk) throw new Error("Missing topic list");
            if (replace) {
                list.replaceChildren(chunk);
                empty.hidden = Boolean(chunk.querySelector(".day-tracker-topic"));
            } else {
                const previous = list.querySelector(":scope > :last-child > .day-tracker-topic:last-of-type");
                const first = chunk.querySelector(".day-tracker-topic");
                if (first && previous) {
                    if (first.dataset.groupId === previous.dataset.groupId) chunk.querySelector("[data-day-group]").remove();
                    if (first.dataset.categoryId === previous.dataset.categoryId) chunk.querySelector("[data-day-category]").remove();
                }
                if (first) list.append(chunk);
            }
            next = chunk.dataset.next;
            announce("");
        } catch (error) {
            if (pending === request && error.name !== "AbortError") {
                failedUrl = url;
                announce("Could not load topics. Try again.");
            }
        } finally {
            if (pending === request) {
                pending = null;
                list.removeAttribute("aria-busy");
                updateMore();
            }
        }
    }

    function applyFilters() {
        debounce = null;
        const params = filters();
        const location = new URL(window.location.href);
        location.search = new URLSearchParams(params);
        history.replaceState(null, "", location);

        document.querySelectorAll("[data-day-date-link]").forEach(link => {
            const url = new URL(link.href);
            url.searchParams.set("q", params.q);
            url.searchParams.set("sort", params.sort);
            link.href = url;
        });

        replacing = true;
        next = null;
        failedUrl = null;
        load(filter.dataset.chunkUrl + "?" + new URLSearchParams({ ...params, scope: filter.dataset.scope }), true);
    }

    q.addEventListener("input", () => {
        clearTimeout(debounce);
        pending?.abort();
        debounce = setTimeout(applyFilters, 250);
        updateMore();
    });
    sort.addEventListener("change", () => {
        clearTimeout(debounce);
        applyFilters();
    });
    date.addEventListener("change", () => {
        if (date.validity.valid) filter.requestSubmit();
    });

    more.addEventListener("click", () => {
        if (failedUrl) {
            load(failedUrl, replacing);
        } else if (next && !pending && !debounce) {
            replacing = false;
            load(next);
        }
    });
    if ("IntersectionObserver" in window) {
        const observer = new IntersectionObserver(entries => {
            if (entries.some(entry => entry.isIntersecting) && next && !pending && !failedUrl && !debounce) {
                replacing = false;
                load(next);
            }
        }, { rootMargin: "0px 0px 220px 0px" });
        observer.observe(document.getElementById("day-tracker-loading"));
    }
    updateMore();

    const statuses = { 0: "Not marked", 1: "Done", 2: "Warning" };
    list.addEventListener("day-tracker-entry-updated", event => {
        const row = event.target;
        const { entry, predicted } = event.detail;
        row.querySelector("[data-entry-status-label]").textContent = statuses[entry?.status] ?? "No entry";
        row.querySelector("[data-entry-note-icon]").hidden = !entry?.hasNote;
        row.querySelector("[data-entry-prediction]").hidden = !predicted;
    });
})();
