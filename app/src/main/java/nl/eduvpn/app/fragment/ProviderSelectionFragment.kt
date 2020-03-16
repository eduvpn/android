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

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import nl.eduvpn.app.EduVPNApplication
import nl.eduvpn.app.MainActivity
import nl.eduvpn.app.R
import nl.eduvpn.app.adapter.AuthTypeProviderAdapter
import nl.eduvpn.app.adapter.OrganizationServerProviderAdapter
import nl.eduvpn.app.base.BaseFragment
import nl.eduvpn.app.databinding.FragmentProviderSelectionBinding
import nl.eduvpn.app.entity.AuthorizationType
import nl.eduvpn.app.entity.Organization
import nl.eduvpn.app.service.ConfigurationService
import nl.eduvpn.app.service.OrganizationService
import nl.eduvpn.app.service.PreferencesService
import nl.eduvpn.app.utils.ErrorDialog
import nl.eduvpn.app.utils.ItemClickSupport
import nl.eduvpn.app.utils.Log
import nl.eduvpn.app.viewmodel.ConnectionViewModel
import nl.eduvpn.app.viewmodel.ProviderSelectionViewModel
import javax.inject.Inject

/**
 * The fragment showing the provider list.
 * Created by Daniel Zolnai on 2016-10-07.
 */
class ProviderSelectionFragment : BaseFragment<FragmentProviderSelectionBinding>() {

    @Inject
    internal lateinit var configurationService: ConfigurationService

    @Inject
    internal lateinit var organizationService: OrganizationService

    @Inject
    internal lateinit var preferencesService: PreferencesService

    private lateinit var authorizationType: AuthorizationType

    private var dataObserver: RecyclerView.AdapterDataObserver? = null

    override val layout = R.layout.fragment_provider_selection

    private val viewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(ProviderSelectionViewModel::class.java)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authorizationType = if (savedInstanceState != null) {
            AuthorizationType.valueOf(savedInstanceState.getString(EXTRA_AUTHORIZATION_TYPE)!!)
        } else {
            AuthorizationType.valueOf(arguments?.getString(EXTRA_AUTHORIZATION_TYPE)!!)
        }
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
        } else if (authorizationType == AuthorizationType.Distributed) {
            binding.header.setText(R.string.select_your_country_title)
            binding.description.visibility = View.VISIBLE
        } else {
            binding.header.setText(R.string.select_your_server_title)
            binding.description.visibility = View.GONE
        }
        binding.viewModel = viewModel
        binding.providerList.setHasFixedSize(true)
        binding.providerList.layoutManager = LinearLayoutManager(view.context, LinearLayoutManager.VERTICAL, false)
        val adapter = if (authorizationType == AuthorizationType.Organization) {
            OrganizationServerProviderAdapter()
        } else {
            AuthTypeProviderAdapter(configurationService, authorizationType)
        }
        binding.providerList.adapter = adapter
        ItemClickSupport.addTo(binding.providerList).setOnItemClickListener { _, position, _ ->
            val instance = adapter.getItem(position)
            if (instance == null) {
                // Should never happen
                val mainView = getView()
                if (mainView != null) {
                    Snackbar.make(mainView, R.string.error_selected_instance_not_found, Snackbar.LENGTH_LONG).show()
                }
                Log.e(TAG, "Instance not found for position: $position")
            } else {
                viewModel.discoverApi(instance)
            }
        }
        // When clicked long on an item, display its name in a toast.
        ItemClickSupport.addTo(binding.providerList).setOnItemLongClickListener { recyclerView, position, _ ->
            val instance = (recyclerView.adapter as AuthTypeProviderAdapter).getItem(position)
            val name = instance?.displayName ?: getString(R.string.display_other_name)
            Toast.makeText(recyclerView.context, name, Toast.LENGTH_LONG).show()
            true
        }
        dataObserver = object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                when {
                    adapter.itemCount > 0 -> binding.providerStatus.visibility = View.GONE
                    configurationService.isPendingDiscovery(authorizationType) -> {
                        binding.providerStatus.setText(R.string.discovering_providers)
                        binding.providerStatus.visibility = View.VISIBLE
                    }
                    else -> {
                        binding.providerStatus.setText(R.string.no_provider_found)
                        binding.providerStatus.visibility = View.VISIBLE
                    }
                }
            }
        }
        dataObserver?.let {
            adapter.registerAdapterDataObserver(it)
            // Trigger initial status
            it.onChanged()
        }

        viewModel.parentAction.observe(viewLifecycleOwner, Observer { parentAction ->
            when (parentAction) {
                is ConnectionViewModel.ParentAction.InitiateConnection -> {
                    activity?.let { activity ->
                        if (!activity.isFinishing) {
                            viewModel.initiateConnection(activity, parentAction.instance, parentAction.discoveredAPI)
                        }
                    }
                }
                is ConnectionViewModel.ParentAction.OpenProfileSelector -> {
                    (activity as? MainActivity)?.openFragment(ProfileSelectionFragment.newInstance(parentAction.profiles), true)
                }
                is ConnectionViewModel.ParentAction.DisplayError -> {
                    ErrorDialog.show(requireContext(), parentAction.title, parentAction.message)
                }
                is ConnectionViewModel.ParentAction.ConnectWithProfile -> {
                    viewModel.openVpnConnectionToProfile(requireActivity(), parentAction.vpnProfile)
                    (activity as? MainActivity)?.openFragment(ConnectionStatusFragment(), false)
                }
            }
        })
        if (adapter is OrganizationServerProviderAdapter) {
            viewModel.currentOrganizationInstances.observe(viewLifecycleOwner, Observer {
                adapter.setInstances(it)
            })
        }
    }


    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (binding.providerList.adapter != null && dataObserver != null) {
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