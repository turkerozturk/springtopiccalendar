(() => {
    const page = document.getElementById("topic-tree-page");
    const layout = document.getElementById("topic-tree-layout");
    const nodes = document.getElementById("topic-tree-nodes");
    const panel = document.getElementById("topic-tree-panel");
    const back = document.getElementById("topic-tree-back");
    const status = document.getElementById("topic-tree-status");
    const fancytree = window.jQuery?.ui?.fancytree;
    if (typeof fancytree?.createTree !== "function") {
        status.textContent = "The tree library could not load. Reload this page to try again.";
        return;
    }
    const profile = document.getElementById("topic-tree-profile");
    const preferences = window.dttTopicTagFilter;
    const tagSelects = [...page.querySelectorAll("[data-tree-tag]")];
    const tagSummary = document.getElementById("topic-tree-tag-summary");
    const tagClear = document.getElementById("topic-tree-tag-clear");
    const mobile = matchMedia("(max-width: 767px)");
    const pageUrl = new URL(location.href);
    let selectedId = pageUrl.searchParams.get("topicId");
    let savedOnLoad = pageUrl.searchParams.get("saved") === "true";
    let treeRequest;
    let panelRequest;

    function renderTitle(node) {
        const title = node.span.querySelector(".fancytree-title");
        const content = document.createDocumentFragment();
        const { topicId, iconKey, lastEntryDate, tags } = node.data;

        if (page.dataset.showIcons === "true" && iconKey) {
            const icon = document.createElement("i");
            icon.className = "topic-tree-icon fa-solid fa-" + iconKey;
            icon.setAttribute("aria-hidden", "true");
            content.append(icon);
        }
        if (topicId && page.dataset.showDate === "true") {
            const date = document.createElement(lastEntryDate ? "time" : "span");
            date.className = "topic-tree-date";
            if (lastEntryDate) {
                date.dateTime = lastEntryDate;
                date.textContent = lastEntryDate;
                date.title = "Last entry date";
            }
            content.append(date);
        }
        const name = document.createElement("span");
        name.className = "topic-tree-name";
        name.textContent = node.title;
        content.append(name);
        if (topicId && page.dataset.showTags === "true" && tags?.length) {
            const badges = document.createElement("span");
            badges.className = "topic-tree-tags";
            for (const tag of tags) {
                const badge = document.createElement("span");
                badge.className = "badge text-bg-light";
                badge.textContent = tag;
                badges.append(badge);
            }
            content.append(badges);
        }
        title.replaceChildren(content);
    }

    function showContent() {
        layout.classList.add("show-content");
        const heading = panel.querySelector("h2");
        if (mobile.matches && heading && document.activeElement !== back) {
            heading.tabIndex = -1;
            heading.focus({ preventScroll: true });
        }
    }

    const tree = fancytree.createTree(nodes, {
        source: [],
        escapeTitles: true,
        icon: false,
        renderNode: (_event, data) => renderTitle(data.node),
        click: (_event, data) => {
            if (data.targetType === "title" && data.node.data.topicId && data.node.isActive()
                    && !layout.classList.contains("show-content")) showContent();
        },
        activate: (_event, data) => {
            if (data.node.data.topicId) openTopic(data.node);
        }
    });

    function setSelection(id) {
        selectedId = id;
        const url = new URL(location.href);
        if (id) url.searchParams.set("topicId", id);
        else url.searchParams.delete("topicId");
        url.searchParams.delete("saved");
        history.replaceState(history.state, "", url);
    }

    function parsePanel(html, id) {
        const doc = new DOMParser().parseFromString(html, "text/html");
        const card = doc.querySelector("[data-panel-topic]");
        if (!card || card.dataset.panelTopic !== String(id)) throw new Error("Topic content missing");
        return card;
    }

    async function openTopic(node) {
        const id = node.data.topicId;
        setSelection(id);
        if (panel.firstElementChild?.dataset.panelTopic === String(id)) return;
        panelRequest?.abort();
        const request = new AbortController();
        panelRequest = request;
        const saved = savedOnLoad;
        savedOnLoad = false;
        layout.classList.add("show-content");
        panel.textContent = "Loading topic…";
        try {
            const response = await fetch(page.dataset.panelUrl + "/" + id + (saved ? "?saved=true" : ""),
                { signal: request.signal, cache: "no-store" });
            if (!response.ok) throw new Error("Topic request failed");
            const card = parsePanel(await response.text(), id);
            if (panelRequest !== request) return;
            panel.replaceChildren(card);
            if (layout.classList.contains("show-content")) showContent();
        } catch (error) {
            if (panelRequest === request && error.name !== "AbortError") {
                panel.textContent = "Could not load this topic.";
                const retry = document.createElement("button");
                retry.className = "btn btn-sm btn-outline-secondary ms-2";
                retry.textContent = "Retry";
                retry.addEventListener("click", () => openTopic(node));
                panel.append(retry);
            }
        }
    }

    back.addEventListener("click", () => {
        layout.classList.remove("show-content");
        tree.getActiveNode()?.setFocus();
    });

    panel.addEventListener("submit", async event => {
        const form = event.target.closest("[data-tree-entry-form]");
        if (!form) return;
        event.preventDefault();
        const button = form.querySelector('button[type="submit"]');
        if (button.disabled) return;
        const id = form.elements.namedItem("topic.id").value;
        const message = form.querySelector("[data-tree-entry-message]");
        button.disabled = true;
        message.hidden = true;
        try {
            const response = await fetch(form.action, {
                method: "POST", headers: { "X-Topic-Tree": "true" },
                body: new URLSearchParams(new FormData(form))
            });
            if (response.status === 409) {
                message.textContent = "This topic already has an entry for that date.";
                message.hidden = false;
                return;
            }
            if (!response.ok) throw new Error("Entry request failed");
            const card = parsePanel(await response.text(), id);
            const node = tree.getNodeByKey("topic-" + id);
            if (node) {
                node.data.lastEntryDate = card.dataset.lastEntry || null;
                if (node.span) renderTitle(node);
            }
            if (form.isConnected) panel.replaceChildren(card);
        } catch {
            message.textContent = "Could not save the entry. Your note is still here.";
            message.hidden = false;
        } finally {
            button.disabled = false;
        }
    });

    function selectedTags() {
        const selected = { include: [], exclude: [] };
        for (const select of tagSelects) {
            if (select.value) selected[select.value].push(select.dataset.treeTag);
        }
        return selected;
    }

    async function loadTree() {
        const selected = selectedTags();
        const count = selected.include.length + selected.exclude.length;
        tagSummary.textContent = count ? `(${selected.include.length} included, ${selected.exclude.length} excluded)` : "";
        tagClear.disabled = !count;
        treeRequest?.abort();
        const request = new AbortController();
        treeRequest = request;
        status.textContent = "Loading topics…";
        try {
            const params = preferences.appendParams(new URLSearchParams(), selected);
            const response = await fetch(page.dataset.nodesUrl + "?" + params,
                { signal: request.signal, cache: "no-store" });
            if (!response.ok) throw new Error("Tree request failed");
            const data = await response.json();
            if (treeRequest !== request) return;

            const expanded = new Set();
            tree.visit(node => { if (node.expanded) expanded.add(node.key); });
            const mapNode = item => ({
                key: item.key, title: item.title, folder: item.folder,
                expanded: expanded.has(item.key) || (count > 0 && item.key.startsWith("group-")),
                iconKey: item.iconKey, topicId: item.topicId, lastEntryDate: item.lastEntryDate, tags: item.tags,
                children: item.children.map(mapNode)
            });
            await tree.reload([{
                key: "database", title: data.databaseName, folder: true, expanded: true, iconKey: "database",
                children: data.groups.map(mapNode)
            }]);
            if (treeRequest !== request) return;
            profile.hidden = !data.profileName;
            profile.textContent = data.profileName || "";
            status.textContent = data.groups.length ? "" : "No topics match this scope.";
            const selectedNode = selectedId && tree.getNodeByKey("topic-" + selectedId);
            if (selectedNode) {
                selectedNode.makeVisible({ noAnimation: true });
                selectedNode.setActive();
            } else if (selectedId) {
                panelRequest?.abort();
                panel.textContent = "Select a topic to add an entry or view its details.";
                layout.classList.remove("show-content");
                setSelection(null);
                savedOnLoad = false;
            }
        } catch (error) {
            if (treeRequest === request && error.name !== "AbortError") {
                status.textContent = "Could not load the tree.";
                const retry = document.createElement("button");
                retry.className = "btn btn-sm btn-outline-secondary ms-2";
                retry.textContent = "Retry";
                retry.addEventListener("click", loadTree);
                status.append(retry);
            }
        }
    }

    function restoreTags() {
        const { include, exclude } = preferences.read();
        for (const select of tagSelects) {
            const tag = select.dataset.treeTag;
            select.value = exclude.includes(tag) ? "exclude" : include.includes(tag) ? "include" : "";
        }
        loadTree();
    }

    function saveTags() {
        preferences.write(selectedTags());
        loadTree();
    }

    tagSelects.forEach(select => select.addEventListener("change", saveTags));
    tagClear.addEventListener("click", () => {
        tagSelects.forEach(select => { select.value = ""; });
        saveTags();
    });
    page.querySelector("#topic-tree-tag-search").addEventListener("input", event => {
        const fold = value => value.toLowerCase().normalize("NFD").replace(/\p{M}/gu, "").replaceAll("ı", "i");
        const query = fold(event.target.value.trim());
        let visible = false;
        tagSelects.forEach(select => {
            const match = fold(select.dataset.treeTag).includes(query);
            select.closest(".topic-tree-tag").hidden = !match;
            visible ||= match;
        });
        page.querySelector("#topic-tree-tag-empty").hidden = visible;
    });
    window.addEventListener("storage", event => {
        if (event.key === preferences.key) restoreTags();
    });

    const splitter = document.getElementById("topic-tree-splitter");
    const splitKey = "dtt.topicTree.split";
    let width = 34;
    function setWidth(percent) {
        width = Math.min(70, Math.max(22, percent));
        layout.style.setProperty("--tree-width", width + "%");
        splitter.setAttribute("aria-valuenow", String(Math.round(width)));
    }

    try {
        const saved = localStorage.getItem(splitKey);
        if (saved && Number.isFinite(Number(saved))) setWidth(Number(saved));
    } catch { /* Keep the splitter usable without storage. */ }

    function saveWidth() {
        try {
            localStorage.setItem(splitKey, String(width));
        } catch { /* Keep the current width for this page. */ }
    }

    splitter.addEventListener("pointerdown", event => {
        if (event.button !== 0) return;
        splitter.setPointerCapture(event.pointerId);
        document.body.classList.add("topic-tree-resizing");
    });
    splitter.addEventListener("pointermove", event => {
        if (!splitter.hasPointerCapture(event.pointerId)) return;
        const box = layout.getBoundingClientRect();
        setWidth(100 * (event.clientX - box.left) / box.width);
    });
    splitter.addEventListener("lostpointercapture", () => {
        document.body.classList.remove("topic-tree-resizing");
        saveWidth();
    });
    splitter.addEventListener("keydown", event => {
        const values = { ArrowLeft: width - 3, ArrowRight: width + 3, Home: 22, End: 70 };
        if (!Object.hasOwn(values, event.key)) return;
        event.preventDefault();
        setWidth(values[event.key]);
        saveWidth();
    });

    restoreTags();
})();
