package net.tuxed.vpnconfigimporter;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Toast;

import net.tuxed.vpnconfigimporter.fragment.ConnectProfileFragment;
import net.tuxed.vpnconfigimporter.fragment.ProviderSelectionFragment;
import net.tuxed.vpnconfigimporter.service.ConnectionService;
import net.tuxed.vpnconfigimporter.service.VPNService;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getName();

    @Inject
    protected ConnectionService _connectionService;

    @Inject
    protected VPNService _vpnService;

    @BindView(R.id.toolbar)
    protected Toolbar _toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        EduVPNApplication.get(this).component().inject(this);
        setSupportActionBar(_toolbar);
        openFragment(new ProviderSelectionFragment());
        // The app might have been reopened from a URL.
        onNewIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.getData() == null) {
            // Not a callback intent.
            return;
        }
        try {
            _connectionService.parseCallbackIntent(intent);
            openFragment(new ConnectProfileFragment());
        } catch (ConnectionService.InvalidConnectionAttemptException ex) {
            ex.printStackTrace();
            // TODO show error dialog.
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        _vpnService.onStart(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        _vpnService.onStop(this);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    public void openFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .addToBackStack(null)
                .add(R.id.contentFrame, fragment)
                .commit();
    }

    @OnClick(R.id.settingsButton)
    protected void onSettingsButtonClicked() {
        findViewById(R.id.settingsButton).animate().rotationBy(520).setDuration(800).setInterpolator(new AccelerateDecelerateInterpolator()).start();
        Toast.makeText(this, "This will open the settings page later...", Toast.LENGTH_LONG).show();
    }


}
