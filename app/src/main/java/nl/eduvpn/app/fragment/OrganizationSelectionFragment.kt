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
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import nl.eduvpn.app.EduVPNApplication
import nl.eduvpn.app.MainActivity
import nl.eduvpn.app.R
import nl.eduvpn.app.adapter.OrganizationAdapter
import nl.eduvpn.app.base.BaseFragment
import nl.eduvpn.app.databinding.FragmentOrganizationSelectionBinding
import nl.eduvpn.app.service.OrganizationService
import nl.eduvpn.app.utils.ErrorDialog
import nl.eduvpn.app.utils.ItemClickSupport
import nl.eduvpn.app.utils.hideKeyboard
import nl.eduvpn.app.viewmodel.ConnectionState
import nl.eduvpn.app.viewmodel.ConnectionViewModel
import nl.eduvpn.app.viewmodel.OrganizationSelectionViewModel
import javax.inject.Inject

/**
 * The fragment showing the list of organizations.
 * Created by Daniel Zolnai on 2020-03-13.
 */
class OrganizationSelectionFragment : BaseFragment<FragmentOrganizationSelectionBinding>() {

    @Inject
    internal lateinit var organizationService: OrganizationService

    private var dataObserver: RecyclerView.AdapterDataObserver? = null

    override val layout = R.layout.fragment_organization_selection

    private val viewModel by viewModels<OrganizationSelectionViewModel> { viewModelFactory }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        EduVPNApplication.get(view.context).component().inject(this)
        binding.viewModel = viewModel
        binding.organizationList.setHasFixedSize(true)
        binding.organizationList.layoutManager = LinearLayoutManager(view.context, LinearLayoutManager.VERTICAL, false)
        val adapter = OrganizationAdapter()
        binding.organizationList.adapter = adapter
        ItemClickSupport.addTo(binding.organizationList).setOnItemClickListener { _, position, _ ->
            val item = adapter.getItem(position)
            if (item is OrganizationAdapter.OrganizationAdapterItem.Header) {
                return@setOnItemClickListener
            } else if (item is OrganizationAdapter.OrganizationAdapterItem.SecureInternet) {
                binding.search.hideKeyboard()
                viewModel.selectOrganizationAndInstance(item.organization, item.server)
            } else if (item is OrganizationAdapter.OrganizationAdapterItem.InstituteAccess) {
                binding.search.hideKeyboard()
                viewModel.selectOrganizationAndInstance(null, item.server)
            }
        }
        dataObserver = object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                when {
                    adapter.itemCount > 0 -> binding.organizationDiscoveryStatus.visibility = View.GONE
                    viewModel.state.value != ConnectionState.Ready -> {
                        binding.organizationDiscoveryStatus.visibility = View.GONE
                    }
                    else -> {
                        binding.organizationDiscoveryStatus.setText(R.string.no_match_found)
                        binding.organizationDiscoveryStatus.visibility = View.VISIBLE
                    }
                }
            }
        }
        dataObserver?.let {
            adapter.registerAdapterDataObserver(it)
            // Trigger initial status
            it.onChanged()
        }
        viewModel.adapterItems.observe(viewLifecycleOwner, Observer {
            adapter.submitList(it)
        })
        viewModel.parentAction.observe(viewLifecycleOwner, Observer { parentAction ->
            when (parentAction) {
                is ConnectionViewModel.ParentAction.InitiateConnection -> {
                    activity?.let { activity ->
                        if (!activity.isFinishing) {
                            viewModel.initiateConnection(activity, parentAction.instance, parentAction.discoveredAPI)
                        }
                    }
                }
                is ConnectionViewModel.ParentAction.ConnectWithProfile -> {
                    viewModel.openVpnConnectionToProfile(requireActivity(), parentAction.vpnProfile)
                    (activity as? MainActivity)?.openFragment(ConnectionStatusFragment(), false)
                }
                is ConnectionViewModel.ParentAction.DisplayError -> {
                    ErrorDialog.show(requireContext(), parentAction.title, parentAction.message)
                }
            }
        })
        binding.search.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                viewModel.artworkVisible.value = false
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        if (binding.organizationList.adapter != null && dataObserver != null) {
            binding.organizationList.adapter?.unregisterAdapterDataObserver(dataObserver!!)
            dataObserver = null
        }
    }
}
