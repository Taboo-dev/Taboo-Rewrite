package io.github.chalkyjeans.taboobot.util.objects;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public record Bookmark(Long userId, String name, String url, String identifier) {

    public static List<Bookmark> fromJson(JSONArray array) {
        List<Bookmark> bookmarks = new ArrayList<>();
        for (Object obj : array) {
            JSONObject json = (JSONObject) obj;
            Bookmark bookmark = new Bookmark(
                    json.getLong("userId"),
                    json.getString("name"),
                    json.getString("url"),
                    json.getString("identifier")
            );
            bookmarks.add(bookmark);
        }
        return bookmarks;
    }

}