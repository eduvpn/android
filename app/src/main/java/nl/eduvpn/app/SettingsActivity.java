package nl.eduvpn.app;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import nl.eduvpn.app.fragment.SettingsFragment;

/**
 * Activity which displays the settings fragment.
 * Created by Daniel Zolnai on 2016-10-22.
 */
public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.contentFrame, new SettingsFragment())
                .commit();
    }
}
