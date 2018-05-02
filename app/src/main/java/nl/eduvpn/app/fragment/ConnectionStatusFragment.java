/*
 *  This file is part of eduVPN.
 *
 *     eduVPN is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     eduVPN is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with eduVPN.  If not, see <http://www.gnu.org/licenses/>.
 */

package nl.eduvpn.app.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.squareup.picasso.Picasso;

import net.openid.appauth.AuthState;

import org.json.JSONObject;

import java.util.List;
import java.util.Observer;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import de.blinkt.openvpn.activities.LogWindow;
import nl.eduvpn.app.EduVPNApplication;
import nl.eduvpn.app.MainActivity;
import nl.eduvpn.app.R;
import nl.eduvpn.app.adapter.MessagesAdapter;
import nl.eduvpn.app.entity.DiscoveredAPI;
import nl.eduvpn.app.entity.Instance;
import nl.eduvpn.app.entity.Profile;
import nl.eduvpn.app.entity.message.Message;
import nl.eduvpn.app.service.APIService;
import nl.eduvpn.app.service.PreferencesService;
import nl.eduvpn.app.service.SerializerService;
import nl.eduvpn.app.service.VPNService;
import nl.eduvpn.app.utils.ErrorDialog;
import nl.eduvpn.app.utils.FormattingUtils;
import nl.eduvpn.app.utils.Log;

/**
 * The fragment which displays the status of the current connection.
 * Created by Daniel Zolnai on 2016-10-07.
 */
public class ConnectionStatusFragment extends Fragment implements VPNService.ConnectionInfoCallback {

    private static final int WAIT_FOR_DISCONNECT_UNTIL_MS = 3000;
    private static final String TAG = ConnectionStatusFragment.class.getName();

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
    protected Button _notificationsSwitchButton;

    @BindView(R.id.connectionInfoSwitchButton)
    protected Button _connectionInfoSwitchButton;

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

    @BindView(R.id.disconnectButton)
    protected Button _disconnectButton;

    private Observer _vpnStatusObserver;
    private Unbinder _unbinder;

    private boolean _userInitiatedDisconnect = false;
    private boolean _userNavigation = false;

    private Handler _gracefulDisconnectHandler = new Handler();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_connection_status, container, false);
        _unbinder = ButterKnife.bind(this, view);
        EduVPNApplication.get(view.getContext()).component().inject(this);
        Profile savedProfile = _preferencesService.getCurrentProfile();
        if (savedProfile != null) {
            _profileName.setText(savedProfile.getDisplayName());
        } else {
            _profileName.setText(R.string.profile_name_not_found);
        }
        _messagesList.setLayoutManager(new LinearLayoutManager(view.getContext(), LinearLayoutManager.VERTICAL, false));
        _messagesList.setAdapter(new MessagesAdapter());
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Instance provider = _preferencesService.getCurrentInstance();
        if (!TextUtils.isEmpty(provider.getLogoUri())) {
            Picasso.with(view.getContext())
                    .load(provider.getLogoUri())
                    .fit()
                    .into(_providerIcon);
        }
        // Load the user and system messages asynchronously.
        DiscoveredAPI discoveredAPI = _preferencesService.getCurrentDiscoveredAPI();
        AuthState authState = _preferencesService.getCurrentAuthState();
        final MessagesAdapter messagesAdapter = (MessagesAdapter) _messagesList.getAdapter();
        _apiService.getJSON(discoveredAPI.getSystemMessagesEndpoint(), authState, new APIService.Callback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject result) {
                try {
                    List<Message> systemMessagesList = _serializerService.deserializeMessageList(result, "system_messages");
                    messagesAdapter.setSystemMessages(systemMessagesList);
                } catch (SerializerService.UnknownFormatException ex) {
                    ErrorDialog.show(getContext(), R.string.error_dialog_title, ex.toString());
                }
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(getContext(),
                        getString(R.string.error_loading_system_messages, errorMessage),
                        Toast.LENGTH_SHORT).show();
            }
        });
        _apiService.getJSON(discoveredAPI.getUserMessagesEndpoint(), authState, new APIService.Callback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject result) {
                try {
                    List<Message> userMessagesList = _serializerService.deserializeMessageList(result, "user_messages");
                    messagesAdapter.setUserMessages(userMessagesList);
                } catch (SerializerService.UnknownFormatException ex) {
                    ErrorDialog.show(getContext(), R.string.error_dialog_title, ex.toString());
                }
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(getContext(),
                        getString(R.string.error_loading_user_messages, errorMessage),
                        Toast.LENGTH_SHORT).show();
            }
        });
        _viewSwitcher.setDisplayedChild(0);
    }

    @Override
    public void onStart() {
        super.onStart();
        _vpnStatusObserver = (o, arg) -> {
            VPNService.VPNStatus status = (VPNService.VPNStatus) arg;
            if (_currentStatusIcon != null) {
                switch (status) {
                    case CONNECTED:
                        _disconnectButton.setText(R.string.button_disconnect);
                        _disconnectButton.setEnabled(true);
                        _userNavigation = false;
                        _currentStatusIcon.setImageResource(R.drawable.connection_status_connected);
                        break;
                    case CONNECTING:
                        _currentStatusIcon.setImageResource(R.drawable.connection_status_connecting);
                        _disconnectButton.setText(R.string.button_disconnect);
                        _userNavigation = false;
                        _disconnectButton.setEnabled(true);
                        break;
                    case PAUSED:
                        _disconnectButton.setText(R.string.button_disconnect);
                        _disconnectButton.setEnabled(true);
                        _userNavigation = false;
                        _currentStatusIcon.setImageResource(R.drawable.connection_status_paused);
                        break;
                    case DISCONNECTED:
                        if (_userInitiatedDisconnect) {
                            // Go back to the home screen.
                            _disconnectButton.setEnabled(false);
                            _userNavigation = false;
                            _gracefulDisconnectHandler.removeCallbacksAndMessages(null);
                            ((MainActivity) getActivity()).openFragment(new HomeFragment(), false);
                        } else {
                            _currentStatusIcon.setImageResource(R.drawable.connection_status_disconnected);
                            _disconnectButton.setEnabled(true);
                            _disconnectButton.setText(R.string.go_back);
                            _userNavigation = true;
                        }
                        break;
                    case FAILED:
                        String message = getString(R.string.error_while_connecting, _vpnService.getErrorString());
                        ErrorDialog.show(getContext(), R.string.error_dialog_title_unable_to_connect, message);
                        _currentStatusIcon.setImageResource(R.drawable.connection_status_disconnected);
                        break;
                    default:
                        throw new RuntimeException("Unhandled VPN status!");
                }
            }
        };
        // Update the icon immediately
        _vpnStatusObserver.update(_vpnService, _vpnService.getStatus());
        _vpnService.addObserver(_vpnStatusObserver);
        _vpnService.attachConnectionInfoListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();
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

    @OnClick({R.id.notificationsSwitchButton, R.id.connectionInfoSwitchButton})
    public void onSwitcherButtonClicked(View view) {
        int selectedBg = R.drawable.switcher_button_bg_selected;
        int defaultBg = R.drawable.switcher_button_bg;
        if (view == _notificationsSwitchButton) {
            _viewSwitcher.setDisplayedChild(0);
            _notificationsSwitchButton.setBackgroundResource(selectedBg);
            _connectionInfoSwitchButton.setBackgroundResource(defaultBg);
        } else {
            _viewSwitcher.setDisplayedChild(1);
            _notificationsSwitchButton.setBackgroundResource(defaultBg);
            _connectionInfoSwitchButton.setBackgroundResource(selectedBg);
        }
    }

    @OnClick(R.id.disconnectButton)
    protected void onDisconnectButtonClicked() {
        if (_userNavigation) {
            ((MainActivity) getActivity()).openFragment(new HomeFragment(), false);
        } else {
            boolean isConnecting = _vpnService.getStatus() == VPNService.VPNStatus.CONNECTING;
            _userInitiatedDisconnect = true;
            _currentStatusIcon.setImageResource(R.drawable.connection_status_disconnected);
            _disconnectButton.setEnabled(false);
            _vpnService.disconnect();
            if (isConnecting) {
                // In this case, if we call disconnect, the process can be killed.
                // That means we won't get any notification from the disconnect event.
                // So we add a timer which waits for the disconnect event. If not received, we assume the process was killed.
                _gracefulDisconnectHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (getActivity() == null || getActivity().isFinishing()) {
                            Log.i(TAG, "Cannot close connection status fragment, because activity was already finished. User probably left the app.");
                            return;
                        }
                        Log.i(TAG, "No disconnect event received from VPN within " + WAIT_FOR_DISCONNECT_UNTIL_MS + " milliseconds. Assuming process died.");
                        ((MainActivity) getActivity()).openFragment(new HomeFragment(), false);
                    }
                }, WAIT_FOR_DISCONNECT_UNTIL_MS);

            }
        }
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
