document.addEventListener("DOMContentLoaded", () => {
    const modal = document.getElementById("sessionTimeoutModal");

    // 로그인하지 않은 화면에서는 실행하지 않는다.
    if (!modal) {
        return;
    }

    const remainingText =
        document.getElementById("sessionRemainingText");

    const extendButton =
        document.getElementById("sessionExtendButton");

    const logoutButton =
        document.getElementById("sessionLogoutButton");

    const SESSION_SECONDS = 30 * 60;
    const WARNING_SECONDS = 5 * 60;
    // 10초 후 팝업, 20초 카운트다운 후 자동 로그아웃
    // const SESSION_SECONDS = 30;
    // const WARNING_SECONDS = 20;
    const WARNING_START_SECONDS =
        SESSION_SECONDS - WARNING_SECONDS;

    /*
     * 사용자 활동이 있을 때마다 서버 요청을 보내면 요청이 너무 많아지므로
     * 최소 1분 간격으로만 세션 연장 요청을 보낸다.
     */
    const KEEP_ALIVE_INTERVAL = 60 * 1000;

    let lastActivityAt = Date.now();
    let lastKeepAliveAt = Date.now();
    let modalOpened = false;
    let checkingInterval = null;

    const activityEvents = [
        "click",
        "keydown",
        "scroll",
        "touchstart"
    ];

    function formatRemainingTime(seconds) {
        const safeSeconds = Math.max(0, seconds);
        const minutes = Math.floor(safeSeconds / 60);
        const remainSeconds = safeSeconds % 60;

        return `${minutes}분 ${String(remainSeconds).padStart(2, "0")}초`;
    }

    function openModal(remainingSeconds) {
        modalOpened = true;

        remainingText.textContent =
            formatRemainingTime(remainingSeconds);

        modal.classList.add("is-open");
        modal.setAttribute("aria-hidden", "false");

        extendButton.focus();
    }

    function closeModal() {
        modalOpened = false;

        modal.classList.remove("is-open");
        modal.setAttribute("aria-hidden", "true");
    }

    async function extendSession(showFailureAlert = false) {
        try {
            const response = await fetch("/session/extend", {
                method: "POST",
                credentials: "same-origin",
                headers: {
                    "X-Requested-With": "XMLHttpRequest"
                }
            });

            /*
             * 세션이 이미 만료된 경우 Spring Security가
             * 로그인 화면으로 리디렉션할 수 있다.
             */
            if (
                !response.ok ||
                response.redirected ||
                response.url.includes("/login")
            ) {
                window.location.href = "/login?expired=true";
                return false;
            }

            lastActivityAt = Date.now();
            lastKeepAliveAt = Date.now();

            closeModal();

            return true;
        } catch (error) {
            console.error("세션 연장 실패:", error);

            if (showFailureAlert) {
                alert(
                    "로그인 시간을 연장하지 못했습니다. 다시 로그인해주세요."
                );
            }

            return false;
        }
    }

    function logout(reason = "") {
        const form = document.createElement("form");

        form.method = "post";
        form.action = "/logout";

        if (reason) {
            const reasonInput =
                document.createElement("input");

            reasonInput.type = "hidden";
            reasonInput.name = "reason";
            reasonInput.value = reason;

            form.appendChild(reasonInput);
        }

        document.body.appendChild(form);
        form.submit();
    }

    function checkSessionTime() {
        const elapsedSeconds = Math.floor(
            (Date.now() - lastActivityAt) / 1000
        );

        const remainingSeconds =
            SESSION_SECONDS - elapsedSeconds;

        if (remainingSeconds <= 0) {
            logout("expired");
            return;
        }

        if (remainingSeconds <= WARNING_SECONDS) {
            if (!modalOpened) {
                openModal(remainingSeconds);
            } else {
                remainingText.textContent =
                    formatRemainingTime(remainingSeconds);
            }

            return;
        }

        if (modalOpened) {
            closeModal();
        }
    }

    function registerActivity() {
        /*
         * 경고창이 열린 뒤에는 단순 마우스나 키보드 동작만으로
         * 연장하지 않고 반드시 로그인 연장 버튼을 누르게 한다.
         */
        if (modalOpened) {
            return;
        }

        lastActivityAt = Date.now();

        const now = Date.now();

        if (now - lastKeepAliveAt >= KEEP_ALIVE_INTERVAL) {
            extendSession(false);
        }
    }

    activityEvents.forEach((eventName) => {
        window.addEventListener(
            eventName,
            registerActivity,
            { passive: true }
        );
    });

    /*
     * 브라우저 탭을 오래 비웠다가 다시 돌아왔을 때
     * 실제 경과시간을 즉시 확인한다.
     */
    document.addEventListener("visibilitychange", () => {
        if (!document.hidden) {
            checkSessionTime();
        }
    });

    extendButton.addEventListener("click", async () => {
        extendButton.disabled = true;
        extendButton.textContent = "연장 중...";

        const success = await extendSession(true);

        extendButton.disabled = false;
        extendButton.textContent = "로그인 연장";

        if (!success) {
            window.location.href = "/login?expired=true";
        }
    });

    logoutButton.addEventListener("click", () => {
        logout();
    });

    checkingInterval = window.setInterval(
        checkSessionTime,
        1000
    );

    window.addEventListener("beforeunload", () => {
        if (checkingInterval) {
            clearInterval(checkingInterval);
        }
    });
});