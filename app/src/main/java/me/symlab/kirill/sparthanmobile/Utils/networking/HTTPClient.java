package me.symlab.kirill.sparthanmobile.Utils.networking;

import android.os.SystemClock;
import android.util.Log;

import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpPost;
import com.koushikdutta.async.http.AsyncHttpResponse;
import com.koushikdutta.async.http.body.MultipartFormDataBody;

import org.json.JSONArray;

import me.symlab.kirill.sparthanmobile.Utils.EasyCallable;

public class HTTPClient implements Client {
    private static final String TAG = "HTTP time SPARTHAN";
    private AsyncHttpPost[] sender;
    private String serverUri;

    @Override
    public void init(String ip, int port) {
        this.serverUri = "http://" + ip + ":" + port + "/";
        sender = new AsyncHttpPost[]{new AsyncHttpPost(serverUri)};
    }

    @Override
    public void send(JSONArray data, EasyCallable<String> successCallback, Runnable failCallback) {
        MultipartFormDataBody body = new MultipartFormDataBody();
        body.addStringPart("EMG_PACKAGE", data.toString());
        sender[0].setBody(body);
        body.addStringPart("TIME", String.valueOf(SystemClock.uptimeMillis()));
        AsyncHttpClient.getDefaultInstance().executeString(sender[0], new AsyncHttpClient.StringCallback() {
            @Override
            public void onCompleted(Exception ex, AsyncHttpResponse source, String result) {
                String gesture;
                if (ex != null) {
                    failCallback.run();
                    sender[0] = new AsyncHttpPost(serverUri);
                    return;
                }
                long start = -1L;
                if (result.contains("_")) {
                    String[] splitted = result.split("_");
                    gesture = splitted[0];
                    start = Long.parseLong(splitted[1]);
                    long end = SystemClock.uptimeMillis();
                    Log.d(TAG, String.valueOf(end - start));
                } else {
                    gesture = result;
                }
                successCallback.accept(gesture);
            }
        });
    }

}
