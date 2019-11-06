/*
 * This file is part of eduVPN.
 *
 * eduVPN is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * eduVPN is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with eduVPN.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package nl.eduvpn.app.fragment

import android.app.Dialog
import android.app.ProgressDialog
import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import net.openid.appauth.AuthState
import nl.eduvpn.app.Constants
import nl.eduvpn.app.EduVPNApplication
import nl.eduvpn.app.MainActivity
import nl.eduvpn.app.R
import nl.eduvpn.app.adapter.ServerAdapter
import nl.eduvpn.app.base.BaseFragment
import nl.eduvpn.app.databinding.FragmentServerSelectionBinding
import nl.eduvpn.app.entity.DiscoveredAPI
import nl.eduvpn.app.entity.Instance
import nl.eduvpn.app.service.APIService
import nl.eduvpn.app.service.ConnectionService
import nl.eduvpn.app.service.HistoryService
import nl.eduvpn.app.service.PreferencesService
import nl.eduvpn.app.service.SerializerService
import nl.eduvpn.app.utils.ErrorDialog
import nl.eduvpn.app.utils.ItemClickSupport
import nl.eduvpn.app.utils.Log
import org.json.JSONObject
import javax.inject.Inject

class ServerSelectionFragment : BaseFragment<FragmentServerSelectionBinding>() {
    override val layout = R.layout.fragment_server_selection

    private var currentDialog: Dialog? = null

    @Inject
    internal lateinit var historyService: HistoryService

    @Inject
    internal lateinit var apiService: APIService

    @Inject
    internal lateinit var serializerService: SerializerService

    @Inject
    internal lateinit var preferencesService: PreferencesService

    @Inject
    internal lateinit var connectionService: ConnectionService

    override fun onAttach(context: Context) {
        super.onAttach(context)
        EduVPNApplication.get(context).component().inject(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = ServerAdapter()
        binding.serverList.adapter = adapter
        binding.serverList.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)

        binding.addServerButton.setOnClickListener {
            (activity as? MainActivity)?.openFragment(CustomProviderFragment(), true)
        }

        val instances = historyService.savedAuthStateList.map { it.instance }

        adapter.submitList(instances)

        ItemClickSupport.addTo(binding.serverList).setOnItemClickListener { _, position, v ->
            val item = adapter.getItem(position)
            discoverApi(item)
        }

        if (arguments?.getBoolean(KEY_RETURNING_FROM_AUTH) == true) {
            val currentInstance = preferencesService.currentInstance
            val discoveredAPI = preferencesService.currentDiscoveredAPI
            val authState = historyService.getCachedAuthState(currentInstance)
            if (currentInstance != null && discoveredAPI != null && authState != null) {
                Log.d(TAG, "Continuing with profile fetch after successful auth.")
                fetchProfiles(currentInstance, discoveredAPI, authState)
            } else {
                Log.e(TAG, "Not all data available after an auth redirect. Instance OK: ${currentInstance != null}, " +
                        "discovery OK: ${discoveredAPI != null}, auth OK: ${authState != null}")
            }
        }
    }

    private fun discoverApi(instance: Instance) {
        // If no discovered API, fetch it first, then initiate the connection for the login
        val dialog = ProgressDialog.show(context, getString(R.string.progress_dialog_title), getString(R.string.api_discovery_message), true)
        currentDialog = dialog
        // Discover the API
        apiService.getJSON(instance.sanitizedBaseURI + Constants.API_DISCOVERY_POSTFIX, null, object : APIService.Callback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                try {
                    val discoveredAPI = serializerService.deserializeDiscoveredAPI(result)
                    val savedToken = historyService.getSavedToken(instance)
                    if (savedToken == null) {
                        dialog.dismiss()
                        currentDialog = null
                        authenticate(instance, discoveredAPI)
                    } else {
                        fetchProfiles(savedToken.instance, discoveredAPI, savedToken.authState)
                    }
                } catch (ex: SerializerService.UnknownFormatException) {
                    Log.e(TAG, "Error parsing discovered API!", ex)
                    ErrorDialog.show(context, R.string.error_dialog_title, ex.toString())
                    dialog.dismiss()
                    currentDialog = null
                }

            }

            override fun onError(errorMessage: String) {
                dialog.dismiss()
                currentDialog = null
                Log.e(TAG, "Error while fetching discovered API: $errorMessage")
                ErrorDialog.show(context, R.string.error_dialog_title, errorMessage)
            }
        })
    }

    /**
     * Starts downloading the list of profiles for a single VPN provider.
     *
     * @param instance      The VPN provider instance.
     * @param discoveredAPI The discovered API containing the URLs.
     * @param authState     The access and refresh token for the API.
     */
    private fun fetchProfiles(instance: Instance, discoveredAPI: DiscoveredAPI, authState: AuthState) {
        currentDialog?.setTitle(R.string.loading_available_profiles)
        apiService.getJSON(discoveredAPI.profileListEndpoint, authState, object : APIService.Callback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                currentDialog?.dismiss()
                currentDialog = null
                val activity = (activity as? MainActivity) ?: return
                try {
                    val profiles = serializerService.deserializeProfileList(result)
                    activity.openFragment(ProfileSelectionFragment.newInstance(profiles), true)
                } catch (ex: SerializerService.UnknownFormatException) {
                    Log.e(TAG, "Error parsing profile list.", ex)
                    if (!activity.isFinishing) {
                        ErrorDialog.show(activity, R.string.error_dialog_title, R.string.error_parsing_profiles)
                    }
                }

            }

            override fun onError(errorMessage: String) {
                Log.e(TAG, "Error fetching profile list: $errorMessage")
                // It is highly probable that the auth state is not valid anymore.
                // Hide the dialog and start the login process
                currentDialog?.dismiss()
                currentDialog = null
                authenticate(instance, discoveredAPI)
            }
        })
    }


    /**
     * Starts connecting to an API provider.
     *
     * @param instance The instance to connect to.
     * @param discoveredAPI The discovered API containing all the required URLs.
     */
    private fun authenticate(instance: Instance, discoveredAPI: DiscoveredAPI) {
        activity?.let {
            if (!it.isFinishing) {
                connectionService.initiateConnection(it, instance, discoveredAPI)
            }
        }
    }

    companion object {
        private val TAG = ServerSelectionFragment::javaClass.name

        private const val KEY_RETURNING_FROM_AUTH = "returning_from_auth"

        fun newInstance(returningFromAuth: Boolean): ServerSelectionFragment {
            val fragment = ServerSelectionFragment()
            val args = Bundle()
            args.putBoolean(KEY_RETURNING_FROM_AUTH, returningFromAuth)
            fragment.arguments = args
            return fragment
        }
    }

}