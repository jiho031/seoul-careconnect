document.addEventListener("DOMContentLoaded", () => {
    const root = document.querySelector("[data-ai-summary-generator]");
    if (!root || root.dataset.initialized === "true") return;
    root.dataset.initialized = "true";

    const policyId = root.dataset.policyId;
    const startButton = root.querySelector("[data-ai-summary-start]");
    const progressBox = root.querySelector("[data-ai-summary-progress]");
    const progressBar = root.querySelector("[data-ai-summary-bar]");
    const stageLabel = root.querySelector("[data-ai-summary-stage]");
    const stepLabel = root.querySelector("[data-ai-summary-step]");
    const message = root.querySelector("[data-ai-summary-message]");
    const runningStates = new Set(["QUEUED", "GENERATING", "SAVING"]);
    let pollTimer = null;
    let requestRunning = false;
    let reloadScheduled = false;

    function schedulePoll() {
        window.clearTimeout(pollTimer);
        pollTimer = window.setTimeout(loadStatus, 900);
    }

    function showFailure(error) {
        progressBox.hidden = false;
        root.dataset.state = "FAILED";
        stageLabel.textContent = "상태 확인 실패";
        stepLabel.textContent = "다시 시도 가능";
        message.textContent = error instanceof Error
            ? error.message
            : "AI 요약 상태를 확인하지 못했습니다.";
        startButton.hidden = false;
        startButton.disabled = false;
        startButton.textContent = "다시 시도";
    }

    function applyStatus(payload) {
        const state = payload.state || "IDLE";
        const stage = Number(payload.stage) || 0;
        const totalStages = Number(payload.totalStages) || 4;

        root.dataset.state = state;
        progressBar.max = totalStages;
        progressBar.value = Math.min(stage, totalStages);
        progressBar.textContent = `${stage}/${totalStages}단계`;
        progressBar.setAttribute(
            "aria-valuetext",
            `${payload.stageLabel || "생성 준비"} ${stage}/${totalStages}단계`
        );
        stageLabel.textContent = payload.stageLabel || "생성 준비";
        stepLabel.textContent = `${stage}/${totalStages}단계`;
        message.textContent = payload.message || "AI 요약 상태를 확인하고 있습니다.";

        if (state === "IDLE") {
            progressBox.hidden = true;
            startButton.hidden = false;
            startButton.disabled = false;
            startButton.textContent = "AI 요약 생성";
            return;
        }

        progressBox.hidden = false;

        if (runningStates.has(state)) {
            startButton.hidden = true;
            startButton.disabled = true;
            schedulePoll();
            return;
        }

        if (state === "COMPLETED") {
            startButton.hidden = true;
            if (!reloadScheduled) {
                reloadScheduled = true;
                window.setTimeout(() => window.location.reload(), 700);
            }
            return;
        }

        startButton.hidden = false;
        startButton.disabled = false;
        startButton.textContent = "다시 시도";
    }

    async function request(url, options) {
        const response = await fetch(url, {
            ...options,
            headers: {
                Accept: "application/json",
                ...(options?.headers || {})
            }
        });
        const payload = await response.json().catch(() => ({}));

        if (!response.ok) {
            throw new Error(payload.message || "AI 요약 요청을 처리하지 못했습니다.");
        }

        return payload;
    }

    async function loadStatus() {
        if (requestRunning || reloadScheduled) return;
        requestRunning = true;

        try {
            const payload = await request(
                `/api/ai/policies/${encodeURIComponent(policyId)}/summary/status`
            );
            applyStatus(payload);
        } catch (error) {
            showFailure(error);
        } finally {
            requestRunning = false;
        }
    }

    async function startGeneration() {
        if (requestRunning || reloadScheduled) return;
        requestRunning = true;
        window.clearTimeout(pollTimer);
        root.dataset.state = "QUEUED";
        startButton.disabled = true;
        startButton.textContent = "요청 중...";
        progressBox.hidden = false;
        stageLabel.textContent = "요청 전송 중";
        stepLabel.textContent = "0/4단계";
        message.textContent = "서버에 AI 요약 생성을 요청하고 있습니다.";

        try {
            const payload = await request(
                `/api/ai/policies/${encodeURIComponent(policyId)}/summary`,
                {method: "POST"}
            );
            applyStatus(payload);
        } catch (error) {
            showFailure(error);
        } finally {
            requestRunning = false;
        }
    }

    startButton.addEventListener("click", startGeneration);
    void loadStatus();
});
