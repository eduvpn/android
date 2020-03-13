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
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import nl.eduvpn.app.EduVPNApplication
import nl.eduvpn.app.MainActivity
import nl.eduvpn.app.R
import nl.eduvpn.app.adapter.OrganizationAdapter
import nl.eduvpn.app.base.BaseFragment
import nl.eduvpn.app.databinding.FragmentOrganizationSelectionBinding
import nl.eduvpn.app.service.ConfigurationService
import nl.eduvpn.app.utils.ErrorDialog
import nl.eduvpn.app.utils.ItemClickSupport
import nl.eduvpn.app.viewmodel.ConnectionViewModel
import javax.inject.Inject

/**
 * The fragment showing the list of organizations.
 * Created by Daniel Zolnai on 2016-10-07.
 */
class OrganizationSelectionFragment : BaseFragment<FragmentOrganizationSelectionBinding>() {

    @Inject
    internal lateinit var configurationService: ConfigurationService

    private var dataObserver: RecyclerView.AdapterDataObserver? = null

    override val layout = R.layout.fragment_organization_selection

    private val viewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(ConnectionViewModel::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        EduVPNApplication.get(view.context).component().inject(this)
        binding.viewModel = viewModel
        binding.providerList.setHasFixedSize(true)
        binding.providerList.layoutManager = LinearLayoutManager(view.context, LinearLayoutManager.VERTICAL, false)
        val adapter = OrganizationAdapter(configurationService)
        binding.providerList.adapter = adapter
        ItemClickSupport.addTo(binding.providerList).setOnItemClickListener { recyclerView, position, _ ->
            // TODO
        }
        dataObserver = object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                when {
                    adapter.itemCount > 0 -> binding.providerStatus.visibility = View.GONE
                    adapter.isDiscoveryPending -> {
                        binding.providerStatus.setText(R.string.discovering_organizations)
                        binding.providerStatus.visibility = View.VISIBLE
                    }
                    else -> {
                        binding.providerStatus.setText(R.string.no_organization_found)
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

        viewModel.parentAction.observe(this, Observer { parentAction ->
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
        private val TAG = OrganizationSelectionFragment::class.java.name
    }
}
