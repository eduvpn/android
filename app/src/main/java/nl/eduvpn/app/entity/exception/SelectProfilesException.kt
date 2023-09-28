package nl.eduvpn.app.entity.exception

import nl.eduvpn.app.entity.Profile


class SelectProfilesException(
    val profiles: List<Profile>
) : Exception()
