document.addEventListener('DOMContentLoaded', () => {
    const list = document.getElementById('topicsTable');
    const stateKey = `dtt:topics:${location.pathname}:${list.dataset.categoryId || ''}`;
    let savedState;

    const table = new DataTable(list, {
        pageLength: 50,
        stateSave: true,
        stateDuration: -1,
        stateSaveCallback(_settings, state) {
            try {
                sessionStorage.setItem(stateKey, JSON.stringify({ ...state, scrollY: window.scrollY }));
            } catch {
                // The list remains usable if browser storage is unavailable.
            }
        },
        stateLoadCallback() {
            try {
                savedState = JSON.parse(sessionStorage.getItem(stateKey));
                return savedState;
            } catch {
                return null;
            }
        }
    });

    list.addEventListener('click', event => {
        if (event.target.closest('[data-remember-topic-list]')) {
            table.state.save();
        }
    });

    if (/^#topic-\d+$/.test(location.hash)) {
        const row = document.getElementById(location.hash.slice(1));
        if (row) {
            requestAnimationFrame(() => {
                row.querySelector('.topic-title').focus({ preventScroll: true });
                if (Number.isFinite(savedState?.scrollY)) {
                    window.scrollTo(0, savedState.scrollY);
                } else {
                    row.scrollIntoView({ block: 'nearest' });
                }
            });
        }
    }
});
