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
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import nl.eduvpn.app.MainActivity;
import nl.eduvpn.app.R;
import nl.eduvpn.app.entity.AuthorizationType;

/**
 * Fragment where the user can select the VPN type he wants to connect to.
 * Created by Daniel Zolnai on 2017-07-31.
 */
public class TypeSelectorFragment extends Fragment {

    private Unbinder _unbinder;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_type_selector, container, false);
        _unbinder = ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        _unbinder.unbind();
    }

    @OnClick(R.id.vpn_option_container_institute_access)
    protected void _onInstituteAccessClicked() {
        ProviderSelectionFragment providerSelectionFragment = new ProviderSelectionFragment();
        Bundle fragmentParameters = new Bundle();
        fragmentParameters.putInt(ProviderSelectionFragment.EXTRA_AUTHORIZATION_TYPE, AuthorizationType.LOCAL);
        providerSelectionFragment.setArguments(fragmentParameters);
        ((MainActivity)getActivity()).openFragment(providerSelectionFragment, true);
    }

    @OnClick(R.id.vpn_option_container_secure_internet)
    protected void _onSecureInternetClicked() {
        ProviderSelectionFragment providerSelectionFragment = new ProviderSelectionFragment();
        Bundle fragmentParameters = new Bundle();
        fragmentParameters.putInt(ProviderSelectionFragment.EXTRA_AUTHORIZATION_TYPE, AuthorizationType.DISTRIBUTED);
        providerSelectionFragment.setArguments(fragmentParameters);
        ((MainActivity)getActivity()).openFragment(providerSelectionFragment, true);
    }

    @OnClick(R.id.other_address)
    protected void _onOtherAddressClicked() {
        ((MainActivity)getActivity()).openFragment(new CustomProviderFragment(), true);
    }
}
