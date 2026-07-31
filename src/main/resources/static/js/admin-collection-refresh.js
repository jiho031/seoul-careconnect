(() => {
    const parser = new DOMParser();
    const collectionUrl = "/admin/collection";

    const parsePage = html => parser.parseFromString(html, "text/html");

    const replaceSections = (nextPage, selectors) => {
        const pairs = selectors.map(selector => ({
            current: document.querySelector(selector),
            replacement: nextPage.querySelector(selector)
        }));

        if (pairs.some(pair => !pair.current || !pair.replacement)) {
            throw new Error("새로고침할 화면 영역을 찾지 못했습니다.");
        }

        pairs.forEach(pair => {
            pair.current.replaceWith(pair.replacement);
        });
    };

    const loadCollectionPage = async options => {
        const response = await fetch(options.url || collectionUrl, {
            method: options.method || "GET",
            body: options.body,
            credentials: "same-origin",
            cache: "no-store",
            headers: {
                "X-Requested-With": "XMLHttpRequest"
            }
        });

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }

        return parsePage(await response.text());
    };

    const showRefreshError = message => {
        const container = document.getElementById("collectionResultMessages");
        if (!container) return;

        const error = document.createElement("div");
        error.className = "result-message error";
        error.textContent = message;
        container.replaceChildren(error);
        error.scrollIntoView({behavior: "smooth", block: "nearest"});
    };

    const refresh = async (button, selectors) => {
        button.classList.add("loading");
        button.disabled = true;

        try {
            const nextPage = await loadCollectionPage({});
            replaceSections(nextPage, selectors);
        } catch (error) {
            button.classList.remove("loading");
            button.disabled = false;
            showRefreshError("수집 현황을 새로고침하지 못했습니다. 잠시 후 다시 시도해 주세요.");
        }
    };

    document.addEventListener("click", event => {
        const statusButton = event.target.closest("#refreshApiStatusBtn");
        if (statusButton) {
            refresh(
                statusButton,
                ["#collectionStats", "#apiStatusCard", "#collectionScheduleCard"]
            );
            return;
        }

        const logButton = event.target.closest("#refreshApiLogBtn");
        if (logButton) {
            refresh(logButton, ["#apiLogCard"]);
        }
    });

    document.addEventListener("submit", async event => {
        const form = event.target.closest("[data-api-collect-form]");
        if (!form) return;

        event.preventDefault();

        const submitButton = form.querySelector("button[type='submit']");
        const originalText = submitButton?.textContent;

        if (submitButton) {
            submitButton.disabled = true;
            submitButton.textContent = "수집 중...";
        }

        try {
            const nextPage = await loadCollectionPage({
                url: form.action,
                method: "POST",
                body: new FormData(form)
            });

            replaceSections(
                nextPage,
                [
                    "#collectionResultMessages",
                    "#collectionStats",
                    "#apiStatusCard",
                    "#apiLogCard",
                    "#collectionScheduleCard"
                ]
            );

            document
                .getElementById("collectionResultMessages")
                ?.scrollIntoView({behavior: "smooth", block: "nearest"});
        } catch (error) {
            if (submitButton) {
                submitButton.disabled = false;
                submitButton.textContent = originalText;
            }
            showRefreshError("이 API를 수집하지 못했습니다. 잠시 후 다시 시도해 주세요.");
        }
    });
})();
