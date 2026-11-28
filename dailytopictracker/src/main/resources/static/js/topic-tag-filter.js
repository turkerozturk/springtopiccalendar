window.dttTopicTagFilter = (() => {
    const key = "dtt.topicTagFilter";
    let state = { include: [], exclude: [] };

    function normalize(value) {
        return Array.isArray(value) ? [...new Set(value.filter(tag => typeof tag === "string" && tag.length))] : [];
    }

    function read() {
        try {
            const saved = JSON.parse(localStorage.getItem(key));
            if (saved) state = { include: normalize(saved.include), exclude: normalize(saved.exclude) };
        } catch {
            // Keep the in-memory preference if storage is unavailable.
        }
        return state;
    }

    function write(selection) {
        state = { include: normalize(selection.include), exclude: normalize(selection.exclude) };
        try {
            localStorage.setItem(key, JSON.stringify(state));
        } catch {
            // The filter still applies to the current page.
        }
    }

    function appendParams(params, selection = read()) {
        selection.include.forEach(tag => params.append("includeTag", tag));
        selection.exclude.forEach(tag => params.append("excludeTag", tag));
        return params;
    }

    return { key, read, write, appendParams };
})();
