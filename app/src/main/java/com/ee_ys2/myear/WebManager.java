package com.ee_ys2.myear;

import android.widget.TextView;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class WebManager {


    private String url="https://myear-flask.azurewebsites.net";
    private String POST="POST";
    private String GET="GET";
    private MainActivity instance;
    private TextView result;
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    /**
     * WebManager constructor
     * @param instance  MainActivity instance
     * @param result    TextView to show the result to the user
     */
    public WebManager(MainActivity instance, TextView result) {
        this.instance=instance;
        this.result=result;
    }

    /**
     * Prepares OkHttp and request to send to back-end.
     * This method sends POST request to back-end.
     * Also receives the result from back-end and displays on result TextView.
     * @param method    Which method to call from back-end flask app.
     * @param json      JSON object to send to back-end
     * @throws IOException
     */
    void sendPost(String method, String json) throws IOException {

        String fullURL=url+"/"+method;

        OkHttpClient client = new OkHttpClient().newBuilder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS).build();


        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(fullURL)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {

                // Read data
                final String responseData = response.body().string();

                // Get result from web service and display
                instance.runOnUiThread(() -> result.setText(responseData));
            }
        });
    }
}
