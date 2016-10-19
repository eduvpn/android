package net.tuxed.vpnconfigimporter.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.ViewSwitcher;

import com.squareup.picasso.Picasso;

import net.tuxed.vpnconfigimporter.EduVPNApplication;
import net.tuxed.vpnconfigimporter.MainActivity;
import net.tuxed.vpnconfigimporter.R;
import net.tuxed.vpnconfigimporter.adapter.MessagesAdapter;
import net.tuxed.vpnconfigimporter.entity.DiscoveredAPI;
import net.tuxed.vpnconfigimporter.entity.Instance;
import net.tuxed.vpnconfigimporter.entity.Profile;
import net.tuxed.vpnconfigimporter.entity.message.Message;
import net.tuxed.vpnconfigimporter.service.APIService;
import net.tuxed.vpnconfigimporter.service.PreferencesService;
import net.tuxed.vpnconfigimporter.service.SerializerService;
import net.tuxed.vpnconfigimporter.service.VPNService;
import net.tuxed.vpnconfigimporter.utils.FormattingUtils;

import org.json.JSONObject;

import java.util.List;
import java.util.Observable;
import java.util.Observer;

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

    @Inject
    protected APIService _apiService;

    @Inject
    protected SerializerService _serializerService;

    @BindView(R.id.messagesList)
    protected RecyclerView _messagesList;

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

    private Observer _vpnStatusObserver;
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
            Picasso.with(view.getContext()).load(provider.getLogoUri()).fit().into(_providerIcon);
        }
        _messagesList.setLayoutManager(new LinearLayoutManager(view.getContext(), LinearLayoutManager.VERTICAL, false));
        _messagesList.setAdapter(new MessagesAdapter());
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Load the user and system messages asynchronously.
        DiscoveredAPI discoveredAPI = _preferencesService.getSavedDiscoveredAPI();
        final MessagesAdapter messagesAdapter= (MessagesAdapter)_messagesList.getAdapter();
        _apiService.getJSON(discoveredAPI.getSystemMessagesAPI(), true, new APIService.Callback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject result) {
                try {
                    List<Message> systemMessagesList = _serializerService.deserializeMessageList(result);
                    messagesAdapter.setSystemMessages(systemMessagesList);
                } catch (SerializerService.UnknownFormatException ex) {
                    // TODO show error
                    ex.printStackTrace();
                }
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(getContext(),
                        getString(R.string.error_loading_system_messages, errorMessage),
                        Toast.LENGTH_SHORT).show();
            }
        });
        _apiService.getJSON(discoveredAPI.getUserMessagesAPI(), true, new APIService.Callback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject result) {
                try {
                    List<Message> userMessagesList = _serializerService.deserializeMessageList(result);
                    messagesAdapter.setUserMessages(userMessagesList);
                } catch (SerializerService.UnknownFormatException ex) {
                    // TODO show error
                    ex.printStackTrace();
                }
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(getContext(),
                        getString(R.string.error_loading_user_messages, errorMessage),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        _vpnStatusObserver = new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                VPNService.VPNStatus status = (VPNService.VPNStatus)arg;
                if (_currentStatusIcon != null) {
                    switch (status) {
                        case CONNECTED:
                            _currentStatusIcon.setImageResource(R.drawable.connection_status_connected);
                            break;
                        case CONNECTING:
                            _currentStatusIcon.setImageResource(R.drawable.connection_status_connecting);
                            break;
                        case PAUSED:
                            _currentStatusIcon.setImageResource(R.drawable.connection_status_paused);
                            break;
                        case DISCONNECTED:
                            _currentStatusIcon.setImageResource(R.drawable.connection_status_disconnected);
                            break;
                        default:
                            throw new RuntimeException("Unhandled VPN status!");
                    }
                }
            }
        };
        // Update the icon immediately
        _vpnStatusObserver.update(_vpnService, _vpnService.getStatus());
        _vpnService.addObserver(_vpnStatusObserver);
        _vpnService.attachConnectionInfoListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (_vpnStatusObserver != null) {
            _vpnService.deleteObserver(_vpnStatusObserver);
        }
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
        // Go back to the home screen.
        ((MainActivity)getActivity()).openFragment(new ProviderSelectionFragment(), false);
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
