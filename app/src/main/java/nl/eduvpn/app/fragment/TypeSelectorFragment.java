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

package nl.eduvpn.app.fragment;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import nl.eduvpn.app.BuildConfig;
import nl.eduvpn.app.MainActivity;
import nl.eduvpn.app.R;
import nl.eduvpn.app.base.BaseFragment;
import nl.eduvpn.app.databinding.FragmentTypeSelectorBinding;
import nl.eduvpn.app.entity.AuthorizationType;

/**
 * Fragment where the user can select the VPN type he wants to connect to.
 * Created by Daniel Zolnai on 2017-07-31.
 */
public class TypeSelectorFragment extends BaseFragment<FragmentTypeSelectorBinding> {

    @Override
    protected int getLayout() {
        return R.layout.fragment_type_selector;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (!BuildConfig.API_DISCOVERY_ENABLED) {
            binding.vpnOptionContainerSecureInternet.setVisibility(View.GONE);
            binding.vpnOptionContainerInstituteAccess.setVisibility(View.GONE);
        }
        binding.vpnOptionContainerSecureInternet.setOnClickListener(v -> _onSecureInternetClicked());
        binding.vpnOptionContainerInstituteAccess.setOnClickListener(v -> _onInstituteAccessClicked());
        binding.otherAddress.setOnClickListener(v -> _onOtherAddressClicked());
    }

    protected void _onInstituteAccessClicked() {
        ((MainActivity)getActivity()).openFragment(ProviderSelectionFragment.Companion.newInstance(AuthorizationType.Local), true);
    }

    protected void _onSecureInternetClicked() {
        ((MainActivity)getActivity()).openFragment(ProviderSelectionFragment.Companion.newInstance(AuthorizationType.Distributed), true);
    }

    protected void _onOtherAddressClicked() {
        ((MainActivity)getActivity()).openFragment(new CustomProviderFragment(), true);
    }
}
