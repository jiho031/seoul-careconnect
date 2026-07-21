document.addEventListener('DOMContentLoaded', () => {
    initPopularPolicyDragScroll();
    initSearchReset();
    initPopularPolicyPreview();
});

function initPopularPolicyDragScroll() {
    const slider = document.querySelector('[data-drag-scroll]');

    if (!slider) {
        return;
    }

    let isDragging = false;
    let startX = 0;
    let startScrollLeft = 0;
    let movedDistance = 0;
    let suppressNextClick = false;

    slider.addEventListener('pointerdown', (event) => {
        // 마우스 왼쪽 버튼만 직접 처리한다.
        if (event.pointerType !== 'mouse' || event.button !== 0) {
            return;
        }

        isDragging = true;
        startX = event.clientX;
        startScrollLeft = slider.scrollLeft;
        movedDistance = 0;
        suppressNextClick = false;

        slider.classList.add('dragging');
    });

    slider.addEventListener('pointermove', (event) => {
        if (!isDragging) {
            return;
        }

        const distance = event.clientX - startX;

        movedDistance = Math.max(
            movedDistance,
            Math.abs(distance)
        );

        // 8px 이상 움직인 경우에만 실제 드래그로 판단한다.
        if (movedDistance > 8) {
            event.preventDefault();
            slider.scrollLeft = startScrollLeft - distance;
        }
    });

    const finishDragging = () => {
        if (!isDragging) {
            return;
        }

        suppressNextClick = movedDistance > 8;
        isDragging = false;

        slider.classList.remove('dragging');
    };

    slider.addEventListener('pointerup', finishDragging);
    slider.addEventListener('pointercancel', finishDragging);
    slider.addEventListener('pointerleave', finishDragging);

    slider.addEventListener(
        'click',
        (event) => {
            if (!suppressNextClick) {
                return;
            }

            event.preventDefault();
            event.stopPropagation();

            suppressNextClick = false;
        },
        true
    );

    slider.addEventListener('dragstart', (event) => {
        event.preventDefault();
    });
}

function initSearchReset() {
    const resetSearchButton =
        document.getElementById('resetSearchButton');

    if (!resetSearchButton) {
        return;
    }

    resetSearchButton.addEventListener('click', () => {
        const searchForm =
            resetSearchButton.closest('form');

        if (!searchForm) {
            return;
        }

        const keyword =
            searchForm.querySelector('[name="keyword"]');

        if (keyword) {
            keyword.value = '';
        }

        searchForm.querySelectorAll('select').forEach((select) => {
            select.value = '';
        });

        const pageInput =
            searchForm.querySelector('input[name="page"]');

        if (pageInput) {
            pageInput.value = '0';
        }

        keyword?.focus();
    });
}

function initPopularPolicyPreview() {
    const preview =
        document.getElementById('popularPolicyPreview');

    const policyLinks =
        document.querySelectorAll('.popular-policy-link');

    const slider =
        document.querySelector('[data-drag-scroll]');

    if (!preview || policyLinks.length === 0) {
        return;
    }

    // 실제 마우스 포인터를 사용하는 환경에서만 실행한다.
    const canHover = window.matchMedia(
        '(hover: hover) and (pointer: fine)'
    ).matches;

    if (!canHover) {
        return;
    }

    const HOVER_DELAY = 500;
    const VIEWPORT_MARGIN = 12;
    const PREVIEW_GAP = 10;

    let hoverTimer = null;
    let activeLink = null;

    const getField = (fieldName) =>
        preview.querySelector(
            `[data-preview-field="${fieldName}"]`
        );

    const getRow = (rowName) =>
        preview.querySelector(
            `[data-preview-row="${rowName}"]`
        );

    const normalizeValue = (value) => {
        if (!value) {
            return '';
        }

        return value.trim();
    };

    const setTextField = (fieldName, value) => {
        const field = getField(fieldName);

        if (!field) {
            return;
        }

        const normalizedValue = normalizeValue(value);

        field.textContent = normalizedValue;
        field.hidden = normalizedValue.length === 0;
    };

    const setInfoRow = (rowName, value) => {
        const row = getRow(rowName);
        const field = getField(rowName);

        if (!row || !field) {
            return;
        }

        const normalizedValue = normalizeValue(value);

        field.textContent = normalizedValue;
        row.hidden = normalizedValue.length === 0;
    };

    const fillPreview = (link) => {
        setTextField(
            'title',
            link.dataset.previewTitle || '정책 정보'
        );

        setTextField(
            'category',
            link.dataset.previewCategory
        );

        setTextField(
            'status',
            link.dataset.previewStatus
        );

        setInfoRow(
            'agency',
            link.dataset.previewAgency
        );

        setInfoRow(
            'region',
            link.dataset.previewRegion
        );

        setInfoRow(
            'target',
            link.dataset.previewTarget
        );

        setInfoRow(
            'period',
            link.dataset.previewPeriod
        );
    };

    const positionPreview = (link) => {
        const linkRect = link.getBoundingClientRect();

        const previewWidth = preview.offsetWidth;
        const previewHeight = preview.offsetHeight;

        let left =
            linkRect.left +
            linkRect.width / 2 -
            previewWidth / 2;

        left = Math.max(
            VIEWPORT_MARGIN,
            Math.min(
                left,
                window.innerWidth -
                previewWidth -
                VIEWPORT_MARGIN
            )
        );

        let top = linkRect.bottom + PREVIEW_GAP;
        let placement = 'bottom';

        // 아래쪽 공간이 부족하면 버튼 위에 표시한다.
        if (
            top + previewHeight >
            window.innerHeight - VIEWPORT_MARGIN
        ) {
            top =
                linkRect.top -
                previewHeight -
                PREVIEW_GAP;

            placement = 'top';
        }

        top = Math.max(VIEWPORT_MARGIN, top);

        preview.style.left = `${Math.round(left)}px`;
        preview.style.top = `${Math.round(top)}px`;

        preview.dataset.placement = placement;
    };

    const showPreview = (link) => {
        fillPreview(link);

        preview.classList.remove('is-visible');
        preview.setAttribute('aria-hidden', 'true');

        positionPreview(link);

        preview.classList.add('is-visible');
        preview.setAttribute('aria-hidden', 'false');
    };

    const hidePreview = () => {
        if (hoverTimer !== null) {
            window.clearTimeout(hoverTimer);
            hoverTimer = null;
        }

        activeLink = null;

        preview.classList.remove('is-visible');
        preview.setAttribute('aria-hidden', 'true');
    };

    policyLinks.forEach((link) => {
        link.addEventListener('mouseenter', () => {
            hidePreview();

            activeLink = link;

            hoverTimer = window.setTimeout(() => {
                const isStillHovered =
                    activeLink === link &&
                    link.matches(':hover');

                const isDragging =
                    slider?.classList.contains('dragging');

                if (!isStillHovered || isDragging) {
                    return;
                }

                showPreview(link);
                hoverTimer = null;
            }, HOVER_DELAY);
        });

        link.addEventListener('mouseleave', hidePreview);

        // 드래그 또는 클릭을 시작할 때도 미리보기를 제거한다.
        link.addEventListener('pointerdown', hidePreview);
    });

    // 목록을 스크롤하거나 화면 위치가 바뀌면 미리보기를 닫는다.
    slider?.addEventListener(
        'scroll',
        hidePreview,
        { passive: true }
    );

    window.addEventListener(
        'scroll',
        hidePreview,
        { passive: true }
    );

    window.addEventListener('resize', hidePreview);

    document.addEventListener('visibilitychange', () => {
        if (document.hidden) {
            hidePreview();
        }
    });
}