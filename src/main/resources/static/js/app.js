(function () {
    var pending = null;

    function proceed(form, submitter) {
        form.removeAttribute('data-confirm');
        if (window.cbSaveScroll) {
            window.cbSaveScroll();
        }
        if (form.requestSubmit) {
            try {
                form.requestSubmit(submitter || undefined);
                return;
            } catch (e) {  }
        }
        form.submit();
    }

    document.addEventListener('submit', function (event) {
        var form = event.target;
        var message = form.getAttribute && form.getAttribute('data-confirm');
        if (!message) {
            return;
        }
        event.preventDefault();
        var modalEl = document.getElementById('cbConfirm');
        if (!modalEl || !window.bootstrap) {
            if (window.confirm(message)) {
                proceed(form, event.submitter);
            }
            return;
        }
        var yes = document.getElementById('cbConfirmYes');
        document.getElementById('cbConfirmText').textContent = message;
        yes.textContent = form.getAttribute('data-confirm-yes') || yes.getAttribute('data-default') || yes.textContent;
        pending = { form: form, submitter: event.submitter };
        window.bootstrap.Modal.getOrCreateInstance(modalEl).show();
    });

    document.addEventListener('click', function (event) {
        if (!event.target.closest || !event.target.closest('#cbConfirmYes')) {
            return;
        }
        var job = pending;
        pending = null;
        var modalEl = document.getElementById('cbConfirm');
        if (modalEl && window.bootstrap) {
            window.bootstrap.Modal.getOrCreateInstance(modalEl).hide();
        }
        if (job) {
            proceed(job.form, job.submitter);
        }
    });

    document.addEventListener('hidden.bs.modal', function (event) {
        if (event.target && event.target.id === 'cbConfirm') {
            pending = null;
        }
    });
})();

document.addEventListener('DOMContentLoaded', function () {
    document.querySelectorAll('.alert.cb-autoclose').forEach(function (alertEl) {
        setTimeout(function () {
            if (!document.body.contains(alertEl)) {
                return;
            }
            if (window.bootstrap && window.bootstrap.Alert) {
                window.bootstrap.Alert.getOrCreateInstance(alertEl).close();
            } else {
                alertEl.remove();
            }
        }, 5000);
    });
});

(function () {
    var NAME = 'clinicbook-consent';

    function read() {
        var match = document.cookie.match(new RegExp('(?:^|; )' + NAME + '=([^;]*)'));
        return match ? decodeURIComponent(match[1]) : null;
    }

    function write(value) {
        document.cookie = NAME + '=' + encodeURIComponent(value) + '; path=/; max-age=' + (60 * 60 * 24 * 180) + '; SameSite=Lax';
    }

    document.addEventListener('DOMContentLoaded', function () {
        var banner = document.getElementById('cbCookie');
        if (!banner) {
            return;
        }
        banner.hidden = read() !== null;
        banner.addEventListener('click', function (event) {
            var button = event.target.closest('[data-consent]');
            if (button) {
                write(button.getAttribute('data-consent'));
                banner.hidden = true;
            }
        });
        document.addEventListener('click', function (event) {
            if (event.target.closest && event.target.closest('[data-cookie-settings]')) {
                banner.hidden = false;
            }
        });
    });
})();

(function () {
    function language() {
        return (document.documentElement.getAttribute('lang') || 'vi').substring(0, 2);
    }

    function pad(n) {
        return ('0' + n).slice(-2);
    }

    function describeDay(value) {
        var parts = (value || '').split('-');
        if (parts.length !== 3) {
            return '';
        }
        var day = new Date(Number(parts[0]), Number(parts[1]) - 1, Number(parts[2]));
        if (isNaN(day.getTime())) {
            return '';
        }
        var weekday = day.toLocaleDateString(language() === 'en' ? 'en-GB' : 'vi-VN', { weekday: 'long' });
        return weekday + ', ' + pad(day.getDate()) + '/' + pad(day.getMonth() + 1) + '/' + day.getFullYear();
    }

    function showWeekday(input) {
        if (!input.id) {
            return;
        }
        var out = document.querySelector('[data-weekday-for="#' + input.id + '"]');
        if (out) {
            out.textContent = describeDay(input.value);
        }
    }

    document.addEventListener('change', function (event) {
        if (event.target.type === 'date') {
            showWeekday(event.target);
        }
    });
    document.addEventListener('input', function (event) {
        if (event.target.type === 'date') {
            showWeekday(event.target);
        }
    });

    document.addEventListener('submit', function (event) {
        var form = event.target;
        if (!form.hasAttribute || !form.hasAttribute('data-slot-form')) {
            return;
        }
        var date = form.querySelector('#slotDate');
        var start = form.querySelector('#startTime');
        var end = form.querySelector('#endTime');
        var problem = null;
        var field = null;
        if (!date.value) {
            problem = 'date'; field = date;
        } else if (!start.value) {
            problem = 'start'; field = start;
        } else if (!end.value) {
            problem = 'end'; field = end;
        } else if (end.value <= start.value) {
            problem = 'order'; field = end;
        } else if (new Date(date.value + 'T' + start.value) <= new Date()) {
            problem = 'past'; field = start;
        }
        var box = form.querySelector('[data-slot-error]');
        if (problem) {
            event.preventDefault();
            if (box) {
                box.textContent = form.getAttribute('data-msg-' + problem);
                box.classList.remove('d-none');
            }
            field.focus();
        } else if (box) {
            box.classList.add('d-none');
        }
    });
})();

document.addEventListener('DOMContentLoaded', function () {
    var path = window.location.pathname;
    var best = null;
    document.querySelectorAll('.cb-navpills [data-nav]').forEach(function (link) {
        var key = link.getAttribute('data-nav');
        var match = key === '/' ? (path === '/' || path === '') : path.indexOf(key) === 0;
        if (match && (!best || key.length > best.getAttribute('data-nav').length)) {
            best = link;
        }
    });
    if (best) {
        best.classList.add('active');
        best.setAttribute('aria-current', 'page');
    }
    var lang = (document.documentElement.getAttribute('lang') || 'vi').substring(0, 2);
    document.querySelectorAll('.cb-lang [data-lang]').forEach(function (a) {
        if (a.getAttribute('data-lang') === lang) {
            a.classList.add('active');
        }
    });
});

document.addEventListener('change', function (event) {
    var el = event.target;
    var auto = el.closest && (el.closest('[data-autosubmit]') || el.hasAttribute('data-autosubmit-field'));
    if (auto && el.form && (el.type === 'radio' || el.type === 'date' || el.tagName === 'SELECT')) {
        el.form.submit();
    }
});

document.addEventListener('click', function (event) {
    var button = event.target.closest('[data-toggle-password]');
    if (!button) {
        return;
    }
    var input = document.querySelector(button.getAttribute('data-toggle-password'));
    var icon = button.querySelector('.bi');
    if (!input) {
        return;
    }
    var show = input.type === 'password';
    input.type = show ? 'text' : 'password';
    if (icon) {
        icon.classList.toggle('bi-eye', !show);
        icon.classList.toggle('bi-eye-slash', show);
    }
});

document.addEventListener('input', function (event) {
    var input = event.target;
    var selector = input.getAttribute && input.getAttribute('data-strength-for');
    if (!selector) {
        return;
    }
    var meter = document.querySelector(selector);
    if (!meter) {
        return;
    }
    var value = input.value;
    var score = 0;
    if (value.length >= 8) { score++; }
    if (/[a-z]/.test(value) && /[A-Z]/.test(value)) { score++; }
    if (/\d/.test(value)) { score++; }
    if (/[^A-Za-z0-9]/.test(value)) { score++; }
    var level = value.length === 0 ? 0 : Math.max(1, score);
    meter.setAttribute('data-level', String(level));
    var labels = (meter.getAttribute('data-labels') || '').split('|');
    var label = meter.querySelector('[data-meter-label]');
    if (label) {
        label.textContent = level === 0 ? '-' : (labels[level - 1] || '');
    }
    var rules = {
        length: value.length >= 8,
        'case': /[a-z]/.test(value) && /[A-Z]/.test(value),
        digit: /\d/.test(value),
        symbol: /[^A-Za-z0-9]/.test(value)
    };
    meter.querySelectorAll('[data-rule]').forEach(function (item) {
        item.classList.toggle('is-met', !!rules[item.getAttribute('data-rule')]);
    });
});

document.addEventListener('submit', function (event) {
    var form = event.target;
    if (!form.hasAttribute || !form.hasAttribute('data-password-confirm')) {
        return;
    }
    var password = form.querySelector(form.getAttribute('data-password-confirm') || '#password');
    var confirm = form.querySelector('#confirmPassword');
    var message = form.querySelector('#confirmMismatch');
    if (password && confirm && password.value !== confirm.value) {
        event.preventDefault();
        confirm.classList.add('is-invalid');
        if (message) { message.classList.remove('d-none'); message.classList.add('d-block'); }
        confirm.focus();
    }
});
document.addEventListener('input', function (event) {
    if (event.target.id === 'confirmPassword') {
        event.target.classList.remove('is-invalid');
        var message = document.getElementById('confirmMismatch');
        if (message) { message.classList.add('d-none'); message.classList.remove('d-block'); }
    }
});

function updateMatchBadge(form) {
    var badge = form.querySelector('[data-match-badge]');
    var password = form.querySelector(form.getAttribute('data-password-confirm') || '#password');
    var confirm = form.querySelector('#confirmPassword');
    if (!badge || !password || !confirm) {
        return;
    }
    var matches = confirm.value.length > 0 && confirm.value === password.value;
    badge.classList.toggle('d-none', !matches);
}
document.addEventListener('input', function (event) {
    var form = event.target.form;
    if (form && form.hasAttribute && form.hasAttribute('data-password-confirm')) {
        updateMatchBadge(form);
    }
});

document.addEventListener('change', function (event) {
    var input = event.target;
    var target = input.getAttribute && input.getAttribute('data-mirror');
    if (!target || !input.checked) {
        return;
    }
    var out = document.querySelector(target);
    if (out) {
        out.textContent = input.getAttribute('data-label') || '';
        out.classList.remove('text-secondary', 'fw-normal');
    }
    var enables = input.getAttribute('data-enables');
    if (enables) {
        document.querySelectorAll(enables).forEach(function (el) { el.disabled = false; });
    }
});

(function () {
    var KEY = 'cb-scroll';

    function save() {
        try {
            sessionStorage.setItem(KEY, JSON.stringify({ path: window.location.pathname, y: window.scrollY, t: Date.now() }));
        } catch (e) {  }
    }

    function restore() {
        try {
            var raw = sessionStorage.getItem(KEY);
            if (!raw) {
                return;
            }
            sessionStorage.removeItem(KEY);
            var saved = JSON.parse(raw);
            if (saved.path === window.location.pathname && Date.now() - saved.t < 20000 && saved.y > 0) {
                window.scrollTo(0, saved.y);
                window.addEventListener('load', function () { window.scrollTo(0, saved.y); });
            }
        } catch (e) {  }
    }

    window.cbSaveScroll = save;
    document.addEventListener('DOMContentLoaded', restore);
    document.addEventListener('submit', function (event) {
        var form = event.target;
        if (!event.defaultPrevented && form.method && form.method.toLowerCase() === 'post') {
            save();
        }
    });
    document.addEventListener('click', function (event) {
        if (event.target.closest && event.target.closest('a[data-keep-scroll]')) {
            save();
        }
    });
})();
