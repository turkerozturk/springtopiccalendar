(() => {
    const root = document.documentElement;
    if (root.hasAttribute("data-weekly-period-navigation")) return;
    root.setAttribute("data-weekly-period-navigation", "");

    document.addEventListener("click", async event => {
        const link = event.target.closest("a[data-weekly-shift]");
        if (!link || event.defaultPrevented || event.button !== 0
                || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey || !window.htmx) return;

        const wrapper = link.closest("#tableWrapperWeeklyCalendar");
        if (!wrapper) return;

        event.preventDefault();
        if (wrapper.getAttribute("aria-busy") === "true") return;
        wrapper.setAttribute("aria-busy", "true");

        const error = wrapper.querySelector("[data-weekly-error]");
        error.textContent = "";
        const url = new URL(link.href);
        url.searchParams.set("tableOnly", "true");
        try {
            const response = await fetch(url);
            if (!response.ok) throw new Error("Calendar request failed");
            const html = new DOMParser().parseFromString(await response.text(), "text/html");
            const table = html.querySelector("#tableWrapperWeeklyCalendar > table");
            if (!table) throw new Error("Calendar table missing from response");
            if (!wrapper.isConnected) return;

            const rightOffset = wrapper.scrollWidth - wrapper.clientWidth - wrapper.scrollLeft;
            const restoreFocus = wrapper.contains(document.activeElement);
            if (typeof window.hideCustomTooltip === "function") window.hideCustomTooltip();
            wrapper.replaceChildren(table);
            window.htmx.process(table);
            wrapper.scrollLeft = wrapper.scrollWidth - wrapper.clientWidth - rightOffset;
            if (restoreFocus) {
                const nextLink = wrapper.querySelector(`a[data-weekly-shift="${link.dataset.weeklyShift}"]`);
                (nextLink ?? wrapper.querySelector("[data-weekly-period]")).focus({ preventScroll: true });
            }
            if (wrapper.dataset.weeklyStandalone === "true") {
                history.replaceState(history.state, "", link.href);
            }
        } catch {
            error.textContent = "Could not load the period. Try again.";
        } finally {
            wrapper.removeAttribute("aria-busy");
        }
    });
})();
