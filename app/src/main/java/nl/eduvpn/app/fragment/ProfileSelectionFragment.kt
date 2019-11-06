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

package nl.eduvpn.app.fragment

import android.app.Dialog
import android.app.ProgressDialog
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import net.openid.appauth.AuthState
import nl.eduvpn.app.BuildConfig
import nl.eduvpn.app.EduVPNApplication
import nl.eduvpn.app.MainActivity
import nl.eduvpn.app.R
import nl.eduvpn.app.adapter.ProfileAdapter
import nl.eduvpn.app.base.BaseFragment
import nl.eduvpn.app.databinding.FragmentProfileSelectionBinding
import nl.eduvpn.app.entity.DiscoveredAPI
import nl.eduvpn.app.entity.Instance
import nl.eduvpn.app.entity.Profile
import nl.eduvpn.app.entity.SavedKeyPair
import nl.eduvpn.app.entity.SavedProfile
import nl.eduvpn.app.service.APIService
import nl.eduvpn.app.service.HistoryService
import nl.eduvpn.app.service.PreferencesService
import nl.eduvpn.app.service.SerializerService
import nl.eduvpn.app.service.VPNService
import nl.eduvpn.app.utils.ErrorDialog
import nl.eduvpn.app.utils.FormattingUtils
import nl.eduvpn.app.utils.ItemClickSupport
import nl.eduvpn.app.utils.Log
import org.json.JSONObject
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.util.ArrayList
import javax.inject.Inject

/**
 * Fragment which is displayed when the app start.
 * Created by Daniel Zolnai on 2016-10-20.
 */
class ProfileSelectionFragment : BaseFragment<FragmentProfileSelectionBinding>() {

    @Inject
    internal lateinit var historyService: HistoryService

    @Inject
    internal lateinit var vpnService: VPNService

    @Inject
    internal lateinit var preferencesService: PreferencesService

    @Inject
    internal lateinit var apiService: APIService

    @Inject
    internal lateinit var serializerService: SerializerService

    private var currentDialog: Dialog? = null

    override val layout = R.layout.fragment_profile_selection

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        EduVPNApplication.get(view.context).component().inject(this)

        // Basic setup of the lists
        binding.profileList.setHasFixedSize(true)
        binding.profileList.layoutManager = LinearLayoutManager(view.context, LinearLayoutManager.VERTICAL, false)

        // Add the adapters
        val profileAdapter = ProfileAdapter(preferencesService.currentInstance!!)
        binding.profileList.adapter = profileAdapter

        val profiles: ArrayList<Profile> = arguments?.getParcelableArrayList(KEY_PROFILES)!!

        profileAdapter.submitList(profiles)

        // Add click listeners
        ItemClickSupport.addTo(binding.profileList).setOnItemClickListener { recyclerView, position, v ->
            val profile = profileAdapter.getItem(position)
            connectWithProfile(profile)
        }

        if (profiles.size == 1) {
            connectWithProfile(profiles[0])
        }
    }

    private fun connectWithProfile(profile: Profile) {
        // We surely have a discovered API and access token, since we just loaded the list with them
        val instance = preferencesService.currentInstance
        val authState = historyService.getCachedAuthState(instance!!)
        val discoveredAPI = preferencesService.currentDiscoveredAPI
        if (authState == null || discoveredAPI == null) {
            ErrorDialog.show(context, R.string.error_dialog_title, R.string.cant_connect_application_state_missing)
            return
        }
        preferencesService.currentInstance(instance)
        preferencesService.storeCurrentProfile(profile)
        preferencesService.storeCurrentAuthState(authState)
        // Always download a new profile.
        // Just to be sure,
        downloadKeyPairIfNeeded(instance, discoveredAPI, profile, authState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        currentDialog?.dismiss()
        currentDialog = null
    }


    /**
     * Downloads the key pair if no cached one found. After that it downloads the profile and connects to it.
     *
     * @param instance      The VPN provider.
     * @param discoveredAPI The discovered API.
     * @param profile       The profile to download.
     */
    private fun downloadKeyPairIfNeeded(instance: Instance, discoveredAPI: DiscoveredAPI,
                                        profile: Profile, authState: AuthState) {
        // First we create a keypair, if there is no saved one yet.
        val savedKeyPair = historyService.getSavedKeyPairForInstance(instance)
        val dialogMessageRes = if (savedKeyPair != null) R.string.vpn_profile_checking_certificate else R.string.vpn_profile_creating_keypair
        val progressDialog = ProgressDialog.show(context, getString(R.string.progress_dialog_title),
                getString(dialogMessageRes), true, false)
        currentDialog = progressDialog
        if (savedKeyPair != null) {
            checkCertificateValidity(instance, discoveredAPI, savedKeyPair, profile, authState, progressDialog)
            return
        }

        var requestData = "display_name=eduVPN"
        try {
            requestData = "display_name=" + URLEncoder.encode(BuildConfig.CERTIFICATE_DISPLAY_NAME, "UTF-8")
        } catch (e: UnsupportedEncodingException) {
            // unable to encode the display name, use default
        }

        val createKeyPairEndpoint = discoveredAPI.createKeyPairEndpoint
        apiService.postResource(createKeyPairEndpoint, requestData, authState, object : APIService.Callback<String> {

            override fun onSuccess(keyPairJson: String) {
                try {
                    val keyPair = serializerService.deserializeKeyPair(JSONObject(keyPairJson))
                    Log.i(TAG, "Created key pair, is it successful: " + keyPair.isOK)
                    // Save it for later
                    val newKeyPair = SavedKeyPair(instance, keyPair)
                    historyService.storeSavedKeyPair(newKeyPair)
                    downloadProfileWithKeyPair(instance, discoveredAPI, newKeyPair, profile, authState, progressDialog)
                } catch (ex: Exception) {
                    progressDialog.dismiss()
                    if (activity != null) {
                        currentDialog = ErrorDialog.show(context, R.string.error_dialog_title, getString(R.string.error_parsing_keypair, ex.message))
                    }
                    Log.e(TAG, "Unable to parse keypair data", ex)
                }

            }

            override fun onError(errorMessage: String) {
                Log.e(TAG, "Error creating keypair: $errorMessage")
                progressDialog.dismiss()
                currentDialog = ErrorDialog.show(context, R.string.error_dialog_title, getString(R.string.error_creating_keypair, errorMessage))
            }
        })
    }

    /**
     * Now that we have the key pair, we can download the profile.
     *
     * @param instance      The API instance definition.
     * @param discoveredAPI The discovered API URLs.
     * @param savedKeyPair  The private key and certificate used to generate the profile.
     * @param profile       The profile to create.
     * @param dialog        Loading dialog which should be dismissed when finished.
     */
    private fun downloadProfileWithKeyPair(instance: Instance, discoveredAPI: DiscoveredAPI,
                                           savedKeyPair: SavedKeyPair, profile: Profile,
                                           authState: AuthState,
                                           dialog: ProgressDialog) {
        dialog.setMessage(getString(R.string.vpn_profile_download_message))
        val requestData = "?profile_id=" + profile.profileId
        apiService.getString(discoveredAPI.profileConfigEndpoint + requestData, authState, object : APIService.Callback<String> {
            override fun onSuccess(vpnConfig: String) {
                // The downloaded profile misses the <cert> and <key> fields. We will insert that via the saved key pair.
                val configName = FormattingUtils.formatProfileName(context!!, instance, profile)
                val vpnProfile = vpnService.importConfig(vpnConfig, configName, savedKeyPair)
                val activity = activity
                if (vpnProfile != null && activity != null) {
                    // Cache the profile
                    val savedProfile = SavedProfile(instance, profile, vpnProfile.uuidString)
                    historyService.cacheSavedProfile(savedProfile)
                    // Connect with the profile
                    dialog.dismiss()
                    vpnService.connect(activity, vpnProfile)
                    (activity as MainActivity).openFragment(ConnectionStatusFragment(), false)
                } else {
                    dialog.dismiss()
                    currentDialog = ErrorDialog.show(context, R.string.error_dialog_title, R.string.error_importing_profile)
                }
            }

            override fun onError(errorMessage: String) {
                dialog.dismiss()
                currentDialog = ErrorDialog.show(context, R.string.error_dialog_title, getString(R.string.error_fetching_profile, errorMessage))
                Log.e(TAG, "Error fetching profile: $errorMessage")
            }
        })
    }

    private fun checkCertificateValidity(instance: Instance, discoveredAPI: DiscoveredAPI, savedKeyPair: SavedKeyPair, profile: Profile, authState: AuthState, dialog: ProgressDialog) {
        val commonName = savedKeyPair.keyPair.certificateCommonName
        if (commonName == null) {
            // Unable to check, better download it again.
            historyService.removeSavedKeyPairs(instance)
            // Try downloading it again.
            dialog.dismiss()
            downloadKeyPairIfNeeded(instance, discoveredAPI, profile, authState)
            Log.w(TAG, "Could not check if certificate is valid. Downloading again.")
        }
        apiService.getJSON(discoveredAPI.getCheckCertificateEndpoint(commonName), authState, object : APIService.Callback<JSONObject> {

            override fun onSuccess(result: JSONObject) {
                try {
                    val isValid = result.getJSONObject("check_certificate").getJSONObject("data").getBoolean("is_valid")
                    if (isValid) {
                        Log.i(TAG, "Certificate appears to be valid.")
                        downloadProfileWithKeyPair(instance, discoveredAPI, savedKeyPair, profile, authState, dialog)
                    } else {
                        dialog.dismiss()
                        val reason = result.getJSONObject("check_certificate").getJSONObject("data").getString("reason")
                        if ("user_disabled" == reason || "certificate_disabled" == reason) {
                            var errorStringId = R.string.error_certificate_disabled
                            if ("user_disabled" == reason) {
                                errorStringId = R.string.error_user_disabled
                            }
                            currentDialog = ErrorDialog.show(context, R.string.error_dialog_title_invalid_certificate, getString(errorStringId))
                        } else {
                            // Remove stored keypair.
                            historyService.removeSavedKeyPairs(instance)
                            Log.i(TAG, "Certificate is invalid. Fetching new one. Reason: $reason")
                            // Try downloading it again.
                            downloadKeyPairIfNeeded(instance, discoveredAPI, profile, authState)
                        }

                    }
                } catch (ex: Exception) {
                    dialog.dismiss()
                    currentDialog = ErrorDialog.show(context, R.string.error_dialog_title, getString(R.string.error_parsing_certificate))
                    Log.e(TAG, "Unexpected certificate call response!", ex)
                }

            }

            override fun onError(errorMessage: String?) {
                dialog.dismiss()
                if (errorMessage != null && (APIService.USER_NOT_AUTHORIZED_ERROR == errorMessage || errorMessage.contains("invalid_grant"))) {
                    currentDialog = ErrorDialog.show(context, getString(R.string.error_dialog_title),
                            getString(R.string.access_rejected_instance, instance.displayName))
                } else {
                    currentDialog = ErrorDialog.show(context, R.string.error_dialog_title, getString(R.string.error_checking_certificate))
                    Log.e(TAG, "Error checking certificate: " + errorMessage!!)
                }

            }
        })
    }

    companion object {

        private val TAG = ProfileSelectionFragment::class.java.name

        private const val KEY_PROFILES = "profiles"

        fun newInstance(profileList: List<Profile>): ProfileSelectionFragment {
            val fragment = ProfileSelectionFragment()
            val arguments = Bundle()
            arguments.putParcelableArrayList(KEY_PROFILES, ArrayList(profileList))
            fragment.arguments = arguments
            return fragment
        }
    }
}
