package nl.eduvpn.app.utils

import com.wireguard.android.backend.Tunnel

class WireGuardTunnel(private val name: String, val onStateChangeFunction: (newState: Tunnel.State) -> Unit) : Tunnel {

    override fun getName() = name

    override fun onStateChange(newState: Tunnel.State) = onStateChangeFunction(newState)
}
