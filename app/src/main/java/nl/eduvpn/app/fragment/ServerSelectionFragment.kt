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
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import nl.eduvpn.app.BuildConfig
import nl.eduvpn.app.EduVPNApplication
import nl.eduvpn.app.MainActivity
import nl.eduvpn.app.R
import nl.eduvpn.app.adapter.ServerAdapter
import nl.eduvpn.app.base.BaseFragment
import nl.eduvpn.app.databinding.FragmentServerSelectionBinding
import nl.eduvpn.app.entity.Instance
import nl.eduvpn.app.utils.ErrorDialog
import nl.eduvpn.app.utils.ItemClickSupport
import nl.eduvpn.app.viewmodel.ConnectionViewModel

class ServerSelectionFragment : BaseFragment<FragmentServerSelectionBinding>() {
    override val layout = R.layout.fragment_server_selection

    private val viewModel by lazy {
        ViewModelProviders.of(requireActivity(), viewModelFactory).get(ConnectionViewModel::class.java)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        EduVPNApplication.get(context).component().inject(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = ServerAdapter()
        binding.viewModel = viewModel
        binding.serverList.adapter = adapter
        binding.serverList.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)

        binding.addServerButton.setOnClickListener {
            @Suppress("ConstantConditionIf")
            if (BuildConfig.API_DISCOVERY_ENABLED) {
                (activity as? MainActivity)?.openFragment(TypeSelectorFragment(), true)
            } else {
                (activity as? MainActivity)?.openFragment(CustomProviderFragment(), true)
            }
        }

        viewModel.instances.observe(this, Observer {
            (binding.serverList.adapter as? ServerAdapter)?.submitList(it)
        })

        viewModel.parentAction.observe(this, Observer { parentAction ->
            when (parentAction) {
                ConnectionViewModel.ParentAction.CheckIfAutoConnectRequired -> tryAutoConnectIfReturningFromAuth()
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
            }
        })

        ItemClickSupport.addTo(binding.serverList).setOnItemClickListener { _, position, _ ->
            val item = adapter.getItem(position)
            viewModel.discoverApi(item)
        }
        ItemClickSupport.addTo(binding.serverList).setOnItemLongClickListener { _, position, _ ->
            val item = adapter.getItem(position)
            displayDeleteDialog(item)
        }

        tryAutoConnectIfReturningFromAuth()
    }


    private fun displayDeleteDialog(instance: Instance): Boolean {
        AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_server)
                .setMessage(getString(R.string.delete_server_message, instance.displayName, instance.sanitizedBaseURI))
                .setPositiveButton(R.string.button_remove) { dialog, _ ->
                    viewModel.deleteAllDataForInstance(instance)
                    dialog.dismiss()
                }
                .setNegativeButton(R.string.delete_server_cancel) { dialog, _ -> dialog.dismiss() }
                .show()
        return true
    }


    private fun tryAutoConnectIfReturningFromAuth() {
        if (arguments?.getBoolean(KEY_RETURNING_FROM_AUTH) == true) {
            if (viewModel.tryAutoConnectIfReturningFromAuth()) {
                arguments?.remove(KEY_RETURNING_FROM_AUTH) // Makes sure it is not triggered twice
            }
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