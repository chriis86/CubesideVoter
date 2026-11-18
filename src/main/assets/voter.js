// Cubeside Voter – läuft in jeder Vote-Seite.
// Trägt den Namen ein, wartet bis das Captcha gelöst ist, klickt dann auf "Voten"
// und meldet der App, sobald die Seite "erfolgreich" oder "schon gevotet" anzeigt.
(function () {
  if (window.CV) return;

  function bodyText() {
    return document.body ? document.body.innerText : "";
  }

  function first(sel) {
    if (!sel) return null;
    var parts = sel.split(",");
    for (var i = 0; i < parts.length; i++) {
      try {
        var el = document.querySelector(parts[i].trim());
        if (el) return el;
      } catch (e) { /* ungültiger Selektor -> nächster */ }
    }
    return null;
  }

  function visible(el) {
    return el && el.offsetParent !== null && el.type !== "hidden";
  }

  // Falls sich die Seite geändert hat: irgendein sichtbares Namensfeld suchen
  function fallbackNameField() {
    var list = document.querySelectorAll("input");
    for (var i = 0; i < list.length; i++) {
      var el = list[i];
      var key = ((el.name || "") + " " + (el.id || "") + " " + (el.placeholder || "")).toLowerCase();
      if (visible(el) && /user|name|nick|ign|spieler/.test(key) && !/captcha|search|suche|mail/.test(key)) return el;
    }
    return null;
  }

  function setValue(el, v) {
    if (!el) return false;
    if (el.value === v) return true;
    var setter = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, "value").set;
    setter.call(el, v);
    el.dispatchEvent(new Event("input", { bubbles: true }));
    el.dispatchEvent(new Event("change", { bubbles: true }));
    return true;
  }

  // 'none' = kein Captcha auf der Seite, 'pending' = noch nicht gelöst, 'solved' = gelöst
  function captchaState() {
    var fields = document.querySelectorAll(
      '[name="cf-turnstile-response"],[name="g-recaptcha-response"],[name="h-captcha-response"]'
    );
    var widget = document.querySelector(
      '.cf-turnstile,.g-recaptcha,.h-captcha,iframe[src*="recaptcha"],iframe[src*="turnstile"],iframe[src*="hcaptcha"],iframe[src*="challenges.cloudflare"]'
    );
    if (fields.length === 0 && !widget) return "none";
    for (var i = 0; i < fields.length; i++) {
      if (fields[i].value && fields[i].value.length > 0) return "solved";
    }
    return "pending";
  }

  function run(site, name, idx) {
    if (window.__cvRunning) return;
    window.__cvRunning = true;

    var filled = false, clicks = 0, lastClick = 0, ticks = 0, finished = false;

    var timer = setInterval(function () {
      ticks++;
      if (finished) return;

      // 1) Erfolg oder "heute schon gevotet"?
      var text = bodyText();
      for (var i = 0; i < site.ok.length; i++) {
        if (text.indexOf(site.ok[i]) >= 0) {
          finished = true;
          clearInterval(timer);
          CVBridge.done(idx);
          return;
        }
      }

      // 2) Namen eintragen
      if (!filled) {
        var input = first(site.nameSel);
        if (!input) input = fallbackNameField();
        if (setValue(input, name)) {
          filled = true;
          if (site.focus) {
            var f = first(site.focus);
            if (f) f.focus();
          }
        }
      }

      // 3) Häkchen setzen (z. B. Datenschutz akzeptieren)
      (site.checks || []).forEach(function (sel) {
        var c = first(sel);
        if (c && !c.checked) c.click();
      });

      if (!filled) {
        CVBridge.status("Suche Namensfeld …");
        return;
      }

      // 4) Seiten, auf denen du selbst klicken musst
      if (!site.btn) {
        CVBridge.status(site.manual || "Bitte selbst auf Voten tippen");
        return;
      }

      // 5) Warten bis das Captcha gelöst ist, dann klicken
      var state = captchaState();
      if (state === "pending") {
        CVBridge.status("Warte auf Captcha – falls nötig, bitte antippen");
        return;
      }
      var now = Date.now();
      if (ticks >= 3 && clicks < 3 && now - lastClick > 10000) {
        var b = first(site.btn);
        if (b) {
          clicks++;
          lastClick = now;
          CVBridge.status("Vote wird abgeschickt …");
          b.click();
        } else {
          CVBridge.status("Vote-Knopf nicht gefunden – bitte selbst tippen");
        }
      }
    }, 1000);
  }

  window.CV = { run: run };
})();
