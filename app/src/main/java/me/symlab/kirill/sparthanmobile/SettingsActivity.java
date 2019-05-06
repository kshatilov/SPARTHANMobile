package me.symlab.kirill.sparthanmobile;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import org.apache.commons.collections4.MapUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import me.symlab.kirill.sparthanmobile.Utils.SettingsStore;

public class SettingsActivity extends Activity {

    private static final String TAG = "Settings Activity";
    private Button manual;
    private Button auto;

    private void updateModeSelector() {
        if (SettingsStore.getInstance().isManual()) {
            manual.setTextColor(getResources().getColor(R.color.colorMain, getTheme()));
            auto.setTextColor(getResources().getColor(R.color.colorInactive, getTheme()));
        } else {
            manual.setTextColor(getResources().getColor(R.color.colorInactive, getTheme()));
            auto.setTextColor(getResources().getColor(R.color.colorMain, getTheme()));
        }
    }

    private void initConnectivityButtons() {
        Map<Integer, SettingsStore.Connectivity> id2connMode = new HashMap<>();
        id2connMode.put(R.id.udp_button, SettingsStore.Connectivity.UDP);
        id2connMode.put(R.id.tcp_button, SettingsStore.Connectivity.TCP);
        id2connMode.put(R.id.http_button, SettingsStore.Connectivity.HTTP);
        Map<SettingsStore.Connectivity, Integer> connMode2id = MapUtils.invertMap(id2connMode);
        RadioGroup connectivityRG = findViewById(R.id.connectivity_radio_group);
        connectivityRG.check(connMode2id.get(SettingsStore.getInstance().getConnectivity()));
        connectivityRG.setOnCheckedChangeListener((radioGroup, id) -> SettingsStore.getInstance().setConnectivity(id2connMode.get(id)));
    }

    private void initCloudSwitch() {
        List<Integer> numbers = IntStream.rangeClosed(SettingsStore.MIN_GESTURES, SettingsStore.MAX_GESTURES)
                .boxed().collect(Collectors.toList());
        Spinner numGesturesSelector = findViewById(R.id.num_gestures_selector);
        final ArrayAdapter<Integer> nAdapter = new ArrayAdapter<Integer>(this, android.R.layout.simple_spinner_item, numbers) {
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView v = (TextView) super.getView(position, convertView, parent);
                v.setTextSize(40);
                v.setTextColor(getResources().getColor(R.color.colorMain, getTheme()));
                return v;
            }

            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View v = super.getDropDownView(position, convertView, parent);
                ((TextView) v).setGravity(Gravity.CENTER);
                return v;
            }

        };
        numGesturesSelector.setAdapter(nAdapter);
        numGesturesSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                SettingsStore.getInstance().setNumGestures(i + SettingsStore.MIN_GESTURES);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        numGesturesSelector.setSelection(SettingsStore.getInstance().getNumGestures() - 1);
        nAdapter.notifyDataSetChanged();
    }

    private void initNumGesturesSelector() {
        List<Integer> numbers = IntStream.rangeClosed(SettingsStore.MIN_GESTURES, SettingsStore.MAX_GESTURES)
                .boxed().collect(Collectors.toList());
        Spinner numGesturesSelector = findViewById(R.id.num_gestures_selector);
        final ArrayAdapter<Integer> nAdapter = new ArrayAdapter<Integer>(this, android.R.layout.simple_spinner_item, numbers) {
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView v = (TextView) super.getView(position, convertView, parent);
                v.setTextSize(40);
                v.setTextColor(getResources().getColor(R.color.colorMain, getTheme()));
                return v;
            }

            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View v = super.getDropDownView(position, convertView, parent);
                ((TextView) v).setGravity(Gravity.CENTER);
                return v;
            }

        };
        numGesturesSelector.setAdapter(nAdapter);
        numGesturesSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                SettingsStore.getInstance().setNumGestures(i + SettingsStore.MIN_GESTURES);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        numGesturesSelector.setSelection(SettingsStore.getInstance().getNumGestures() - SettingsStore.MIN_GESTURES);
        nAdapter.notifyDataSetChanged();
    }

    private void initControlModeSelectors() {
        manual = findViewById(R.id.button_manual);
        auto = findViewById(R.id.button_auto);

        updateModeSelector();

        manual.setOnClickListener(e -> {
            SettingsStore.getInstance().setManual(true);
            updateModeSelector();
        });

        auto.setOnClickListener(e -> {
            SettingsStore.getInstance().setManual(false);
            updateModeSelector();
        });
    }

    private void initBackButton() {
        View backButton = findViewById(R.id.back_button);
        ImageView backIcon = findViewById(R.id.back_icon);
        TextView backLabel = findViewById(R.id.back_label);
        backIcon.setColorFilter(R.color.colorInactive);
        View.OnClickListener listener = (view) -> {
            backIcon.setColorFilter(R.color.colorPrimary);
            backLabel.setTextColor(getResources().getColor(R.color.colorMain, getTheme()));
            startActivity(new Intent(this, MainActivity.class));
        };
        backButton.setOnClickListener(listener);
        backIcon.setOnClickListener(listener);
    }

    private void initGUI() {
        getWindow().getDecorView().setBackgroundColor(Color.WHITE);

        // Use cloud switch
        initCloudSwitch();

        // Number of gestures
        initNumGesturesSelector();

        // Control mode Selection
        initControlModeSelectors();

        // connectivity buttons
        initConnectivityButtons();

        // back button
       initBackButton();
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        initGUI();
    }
}
