package io.github.chalkyjeans.taboobot.backend;

import io.github.chalkyjeans.taboobot.config.TabooConfig;
import io.github.chalkyjeans.taboobot.util.objects.Bookmark;
import io.github.chalkyjeans.taboobot.util.objects.SearchHistory;
import okhttp3.*;
import org.json.JSONArray;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
public class TabooAPI {

    private static String username;
    private static String password;

    public TabooAPI(TabooConfig config) {
        username = config.getBackend().getUsername();
        password = config.getBackend().getPassword();
    }

    static OkHttpClient client = new OkHttpClient.Builder()
            .authenticator((route, response) -> response.request().newBuilder().header("Authorization", Credentials.basic(username, password)).build())
            .build();
    static String baseUrl = "http://localhost:8080/";

    public static class Bookmarks {

        public static void saveBookmark(Bookmark bookmark) {
            RequestBody body = new FormBody.Builder()
                    .add("name", bookmark.name())
                    .add("url", bookmark.url())
                    .add("identifier", bookmark.identifier())
                    .build();
            Request request = new Request.Builder()
                    .url(baseUrl + "resource/bookmark/" + bookmark.userId())
                    .post(body)
                    .build();
            try (Response response = client.newCall(request).execute()) {
                int code = response.code();
                System.out.println(code);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public static List<Bookmark> getBookmarks(Long userId) {
            Request request = new Request.Builder()
                    .url(baseUrl + "resource/bookmark/" + userId)
                    .build();
            String res;
            JSONArray array = new JSONArray();
            try (Response response = client.newCall(request).execute()) {
                if (response.body() != null) {
                    res = response.body().string();
                    array = new JSONArray(res);
                }
            } catch (Exception e) {
                e.printStackTrace();
                return Collections.emptyList();
            }
            return Bookmark.fromJson(array);
        }

        public static void deleteBookmarks(Long userId) {
            Request request = new Request.Builder()
                    .url(baseUrl + "resource/bookmark/" + userId)
                    .delete()
                    .build();
            try (Response response = client.newCall(request).execute()) {
                int code = response.code();
                System.out.println(code);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static class SearchHistories {

        public static void saveSearchHistory(SearchHistory searchHistory) {
            RequestBody body = new FormBody.Builder()
                    .add("name", searchHistory.name())
                    .add("url", searchHistory.url())
                    .add("identifier", searchHistory.identifier())
                    .build();
            Request request = new Request.Builder()
                    .url(baseUrl + "resource/search-history/" + searchHistory.userId())
                    .post(body)
                    .build();
            try (Response response = client.newCall(request).execute()) {
                int code = response.code();
                System.out.println(code);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public static void saveSearchHistory(List<SearchHistory> historyList) {
            historyList.forEach(SearchHistories::saveSearchHistory);
        }

        public static List<SearchHistory> getSearchHistory(Long userId) {
            Request request = new Request.Builder()
                    .url(baseUrl + "resource/search-history/" + userId)
                    .build();
            String res;
            JSONArray array = new JSONArray();
            try (Response response = client.newCall(request).execute()) {
                if (response.body() != null) {
                    res = response.body().string();
                    array = new JSONArray(res);
                }
            } catch (Exception e) {
                e.printStackTrace();
                return Collections.emptyList();
            }
            return SearchHistory.fromJson(array);
        }

        public static void deleteSearchHistory(Long userId) {
            Request request = new Request.Builder()
                    .url(baseUrl + "resource/search-history/" + userId)
                    .delete()
                    .build();
            try (Response response = client.newCall(request).execute()) {
                int code = response.code();
                System.out.println(code);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

}
