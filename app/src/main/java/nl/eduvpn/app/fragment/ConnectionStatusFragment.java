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
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import net.openid.appauth.AuthState;

import org.json.JSONObject;

import java.util.List;
import java.util.Observer;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import de.blinkt.openvpn.activities.LogWindow;
import nl.eduvpn.app.EduVPNApplication;
import nl.eduvpn.app.MainActivity;
import nl.eduvpn.app.R;
import nl.eduvpn.app.adapter.MessagesAdapter;
import nl.eduvpn.app.base.BaseFragment;
import nl.eduvpn.app.databinding.FragmentConnectionStatusBinding;
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
public class ConnectionStatusFragment extends BaseFragment<FragmentConnectionStatusBinding> implements VPNService.ConnectionInfoCallback {

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

    private Observer _vpnStatusObserver;

    private boolean _userInitiatedDisconnect = false;
    private boolean _userNavigation = false;

    private Handler _gracefulDisconnectHandler = new Handler();


    @Override
    protected int getLayout() {
        return R.layout.fragment_connection_status;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EduVPNApplication.get(view.getContext()).component().inject(this);
        Profile savedProfile = _preferencesService.getCurrentProfile();
        if (savedProfile != null) {
            binding.profileName.setText(savedProfile.getDisplayName());
        } else {
            binding.profileName.setText(R.string.profile_name_not_found);
        }
        binding.notifications.messagesList.setLayoutManager(new LinearLayoutManager(view.getContext(), LinearLayoutManager.VERTICAL, false));
        binding.notifications.messagesList.setAdapter(new MessagesAdapter());

        binding.connectionInfo.viewLogButton.setOnClickListener(v -> onViewLogClicked());
        binding.disconnectButton.setOnClickListener(v -> onDisconnectButtonClicked());
        binding.notificationsSwitchButton.setOnClickListener(this::onSwitcherButtonClicked);
        binding.connectionInfoSwitchButton.setOnClickListener(this::onSwitcherButtonClicked);

        Instance provider = _preferencesService.getCurrentInstance();
        if (!TextUtils.isEmpty(provider.getLogoUri())) {
            Picasso.get()
                    .load(provider.getLogoUri())
                    .fit()
                    .into(binding.providerIcon);
        }
        // Load the user and system messages asynchronously.
        DiscoveredAPI discoveredAPI = _preferencesService.getCurrentDiscoveredAPI();
        AuthState authState = _preferencesService.getCurrentAuthState();
        final MessagesAdapter messagesAdapter = (MessagesAdapter) binding.notifications.messagesList.getAdapter();
        _apiService.getJSON(discoveredAPI.getSystemMessagesEndpoint(), authState, new APIService.Callback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject result) {
                try {
                    List<Message> systemMessagesList = _serializerService.deserializeMessageList(result, "system_messages");
                    messagesAdapter.setSystemMessages(systemMessagesList);
                    if (!systemMessagesList.isEmpty()) {
                        onSwitcherButtonClicked(binding.notificationsSwitchButton);
                    }
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
        binding.switcher.setDisplayedChild(0);
    }

    @Override
    public void onStart() {
        super.onStart();
        _vpnStatusObserver = (o, arg) -> {
            VPNService.VPNStatus status = (VPNService.VPNStatus) arg;
            if (binding.connectionStatusIcon != null) {
                switch (status) {
                    case CONNECTED:
                        binding.disconnectButton.setText(R.string.button_disconnect);
                        binding.disconnectButton.setEnabled(true);
                        _userNavigation = false;
                        binding.connectionStatusIcon.setImageResource(R.drawable.connection_status_connected);
                        break;
                    case CONNECTING:
                        binding.connectionStatusIcon.setImageResource(R.drawable.connection_status_connecting);
                        binding.disconnectButton.setText(R.string.button_disconnect);
                        _userNavigation = false;
                        binding.disconnectButton.setEnabled(true);
                        break;
                    case PAUSED:
                        binding.disconnectButton.setText(R.string.button_disconnect);
                        binding.disconnectButton.setEnabled(true);
                        _userNavigation = false;
                        binding.connectionStatusIcon.setImageResource(R.drawable.connection_status_paused);
                        break;
                    case DISCONNECTED:
                        if (_userInitiatedDisconnect) {
                            // Go back to the home screen.
                            binding.disconnectButton.setEnabled(false);
                            _userNavigation = false;
                            _gracefulDisconnectHandler.removeCallbacksAndMessages(null);
                            ((MainActivity) getActivity()).openFragment(HomeFragment.newInstance(false), false);
                        } else {
                            binding.connectionStatusIcon.setImageResource(R.drawable.connection_status_disconnected);
                            binding.disconnectButton.setEnabled(true);
                            binding.disconnectButton.setText(R.string.go_back);
                            _userNavigation = true;
                        }
                        break;
                    case FAILED:
                        String message = getString(R.string.error_while_connecting, _vpnService.getErrorString());
                        ErrorDialog.show(getContext(), R.string.error_dialog_title_unable_to_connect, message);
                        binding.connectionStatusIcon.setImageResource(R.drawable.connection_status_disconnected);
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

    public void onSwitcherButtonClicked(View view) {
        int selectedBg = R.drawable.switcher_button_bg_selected;
        int defaultBg = R.drawable.switcher_button_bg;
        if (view == binding.notificationsSwitchButton) {
            binding.switcher.setDisplayedChild(1);
            binding.notificationsSwitchButton.setBackgroundResource(selectedBg);
            binding.connectionInfoSwitchButton.setBackgroundResource(defaultBg);
        } else {
            binding.switcher.setDisplayedChild(0);
            binding.notificationsSwitchButton.setBackgroundResource(defaultBg);
            binding.connectionInfoSwitchButton.setBackgroundResource(selectedBg);
        }
    }

    protected void onDisconnectButtonClicked() {
        if (_userNavigation) {
            ((MainActivity) getActivity()).openFragment(HomeFragment.newInstance(false), false);
        } else {
            boolean isConnecting = _vpnService.getStatus() == VPNService.VPNStatus.CONNECTING;
            _userInitiatedDisconnect = true;
            binding.connectionStatusIcon.setImageResource(R.drawable.connection_status_disconnected);
            binding.disconnectButton.setEnabled(false);
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
                        ((MainActivity) getActivity()).openFragment(HomeFragment.newInstance(false), false);
                    }
                }, WAIT_FOR_DISCONNECT_UNTIL_MS);

            }
        }
    }

    protected void onViewLogClicked() {
        Intent intent = new Intent(getActivity(), LogWindow.class);
        startActivity(intent);
    }

    @Override
    public void updateStatus(Long secondsConnected, Long bytesIn, Long bytesOut) {
        binding.connectionInfo.durationValue.setText(FormattingUtils.formatDurationSeconds(getContext(), secondsConnected));
        binding.connectionInfo.bytesInValue.setText(FormattingUtils.formatBytesTraffic(getContext(), bytesIn));
        binding.connectionInfo.bytesOutValue.setText(FormattingUtils.formatBytesTraffic(getContext(), bytesOut));
    }

    @Override
    public void metadataAvailable(String localIpV4, String localIpV6) {
        String ipV4DisplayText = localIpV4 == null ? getString(R.string.not_available) : localIpV4;
        binding.connectionInfo.ipV4Value.setText(ipV4DisplayText);
        String ipV6DisplayText = localIpV6 == null ? getString(R.string.not_available) : localIpV6;
        binding.connectionInfo.ipV6Value.setText(ipV6DisplayText);
    }

}
