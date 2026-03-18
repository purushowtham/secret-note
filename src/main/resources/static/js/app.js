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

    const fileUpload = document.getElementById('file-upload');
    const uploadStatus = document.getElementById('upload-status');
    const attachmentsList = document.getElementById('attachments-list');

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
                contentArea.value = data.data.content;
                currentCode = code;
                renderAttachments(data.data.attachments);
                accessView.style.display = 'none';
                editorView.style.display = 'block';
                showAlert('Note decrypted back successfully!');
            } else if (data.error === "Note not found") {
                contentArea.value = '';
                currentCode = code;
                renderAttachments([]);
                accessView.style.display = 'none';
                editorView.style.display = 'block';
                showAlert('No existing note found. Start typing to create a new one!');
            } else {
                showAlert(data.error || 'Failed to access note.', true);
            }
        } catch (e) {
            showAlert('Network error occurred.', true);
        }
    });

    saveBtn.addEventListener('click', async () => {
        const content = contentArea.value.trim();
        if (!content && document.querySelectorAll('.attachment-item').length === 0) {
            showAlert('Cannot save an empty note without attachments.', true);
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
        if (!confirm('Are you absolutely sure you want to delete this note and all attachments? This cannot be undone.')) {
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
        renderAttachments([]);
        editorView.style.display = 'none';
        accessView.style.display = 'block';
    }

    function renderAttachments(attachments) {
        attachmentsList.innerHTML = '';
        if (!attachments || attachments.length === 0) {
            attachmentsList.innerHTML = '<span style="color: var(--text-muted); font-size: 0.85rem;">No attachments yet.</span>';
            return;
        }

        attachments.forEach(att => {
            const item = document.createElement('div');
            item.className = 'attachment-item';
            
            const isImage = att.url.match(/\.(jpeg|jpg|gif|png|webp)$/i) != null;
            const iconSvg = isImage 
                ? '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect><circle cx="8.5" cy="8.5" r="1.5"></circle><polyline points="21 15 16 10 5 21"></polyline></svg>'
                : '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M13 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z"></path><polyline points="13 2 13 9 20 9"></polyline></svg>';

            item.innerHTML = `
                <div class="attachment-info">
                    <span style="color: var(--text-muted); display: flex; align-items: center;">${iconSvg}</span>
                    <a href="${att.url}" target="_blank" class="attachment-name" title="${att.originalFilename}">${att.originalFilename}</a>
                </div>
                <div class="attachment-actions">
                    <button class="btn-icon-small delete-att-btn" data-id="${att.id}" title="Delete file">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
                    </button>
                </div>
            `;
            attachmentsList.appendChild(item);
        });

        document.querySelectorAll('.delete-att-btn').forEach(btn => {
            btn.addEventListener('click', async (e) => {
                const fileId = e.currentTarget.getAttribute('data-id');
                await deleteAttachment(fileId);
            });
        });
    }

    fileUpload.addEventListener('change', async (e) => {
        const file = e.target.files[0];
        if (!file) return;

        // Auto save to ensure note exists
        await fetch('/api/note/save', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ code: currentCode, content: contentArea.value.trim() || ' ' })
        });

        const formData = new FormData();
        formData.append('code', currentCode);
        formData.append('file', file);

        uploadStatus.textContent = 'Uploading...';
        fileUpload.disabled = true;

        try {
            const response = await fetch('/api/note/upload', {
                method: 'POST',
                body: formData
            });

            const data = await response.json();

            if (response.ok) {
                showAlert('File uploaded successfully!');
                await refreshAttachments();
            } else {
                showAlert(data.error || 'Failed to upload file.', true);
            }
        } catch (err) {
            showAlert('Network error during upload.', true);
        } finally {
            uploadStatus.textContent = '';
            fileUpload.disabled = false;
            fileUpload.value = '';
        }
    });

    async function deleteAttachment(fileId) {
        if (!confirm('Are you sure you want to delete this file?')) return;
        
        try {
            const response = await fetch('/api/note/file', {
                method: 'DELETE',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ code: currentCode, fileId: parseInt(fileId) })
            });

            const data = await response.json();

            if (response.ok) {
                showAlert('File deleted successfully.');
                await refreshAttachments();
            } else {
                showAlert(data.error || 'Failed to delete file.', true);
            }
        } catch (err) {
            showAlert('Network error during file deletion.', true);
        }
    }

    async function refreshAttachments() {
        try {
            const response = await fetch('/api/note/access', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ code: currentCode })
            });
            const data = await response.json();
            if (response.ok) {
                renderAttachments(data.data.attachments);
            }
        } catch (e) {
            console.error(e);
        }
    }
});
