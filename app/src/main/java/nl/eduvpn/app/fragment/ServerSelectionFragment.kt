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
import com.google.android.material.snackbar.Snackbar
import nl.eduvpn.app.BuildConfig
import nl.eduvpn.app.EduVPNApplication
import nl.eduvpn.app.MainActivity
import nl.eduvpn.app.R
import nl.eduvpn.app.adapter.OrganizationAdapter
import nl.eduvpn.app.base.BaseFragment
import nl.eduvpn.app.databinding.FragmentServerSelectionBinding
import nl.eduvpn.app.entity.Instance
import nl.eduvpn.app.utils.ErrorDialog
import nl.eduvpn.app.utils.ItemClickSupport
import nl.eduvpn.app.viewmodel.BaseConnectionViewModel
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
            val activity = activity as? MainActivity ?: return@OrganizationAdapter
            activity.selectCountry()
        }
        binding.viewModel = viewModel
        binding.serverList.adapter = adapter
        binding.serverList.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        viewModel.adapterItems.observe(viewLifecycleOwner) {
            adapter.submitList(it)
        }

        viewModel.parentAction.observe(viewLifecycleOwner) { parentAction ->
            when (parentAction) {
                is BaseConnectionViewModel.ParentAction.OpenProfileSelector -> {
                    (activity as? MainActivity)?.openFragment(
                        ProfileSelectionFragment.newInstance(
                            parentAction.profiles
                        ), true
                    )
                }
                is BaseConnectionViewModel.ParentAction.ShowContextCanceledToast -> {
                    Snackbar.make(view, parentAction.message, Snackbar.LENGTH_LONG).show()
                }
                is BaseConnectionViewModel.ParentAction.DisplayError -> {
                    ErrorDialog.show(requireContext(), parentAction.title, parentAction.message)
                }
                else -> {
                    // Do nothing.
                }
            }
        }

        ItemClickSupport.addTo(binding.serverList).setOnItemClickListener { _, position, _ ->
            val item = adapter.getItem(position)
            if (item is OrganizationAdapter.OrganizationAdapterItem.SecureInternet) {
                viewModel.connectingTo.value = item.server
                viewModel.getProfiles(item.server)
            } else if (item is OrganizationAdapter.OrganizationAdapterItem.InstituteAccess) {
                viewModel.connectingTo.value = item.server
                viewModel.getProfiles(item.server)
            }
        }.setOnItemLongClickListener { _, position, _ ->
            val item = adapter.getItem(position)
            // If type is distributed access, then it is an organization server, which can be reset from Settings instead
            if (item is OrganizationAdapter.OrganizationAdapterItem.InstituteAccess) {
                displayDeleteDialog(item.server)
                return@setOnItemLongClickListener true
            }
            return@setOnItemLongClickListener false
        }
        binding.warning.setOnClickListener {
            ErrorDialog.show(it.context, R.string.warning_title, viewModel.warning.value!!)
        }
        binding.addServerButton.setOnClickListener {
            openAddServerFragment(true)
        }
    }

    private fun openAddServerFragment(openOnTop: Boolean) {
        @Suppress("ConstantConditionIf")
        if (BuildConfig.API_DISCOVERY_ENABLED) {
            (activity as? MainActivity)?.openFragment(OrganizationSelectionFragment(), openOnTop)
        } else {
            (activity as? MainActivity)?.openFragment(AddServerFragment(), openOnTop)
        }
    }

    private fun displayDeleteDialog(instance: Instance) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_server)
            .setMessage(
                getString(
                    R.string.delete_server_message,
                    instance.displayName.bestTranslation,
                    instance.sanitizedBaseURI
                )
            )
            .setPositiveButton(R.string.button_remove) { dialog, _ ->
                viewModel.deleteAllDataForInstance(instance)
                if (viewModel.hasNoMoreServers()) {
                    openAddServerFragment(false)
                }
                dialog.dismiss()
            }
            .setNegativeButton(R.string.delete_server_cancel) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    fun connectToSelectedInstance() {
        if (viewModel.connectingTo.value != null) {
            viewModel.discoverApi(viewModel.connectingTo.value!!)
            viewModel.connectingTo.value = null
        }
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
