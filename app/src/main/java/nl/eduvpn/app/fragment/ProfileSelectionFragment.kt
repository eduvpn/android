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
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.eduvpn.app.EduVPNApplication
import nl.eduvpn.app.R
import nl.eduvpn.app.adapter.ProfileAdapter
import nl.eduvpn.app.base.BaseFragment
import nl.eduvpn.app.databinding.FragmentProfileSelectionBinding
import nl.eduvpn.app.entity.Profile
import nl.eduvpn.app.utils.ErrorDialog
import nl.eduvpn.app.utils.ItemClickSupport
import nl.eduvpn.app.viewmodel.BaseConnectionViewModel
import nl.eduvpn.app.viewmodel.ProfileSelectionViewModel

/**
 * Fragment which is displayed when the app start.
 * Created by Daniel Zolnai on 2016-10-20.
 */
class ProfileSelectionFragment : BaseFragment<FragmentProfileSelectionBinding>() {

    private val viewModel by viewModels<ProfileSelectionViewModel> { viewModelFactory }

    override val layout = R.layout.fragment_profile_selection

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        EduVPNApplication.get(view.context).component().inject(this)

        binding.viewModel = viewModel

        // Basic setup of the lists
        binding.profileList.setHasFixedSize(true)
        binding.profileList.layoutManager =
            LinearLayoutManager(view.context, LinearLayoutManager.VERTICAL, false)

        // Add the adapters
        val profileAdapter = ProfileAdapter(viewModel.getProfileInstance())
        binding.profileList.adapter = profileAdapter

        //todo: fix deprecation when new compat library released https://issuetracker.google.com/issues/242048899
        @Suppress("DEPRECATION") val profiles: ArrayList<Profile> =
            arguments?.getParcelableArrayList(KEY_PROFILES)!!

        profileAdapter.submitList(profiles)

        println("Connected to ${viewModel.parentAction}")
        viewModel.parentAction.observe(viewLifecycleOwner) { parentAction ->
            println("PARENTACTION: $parentAction")
            when (parentAction) {
                is BaseConnectionViewModel.ParentAction.DisplayError -> {
                    ErrorDialog.show(requireContext(), parentAction.title, parentAction.message)
                }
                else -> {
                    // Do nothing.
                }
            }
        }

        // Add click listeners
        ItemClickSupport.addTo(binding.profileList).setOnItemClickListener { _, position, _ ->
            val profile = profileAdapter.getItem(position)
            println("Connect to profile: ${profile.profileId}")
            selectProfileToConnectTo(profile)
        }

        if (profiles.size == 1) {
            selectProfileToConnectTo(profiles[0])
        }
    }

    private fun selectProfileToConnectTo(profile: Profile) {
        viewModel.viewModelScope.launch {
            viewModel.selectProfileToConnectTo(profile).onFailure { thr ->
                withContext(Dispatchers.Main) {
                    ErrorDialog.show(requireContext(), thr)
                }
            }
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
