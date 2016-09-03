package com.bignerdranch.android.locatrbnrg;

import android.net.Uri;
import android.util.Log;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by smaikap on 15/8/16.
 */
public class FlickrFetcher {
    private static final String TAG = "FlickrFetcher";
    private static final String API_KEY = "96de86408a603386edbb592ce40f033e";
    private static final String API_SECRET = "2aaa42562c8e2883";
    private static final String FETCH_RECENT_METHOD = "flickr.photos.getRecent";
    private static final String SEARCH_METHOD = "flickr.photos.search";
    private static final Uri END_POINT = Uri.parse("https://api.flickr.com/services/rest")
            .buildUpon()
            .appendQueryParameter("api_key", API_KEY)
            .appendQueryParameter("format", "json")
            .appendQueryParameter("nojsoncallback", "1")
            .appendQueryParameter("extras", "url_s")
            .build();

    public byte[] getUrlBytes(final String urlSpec) throws IOException {
        final URL url = new URL(urlSpec);
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        ByteArrayOutputStream outputStream = null;
        InputStream inputStream = null;
        try {
            outputStream = new ByteArrayOutputStream();
            inputStream = connection.getInputStream();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException(connection.getResponseMessage() + " : with " + urlSpec);
            }

            int byteRead = 0;
            final byte[] buffer = new byte[1024];
            while ((byteRead = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, byteRead);
            }
            return outputStream.toByteArray();
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }

            if (outputStream != null) {
                outputStream.close();
            }

            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public String getUrlString(final String urlSpec) throws IOException {
        return new String(this.getUrlBytes(urlSpec));
    }

    public List<GalleryItem> fetchRecentPhotos(final String pageNumber) {
        final String url = this.buildUrl(FETCH_RECENT_METHOD, null, pageNumber);
        return downloadGalleryItem(url);
    }

    public List<GalleryItem> searchPhotos(final String query) {
        final String url = this.buildUrl(SEARCH_METHOD, query, null);
        return downloadGalleryItem(url);
    }

    private List<GalleryItem> downloadGalleryItem(final String url) {
        final List<GalleryItem> items = new ArrayList<>();
        try {
            final String jsonString = this.getUrlString(url);
            Log.v(TAG, "Received JSON : " + jsonString);
            final JSONObject jsonBody = new JSONObject(jsonString);
            this.parseItemUsingGson(items, jsonBody);
        } catch (IOException e) {
            Log.e(TAG, "Failed to fetch items", e);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse jSon", e);
        }
        return items;
    }

    private void parseItemUsingGson(final List<GalleryItem> items, final JSONObject jsonBody) throws JSONException {
        final JSONObject photoJSonObject = jsonBody.getJSONObject("photos");
        final JSONArray photoJsonArray = photoJSonObject.getJSONArray("photo");

        for (int i = 0; i < photoJsonArray.length(); i++) {
            final JSONObject photoJsonObject = photoJsonArray.getJSONObject(i);
            final Gson gson = new Gson();
            final GalleryItem galleryItem = gson.fromJson(photoJsonObject.toString(), GalleryItem.class);
            items.add(galleryItem);
        }
    }

    private String buildUrl(final String method, final String query, final String pageNumber) {
        final Uri.Builder uriBuilder = END_POINT.buildUpon();
        uriBuilder.appendQueryParameter("method", method);
        if (SEARCH_METHOD.equals(method)) {
            uriBuilder.appendQueryParameter("text", query);
        }
        if (pageNumber != null) {
            uriBuilder.appendQueryParameter("page", pageNumber);
        }
        return uriBuilder.build().toString();
    }
}
