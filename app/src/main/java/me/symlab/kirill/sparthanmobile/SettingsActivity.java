package me.symlab.kirill.sparthanmobile;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import me.symlab.kirill.sparthanmobile.Utils.SettingsStore;

public class SettingsActivity extends Activity {

    private static final String TAG = "Settings Activity";

    private void initGUI() {
        getWindow().getDecorView().setBackgroundColor(Color.WHITE);

        // Classifiers array

        // Use cloud switch
        Switch cloudSwitch = findViewById(R.id.cloud_switch);
        cloudSwitch.setTrackTintList(new ColorStateList(new int[][]{
                new int[]{-android.R.attr.state_checked},
                new int[]{android.R.attr.state_checked},
        }, new int[]{
                getResources().getColor(R.color.colorInactive, getTheme()),
                getResources().getColor(R.color.colorMain, getTheme())
        }));
        cloudSwitch.setChecked(SettingsStore.getInstance().useCloud());
        cloudSwitch.setOnCheckedChangeListener((_cloudSwitch, isChecked) -> {
            SettingsStore.getInstance().setUseCloud(isChecked);
        });

        // Number of gestures
        List<Integer> numbers = IntStream.rangeClosed(SettingsStore.MIN_GESTURES, SettingsStore.MAX_GESTURES)
                .boxed().collect(Collectors.toList());
        Spinner numGesturesSelector = findViewById(R.id.num_gestures_selector);
        ArrayAdapter<Integer> nAdapter = new ArrayAdapter<Integer>(this, android.R.layout.simple_spinner_item, numbers){
            public View getView(int position, View convertView,ViewGroup parent) {
                TextView v = (TextView) super.getView(position, convertView, parent);
                v.setTextSize(20);
                v.setTextColor(getResources().getColor(R.color.colorMain, getTheme()));
                return v;
            }

            public View getDropDownView(int position, View convertView,ViewGroup parent) {
                View v = super.getDropDownView(position, convertView,parent);
                ((TextView) v).setGravity(Gravity.CENTER);
                return v;
            }

        };
        numGesturesSelector.setAdapter(nAdapter);
        numGesturesSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                Log.d(TAG, "onItemSelected: " + i + SettingsStore.MIN_GESTURES);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        // back button
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        initGUI();
    }
}
