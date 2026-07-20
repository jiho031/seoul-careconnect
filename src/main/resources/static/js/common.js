document.addEventListener("DOMContentLoaded", () => {
  const navToggle = document.querySelector(".nav-toggle");

  if (navToggle) {
    navToggle.addEventListener("click", () => {
      const isOpen =
          document.body.classList.toggle("nav-open");

      navToggle.setAttribute(
          "aria-expanded",
          String(isOpen)
      );
    });
  }

  /*
   * 아직 연결되지 않은 href="#" 링크의
   * 화면 맨 위 이동 방지
   */
  document
      .querySelectorAll('a[href="#"]')
      .forEach((link) => {
        link.addEventListener("click", (event) => {
          event.preventDefault();
        });
      });
});