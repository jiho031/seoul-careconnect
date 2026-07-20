document.addEventListener('DOMContentLoaded', () => {
    const slider = document.querySelector('[data-drag-scroll]');

    if (!slider) {
        return;
    }

    let isDragging = false;
    let startX = 0;
    let startScrollLeft = 0;
    let movedDistance = 0;

    slider.addEventListener('pointerdown', (event) => {
        // 마우스 왼쪽 버튼만 직접 처리
        if (event.pointerType !== 'mouse' || event.button !== 0) {
            return;
        }

        isDragging = true;
        startX = event.clientX;
        startScrollLeft = slider.scrollLeft;
        movedDistance = 0;

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

        // 8px 이상 움직였을 때만 실제 드래그 처리
        if (movedDistance > 8) {
            event.preventDefault();
            slider.scrollLeft = startScrollLeft - distance;
        }
    });

    const finishDragging = () => {
        if (!isDragging) {
            return;
        }

        isDragging = false;
        slider.classList.remove('dragging');
    };

    slider.addEventListener('pointerup', finishDragging);
    slider.addEventListener('pointercancel', finishDragging);
    slider.addEventListener('pointerleave', finishDragging);

    slider.addEventListener(
        'click',
        (event) => {

            if (movedDistance <= 8) {
                return;
            }

            event.preventDefault();
            event.stopPropagation();
            movedDistance = 0;
        },
        true
    );

    slider.addEventListener('dragstart', (event) => {
        event.preventDefault();
    });

    const resetSearchButton =
        document.getElementById('resetSearchButton');

    if (resetSearchButton) {
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
});