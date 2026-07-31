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

        function initializeAdminFloatingMenu() {
            const menu =
                document.querySelector(".admin-menu");
            const side =
                menu?.closest(".admin-side");

            if (
                !menu
                || !side
                || menu.dataset.floatingMenuInitialized
            ) {
                return;
            }

            menu.dataset.floatingMenuInitialized = "true";

            if (!menu.id) {
                menu.id = "adminFloatingMenu";
            }

            const links =
                Array.from(menu.querySelectorAll("a[href]"));
            let activeLink =
                menu.querySelector("a.active")
                || links.find((link) => {
                    const url =
                        new URL(link.href, window.location.href);

                    return (
                        url.pathname === window.location.pathname
                        && url.search === window.location.search
                    );
                })
                || links[0];
            let navigationPending = false;

            links.forEach((link) => {
                if (link === activeLink) {
                    link.classList.add("active");
                    link.setAttribute("aria-current", "page");
                } else {
                    link.removeAttribute("aria-current");
                }
            });

            const indicator =
                document.createElement("span");
            indicator.className =
                "admin-menu-active-indicator";
            indicator.setAttribute("aria-hidden", "true");
            menu.prepend(indicator);

            const toggle =
                document.createElement("button");
            toggle.type = "button";
            toggle.className = "admin-menu-toggle";
            toggle.setAttribute("aria-controls", menu.id);
            toggle.setAttribute("aria-expanded", "false");

            const toggleIcon =
                document.createElement("span");
            toggleIcon.className = "admin-menu-toggle-icon";
            toggleIcon.setAttribute("aria-hidden", "true");

            for (let index = 0; index < 3; index += 1) {
                toggleIcon.append(
                    document.createElement("span")
                );
            }

            toggle.append(toggleIcon);
            side.append(toggle);

            const compactQuery =
                window.matchMedia("(max-width: 1024px)");
            const reducedMotionQuery =
                window.matchMedia(
                    "(prefers-reduced-motion: reduce)"
                );

            function activeLabel() {
                return activeLink
                    ? activeLink.textContent
                        .replace(/\s+/g, " ")
                        .trim()
                    : "";
            }

            function updateToggleLabel(isOpen) {
                const currentLabel = activeLabel();
                const action = isOpen ? "닫기" : "열기";
                const current = currentLabel
                    ? `, 현재 ${currentLabel}`
                    : "";

                toggle.setAttribute(
                    "aria-label",
                    `관리자 메뉴 ${action}${current}`
                );
                toggle.title =
                    `관리자 메뉴 ${action}${current}`;
            }

            function updateIndicator(
                link = activeLink,
                animate = true
            ) {
                if (!link || !compactQuery.matches) {
                    return;
                }

                indicator.classList.toggle(
                    "is-positioning",
                    !animate
                );
                indicator.style.width =
                    `${link.offsetWidth}px`;
                indicator.style.height =
                    `${link.offsetHeight}px`;
                indicator.style.transform =
                    `translate3d(${link.offsetLeft}px, `
                    + `${link.offsetTop}px, 0)`;

                if (!animate) {
                    requestAnimationFrame(() => {
                        indicator.classList.remove(
                            "is-positioning"
                        );
                    });
                }
            }

            function setMenuOpen(
                isOpen,
                focusTarget = false
            ) {
                if (!compactQuery.matches) {
                    document.body.classList.remove(
                        "admin-menu-open"
                    );
                    toggle.setAttribute(
                        "aria-expanded",
                        "false"
                    );
                    menu.removeAttribute("aria-hidden");
                    menu.inert = false;
                    updateToggleLabel(false);
                    return;
                }

                document.body.classList.toggle(
                    "admin-menu-open",
                    isOpen
                );

                if (!isOpen && focusTarget) {
                    toggle.focus({
                        preventScroll: true
                    });
                }

                toggle.setAttribute(
                    "aria-expanded",
                    String(isOpen)
                );
                menu.setAttribute(
                    "aria-hidden",
                    String(!isOpen)
                );
                menu.inert = !isOpen;
                updateToggleLabel(isOpen);

                if (isOpen) {
                    requestAnimationFrame(() => {
                        updateIndicator(activeLink, false);

                        if (focusTarget) {
                            activeLink?.focus({
                                preventScroll: true
                            });
                        }
                    });
                }
            }

            function selectLink(link) {
                links.forEach((item) => {
                    item.classList.toggle(
                        "active",
                        item === link
                    );

                    if (item === link) {
                        item.setAttribute(
                            "aria-current",
                            "page"
                        );
                    } else {
                        item.removeAttribute(
                            "aria-current"
                        );
                    }
                });

                activeLink = link;
                updateToggleLabel(true);
                updateIndicator(link);
            }

            function syncViewport() {
                setMenuOpen(false);

                if (compactQuery.matches) {
                    menu.setAttribute(
                        "aria-hidden",
                        "true"
                    );
                    menu.inert = true;
                }
            }

            toggle.addEventListener("click", () => {
                if (navigationPending) {
                    return;
                }

                const isOpen =
                    !document.body.classList.contains(
                        "admin-menu-open"
                    );

                setMenuOpen(isOpen, isOpen);
            });

            menu.addEventListener("click", (event) => {
                const link =
                    event.target.closest("a[href]");

                if (
                    !link
                    || !compactQuery.matches
                    || event.defaultPrevented
                    || event.button !== 0
                    || event.metaKey
                    || event.ctrlKey
                    || event.shiftKey
                    || event.altKey
                ) {
                    return;
                }

                if (navigationPending) {
                    event.preventDefault();
                    return;
                }

                const url =
                    new URL(link.href, window.location.href);

                if (url.origin !== window.location.origin) {
                    return;
                }

                event.preventDefault();

                if (
                    url.pathname === window.location.pathname
                    && url.search === window.location.search
                ) {
                    selectLink(link);
                    setMenuOpen(false, true);
                    return;
                }

                selectLink(link);
                navigationPending = true;
                toggle.disabled = true;

                window.setTimeout(
                    () => {
                        document.body.classList.add(
                            "admin-page-leaving"
                        );
                    },
                    reducedMotionQuery.matches ? 0 : 230
                );

                window.setTimeout(
                    () => {
                        window.location.assign(url.href);
                    },
                    reducedMotionQuery.matches ? 0 : 420
                );
            });

            document.addEventListener(
                "pointerdown",
                (event) => {
                    if (
                        !compactQuery.matches
                        || !document.body.classList.contains(
                            "admin-menu-open"
                        )
                        || menu.contains(event.target)
                        || toggle.contains(event.target)
                    ) {
                        return;
                    }

                    setMenuOpen(false);
                }
            );

            document.addEventListener(
                "keydown",
                (event) => {
                    if (
                        event.key === "Escape"
                        && document.body.classList.contains(
                            "admin-menu-open"
                        )
                    ) {
                        setMenuOpen(false, true);
                    }
                }
            );

            compactQuery.addEventListener(
                "change",
                syncViewport
            );

            window.addEventListener("resize", () => {
                if (
                    compactQuery.matches
                    && document.body.classList.contains(
                        "admin-menu-open"
                    )
                ) {
                    updateIndicator(activeLink, false);
                }
            });

            window.addEventListener("pageshow", () => {
                navigationPending = false;
                toggle.disabled = false;
                document.body.classList.remove(
                    "admin-page-leaving"
                );
            });

            document.body.classList.add(
                "admin-floating-menu-ready"
            );
            updateToggleLabel(false);
            syncViewport();
        }

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
        initializeAdminFloatingMenu();
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
