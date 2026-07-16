const navToggle = document.querySelector('.nav-toggle');

if (navToggle) {
  navToggle.addEventListener('click', () => {
    const isOpen = document.body.classList.toggle('nav-open');
    navToggle.setAttribute('aria-expanded', String(isOpen));
  });
}

document.querySelectorAll('a[href="#"]').forEach((link) => {
  link.addEventListener('click', (event) => {
    event.preventDefault();
  });
});

document.addEventListener("DOMContentLoaded", function () {
  const openButton =
      document.querySelector("[data-admin-request-open]");

  const modal =
      document.getElementById("adminRequestModal");

  if (!openButton || !modal) {
    return;
  }

  const closeButtons =
      modal.querySelectorAll("[data-admin-request-close]");

  const form =
      document.getElementById("adminRequestForm");

  const fileInput =
      document.getElementById("adminRequestFile");

  const fileName =
      document.getElementById("adminRequestFileName");

  function openAdminRequestModal() {
    modal.hidden = false;
    document.body.classList.add("admin-request-open");

    const firstInput =
        modal.querySelector("select, textarea, input");

    if (firstInput) {
      setTimeout(() => firstInput.focus(), 0);
    }
  }

  function closeAdminRequestModal() {
    modal.hidden = true;
    document.body.classList.remove("admin-request-open");
    openButton.focus();
  }

  openButton.addEventListener(
      "click",
      openAdminRequestModal
  );

  closeButtons.forEach(function (button) {
    button.addEventListener(
        "click",
        closeAdminRequestModal
    );
  });

  document.addEventListener("keydown", function (event) {
    if (event.key === "Escape" && !modal.hidden) {
      closeAdminRequestModal();
    }
  });

  fileInput.addEventListener("change", function () {
    const file = fileInput.files[0];

    if (!file) {
      fileName.textContent =
          "선택된 파일이 없습니다.";
      return;
    }

    const maxFileSize = 10 * 1024 * 1024;

    if (file.size > maxFileSize) {
      alert("첨부파일은 10MB 이하만 가능합니다.");

      fileInput.value = "";
      fileName.textContent =
          "선택된 파일이 없습니다.";
      return;
    }

    fileName.textContent = file.name;
  });

  form.addEventListener("submit", function (event) {
    /*
     * 백엔드가 연결되면 아래 event.preventDefault()와
     * alert 부분을 삭제하면 실제 POST 요청이 전송됩니다.
     */
    event.preventDefault();

    if (!form.checkValidity()) {
      form.reportValidity();
      return;
    }

    alert(
        "관리자 권한 신청이 접수되었습니다.\n" +
        "최고관리자 검토 후 결과가 안내됩니다."
    );

    form.reset();
    fileName.textContent =
        "선택된 파일이 없습니다.";

    closeAdminRequestModal();
  });
});
