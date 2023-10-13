package nl.eduvpn.app.utils

import com.wireguard.android.backend.Tunnel

class WireGuardTunnel(
    private val name: String,
    private val onStateChangeFunction: (newState: Tunnel.State) -> Unit
) : Tunnel {

    override fun getName() = name

    var state: Tunnel.State = Tunnel.State.DOWN
    private set

    override fun onStateChange(newState: Tunnel.State) {
        state = newState
        onStateChangeFunction(newState)
    }
}
