package me.symlab.kirill.sparthanmobile.Utils.networking;

import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.json.JSONArray;

import java.util.Queue;

import me.symlab.kirill.sparthanmobile.Utils.EasyCallable;

import static me.symlab.kirill.sparthanmobile.MainActivity.IMG_X;
import static me.symlab.kirill.sparthanmobile.MainActivity.IMG_Y;

public interface Client {

    void init(String ip, int port);

    void send(JSONArray data, EasyCallable<String> successCallback, Runnable failCallback);

    class DummyPackage {
        public static JSONArray getOne() {
            Queue<float[]> q = new CircularFifoQueue<>(IMG_X);
            for (int i = 0; i < IMG_X; ++i) {
                q.add(new float[IMG_Y]);
            }
            return new JSONArray(q);
        }
    }

}


