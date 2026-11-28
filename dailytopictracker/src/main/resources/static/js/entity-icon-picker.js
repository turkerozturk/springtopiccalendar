document.querySelectorAll("[data-entity-icon-picker]").forEach(picker => {
    const search = picker.querySelector("[data-icon-search]");
    const choices = [...picker.querySelectorAll("[data-icon-choice]")];
    const preview = picker.querySelector("[data-icon-preview]");
    const label = picker.querySelector("[data-icon-label]");
    const empty = picker.querySelector("[data-icon-empty]");

    search.addEventListener("input", () => {
        const query = search.value.trim().toLowerCase();
        let matches = 0;
        choices.forEach(choice => {
            const isNone = choice.querySelector("input").value === "";
            const match = choice.dataset.iconChoice.toLowerCase().includes(query);
            choice.hidden = !isNone && !match;
            if (!isNone && match) matches++;
        });
        empty.hidden = matches !== 0;
    });
    search.addEventListener("keydown", event => {
        if (event.key === "Enter") event.preventDefault();
    });

    picker.addEventListener("change", event => {
        const input = event.target;
        if (!input.matches('input[name="iconKey"]')) return;
        preview.hidden = !input.value;
        preview.className = input.value ? "fa-solid fa-" + input.value : "fa-solid";
        label.textContent = input.closest("[data-icon-choice]").dataset.iconChoice;
    });
});
