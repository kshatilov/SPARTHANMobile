package me.symlab.kirill.sparthanmobile;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import me.symlab.kirill.sparthanmobile.Utils.Utils;

public class SettingsActivity extends Activity {


    private void initGUI() {
        getWindow().getDecorView().setBackgroundColor(Color.WHITE);

        // Classifiers array

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
