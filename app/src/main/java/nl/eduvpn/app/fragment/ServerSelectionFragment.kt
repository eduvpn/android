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

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import nl.eduvpn.app.EduVPNApplication
import nl.eduvpn.app.MainActivity
import nl.eduvpn.app.R
import nl.eduvpn.app.adapter.OrganizationAdapter
import nl.eduvpn.app.base.BaseFragment
import nl.eduvpn.app.databinding.FragmentServerSelectionBinding
import nl.eduvpn.app.utils.ErrorDialog
import nl.eduvpn.app.utils.ItemClickSupport
import nl.eduvpn.app.viewmodel.ConnectionViewModel
import nl.eduvpn.app.viewmodel.ServerSelectionViewModel

class ServerSelectionFragment : BaseFragment<FragmentServerSelectionBinding>() {
    override val layout = R.layout.fragment_server_selection

    private val viewModel by viewModels<ServerSelectionViewModel> { viewModelFactory }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        EduVPNApplication.get(context).component().inject(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = OrganizationAdapter {
            val countryList = viewModel.requestCountryList()
            if (countryList == null) {
                ErrorDialog.show(requireContext(), R.string.unexpected_error, R.string.error_countries_are_not_available)
            } else {
                AlertDialog.Builder(requireContext())
                        .setItems(countryList.map { it.second }.toTypedArray()) { _, which ->
                            val selectedInstance = countryList[which]
                            viewModel.changePreferredCountry(selectedInstance.first)
                        }.show()
            }
        }
        binding.viewModel = viewModel
        binding.serverList.adapter = adapter
        binding.serverList.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
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
                is ConnectionViewModel.ParentAction.OpenProfileSelector -> {
                    (activity as? MainActivity)?.openFragment(ProfileSelectionFragment.newInstance(parentAction.profiles), true)
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

        ItemClickSupport.addTo(binding.serverList).setOnItemClickListener { _, position, _ ->
            val item = adapter.getItem(position)
            if (item is OrganizationAdapter.OrganizationAdapterItem.SecureInternet) {
                viewModel.discoverApi(item.server, null)
            } else if (item is OrganizationAdapter.OrganizationAdapterItem.InstituteAccess) {
                viewModel.discoverApi(item.server, null)
            }
        }
        binding.warning.setOnClickListener {
            ErrorDialog.show(it.context, R.string.warning_title, viewModel.warning.value!!)
        }
        binding.addServerButton.setOnClickListener {
            (activity as? MainActivity)?.openFragment(OrganizationSelectionFragment(), true)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    companion object {
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