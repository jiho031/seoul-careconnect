document.addEventListener('DOMContentLoaded', () => {
    initFavoritePolicyPreview();
});

function initFavoritePolicyPreview() {

    const preview =
        document.getElementById('favoritePolicyPreview');

    const policyLinks =
        document.querySelectorAll('.favorite-preview-target');

    if (!preview || policyLinks.length === 0) {
        return;
    }

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

        const normalizedValue =
            normalizeValue(value);

        field.textContent =
            normalizedValue;

        field.hidden =
            normalizedValue.length === 0;
    };

    const setInfoRow = (rowName, value) => {

        const row = getRow(rowName);
        const field = getField(rowName);

        if (!row || !field) {
            return;
        }

        const normalizedValue =
            normalizeValue(value);

        field.textContent =
            normalizedValue;

        row.hidden =
            normalizedValue.length === 0;
    };

    const fillPreview = (link) => {

        setTextField(
            'title',
            link.dataset.previewTitle
            || '정책 정보'
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

        const linkRect =
            link.getBoundingClientRect();

        const previewWidth =
            preview.offsetWidth;

        const previewHeight =
            preview.offsetHeight;

        let left =
            linkRect.left
            + linkRect.width / 2
            - previewWidth / 2;

        left = Math.max(
            VIEWPORT_MARGIN,
            Math.min(
                left,
                window.innerWidth
                - previewWidth
                - VIEWPORT_MARGIN
            )
        );


        let top =
            linkRect.bottom
            + PREVIEW_GAP;

        let placement = 'bottom';

        if (
            top + previewHeight
            >
            window.innerHeight
            - VIEWPORT_MARGIN
        ) {

            top =
                linkRect.top
                - previewHeight
                - PREVIEW_GAP;

            placement = 'top';
        }


        top = Math.max(
            VIEWPORT_MARGIN,
            top
        );


        preview.style.left =
            `${Math.round(left)}px`;

        preview.style.top =
            `${Math.round(top)}px`;

        preview.dataset.placement =
            placement;
    };

    const showPreview = (link) => {

        fillPreview(link);

        preview.classList.remove(
            'is-visible'
        );

        preview.setAttribute(
            'aria-hidden',
            'true'
        );

        positionPreview(link);


        preview.classList.add(
            'is-visible'
        );

        preview.setAttribute(
            'aria-hidden',
            'false'
        );
    };

    const hidePreview = () => {

        if (hoverTimer !== null) {

            window.clearTimeout(
                hoverTimer
            );

            hoverTimer = null;
        }

        activeLink = null;

        preview.classList.remove(
            'is-visible'
        );

        preview.setAttribute(
            'aria-hidden',
            'true'
        );
    };

    policyLinks.forEach((link) => {

        link.addEventListener(
            'mouseenter',
            () => {

                hidePreview();

                activeLink = link;


                hoverTimer =
                    window.setTimeout(
                        () => {

                            const isStillHovered =
                                activeLink === link
                                &&
                                link.matches(':hover');


                            if (!isStillHovered) {
                                return;
                            }


                            showPreview(link);

                            hoverTimer = null;

                        },
                        HOVER_DELAY
                    );
            }
        );


        link.addEventListener(
            'mouseleave',
            hidePreview
        );

        link.addEventListener(
            'pointerdown',
            hidePreview
        );
    });

    window.addEventListener(
        'scroll',
        hidePreview,
        { passive: true }
    );


    window.addEventListener(
        'resize',
        hidePreview
    );


    document.addEventListener(
        'visibilitychange',
        () => {

            if (document.hidden) {
                hidePreview();
            }
        }
    );
}