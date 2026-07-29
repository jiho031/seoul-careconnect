document.addEventListener("DOMContentLoaded", () => {
    const root = document.getElementById("aiPolicyAssistant");
    if (!root || root.dataset.initialized === "true") return;
    root.dataset.initialized = "true";

    const launcher = root.querySelector("[data-ai-assistant-launcher]");
    const panel = root.querySelector("#aiAssistantPanel");
    const closeButton = root.querySelector("[data-ai-assistant-close]");
    const form = root.querySelector("[data-ai-assistant-form]");
    const input = root.querySelector("[data-ai-assistant-input]");
    const submitButton = root.querySelector("[data-ai-assistant-submit]");
    const messages = root.querySelector("[data-ai-assistant-messages]");
    const contextText = root.querySelector("[data-ai-assistant-context]");
    const policyId = root.dataset.policyId || null;
    const policyTitle = root.dataset.policyTitle || "";
    const history = [];
    let busy = false;
    let policySummaryLoaded = false;
    let policySummaryLoading = false;
    let lastFocusedElement = null;

    contextText.textContent = policyId && policyTitle
        ? `"${policyTitle}" 정책을 기준으로 답변합니다.`
        : "공개 중인 서울 정책을 찾아 답변합니다.";

    function openPanel(prompt) {
        lastFocusedElement = document.activeElement;
        panel.hidden = false;
        panel.setAttribute("aria-hidden", "false");
        launcher.setAttribute("aria-expanded", "true");
        if (prompt) input.value = prompt;
        if (policyId) void loadPolicySummary();
        window.requestAnimationFrame(() => input.focus());
    }

    function closePanel() {
        panel.hidden = true;
        panel.setAttribute("aria-hidden", "true");
        launcher.setAttribute("aria-expanded", "false");
        if (lastFocusedElement instanceof HTMLElement) lastFocusedElement.focus();
    }

    function appendMessage(role, content, sources) {
        const article = document.createElement("article");
        article.className = `ai-assistant-message is-${role}`;

        if (role === "assistant") {
            const avatar = document.createElement("span");
            avatar.className = "ai-assistant-avatar";
            avatar.setAttribute("aria-hidden", "true");
            avatar.textContent = "AI";
            article.appendChild(avatar);
        }

        const bubble = document.createElement("div");
        const paragraph = document.createElement("p");
        paragraph.textContent = content;
        bubble.appendChild(paragraph);

        if (Array.isArray(sources) && sources.length > 0) {
            bubble.appendChild(buildSources(sources));
        }

        article.appendChild(bubble);
        messages.appendChild(article);
        messages.scrollTop = messages.scrollHeight;
        return article;
    }

    function buildSources(sources) {
        const container = document.createElement("div");
        container.className = "ai-assistant-sources";

        sources.forEach((source, index) => {
            const card = document.createElement("section");
            card.className = "ai-assistant-source";

            const title = document.createElement("strong");
            title.textContent = `[${index + 1}] ${source.title}`;
            card.appendChild(title);

            const summary = document.createElement("span");
            summary.textContent = source.summary;
            card.appendChild(summary);

            const meta = document.createElement("small");
            meta.textContent = `${source.agency} · ${source.applicationPeriod} · 기준 ${source.sourceUpdatedDate}`;
            card.appendChild(meta);

            const links = document.createElement("div");
            links.className = "ai-assistant-source-links";
            links.appendChild(createLink("정책 상세", source.detailUrl, false));

            if (isSafeHttpUrl(source.officialUrl)) {
                links.appendChild(createLink("공식 페이지", source.officialUrl, true));
            }

            card.appendChild(links);
            container.appendChild(card);
        });

        return container;
    }

    function createLink(label, href, external) {
        const link = document.createElement("a");
        link.textContent = label;
        link.href = href;
        if (external) {
            link.target = "_blank";
            link.rel = "noopener noreferrer";
        }
        return link;
    }

    function isSafeHttpUrl(value) {
        if (!value) return false;
        try {
            const url = new URL(value);
            return url.protocol === "http:" || url.protocol === "https:";
        } catch {
            return false;
        }
    }

    function setBusy(value) {
        busy = value;
        submitButton.disabled = value;
        input.disabled = value;
        root.querySelectorAll("[data-ai-suggestion]").forEach(button => {
            button.disabled = value;
        });
    }

    async function loadPolicySummary() {
        if (!policyId || policySummaryLoaded || policySummaryLoading) return;

        policySummaryLoading = true;
        root.querySelector("[data-ai-initial-message]")?.remove();
        const loading = appendMessage("loading", "이 정책을 쉽게 정리하고 있습니다. 처음 한 번만 생성해 저장합니다.");
        setBusy(true);

        try {
            const response = await fetch(`/api/ai/assistant/policies/${encodeURIComponent(policyId)}/summary`, {
                method: "POST",
                headers: {"Content-Type": "application/json"}
            });
            const payload = await response.json().catch(() => ({}));
            if (!response.ok) {
                throw new Error(payload.message || "AI 정책 요약을 준비할 수 없습니다.");
            }

            loading.remove();
            appendMessage("assistant", payload.answer, payload.sources);
            history.push({role: "assistant", content: payload.answer});
            policySummaryLoaded = true;
        } catch (error) {
            loading.remove();
            appendMessage(
                "assistant",
                error instanceof Error
                    ? error.message
                    : "AI 정책 요약을 일시적으로 준비할 수 없습니다."
            );
        } finally {
            policySummaryLoading = false;
            setBusy(false);
            input.focus();
        }
    }

    async function ask(question) {
        if (busy) return;

        const previousHistory = history.slice(-6);
        appendMessage("user", question);
        history.push({role: "user", content: question});
        const loading = appendMessage("loading", "정책 정보를 찾고 답변을 준비하고 있습니다.");
        setBusy(true);

        try {
            const response = await fetch("/api/ai/assistant", {
                method: "POST",
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify({
                    question,
                    policyId: policyId ? Number(policyId) : null,
                    history: previousHistory
                })
            });

            const payload = await response.json().catch(() => ({}));
            if (!response.ok) {
                throw new Error(payload.message || "AI 정책 도우미를 이용할 수 없습니다.");
            }

            loading.remove();
            appendMessage("assistant", payload.answer, payload.sources);
            history.push({role: "assistant", content: payload.answer});
        } catch (error) {
            loading.remove();
            appendMessage(
                "assistant",
                error instanceof Error
                    ? error.message
                    : "AI 정책 도우미를 일시적으로 이용할 수 없습니다."
            );
        } finally {
            setBusy(false);
            input.focus();
        }
    }

    launcher.addEventListener("click", () => {
        if (panel.hidden) {
            openPanel();
        } else {
            closePanel();
        }
    });

    closeButton.addEventListener("click", closePanel);

    form.addEventListener("submit", event => {
        event.preventDefault();
        const question = input.value.trim();
        if (!question) return;
        input.value = "";
        ask(question);
    });

    input.addEventListener("keydown", event => {
        if (event.key === "Enter" && !event.shiftKey) {
            event.preventDefault();
            form.requestSubmit();
        }
    });

    root.querySelectorAll("[data-ai-suggestion]").forEach(button => {
        button.addEventListener("click", () => {
            input.value = button.dataset.aiSuggestion || "";
            form.requestSubmit();
        });
    });

    document.querySelectorAll("[data-ai-assistant-open]").forEach(button => {
        button.addEventListener("click", event => {
            event.preventDefault();
            openPanel(button.dataset.aiAssistantPrompt || "");
        });
    });

    document.addEventListener("keydown", event => {
        if (event.key === "Escape" && !panel.hidden) closePanel();
    });
});
