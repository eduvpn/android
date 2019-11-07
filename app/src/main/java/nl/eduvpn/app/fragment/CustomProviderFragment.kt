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

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import nl.eduvpn.app.EduVPNApplication
import nl.eduvpn.app.MainActivity
import nl.eduvpn.app.R
import nl.eduvpn.app.base.BaseFragment
import nl.eduvpn.app.databinding.FragmentCustomProviderBinding
import nl.eduvpn.app.entity.AuthorizationType
import nl.eduvpn.app.entity.Instance
import nl.eduvpn.app.utils.ErrorDialog
import nl.eduvpn.app.viewmodel.ConnectionViewModel

/**
 * Fragment where you can give the URL to a custom provider.
 * Created by Daniel Zolnai on 2016-10-11.
 */
class CustomProviderFragment : BaseFragment<FragmentCustomProviderBinding>() {

    override val layout = R.layout.fragment_custom_provider

    private val viewModel by lazy {
        ViewModelProviders.of(requireActivity(), viewModelFactory).get(ConnectionViewModel::class.java)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        EduVPNApplication.get(view.context).component().inject(this)
        binding.viewModel = viewModel
        binding.customProviderConnect.setOnClickListener { onConnectClicked() }

        // Put the cursor in the field and show the keyboard automatically.
        binding.customProviderUrl.requestFocus()
        val inputMethodManager = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.showSoftInput(binding.customProviderUrl, InputMethodManager.SHOW_IMPLICIT)

        viewModel.parentAction.observe(this, Observer { parentAction ->
            when (parentAction) {
                is ConnectionViewModel.ParentAction.InitiateConnection -> {
                    activity?.let { activity ->
                        if (!activity.isFinishing) {
                            viewModel.initiateConnection(activity, parentAction.instance, parentAction.discoveredAPI)
                        }
                    }
                }
                is ConnectionViewModel.ParentAction.DisplayError -> {
                    ErrorDialog.show(requireContext(), parentAction.title, parentAction.message)
                }
            }
        })

    }

    private fun onConnectClicked() {
        val prefix = getString(R.string.custom_provider_prefix)
        val postfix = binding.customProviderUrl.text.toString()
        val url = prefix + postfix
        val customProviderInstance = createCustomProviderInstance(url)
        viewModel.discoverApi(customProviderInstance)
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    /**
     * Creates a custom provider instance for caching.
     *
     * @param baseUri The base URI of the instance.
     * @return A new instance.
     */
    private fun createCustomProviderInstance(baseUri: String): Instance {
        return Instance(baseUri, getString(R.string.custom_provider_display_name), null, AuthorizationType.Local, true)
    }
}
