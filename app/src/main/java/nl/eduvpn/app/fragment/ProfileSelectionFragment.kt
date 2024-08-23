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
import androidx.core.os.BundleCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
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

        val profiles: ArrayList<Profile> =  BundleCompat.getParcelableArrayList(requireArguments(), KEY_PROFILES, Profile::class.java)!!

        profileAdapter.submitList(profiles)
        viewModel.parentAction.observe(viewLifecycleOwner) { parentAction ->
            when (parentAction) {
                is BaseConnectionViewModel.ParentAction.DisplayError -> {
                    ErrorDialog.show(requireActivity(), parentAction.title, parentAction.message)
                }
                is BaseConnectionViewModel.ParentAction.ShowContextCanceledToast -> {
                    Snackbar.make(view, parentAction.message, Snackbar.LENGTH_LONG).show()
                }
                else -> {
                    // Do nothing.
                }
            }
        }

        // Add click listeners
        ItemClickSupport.addTo(binding.profileList).setOnItemClickListener { _, position, _ ->
            val profile = profileAdapter.getItem(position)
            selectProfileToConnectTo(profile)
        }

        if (profiles.size == 1) {
            selectProfileToConnectTo(profiles[0])
        }
    }

    private fun selectProfileToConnectTo(profile: Profile) {
        viewModel.viewModelScope.launch {
            viewModel.selectProfileToConnectTo(profile, preferTcp = false).onFailure { thr ->
                withContext(Dispatchers.Main) {
                    ErrorDialog.show(requireActivity(), thr)
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
