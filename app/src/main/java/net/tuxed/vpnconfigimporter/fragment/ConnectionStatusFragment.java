package net.tuxed.vpnconfigimporter.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.ViewSwitcher;

import com.squareup.picasso.Picasso;

import net.tuxed.vpnconfigimporter.EduVPNApplication;
import net.tuxed.vpnconfigimporter.R;
import net.tuxed.vpnconfigimporter.entity.Instance;
import net.tuxed.vpnconfigimporter.entity.Profile;
import net.tuxed.vpnconfigimporter.service.PreferencesService;
import net.tuxed.vpnconfigimporter.service.VPNService;
import net.tuxed.vpnconfigimporter.utils.FormattingUtils;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import de.blinkt.openvpn.activities.LogWindow;

/**
 * The fragment which displays the status of the current connection.
 * Created by Daniel Zolnai on 2016-10-07.
 */
public class ConnectionStatusFragment extends Fragment implements VPNService.ConnectionInfoCallback {

    private enum Screen {NOTIFICATIONS, CONNECTION_INFO}

    @Inject
    protected VPNService _vpnService;

    @Inject
    protected PreferencesService _preferencesService;

    @BindView(R.id.profileName)
    protected TextView _profileName;

    @BindView(R.id.providerIcon)
    protected ImageView _providerIcon;

    @BindView(R.id.connectionStatusIcon)
    protected ImageView _currentStatusIcon;

    @BindView(R.id.switcher)
    protected ViewSwitcher _viewSwitcher;

    @BindView(R.id.notificationsSwitchButton)
    protected ToggleButton _notificationsSwitchButton;

    @BindView(R.id.connectionInfoSwitchButton)
    protected ToggleButton _connectionInfoSwitchButton;

    @BindView(R.id.ipV4Value)
    protected TextView _ipV4Text;

    @BindView(R.id.ipV6Value)
    protected TextView _ipV6Text;

    @BindView(R.id.durationValue)
    protected TextView _durationText;

    @BindView(R.id.bytesInValue)
    protected TextView _bytesInText;

    @BindView(R.id.bytesOutValue)
    protected TextView _bytesOutText;


    private Screen _currentScreen = Screen.NOTIFICATIONS;
    private Unbinder _unbinder;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_connection_status, container, false);
        _unbinder = ButterKnife.bind(this, view);
        EduVPNApplication.get(view.getContext()).component().inject(this);
        Profile savedProfile = _preferencesService.getSavedProfile();
        Instance provider = _preferencesService.getSavedInstance();
        _profileName.setText(savedProfile.getDisplayName());
        if (provider.getLogoUri() != null) {
            Picasso.with(view.getContext()).load(provider.getLogoUri()).into(_providerIcon);
        }
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        _vpnService.attachConnectionInfoListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        _vpnService.detachConnectionInfoListener();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        _unbinder.unbind();
    }

    @OnClick({ R.id.notificationsSwitchButton, R.id.connectionInfoSwitchButton })
    public void onSwitcherButtonClicked(View view) {
        boolean switchToNotifications;
        boolean dontSwitch; // Used to determine if the button for the current screen was unchecked
        if (view == _notificationsSwitchButton) {
            switchToNotifications = _notificationsSwitchButton.isChecked();
            dontSwitch = !_notificationsSwitchButton.isChecked() && _currentScreen == Screen.NOTIFICATIONS;
        } else {
            switchToNotifications = !_connectionInfoSwitchButton.isChecked();
            dontSwitch = !_connectionInfoSwitchButton.isChecked() && _currentScreen == Screen.CONNECTION_INFO;
        }
        if (!dontSwitch) {
            int openChildId = switchToNotifications ? 0 : 1;
            _viewSwitcher.setDisplayedChild(openChildId);
        } else {
            switchToNotifications = !switchToNotifications;
        }
        _notificationsSwitchButton.setChecked(switchToNotifications);
        _connectionInfoSwitchButton.setChecked(!switchToNotifications);
        _currentScreen = switchToNotifications ? Screen.NOTIFICATIONS : Screen.CONNECTION_INFO;
    }

    @OnClick(R.id.disconnectButton)
    protected void onDisconnectButtonClicked() {
        _vpnService.disconnect();
    }

    @OnClick(R.id.viewLogButton)
    protected void onViewLogClicked() {
        Intent intent = new Intent(getActivity(), LogWindow.class);
        startActivity(intent);
    }

    @Override
    public void updateStatus(Long secondsConnected, Long bytesIn, Long bytesOut) {
        _durationText.setText(FormattingUtils.formatDurationSeconds(getContext(), secondsConnected));
        _bytesInText.setText(FormattingUtils.formatBytesTraffic(getContext(), bytesIn));
        _bytesOutText.setText(FormattingUtils.formatBytesTraffic(getContext(), bytesOut));
    }

    @Override
    public void metadataAvailable(String localIpV4, String localIpV6) {
        String ipV4DisplayText = localIpV4 == null ? getString(R.string.not_available) : localIpV4;
        _ipV4Text.setText(ipV4DisplayText);
        String ipV6DisplayText = localIpV6 == null ? getString(R.string.not_available) : localIpV6;
        _ipV6Text.setText(ipV6DisplayText);
    }

}
