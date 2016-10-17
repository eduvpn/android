package net.tuxed.vpnconfigimporter.fragment;

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

import net.tuxed.vpnconfigimporter.EduVPNApplication;
import net.tuxed.vpnconfigimporter.R;
import net.tuxed.vpnconfigimporter.service.VPNService;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

/**
 * The fragment which displays the status of the current connection.
 * Created by Daniel Zolnai on 2016-10-07.
 */
public class ConnectionStatusFragment extends Fragment implements VPNService.ConnectionInfoCallback {

    private enum Screen {NOTIFICATIONS, CONNECTION_INFO}

    @Inject
    protected VPNService _vpnService;

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

    @BindView(R.id.serverIpValue)
    protected TextView _serverIpText;

    @BindView(R.id.ipV4Value)
    protected TextView _ipV4Text;

    @BindView(R.id.ipV6Value)
    protected TextView _ipV6Text;

    @BindView(R.id.portValue)
    protected TextView _portText;

    @BindView(R.id.serverValue)
    protected TextView _serverUrlText;

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
        // TODO open log window
    }

    @Override
    public void updateStatus(Long secondsConnected, Long bytesIn, Long bytesOut) {
        // TODO format seconds and bytes correctly
        if (secondsConnected == null) {
            _durationText.setText(R.string.not_available);
        } else {
            _durationText.setText(String.valueOf(secondsConnected)+ "s");
        }
        _bytesInText.setText(bytesIn / 1024 + " kB");
        _bytesOutText.setText(bytesOut / 1024 + " kB");
    }

    @Override
    public void metadataAvailable(String serverUrl, String serverIp, String localIpV4, String localIpV6, String port) {
        _serverUrlText.setText(serverUrl);
        _serverIpText.setText(serverIp);
        _ipV4Text.setText(localIpV4);
        _ipV6Text.setText(localIpV6);
        _portText.setText(port);
    }

}
