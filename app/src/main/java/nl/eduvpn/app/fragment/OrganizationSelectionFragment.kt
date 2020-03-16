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
import android.text.Editable
import android.text.TextWatcher
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
import nl.eduvpn.app.entity.AuthorizationType
import nl.eduvpn.app.service.ConfigurationService
import nl.eduvpn.app.service.OrganizationService
import nl.eduvpn.app.utils.ErrorDialog
import nl.eduvpn.app.utils.ItemClickSupport
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

    private val viewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(OrganizationSelectionViewModel::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        EduVPNApplication.get(view.context).component().inject(this)
        binding.viewModel = viewModel
        binding.organizationList.setHasFixedSize(true)
        binding.organizationList.layoutManager = LinearLayoutManager(view.context, LinearLayoutManager.VERTICAL, false)
        val adapter = OrganizationAdapter(organizationService)
        binding.organizationList.adapter = adapter
        binding.search.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adapter.searchFilter = s?.toString()
            }

            override fun afterTextChanged(s: Editable?) {
                // Unused.
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Unused.
            }
        })
        ItemClickSupport.addTo(binding.organizationList).setOnItemClickListener { _, position, _ ->
            val organization = adapter.getItem(position) ?: return@setOnItemClickListener
            viewModel.selectOrganization(organization)
        }
        dataObserver = object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                when {
                    adapter.itemCount > 0 -> binding.organizationDiscoveryStatus.visibility = View.GONE
                    adapter.isDiscoveryPending -> {
                        binding.organizationDiscoveryStatus.setText(R.string.discovering_organizations)
                        binding.organizationDiscoveryStatus.visibility = View.VISIBLE
                    }
                    else -> {
                        binding.organizationDiscoveryStatus.setText(R.string.no_organization_found)
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

        viewModel.parentAction.observe(viewLifecycleOwner, Observer { parentAction ->
            when (parentAction) {
                is OrganizationSelectionViewModel.ParentAction.OpenProviderSelector -> {
                    (activity as? MainActivity)?.openFragment(ProviderSelectionFragment.newInstance(AuthorizationType.Organization), false)
                }
            }
        })

    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (binding.organizationList.adapter != null && dataObserver != null) {
            binding.organizationList.adapter?.unregisterAdapterDataObserver(dataObserver!!)
            dataObserver = null
        }
    }
}
