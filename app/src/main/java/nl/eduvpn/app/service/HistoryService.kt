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
package nl.eduvpn.app.service

import nl.eduvpn.app.entity.AddedServers
import nl.eduvpn.app.entity.CertExpiryTimes
import nl.eduvpn.app.entity.CurrentServer
import nl.eduvpn.app.entity.Instance
import nl.eduvpn.app.entity.exception.CommonException
import nl.eduvpn.app.service.SerializerService.UnknownFormatException
import nl.eduvpn.app.utils.Listener
import nl.eduvpn.app.utils.Log
import java.util.LinkedList
import java.util.function.Consumer

/**
 * Service which stores previously used access token and profile names.
 * This allows us to skip some steps, which will make the user experience more fluid.
 * Created by Daniel Zolnai on 2016-10-20.
 */
class HistoryService(private val backendService: BackendService) {
    var addedServers: AddedServers? = null
        private set

    private val _listeners: MutableList<Listener> = LinkedList()

    /**
     * Loads the state of the service.
     */
    @kotlin.jvm.Throws(Exception::class)
    fun load() {
        try {
            addedServers = backendService.getAddedServers()
            notifyListeners()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    fun addListener(listener: Listener) {
        if (!_listeners.contains(listener)) {
            _listeners.add(listener)
        }
    }

    fun removeListener(listener: Listener) {
        _listeners.remove(listener)
    }

    private fun notifyListeners() {
        _listeners.forEach(Consumer { l: Listener -> l.update(this, null) })
    }


    val currentServer: CurrentServer?
        get() = backendService.getCurrentServer()

    val certExpiryTimes: CertExpiryTimes?
        get() {
            try {
                return backendService.getCertExpiryTimes()
            } catch (ex: Exception) {
                Log.w(TAG, "Could not determine cert expiry times!", ex)
                return null
            }
        }


    fun hasSecureInternetServer(): Boolean {
        return addedServers?.secureInternetServer != null
    }

    /**
     * Removes all saved data for an instance.
     *
     * @param instance The instance to remove the data for.
     */
    @Throws(CommonException::class)
    fun removeAllDataForInstance(instance: Instance) {
        backendService.removeServer(instance)
        load()
        notifyListeners()
    }

    /***
     * Removes all saved data in this app.
     */
    @Throws(CommonException::class, UnknownFormatException::class)
    suspend fun removeOrganizationData() {
        val instancesToRemove = backendService.getAddedServers().asInstances()
        var errorThrown: CommonException? = null
        for (instance in instancesToRemove) {
            try {
                removeAllDataForInstance(instance)
            } catch (ex: CommonException) {
                errorThrown = ex
            }
        }
        if (errorThrown != null) {
            throw errorThrown
        }
    }

    companion object {
        private val TAG: String = HistoryService::class.java.name
    }
}
