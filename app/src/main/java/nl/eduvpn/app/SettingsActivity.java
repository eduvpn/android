package nl.eduvpn.app;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import nl.eduvpn.app.fragment.SettingsFragment;

import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

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

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }
}
