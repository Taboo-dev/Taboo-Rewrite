package io.github.chalkyjeans.taboobot.util.objects;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public record SearchHistory(Long userId, String name, String url, String identifier) {

    public static List<SearchHistory> fromJson(JSONArray array) {
        List<SearchHistory> searchHistories = new ArrayList<>();
        for (Object obj : array) {
            JSONObject json = (JSONObject) obj;
            SearchHistory searchHistory = new SearchHistory(
                    json.getLong("userId"),
                    json.getString("name"),
                    json.getString("url"),
                    json.getString("identifier")
            );
            searchHistories.add(searchHistory);
        }
        return searchHistories;
    }

}
