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