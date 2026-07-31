document.addEventListener("DOMContentLoaded", () => {
    const openButtons = document.querySelectorAll("[data-ai-modal-open]");

    openButtons.forEach(button => {
        button.addEventListener("click", () => {
            const modalId = button.dataset.aiModalOpen;
            const dialog = modalId ? document.getElementById(modalId) : null;

            if (dialog instanceof HTMLDialogElement) {
                dialog.showModal();
                dialog.querySelector("[data-ai-modal-close]")?.focus();
            }
        });
    });

    document.querySelectorAll(".ai-summary-modal").forEach(dialog => {
        if (!(dialog instanceof HTMLDialogElement)) return;

        dialog.querySelectorAll("[data-ai-modal-close]").forEach(button => {
            button.addEventListener("click", () => dialog.close());
        });

        // 어두운 배경 영역을 누르면 모달을 닫는다.
        dialog.addEventListener("click", event => {
            if (event.target === dialog) dialog.close();
        });
    });
});
