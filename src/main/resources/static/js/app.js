document.addEventListener('DOMContentLoaded', () => {
    const accessView = document.getElementById('access-view');
    const editorView = document.getElementById('editor-view');
    const alertBox = document.getElementById('alert-box');
    
    const codeInput = document.getElementById('encryption-code');
    const accessBtn = document.getElementById('access-btn');
    
    const contentArea = document.getElementById('note-content');
    const saveBtn = document.getElementById('save-btn');
    const deleteBtn = document.getElementById('delete-btn');
    const backBtn = document.getElementById('back-btn');

    let currentCode = '';

    function showAlert(message, isError = false) {
        alertBox.textContent = message;
        alertBox.className = `alert ${isError ? 'alert-error' : 'alert-success'}`;
        alertBox.style.display = 'block';
        setTimeout(() => { alertBox.style.display = 'none'; }, 5000);
    }

    accessBtn.addEventListener('click', async () => {
        const code = codeInput.value.trim();
        if (!code) {
            showAlert('Please enter a secret code.', true);
            return;
        }

        try {
            const response = await fetch('/api/note/access', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ code })
            });
            
            const data = await response.json();

            if (response.ok) {
                // Note exists and decrypted successfully
                contentArea.value = data.data;
                currentCode = code;
                accessView.style.display = 'none';
                editorView.style.display = 'block';
                showAlert('Note decrypted back successfully!');
            } else if (data.error === "Note not found") {
                // Note doesn't exist, allow creating new
                contentArea.value = '';
                currentCode = code;
                accessView.style.display = 'none';
                editorView.style.display = 'block';
                showAlert('No existing note found. Start typing to create a new one!');
            } else {
                // Invalid code or other error
                showAlert(data.error || 'Failed to access note.', true);
            }
        } catch (e) {
            showAlert('Network error occurred.', true);
        }
    });

    saveBtn.addEventListener('click', async () => {
        const content = contentArea.value.trim();
        if (!content) {
            showAlert('Cannot save an empty note.', true);
            return;
        }

        try {
            const response = await fetch('/api/note/save', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ code: currentCode, content })
            });

            const data = await response.json();
            
            if (response.ok) {
                showAlert('Note saved successfully!');
            } else {
                showAlert(data.error || 'Failed to save note.', true);
            }
        } catch (e) {
            showAlert('Network error occurred.', true);
        }
    });

    deleteBtn.addEventListener('click', async () => {
        if (!confirm('Are you absolutely sure you want to delete this note? This cannot be undone.')) {
            return;
        }

        try {
            const response = await fetch('/api/note/delete', {
                method: 'DELETE',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ code: currentCode })
            });

            const data = await response.json();

            if (response.ok || data.error === "Note not found") {
                showAlert('Note deleted successfully.');
                exitEditor();
            } else {
                showAlert(data.error || 'Failed to delete note.', true);
            }
        } catch (e) {
            showAlert('Network error occurred.', true);
        }
    });

    backBtn.addEventListener('click', exitEditor);

    function exitEditor() {
        currentCode = '';
        codeInput.value = '';
        contentArea.value = '';
        editorView.style.display = 'none';
        accessView.style.display = 'block';
    }
});
