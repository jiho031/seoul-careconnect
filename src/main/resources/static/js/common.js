(() => {
    /*
     * nav.html과 개별 페이지에서 common.js가 동시에 로드되어도
     * 이벤트를 한 번만 등록한다.
     */
    if (window.__SCC_COMMON_INITIALIZED__) {
        return;
    }

    window.__SCC_COMMON_INITIALIZED__ = true;

    function initializeCommonNavigation() {
        const navToggle =
            document.querySelector(".nav-toggle");

        const mobileBreakpoint = 1320;

    function initializeFormResets() {
        document
            .querySelectorAll("[data-reset-form]")
            .forEach((button) => {
                button.addEventListener("click", () => {
                    const form =
                        button.form
                        || button
                            .closest(".admin-card, .policy-container")
                            ?.querySelector("form");

                    if (!form) {
                        return;
                    }

                    Array.from(form.elements).forEach((control) => {
                        if (control.tagName === "SELECT") {
                            const emptyOption =
                                Array.from(control.options)
                                    .find((option) =>
                                        option.value === ""
                                    );

                            control.value = emptyOption
                                ? ""
                                : control.options[0]?.value || "";
                            return;
                        }

                        if (control.tagName !== "INPUT") {
                            return;
                        }

                        if (control.type === "hidden") {
                            control.value =
                                control.name === "page"
                                    ? "0"
                                    : "";
                            return;
                        }

                        if (
                            control.type === "checkbox"
                            || control.type === "radio"
                        ) {
                            control.checked = false;
                            return;
                        }

                        if (
                            control.type !== "submit"
                            && control.type !== "button"
                        ) {
                            control.value = "";
                        }
                    });

                    form
                        .querySelector(
                            "input:not([type='hidden']), select"
                        )
                        ?.focus();
                });
            });
    }

    let paginationRequestController = null;

    function initializeAsyncPagination() {
        document.addEventListener("click", async (event) => {
            if (!(event.target instanceof Element)) {
                return;
            }

            const link =
                event.target.closest(
                    "[data-async-pagination] a[href]"
                );

            if (
                !link
                || event.defaultPrevented
                || event.button !== 0
                || event.metaKey
                || event.ctrlKey
                || event.shiftKey
                || event.altKey
            ) {
                return;
            }

            if (
                link.classList.contains("disabled")
                || link.getAttribute("aria-current") === "page"
            ) {
                event.preventDefault();
                return;
            }

            const container =
                link.closest("[data-pagination-container]");

            if (!container) {
                return;
            }

            const containerName =
                container.dataset.paginationContainer;
            const url =
                new URL(link.href, window.location.href);

            if (
                !containerName
                || url.origin !== window.location.origin
            ) {
                return;
            }

            event.preventDefault();

            const scrollX = window.scrollX;
            const scrollY = window.scrollY;

            paginationRequestController?.abort();
            paginationRequestController =
                new AbortController();

            container.setAttribute("aria-busy", "true");

            try {
                const response = await fetch(url, {
                    headers: {
                        "X-Requested-With": "XMLHttpRequest"
                    },
                    signal:
                        paginationRequestController.signal
                });

                if (!response.ok) {
                    throw new Error(
                        `페이지 조회 실패: ${response.status}`
                    );
                }

                const nextDocument =
                    new DOMParser().parseFromString(
                        await response.text(),
                        "text/html"
                    );

                const nextContainer =
                    Array.from(
                        nextDocument.querySelectorAll(
                            "[data-pagination-container]"
                        )
                    ).find((element) =>
                        element.dataset.paginationContainer
                        === containerName
                    );

                if (!nextContainer) {
                    throw new Error(
                        "페이지 목록 영역을 찾지 못했습니다."
                    );
                }

                container.replaceWith(nextContainer);
                window.history.replaceState(
                    null,
                    "",
                    url.href
                );
                window.scrollTo(scrollX, scrollY);

                nextContainer
                    .querySelector('[aria-current="page"]')
                    ?.focus({ preventScroll: true });
            } catch (error) {
                if (error.name === "AbortError") {
                    return;
                }

                window.location.assign(url.href);
            }
        });
    }

        function closeNavigation() {
            document.body.classList.remove("nav-open");

            if (navToggle) {
                navToggle.setAttribute(
                    "aria-expanded",
                    "false"
                );

                navToggle.setAttribute(
                    "aria-label",
                    "메뉴 열기"
                );
            }
        }

        function openNavigation() {
            document.body.classList.add("nav-open");

            if (navToggle) {
                navToggle.setAttribute(
                    "aria-expanded",
                    "true"
                );

                navToggle.setAttribute(
                    "aria-label",
                    "메뉴 닫기"
                );
            }
        }

        function toggleNavigation() {
            const isOpen =
                document.body.classList.contains("nav-open");

            if (isOpen) {
                closeNavigation();
            } else {
                openNavigation();
            }
        }

        /*
         * 햄버거 버튼
         */
        if (navToggle) {
            navToggle.addEventListener(
                "click",
                toggleNavigation
            );
        }

        /*
         * 모바일 메뉴의 링크를 누르면 메뉴 닫기
         */
        document
            .querySelectorAll(
                "#globalNav a, .nav-actions a"
            )
            .forEach((link) => {
                link.addEventListener("click", () => {
                    if (
                        window.innerWidth <=
                        mobileBreakpoint
                    ) {
                        closeNavigation();
                    }
                });
            });

        /*
         * 로그아웃 버튼 클릭 시 메뉴 닫기
         */
        document
            .querySelectorAll(
                ".nav-actions button"
            )
            .forEach((button) => {
                button.addEventListener("click", () => {
                    if (
                        window.innerWidth <=
                        mobileBreakpoint
                    ) {
                        closeNavigation();
                    }
                });
            });

        /*
         * 데스크톱 화면으로 전환하면
         * 모바일 메뉴 상태 초기화
         */
        window.addEventListener("resize", () => {
            if (
                window.innerWidth >
                mobileBreakpoint
            ) {
                closeNavigation();
            }
        });

        /*
         * ESC 키로 메뉴 닫기
         */
        document.addEventListener(
            "keydown",
            (event) => {
                if (event.key === "Escape") {
                    closeNavigation();
                }
            }
        );

        /*
         * 아직 연결되지 않은 href="#" 링크의
         * 화면 상단 이동 방지
         */
        document
            .querySelectorAll('a[href="#"]')
            .forEach((link) => {
                link.addEventListener(
                    "click",
                    (event) => {
                        event.preventDefault();
                    }
                );
            });

        initializeFormResets();
        initializeAsyncPagination();
    }

    /*
     * 스크립트가 DOM 생성 전이나 후 어느 시점에 로드되어도 실행
     */
    if (document.readyState === "loading") {
        document.addEventListener(
            "DOMContentLoaded",
            initializeCommonNavigation,
            { once: true }
        );
    } else {
        initializeCommonNavigation();
    }
})();
