package de.cubevote;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.Locale;

public class MainActivity extends Activity {

    private SharedPreferences p;
    private TextView voteState;
    private Button timeBtn;
    private LinearLayout siteBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        p = Sites.prefs(this);

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(18);
        root.setPadding(pad, pad, pad, pad);
        scroll.addView(root);

        root.addView(heading("Cubeside Voter", 26));
        root.addView(small("Votet nacheinander auf allen Listen für cubeside.de."));

        // Name
        root.addView(heading("Minecraft-Name", 17));
        EditText nameField = new EditText(this);
        nameField.setSingleLine(true);
        nameField.setHint("z. B. _Rewe");
        nameField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        nameField.setText(p.getString("name", ""));
        nameField.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) {}
            public void afterTextChanged(Editable s) {
                p.edit().putString("name", s.toString().trim()).apply();
            }
        });
        root.addView(nameField);

        // Status + Start
        voteState = new TextView(this);
        voteState.setTextSize(16);
        voteState.setPadding(0, dp(14), 0, dp(6));
        root.addView(voteState);

        Button start = new Button(this);
        start.setText("Jetzt voten ▶");
        start.setTextSize(18);
        start.setOnClickListener(v -> {
            if (p.getString("name", "").isEmpty()) {
                Toast.makeText(this, "Bitte zuerst deinen Namen eintragen", Toast.LENGTH_SHORT).show();
                return;
            }
            startActivity(new Intent(this, VoteActivity.class));
        });
        root.addView(start);

        Button stats = new Button(this);
        stats.setText("Meine Vote-Statistik");
        stats.setOnClickListener(v -> startActivity(
                new Intent(this, VoteActivity.class).putExtra(VoteActivity.EXTRA_STATS_ONLY, true)));
        root.addView(stats);

        // Erinnerung
        root.addView(heading("Erinnerung", 17));
        CheckBox rem = new CheckBox(this);
        rem.setText("Täglich erinnern, wenn noch nicht gevotet");
        rem.setChecked(p.getBoolean("reminder", true));
        rem.setOnCheckedChangeListener((b, on) -> {
            p.edit().putBoolean("reminder", on).apply();
            ReminderReceiver.schedule(this);
        });
        root.addView(rem);

        timeBtn = new Button(this);
        timeBtn.setOnClickListener(v -> new TimePickerDialog(this, (tp, h, m) -> {
            p.edit().putInt("remHour", h).putInt("remMin", m).apply();
            updateTime();
            ReminderReceiver.schedule(this);
        }, p.getInt("remHour", 18), p.getInt("remMin", 0), true).show());
        root.addView(timeBtn);
        updateTime();

        // Seiten
        root.addView(heading("Vote-Seiten", 17));
        root.addView(small("Haken = wird gevotet. Lange drücken = Adresse ändern."));
        siteBox = new LinearLayout(this);
        siteBox.setOrientation(LinearLayout.VERTICAL);
        root.addView(siteBox);
        buildSiteList();

        setContentView(scroll);

        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 7);
        }
        ReminderReceiver.schedule(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Sites.votedToday(this)) {
            voteState.setText("✅ Heute schon gevotet");
            voteState.setTextColor(Color.parseColor("#2E7D32"));
        } else {
            voteState.setText("⏳ Heute noch nicht gevotet");
            voteState.setTextColor(Color.parseColor("#C62828"));
        }
    }

    private void buildSiteList() {
        siteBox.removeAllViews();
        for (JSONObject s : Sites.all(this)) {
            String id = s.optString("id");
            CheckBox cb = new CheckBox(this);
            boolean custom = !s.optString("url").equals(s.optString("defaultUrl"));
            cb.setText(s.optString("name") + (custom ? "  (eigene Adresse)" : ""));
            cb.setChecked(p.getBoolean("en_" + id, true));
            cb.setOnCheckedChangeListener((b, on) -> p.edit().putBoolean("en_" + id, on).apply());
            cb.setOnLongClickListener(v -> {
                editUrl(s);
                return true;
            });
            siteBox.addView(cb);
        }
    }

    private void editUrl(JSONObject s) {
        String id = s.optString("id");
        EditText e = new EditText(this);
        e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        e.setText(s.optString("url"));
        new AlertDialog.Builder(this)
                .setTitle(s.optString("name"))
                .setView(e)
                .setPositiveButton("Speichern", (d, w) -> {
                    p.edit().putString("url_" + id, e.getText().toString().trim()).apply();
                    buildSiteList();
                })
                .setNeutralButton("Standard", (d, w) -> {
                    p.edit().remove("url_" + id).apply();
                    buildSiteList();
                })
                .setNegativeButton("Abbrechen", null)
                .show();
    }

    private void updateTime() {
        timeBtn.setText(String.format(Locale.GERMANY, "Uhrzeit: %02d:%02d Uhr",
                p.getInt("remHour", 18), p.getInt("remMin", 0)));
    }

    private TextView heading(String text, int size) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(size);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextColor(Color.parseColor("#1B5E20"));
        t.setPadding(0, dp(16), 0, dp(4));
        return t;
    }

    private TextView small(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(14);
        t.setTextColor(Color.DKGRAY);
        return t;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
