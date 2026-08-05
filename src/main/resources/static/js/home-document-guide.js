(() => {
    const modal = document.getElementById("documentPreparationModal");
    if (!modal) return;

    const selectView = modal.querySelector("[data-document-preparation-select]");
    const checklistView = modal.querySelector("[data-document-preparation-checklist]");
    const guideView = modal.querySelector("[data-document-preparation-guide]");
    const checklist = modal.querySelector("[data-document-modal-checklist]");
    const loading = modal.querySelector("[data-document-preparation-loading]");
    const status = modal.querySelector("[data-document-modal-status]");
    const heading = modal.querySelector("[data-document-policy-heading]");
    const detailLink = modal.querySelector("[data-document-detail-link]");
    const guideContent = modal.querySelector("[data-document-guide-content]");
    let currentPolicyId;
    let statusTimer;

    const make = (tag, className, text) => {
        const node = document.createElement(tag);
        if (className) node.className = className;
        if (text !== undefined && text !== null) node.textContent = text;
        return node;
    };

    const showView = view => {
        [selectView, checklistView, guideView].forEach(section => {
            if (section) section.hidden = section !== view;
        });
    };

    const setStatus = (message, error = false) => {
        if (!status) return;
        window.clearTimeout(statusTimer);
        status.textContent = message;
        status.classList.toggle("is-error", error);
        if (message && !error) {
            statusTimer = window.setTimeout(() => {
                status.textContent = "";
            }, 2500);
        }
    };

    const addTextSection = (container, title, text, className = "document-modal-guide-section") => {
        if (!text) return;
        const section = make("section", className);
        section.append(make("h4", "", title), make("p", "", text));
        container.append(section);
    };

    const addListSection = (container, title, values, ordered = false) => {
        if (!Array.isArray(values) || values.length === 0) return;
        const section = make("section", "document-modal-guide-section");
        section.append(make("h4", "", title));
        const list = make(ordered ? "ol" : "ul");
        values.forEach(value => list.append(make("li", "", value)));
        section.append(list);
        container.append(section);
    };

    const addGuideLink = (container, text, href, primary = false) => {
        if (!href) return;
        const link = make(
            "a",
            primary ? "document-modal-primary-link" : "document-modal-secondary-link",
            text
        );
        link.href = href;
        link.target = "_blank";
        link.rel = "noopener noreferrer";
        container.append(link);
    };

    const showGuide = documentItem => {
        if (!guideContent || !guideView) return;
        guideContent.replaceChildren();

        const eyebrow = make(
            "span",
            "document-modal-guide-eyebrow",
            `${documentItem.categoryLabel} · ${documentItem.requirementLabel}`
        );
        const title = make("h3", "", documentItem.guideTitle || documentItem.displayName);
        guideContent.append(eyebrow, title);

        const policyCondition = make("section", "document-modal-policy-condition");
        policyCondition.append(
            make("h4", "", "이 정책의 제출 기준"),
            make("p", "", documentItem.originalText)
        );
        if (documentItem.conditionText) {
            policyCondition.append(
                make("span", "", `조건: ${documentItem.conditionText}`)
            );
        }
        if (documentItem.alternativeGroup) {
            policyCondition.append(
                make("span", "", "같은 묶음의 서류 중 하나를 제출하는 항목입니다.")
            );
        }
        guideContent.append(policyCondition);

        if (documentItem.guideAvailable) {
            const issuerText = documentItem.issuer
                ? `${documentItem.issuer}\n${documentItem.guideSummary || ""}`.trim()
                : documentItem.guideSummary;
            addTextSection(guideContent, "어디서 준비하나요?", issuerText);
            addListSection(guideContent, "발급·준비 순서", documentItem.steps, true);
            addListSection(
                guideContent,
                "미리 확인할 것",
                documentItem.preparationItems
            );
            addTextSection(
                guideContent,
                "확인해 주세요",
                documentItem.caution,
                "document-modal-guide-caution"
            );
        } else {
            addTextSection(
                guideContent,
                "서류 정보",
                "정책 원문에서 확인된 제출 항목입니다. 공통 발급처가 확정되지 않은 정책 전용 서류이므로 공식 양식이나 공고 원문을 이용해 주세요."
            );
        }

        if (documentItem.attachmentName) {
            addTextSection(
                guideContent,
                "공식 첨부파일",
                documentItem.attachmentName,
                "document-modal-attachment"
            );
        }

        const links = make("div", "document-modal-guide-links");
        addGuideLink(links, "공식 양식 받기", documentItem.downloadUrl, true);
        addGuideLink(links, "공식 발급처 열기", documentItem.officialGuideUrl, true);
        addGuideLink(
            links,
            documentItem.helpName || "공식 도움받기",
            documentItem.helpUrl
        );
        addGuideLink(
            links,
            documentItem.helpUrl ? "공고 원문 보기" : "공고 문의처 확인하기",
            documentItem.officialPageUrl
        );
        if (links.childElementCount > 0) guideContent.append(links);

        if (documentItem.verifiedOn) {
            guideContent.append(
                make(
                    "small",
                    "document-modal-verified",
                    `공식 정보 확인일 ${documentItem.verifiedOn}`
                )
            );
        }
        showView(guideView);
    };

    const saveProgress = async (checkbox, item, documentItem) => {
        const completed = checkbox.checked;
        checkbox.disabled = true;
        try {
            const response = await fetch(
                `/api/policies/${encodeURIComponent(currentPolicyId)}`
                + `/documents/${encodeURIComponent(documentItem.policyDocumentId)}`
                + "/progress",
                {
                    method: "POST",
                    headers: {"Content-Type": "application/json"},
                    body: JSON.stringify({completed})
                }
            );
            if (!response.ok) throw new Error(`HTTP ${response.status}`);
            item.classList.toggle("is-complete", completed);
            setStatus(completed ? "준비 완료로 저장했습니다." : "체크를 해제했습니다.");
        } catch (error) {
            checkbox.checked = !completed;
            item.classList.toggle("is-complete", !completed);
            setStatus("체크 상태를 저장하지 못했습니다. 잠시 후 다시 시도해 주세요.", true);
        } finally {
            checkbox.disabled = false;
        }
    };

    const renderChecklist = payload => {
        checklist.replaceChildren();
        const completedKeys = new Set(payload.completedDocumentKeys || []);
        const documents = Array.isArray(payload.documents) ? payload.documents : [];

        if (documents.length === 0) {
            checklist.append(
                make(
                    "p",
                    "document-preparation-empty",
                    "공식 원문에 별도 제출 서류가 확인되지 않았습니다."
                )
            );
            return;
        }

        documents.forEach(documentItem => {
            const complete = completedKeys.has(documentItem.checklistKey);
            const item = make(
                "div",
                `document-modal-check-item${complete ? " is-complete" : ""}`
            );
            const label = make("label");
            const checkbox = make("input");
            checkbox.type = "checkbox";
            checkbox.checked = complete;

            const copy = make("span", "document-modal-check-copy");
            const meta = make("span", "document-modal-check-meta");
            meta.append(
                make(
                    "b",
                    `is-${String(documentItem.requirementType || "").toLowerCase()}`,
                    documentItem.requirementLabel
                ),
                make("span", "", documentItem.categoryLabel)
            );
            copy.append(meta, make("strong", "", documentItem.displayName));
            if (documentItem.conditionText) {
                copy.append(
                    make("small", "", `제출 조건: ${documentItem.conditionText}`)
                );
            }
            label.append(checkbox, copy);
            checkbox.addEventListener(
                "change",
                () => saveProgress(checkbox, item, documentItem)
            );

            const actions = make("div", "document-modal-check-actions");
            const guideButton = make("button", "", "발급·준비 안내");
            guideButton.type = "button";
            guideButton.addEventListener("click", () => showGuide(documentItem));
            actions.append(guideButton);
            if (documentItem.downloadUrl) {
                const download = make("a", "", "공식 양식 받기");
                download.href = documentItem.downloadUrl;
                download.target = "_blank";
                download.rel = "noopener noreferrer";
                actions.append(download);
            }

            item.append(label, actions);
            checklist.append(item);
        });
    };

    const loadPolicy = async button => {
        if (!checklistView || !checklist) return;
        currentPolicyId = button.dataset.documentPolicyId;
        heading.textContent = button.dataset.documentPolicyTitle || "정책 필요 서류";
        detailLink.href = `/policies/${encodeURIComponent(currentPolicyId)}#document-checklist`;
        checklist.replaceChildren();
        loading.hidden = false;
        loading.textContent = "필요 서류를 불러오고 있습니다.";
        setStatus("");
        showView(checklistView);

        try {
            const response = await fetch(
                `/api/policies/${encodeURIComponent(currentPolicyId)}/documents`,
                {headers: {"Accept": "application/json"}}
            );
            if (!response.ok) throw new Error(`HTTP ${response.status}`);
            const payload = await response.json();
            heading.textContent = payload.policyTitle || heading.textContent;
            renderChecklist(payload);
            loading.hidden = true;
        } catch (error) {
            loading.hidden = false;
            loading.textContent = "필요 서류를 불러오지 못했습니다. 정책 상세 화면에서 다시 확인해 주세요.";
        }
    };

    document.querySelectorAll("[data-document-preparation-open]").forEach(button => {
        button.addEventListener("click", () => {
            if (typeof modal.showModal === "function") {
                modal.showModal();
            } else {
                modal.setAttribute("open", "");
            }
        });
    });

    modal.querySelectorAll("[data-document-preparation-close]").forEach(button => {
        button.addEventListener("click", () => modal.close());
    });

    modal.querySelectorAll("[data-document-policy-id]").forEach(button => {
        button.addEventListener("click", () => loadPolicy(button));
    });

    modal.querySelectorAll("[data-document-back-to-policies]").forEach(button => {
        button.addEventListener("click", () => showView(selectView));
    });

    modal.querySelectorAll("[data-document-back-to-checklist]").forEach(button => {
        button.addEventListener("click", () => showView(checklistView));
    });

    modal.addEventListener("click", event => {
        if (event.target === modal) modal.close();
    });

    modal.addEventListener("close", () => {
        if (selectView) showView(selectView);
    });
})();
