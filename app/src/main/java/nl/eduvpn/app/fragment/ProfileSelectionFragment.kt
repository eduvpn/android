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
import nl.eduvpn.app.EduVPNApplication
import nl.eduvpn.app.MainActivity
import nl.eduvpn.app.R
import nl.eduvpn.app.adapter.ProfileAdapter
import nl.eduvpn.app.base.BaseFragment
import nl.eduvpn.app.databinding.FragmentProfileSelectionBinding
import nl.eduvpn.app.entity.Profile
import nl.eduvpn.app.utils.ErrorDialog
import nl.eduvpn.app.utils.ItemClickSupport
import nl.eduvpn.app.viewmodel.BaseConnectionViewModel
import java.util.ArrayList

/**
 * Fragment which is displayed when the app start.
 * Created by Daniel Zolnai on 2016-10-20.
 */
class ProfileSelectionFragment : BaseFragment<FragmentProfileSelectionBinding>() {

    private val viewModel by viewModels<BaseConnectionViewModel> { viewModelFactory }

    override val layout = R.layout.fragment_profile_selection

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        EduVPNApplication.get(view.context).component().inject(this)

        binding.viewModel = viewModel

        // Basic setup of the lists
        binding.profileList.setHasFixedSize(true)
        binding.profileList.layoutManager = LinearLayoutManager(view.context, LinearLayoutManager.VERTICAL, false)

        // Add the adapters
        val profileAdapter = ProfileAdapter(viewModel.getProfileInstance())
        binding.profileList.adapter = profileAdapter

        val profiles: ArrayList<Profile> = arguments?.getParcelableArrayList(KEY_PROFILES)!!

        profileAdapter.submitList(profiles)

        viewModel.parentAction.observe(this, Observer { parentAction ->
            when (parentAction) {
                is BaseConnectionViewModel.ParentAction.InitiateConnection -> {
                    activity?.let { activity ->
                        if (!activity.isFinishing) {
                            viewModel.initiateConnection(activity, parentAction.instance, parentAction.discoveredAPI)
                        }
                    }
                }
                is BaseConnectionViewModel.ParentAction.ConnectWithProfile -> {
                    viewModel.openVpnConnectionToProfile(requireActivity(), parentAction.vpnProfile)
                    (activity as? MainActivity)?.openFragment(ConnectionStatusFragment(), false)
                }
                is BaseConnectionViewModel.ParentAction.DisplayError -> {
                    ErrorDialog.show(requireContext(), parentAction.title, parentAction.message)
                }
            }
        })

        // Add click listeners
        ItemClickSupport.addTo(binding.profileList).setOnItemClickListener { _, position, _ ->
            val profile = profileAdapter.getItem(position)
            viewModel.selectProfileToConnectTo(profile)
        }

        if (profiles.size == 1) {
            viewModel.selectProfileToConnectTo(profiles[0])
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    companion object {

        private const val KEY_PROFILES = "profiles"

        fun newInstance(profileList: List<Profile>): ProfileSelectionFragment {
            val fragment = ProfileSelectionFragment()
            val arguments = Bundle()
            arguments.putParcelableArrayList(KEY_PROFILES, ArrayList(profileList))
            fragment.arguments = arguments
            return fragment
        }
    }
}
