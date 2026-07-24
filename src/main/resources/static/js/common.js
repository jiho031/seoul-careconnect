document.addEventListener("DOMContentLoaded", () => {
    const navToggle = document.querySelector(".nav-toggle");
    const mobileBreakpoint = 1320;

    function closeNavigation() {
        document.body.classList.remove("nav-open");

        if (navToggle) {
            navToggle.setAttribute("aria-expanded", "false");
        }
    }

    function openNavigation() {
        document.body.classList.add("nav-open");

        if (navToggle) {
            navToggle.setAttribute("aria-expanded", "true");
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

    // 햄버거 버튼
    if (navToggle) {
        navToggle.addEventListener("click", toggleNavigation);
    }

    // 모바일 메뉴에서 링크를 누르면 메뉴 닫기
    document
        .querySelectorAll("#globalNav a, .nav-actions a")
        .forEach((link) => {
            link.addEventListener("click", () => {
                if (window.innerWidth <= mobileBreakpoint) {
                    closeNavigation();
                }
            });
        });

    // 로그아웃 버튼을 누른 경우에도 메뉴 닫기
    document
        .querySelectorAll(".nav-actions button")
        .forEach((button) => {
            button.addEventListener("click", () => {
                if (window.innerWidth <= mobileBreakpoint) {
                    closeNavigation();
                }
            });
        });

    // 데스크톱 크기로 돌아가면 모바일 메뉴 상태 초기화
    window.addEventListener("resize", () => {
        if (window.innerWidth > mobileBreakpoint) {
            closeNavigation();
        }
    });

    // ESC 키로 메뉴 닫기
    document.addEventListener("keydown", (event) => {
        if (event.key === "Escape") {
            closeNavigation();
        }
    });

    // 아직 연결하지 않은 href="#" 링크의 화면 상단 이동 방지
    document
        .querySelectorAll('a[href="#"]')
        .forEach((link) => {
            link.addEventListener("click", (event) => {
                event.preventDefault();
            });
        });
});