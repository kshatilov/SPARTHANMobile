package me.symlab.kirill.sparthanmobile;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.ncorti.myonnaise.Myo;
import com.ncorti.myonnaise.MyoStatus;
import com.ncorti.myonnaise.Myonnaise;

import org.apache.commons.collections4.queue.CircularFifoQueue;
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
import me.symlab.kirill.sparthanmobile.Utils.Utils;

public class MainActivity extends Activity {
    /* GENERAL */
    private static String TAG = "MYOSPARTHAN";
    private static String NOT_MY_MYO_ADDRESS = "EE:BF:D5:D9:3A:F7";
    private static int IMG_X = 200;
    private static int IMG_Y = 8;
    private static int BATCH_SIZE = 1;
    private static int MYO_POLLING_FREQUENCY = 200;
    private static int CLASSIFICATION_INTERVAL = 300;

    /* MYO */
    Myo myo = null;

    /* TFLITE */
    private Interpreter tflite;
    private final Queue<float[]> q = new CircularFifoQueue<>(IMG_X);
    private ByteBuffer inp;
    private static String modelFile = "graph.tflite";
    private long[][] out;

    /* GESTURES */
    static Map<Long, Integer> gestures = new HashMap<>();

    static {
        gestures.put(0L, R.id.fist);
        gestures.put(1L, R.id.palm);
        gestures.put(2L, R.id.thumb);
        gestures.put(3L, R.id.point);
        gestures.put(4L, R.id.peace);
    }

    private Animation expandAnimation;
    private int activeId = -1;


    @SuppressLint("CheckResult")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        TextView searchLabel = findViewById(R.id.search_label);
        TextView connectLabel = findViewById(R.id.connect_label);
        TextView streamLabel = findViewById(R.id.stream_label);

        getWindow().getDecorView().setBackgroundColor(Color.WHITE);
        Animation connectingAnimation = AnimationUtils.loadAnimation(this, R.anim.animation);
        ImageView circle = findViewById(R.id.circle);
        circle.startAnimation(connectingAnimation);
        circle.setOnClickListener(view -> view.startAnimation(connectingAnimation));
        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);

        expandAnimation = AnimationUtils.loadAnimation(this, R.anim.a2);

        // LOADING MODEL
        try {
            Interpreter.Options options = new Interpreter.Options();
            tflite = new Interpreter(Utils.loadModelFile(MainActivity.this, modelFile), options);
            tflite.resizeInput(0, new int[]{BATCH_SIZE, IMG_Y, IMG_X, 1});
            inp = ByteBuffer.allocateDirect(BATCH_SIZE * IMG_Y * IMG_X * (Float.SIZE / Byte.SIZE));
            out = new long[BATCH_SIZE][1];
        } catch (IOException e) {
            e.printStackTrace();
        }


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

        // CLASSIFICATION ROUTINE
        final long interval = CLASSIFICATION_INTERVAL;
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (q.size() < IMG_X) {
                    return;
                }
                inp = Utils.convertT(q, BATCH_SIZE, IMG_X, IMG_Y);
                long startTime = SystemClock.uptimeMillis();
                tflite.run(inp, out);
                long endTime = SystemClock.uptimeMillis();
                final int id = gestures.get(out[0][0]);
                runOnUiThread(() -> {
                    highlightGesture(id);
                });
                out = new long[BATCH_SIZE][1];

            }
        }, interval, interval);
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
