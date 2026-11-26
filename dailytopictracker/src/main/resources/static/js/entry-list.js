const noteModal = document.getElementById('entry-note-modal');
const noteContent = document.getElementById('entry-note-content');
const copyButton = document.getElementById('entry-note-copy');
const copyStatus = document.getElementById('entry-note-copy-status');
let noteRequest;

noteModal.addEventListener('show.bs.modal', async event => {
    const link = event.relatedTarget;
    noteRequest?.abort();
    const request = new AbortController();
    noteRequest = request;

    document.getElementById('entry-note-title').textContent = link.dataset.topicName;
    document.getElementById('entry-note-date').textContent = link.dataset.entryDate || 'No date';
    document.getElementById('entry-note-edit').href = link.closest('.entry-row').querySelector('.entry-edit').href;
    document.getElementById('entry-note-tracker').href = link.dataset.trackerUrl;
    noteContent.textContent = 'Loading note…';
    copyButton.disabled = true;
    copyStatus.textContent = '';

    try {
        const response = await fetch(link.href, { signal: request.signal });
        if (!response.ok || response.redirected) {
            throw new Error('Note request failed');
        }
        const html = await response.text();
        if (!request.signal.aborted) {
            // The note endpoint renders and sanitizes Markdown before returning HTML.
            noteContent.innerHTML = html;
            copyButton.disabled = false;
        }
    } catch (error) {
        if (!request.signal.aborted) {
            noteContent.textContent = 'Could not load the note. Close this window and try again.';
        }
    }
});

noteModal.addEventListener('hide.bs.modal', () => noteRequest?.abort());

copyButton.addEventListener('click', async () => {
    try {
        await navigator.clipboard.writeText(noteContent.innerText);
        copyStatus.textContent = 'Note copied.';
    } catch (error) {
        copyStatus.textContent = 'Could not copy the note.';
    }
});
