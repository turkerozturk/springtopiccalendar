document.addEventListener('DOMContentLoaded', () => {
    if (/^#topic-\d+$/.test(location.hash)) {
        const row = document.getElementById(location.hash.slice(1));
        row?.querySelector('.topic-title')?.focus({ preventScroll: true });
    }

    document.getElementById('topic-calendar').addEventListener('show.bs.modal', () => {
        document.getElementById('topic-calendar-content').textContent = 'Loading calendar…';
    });

    document.getElementById('topicInfoModal').addEventListener('show.bs.modal', () => {
        document.getElementById('topicInfoContent').textContent = 'Loading overview…';
    });
});
