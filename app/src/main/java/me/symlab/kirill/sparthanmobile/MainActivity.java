package me.symlab.kirill.sparthanmobile;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.ncorti.myonnaise.Myo;
import com.ncorti.myonnaise.MyoStatus;
import com.ncorti.myonnaise.Myonnaise;

import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.json.JSONArray;
import org.tensorflow.lite.Interpreter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;
import me.symlab.kirill.sparthanmobile.Utils.EasyCallable;
import me.symlab.kirill.sparthanmobile.Utils.SettingsStore;
import me.symlab.kirill.sparthanmobile.Utils.Utils;
import me.symlab.kirill.sparthanmobile.Utils.networking.Client;
import me.symlab.kirill.sparthanmobile.Utils.networking.HTTPClient;
import me.symlab.kirill.sparthanmobile.Utils.networking.UDPClient;

public class MainActivity extends Activity {
    /* GENERAL */
    private static String TAG = "MYOSPARTHAN";
    private static String NOT_MY_MYO_ADDRESS = "EE:BF:D5:D9:3A:F7";
    public static int IMG_X = 200;
    public static int IMG_Y = 8;
    private static int BATCH_SIZE = 1;
    private static int MYO_POLLING_FREQUENCY = 200;
    private static int CLASSIFICATION_INTERVAL = 600;
    private static final String P2C_IP = "192.168.137.1";
    private int P2B_SERVER_PORT = 5000;
    private Client client;
    private static final int PORT = 80;

    /* CALLBACKS */
    private Runnable failCallback;
    private EasyCallable<String> successCallback;

    public enum GESTURES {
        POINT("POINT"),
        PEACE("PEACE"),
        THUMB("THUMB"),
        FIST("FIST"),
        PALM("PALM");
        private final String label;

        GESTURES(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    /* MYO */
    Myo myo = null;

    /* TFLITE */
    private Interpreter tflite;
    private final Queue<float[]> q = new CircularFifoQueue<>(IMG_X);
    private ByteBuffer inp;
    private static String modelFile = "graph.tflite";
    private long[][] out;

    /* GESTURES */
    static Map<GESTURES, Integer> gesturesImages = new HashMap<>();

    static {
        gesturesImages.put(GESTURES.FIST, R.id.fist);
        gesturesImages.put(GESTURES.PALM, R.id.palm);
        gesturesImages.put(GESTURES.THUMB, R.id.thumb);
        gesturesImages.put(GESTURES.POINT, R.id.point);
        gesturesImages.put(GESTURES.PEACE, R.id.peace);
    }

    String gesture;
    static Map<Long, GESTURES> gesturesLabels = new HashMap<>();

    static {
        gesturesLabels.put(0L, GESTURES.FIST);
        gesturesLabels.put(1L, GESTURES.PALM);
        gesturesLabels.put(2L, GESTURES.THUMB);
        gesturesLabels.put(3L, GESTURES.POINT);
        gesturesLabels.put(4L, GESTURES.PEACE);
    }

    private Animation expandAnimation;
    private int activeId = -1;

    private void initCNN() {
        try {
            Interpreter.Options options = new Interpreter.Options();
            tflite = new Interpreter(Utils.loadModelFile(MainActivity.this, modelFile), options);
            tflite.resizeInput(0, new int[]{BATCH_SIZE, IMG_Y, IMG_X, 1});
            inp = ByteBuffer.allocateDirect(BATCH_SIZE * IMG_Y * IMG_X * (Float.SIZE / Byte.SIZE));
            out = new long[BATCH_SIZE][1];
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initGUI() {
        getWindow().getDecorView().setBackgroundColor(Color.WHITE);
        Animation connectingAnimation = AnimationUtils.loadAnimation(this, R.anim.animation);
        ImageView circle = findViewById(R.id.circle);
        circle.startAnimation(connectingAnimation);
        circle.setOnClickListener(view -> view.startAnimation(connectingAnimation));
        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        requestPermissions(new String[]{Manifest.permission.INTERNET}, 1);
        requestPermissions(new String[]{Manifest.permission.ACCESS_WIFI_STATE}, 1);
        expandAnimation = AnimationUtils.loadAnimation(this, R.anim.a2);

        // settings button
        View settingsButton = findViewById(R.id.settings_button);
        ImageView settingsIcon = findViewById(R.id.settings_icon);
        TextView settingsLabel = findViewById(R.id.settings_label);
        settingsIcon.setColorFilter(R.color.colorInactive);
        View.OnClickListener listener = (view) -> {
            RotateAnimation rotateAnimation = new RotateAnimation(0, 45,
                    Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            rotateAnimation.setDuration(500L);
            rotateAnimation.setRepeatCount(0);
            settingsIcon.startAnimation(rotateAnimation);
            settingsIcon.setColorFilter(R.color.colorPrimary);
            settingsLabel.setTextColor(getResources().getColor(R.color.colorMain, getTheme()));
            startActivity(new Intent(this, SettingsActivity.class));
        };
        settingsButton.setOnClickListener(listener);
        settingsIcon.setOnClickListener(listener);

        initGestureButtons();
    }

    private void initGestureButtons() {
        gesturesImages.entrySet().forEach(entry ->
        {
            findViewById(entry.getValue()).setOnClickListener(view ->
            {
                //TODO get label from view.getId() + send command to board
                client.send(Client.DummyPackage.getOne(), successCallback, failCallback);
            });
        });
    }

    private void initCallbacks() {
        failCallback = () -> {
            if (SettingsStore.getInstance().useCloud()) {
                runOnUiThread(() ->
                        Toast.makeText(getApplicationContext(), getString(R.string.SERVER_UNAVAILABLE), Toast.LENGTH_SHORT).show());
            }
        };

        final int[] viewId = new int[1];
        successCallback = (String g) -> {
            gesture = g;
            viewId[0] = gesturesImages.get(GESTURES.valueOf(gesture));
            runOnUiThread(() -> {
                highlightGesture(viewId[0]);
            });
        };
    }


    private void initComms() {
        //HTTP Server
        Handler serverThread = new Handler();
        serverThread.post(new Runnable() {
            @Override
            public void run() {
                AsyncHttpServer server = new AsyncHttpServer();
                server.get("/", (request, response) -> response.send(gesture));
                server.listen(P2B_SERVER_PORT);
            }
        });

        //Client
        SettingsStore.Connectivity type = SettingsStore.getInstance().getConnectivity();
        client = type == SettingsStore.Connectivity.HTTP ? new HTTPClient() :
                type == SettingsStore.Connectivity.UDP ? new UDPClient() : null;
        client.init(P2C_IP, PORT);

        if(type == SettingsStore.Connectivity.UDP) {
//            new Thread(new UDPClient.UDPClientListen()).start();
        }
    }

    private void initMYO() {
        TextView searchLabel = findViewById(R.id.search_label);
        TextView connectLabel = findViewById(R.id.connect_label);
        TextView streamLabel = findViewById(R.id.stream_label);
        Myonnaise myonnaise = new Myonnaise(this);
        myonnaise.getMyo(NOT_MY_MYO_ADDRESS).subscribeWith(new DisposableSingleObserver<Myo>() {
            @Override
            public void onSuccess(Myo _myo) {
                myo = _myo;
                searchLabel.setTextColor(getResources().getColor(R.color.colorInactive, getTheme()));
                connectLabel.setTextColor(getResources().getColor(R.color.colorMain, getTheme()));
                myo.connect(getApplicationContext());

                myo.statusObservable()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(it -> {
                            if (it == MyoStatus.READY) {
                                connectLabel.setTextColor(getResources().getColor(R.color.colorInactive, getTheme()));
                                streamLabel.setTextColor(getResources().getColor(R.color.colorMain, getTheme()));
                                Handler handler = new Handler();
                                handler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        myo.sendCommand(Utils.getStreamCmd());
                                        myo.setFrequency(MYO_POLLING_FREQUENCY);
                                    }
                                }, 2000);

                            }
                        });
                myo.dataFlowable()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .onBackpressureDrop()
                        .subscribe(q::add);

            }

            @Override
            public void onError(Throwable e) {
                Log.d(TAG, "onError: Failed to connect to myo");
            }
        });
    }

    private void startCR() {
        final long interval = CLASSIFICATION_INTERVAL;
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (q.size() < IMG_X) {
                    return;
                }

                if (SettingsStore.getInstance().useCloud()) {
                    // Sending package to the cloud server to classify
                    client.send(new JSONArray(q), successCallback, failCallback);
                } else {
                    // Classify gesture locally
                    final int[] viewId = new int[1];
                    inp = Utils.convertT(q, BATCH_SIZE, IMG_X, IMG_Y);
                    long startTime = SystemClock.uptimeMillis();
                    tflite.run(inp, out);
                    gesture = gesturesLabels.get(out[0][0]).toString();
                    viewId[0] = gesturesImages.get(GESTURES.valueOf(gesture));
                    out = new long[BATCH_SIZE][1];
                    long endTime = SystemClock.uptimeMillis();
                    Log.d(TAG, String.valueOf(endTime - startTime));
                    runOnUiThread(() -> highlightGesture(viewId[0]));
                }
            }
        }, interval, interval);
    }

    @SuppressLint("CheckResult")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate: init()");
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        setContentView(R.layout.activity_main);

        // GUI
        this.initGUI();
        //Networking
        this.initComms();
        // LOADING MODEL
        this.initCNN();
        // MYO
        this.initMYO();
        // CLASSIFICATION ROUTINE
        this.startCR();
        // NETWORKING CALLBACKS
        this.initCallbacks();
    }

    private void highlightGesture(int id) {
        if (activeId == id) {
            return;
        }
        activeId = id;
        //dim others
        for (int _id : new int[]{
                R.id.fist,
                R.id.palm,
                R.id.peace,
                R.id.point,
                R.id.thumb
        }) {
            ImageView v = findViewById(_id);
            if (v != null) {
                v.setColorFilter(getResources().getColor(R.color.colorInactive, getTheme()));
                v.setAnimation(null);
            }
        }
        //highlight the one
        ImageView v = findViewById(id);
        if (v != null) {
            v.setColorFilter(getResources().getColor(R.color.colorMain, getTheme()));
            v.startAnimation(expandAnimation);
        }
    }
}
