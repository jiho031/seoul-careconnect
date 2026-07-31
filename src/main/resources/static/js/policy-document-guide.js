(() => {
    const status = document.querySelector("[data-document-progress-status]");
    let statusTimer;

    const updateStatus = (message, error = false) => {
        if (!status) return;
        window.clearTimeout(statusTimer);
        status.textContent = message;
        status.classList.toggle("is-error", error);

        if (!error) {
            statusTimer = window.setTimeout(() => {
                status.textContent = "준비한 서류를 체크하면 로그인 계정에 저장됩니다.";
            }, 2500);
        }
    };

    document.querySelectorAll("[data-document-guide-open]").forEach(button => {
        button.addEventListener("click", () => {
            const dialog = document.getElementById(button.dataset.documentGuideOpen);
            if (!dialog) return;

            if (typeof dialog.showModal === "function") {
                dialog.showModal();
            } else {
                dialog.setAttribute("open", "");
            }
        });
    });

    document.querySelectorAll(".document-guide-modal").forEach(dialog => {
        dialog.querySelectorAll("[data-document-guide-close]").forEach(button => {
            button.addEventListener("click", () => dialog.close());
        });

        dialog.addEventListener("click", event => {
            if (event.target === dialog) {
                dialog.close();
            }
        });
    });

    document.querySelectorAll(".document-progress-checkbox").forEach(checkbox => {
        checkbox.addEventListener("change", async () => {
            const completed = checkbox.checked;
            const item = checkbox.closest(".document-checklist-item");
            checkbox.disabled = true;

            try {
                const response = await fetch(
                    `/api/policies/${encodeURIComponent(checkbox.dataset.policyId)}`
                    + `/documents/${encodeURIComponent(checkbox.dataset.policyDocumentId)}`
                    + "/progress",
                    {
                        method: "POST",
                        headers: {"Content-Type": "application/json"},
                        body: JSON.stringify({completed})
                    }
                );

                if (!response.ok) {
                    throw new Error(`HTTP ${response.status}`);
                }

                item?.classList.toggle("is-complete", completed);
                updateStatus(completed ? "준비 완료로 저장했습니다." : "체크를 해제했습니다.");
            } catch (error) {
                checkbox.checked = !completed;
                item?.classList.toggle("is-complete", !completed);
                updateStatus("체크 상태를 저장하지 못했습니다. 잠시 후 다시 시도해 주세요.", true);
            } finally {
                checkbox.disabled = false;
            }
        });
    });
})();
