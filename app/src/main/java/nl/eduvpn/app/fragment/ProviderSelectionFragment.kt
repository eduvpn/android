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

import android.app.ProgressDialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import nl.eduvpn.app.Constants
import nl.eduvpn.app.EduVPNApplication
import nl.eduvpn.app.R
import nl.eduvpn.app.adapter.ProviderAdapter
import nl.eduvpn.app.base.BaseFragment
import nl.eduvpn.app.databinding.FragmentProviderSelectionBinding
import nl.eduvpn.app.entity.AuthorizationType
import nl.eduvpn.app.entity.DiscoveredAPI
import nl.eduvpn.app.entity.Instance
import nl.eduvpn.app.service.APIService
import nl.eduvpn.app.service.ConfigurationService
import nl.eduvpn.app.service.ConnectionService
import nl.eduvpn.app.service.PreferencesService
import nl.eduvpn.app.service.SerializerService
import nl.eduvpn.app.utils.ErrorDialog
import nl.eduvpn.app.utils.ItemClickSupport
import nl.eduvpn.app.utils.Log
import org.json.JSONObject
import javax.inject.Inject

/**
 * The fragment showing the provider list.
 * Created by Daniel Zolnai on 2016-10-07.
 */
class ProviderSelectionFragment : BaseFragment<FragmentProviderSelectionBinding>() {

    @Inject
    internal lateinit var configurationService: ConfigurationService

    @Inject
    internal lateinit var serializerService: SerializerService

    @Inject
    internal lateinit var apiService: APIService

    @Inject
    internal lateinit var connectionService: ConnectionService

    @Inject
    internal lateinit var preferencesService: PreferencesService

    private lateinit var authorizationType: AuthorizationType

    private var dataObserver: RecyclerView.AdapterDataObserver? = null

    private var currentDialog: ProgressDialog? = null

    override val layout = R.layout.fragment_provider_selection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            authorizationType = AuthorizationType.valueOf(savedInstanceState.getString(EXTRA_AUTHORIZATION_TYPE)!!)
        } else {
            authorizationType = AuthorizationType.valueOf(arguments?.getString(EXTRA_AUTHORIZATION_TYPE)!!)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        currentDialog?.dismiss()
    }

    override fun onStart() {
        super.onStart()
        currentDialog?.show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(EXTRA_AUTHORIZATION_TYPE, authorizationType.name)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        EduVPNApplication.get(view.context).component().inject(this)
        if (authorizationType == AuthorizationType.Local) {
            binding.header.setText(R.string.select_your_institution_title)
            binding.description.visibility = View.GONE
        } else {
            binding.header.setText(R.string.select_your_country_title)
            binding.description.visibility = View.VISIBLE
        }
        binding.providerList.setHasFixedSize(true)
        binding.providerList.layoutManager = LinearLayoutManager(view.context, LinearLayoutManager.VERTICAL, false)
        val adapter = ProviderAdapter(configurationService, authorizationType)
        binding.providerList.adapter = adapter
        ItemClickSupport.addTo(binding.providerList).setOnItemClickListener { recyclerView, position, v ->
            val instance = (recyclerView.adapter as ProviderAdapter).getItem(position)
            if (instance == null) {
                // Should never happen
                val mainView = getView()
                if (mainView != null) {
                    Snackbar.make(mainView, R.string.error_selected_instance_not_found, Snackbar.LENGTH_LONG).show()
                }
                Log.e(TAG, "Instance not found for position: $position")
            } else {
                discoverApi(instance)
            }
        }
        // When clicked long on an item, display its name in a toast.
        ItemClickSupport.addTo(binding.providerList).setOnItemLongClickListener { recyclerView, position, v ->
            val instance = (recyclerView.adapter as ProviderAdapter).getItem(position)
            val name = instance?.displayName ?: getString(R.string.display_other_name)
            Toast.makeText(recyclerView.context, name, Toast.LENGTH_LONG).show()
            true
        }
        dataObserver = object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                if (adapter.itemCount > 0) {
                    binding.providerStatus.visibility = View.GONE
                } else if (adapter.isDiscoveryPending) {
                    binding.providerStatus.setText(R.string.discovering_providers)
                    binding.providerStatus.visibility = View.VISIBLE
                } else {
                    binding.providerStatus.setText(R.string.no_provider_found)
                    binding.providerStatus.visibility = View.VISIBLE
                }
            }
        }
        dataObserver?.let {
            adapter.registerAdapterDataObserver(it)
            // Trigger initial status
            it.onChanged()
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
                    dialog.dismiss()
                    currentDialog = null
                    authenticate(instance, discoveredAPI)
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


    override fun onDestroyView() {
        super.onDestroyView()
        if (binding.providerList != null && binding.providerList.adapter != null && dataObserver != null) {
            binding.providerList.adapter?.unregisterAdapterDataObserver(dataObserver!!)
            dataObserver = null
        }
    }

    companion object {

        private val TAG = ProviderSelectionFragment::class.java.name
        private const val EXTRA_AUTHORIZATION_TYPE = "extra_authorization_type"

        fun newInstance(authorizationType: AuthorizationType): ProviderSelectionFragment {
            val fragment = ProviderSelectionFragment()
            val args = Bundle()
            args.putString(EXTRA_AUTHORIZATION_TYPE, authorizationType.name)
            fragment.arguments = args
            return fragment
        }
    }
}
