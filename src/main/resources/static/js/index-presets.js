(() => {
  const modal = document.getElementById('presetModal');
  const openButton = document.getElementById('openPresetModal');
  const searchForm = document.querySelector('.search-card');
  const presetList = document.getElementById('presetList');
  const saveForm = document.getElementById('presetSaveForm');
  const nameInput = document.getElementById('presetName');

  if (!modal || !openButton || !searchForm || !presetList || !saveForm || !nameInput) {
    return;
  }

  let lastFocusedElement = null;

  const fields = ['keyword', 'district', 'ageGroup', 'category', 'targetKeyword'];

  const currentValues = () => Object.fromEntries(
    fields.map((name) => [name, searchForm.elements[name]?.value ?? ''])
  );

  const openModal = async () => {
    lastFocusedElement = document.activeElement;
    modal.classList.add('is-open');
    modal.setAttribute('aria-hidden', 'false');
    document.body.classList.add('preset-modal-open');
    nameInput.focus();
    await loadPresets();
  };

  const closeModal = () => {
    modal.classList.remove('is-open');
    modal.setAttribute('aria-hidden', 'true');
    document.body.classList.remove('preset-modal-open');
    lastFocusedElement?.focus();
  };

  const request = async (url, options = {}) => {
    const response = await fetch(url, {
      headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
      ...options
    });

    if (!response.ok) {
      const error = await response.json().catch(() => ({}));
      throw new Error(error.message || '프리셋을 처리하지 못했습니다.');
    }

    return response.status === 204 ? null : response.json();
  };

  const createButton = (text, className, action) => {
    const button = document.createElement('button');
    button.type = 'button';
    button.className = className;
    button.textContent = text;
    button.addEventListener('click', action);
    return button;
  };

  const renderPresets = (presets) => {
    presetList.replaceChildren();

    if (!presets.length) {
      const empty = document.createElement('p');
      empty.className = 'preset-empty';
      empty.textContent = '저장된 프리셋이 없습니다.';
      presetList.append(empty);
      return;
    }

    presets.forEach((preset) => {
      const item = document.createElement('article');
      item.className = 'preset-item';

      const text = document.createElement('div');
      const title = document.createElement('strong');
      const summary = document.createElement('p');
      title.textContent = preset.name;
      summary.textContent = presetSummary(preset);
      text.append(title, summary);

      const actions = document.createElement('div');
      actions.className = 'preset-item-actions';
      actions.append(
        createButton('불러오기', 'preset-load-button', () => applyPreset(preset)),
        createButton('삭제', 'preset-delete-button', () => deletePreset(preset.presetId))
      );

      item.append(text, actions);
      presetList.append(item);
    });
  };

  const presetSummary = (preset) => {
    const labels = fields
      .map((name) => {
        const field = searchForm.elements[name];
        if (!preset[name] || !field) return null;
        const option = Array.from(field.options || []).find((item) => item.value === preset[name]);
        return option ? option.textContent.trim() : preset[name];
      })
      .filter(Boolean);

    return labels.length ? labels.join(' · ') : '전체 조건';
  };

  const loadPresets = async () => {
    presetList.textContent = '프리셋을 불러오는 중입니다.';

    try {
      renderPresets(await request('/api/search-presets'));
    } catch (error) {
      presetList.textContent = error.message;
    }
  };

  const applyPreset = (preset) => {
    fields.forEach((name) => {
      if (searchForm.elements[name]) {
        searchForm.elements[name].value = preset[name] || '';
      }
    });
    closeModal();
  };

  const deletePreset = async (presetId) => {
    try {
      await request(`/api/search-presets/${presetId}`, { method: 'DELETE' });
      await loadPresets();
    } catch (error) {
      window.alert(error.message);
    }
  };

  openButton.addEventListener('click', openModal);

  modal.querySelectorAll('[data-preset-close]').forEach((element) => {
    element.addEventListener('click', closeModal);
  });

  document.addEventListener('keydown', (event) => {
    if (event.key === 'Escape' && modal.classList.contains('is-open')) {
      closeModal();
    }
  });

  saveForm.addEventListener('submit', async (event) => {
    event.preventDefault();

    try {
      await request('/api/search-presets', {
        method: 'POST',
        body: JSON.stringify({ name: nameInput.value, ...currentValues() })
      });
      nameInput.value = '';
      await loadPresets();
    } catch (error) {
      window.alert(error.message);
    }
  });
})();
