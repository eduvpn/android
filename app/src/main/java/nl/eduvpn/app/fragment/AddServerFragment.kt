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
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.viewModels
import nl.eduvpn.app.EduVPNApplication
import nl.eduvpn.app.MainActivity
import nl.eduvpn.app.R
import nl.eduvpn.app.base.BaseFragment
import nl.eduvpn.app.databinding.FragmentAddServerBinding
import nl.eduvpn.app.entity.AuthorizationType
import nl.eduvpn.app.entity.Instance
import nl.eduvpn.app.entity.TranslatableString
import nl.eduvpn.app.utils.ErrorDialog
import nl.eduvpn.app.utils.hideKeyboard
import nl.eduvpn.app.viewmodel.AddServerViewModel
import nl.eduvpn.app.viewmodel.BaseConnectionViewModel


/**
 * The fragment where the user can add a server using a custom URL.
 * Created by Daniel Zolnai on 2020-08-23.
 */
class AddServerFragment : BaseFragment<FragmentAddServerBinding>() {

    override val layout = R.layout.fragment_add_server

    private val viewModel by viewModels<AddServerViewModel> { viewModelFactory }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        EduVPNApplication.get(view.context).component().inject(this)
        binding.viewModel = viewModel
        binding.addServerButton.setOnClickListener {
            binding.serverUrl.hideKeyboard()
            addServer()
        }
        binding.serverUrl.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                addServer()
                true
            } else {
                false
            }
        }
        viewModel.parentAction.observe(viewLifecycleOwner) { parentAction ->
            when (parentAction) {
                is BaseConnectionViewModel.ParentAction.ConnectWithConfig -> {
                    viewModel.connectionToConfig(requireActivity(), parentAction.vpnConfig)
                    (activity as? MainActivity)?.openFragment(ConnectionStatusFragment(), false)
                }
                is BaseConnectionViewModel.ParentAction.DisplayError -> {
                    ErrorDialog.show(requireContext(), parentAction.title, parentAction.message)
                }
                else -> {
                    // Do nothing.
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    private fun addServer() {
        val url = binding.serverUrl.text.toString()
        val customUrl = if (url.startsWith("http://") || url.startsWith("https://")) {
            url
        } else {
            "https://${url}"
        }
        val customInstance = Instance(
            customUrl,
            TranslatableString(getString(R.string.custom_provider_display_name)),
            TranslatableString(),
            null,
            AuthorizationType.Local,
            null,
            true,
            null,
            emptyList()
        )
        viewModel.discoverApi(customInstance)
    }
}
