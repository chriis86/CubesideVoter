package de.cubevote;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Liest die Vote-Seiten aus assets/sites.json und die Einstellungen des Nutzers. */
public final class Sites {
    public static final String PREFS = "voter";
    public static final String STATS_URL = "https://stats.cubeside.de/list/main/votes/?player=";

    private Sites() {}

    public static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static String readAsset(Context c, String name) {
        try (InputStream in = c.getAssets().open(name)) {
            byte[] buf = new byte[in.available()];
            int off = 0;
            while (off < buf.length) {
                int r = in.read(buf, off, buf.length - off);
                if (r < 0) break;
                off += r;
            }
            return new String(buf, 0, off, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    /** Alle Seiten (auch deaktivierte), mit ggf. geänderter URL. */
    public static List<JSONObject> all(Context c) {
        List<JSONObject> list = new ArrayList<>();
        SharedPreferences p = prefs(c);
        try {
            JSONArray arr = new JSONArray(readAsset(c, "sites.json"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject s = arr.getJSONObject(i);
                String id = s.getString("id");
                s.put("defaultUrl", s.getString("url"));
                String custom = p.getString("url_" + id, null);
                if (custom != null && !custom.isEmpty()) s.put("url", custom);
                list.add(s);
            }
        } catch (Exception ignored) {}
        return list;
    }

    /** Nur die eingeschalteten Seiten in Reihenfolge. */
    public static List<JSONObject> enabled(Context c) {
        List<JSONObject> out = new ArrayList<>();
        SharedPreferences p = prefs(c);
        for (JSONObject s : all(c)) {
            if (p.getBoolean("en_" + s.optString("id"), true)) out.add(s);
        }
        return out;
    }

    public static String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.GERMANY).format(new Date());
    }

    public static boolean votedToday(Context c) {
        return today().equals(prefs(c).getString("lastVote", ""));
    }

    public static void markVoted(Context c) {
        prefs(c).edit().putString("lastVote", today()).apply();
    }
}
