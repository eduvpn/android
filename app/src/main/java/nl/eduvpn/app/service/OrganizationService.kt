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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import nl.eduvpn.app.BuildConfig
import nl.eduvpn.app.Constants
import nl.eduvpn.app.entity.OrganizationList
import nl.eduvpn.app.entity.ServerList
import nl.eduvpn.app.entity.exception.InvalidSignatureException
import nl.eduvpn.app.utils.Log
import nl.eduvpn.app.utils.await
import nl.eduvpn.app.utils.runCatchingCoroutine
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

/**
 * Service which provides the configurations for organization related data model.
 * Created by Daniel Zolnai on 2016-10-07.
 */
class OrganizationService(private val serializerService: SerializerService,
                          private val securityService: SecurityService,
                          private val okHttpClient: OkHttpClient) {


    suspend fun fetchServerList(): ServerList {
        return coroutineScope {
            val serverListUrl = BuildConfig.ORGANIZATION_LIST_BASE_URL + "server_list.json"

            val signatureDeferred = async {
                runCatchingCoroutine {
                    createSignatureSingle(serverListUrl)
                }.mapCatching { signature ->
                    if (signature.isBlank()) {
                        throw IllegalArgumentException("Signature of server list is empty!")
                    }
                    signature
                }.onFailure { it ->
                    Log.w(TAG, "Unable to fetch server list!", it)
                }.getOrThrow()
            }
            val serverListDeferred = async {
                runCatchingCoroutine {
                    createGetJsonSingle(serverListUrl)
                }.mapCatching { serverList ->
                    if (serverList.isBlank()) {
                        throw IllegalArgumentException("Server list is empty!")
                    }
                    serverList
                }.onFailure { it ->
                    Log.w(TAG, "Unable to fetch signature of server list!", it)
                }.getOrThrow()
            }

            val serverList = serverListDeferred.await()
            val signature = signatureDeferred.await()

            try {
                if (withContext(Dispatchers.Default) { securityService.verifyMinisign(serverList, signature) }) {
                    val organizationListJson = JSONObject(serverList)
                    serializerService.deserializeServerList(organizationListJson)
                } else {
                    throw InvalidSignatureException("Signature validation failed for server list!")
                }
            } catch (ex: Exception) {
                Log.w(TAG, "Unable to verify signature", ex)
                throw InvalidSignatureException("Signature validation failed for server list!")
            }
        }
    }

    suspend fun fetchOrganizations(): OrganizationList {
        return coroutineScope {
            val listUrl = BuildConfig.ORGANIZATION_LIST_BASE_URL + "organization_list.json"

            val organizationListDeferred = async {
                runCatchingCoroutine {
                    createGetJsonSingle(listUrl)
                }.mapCatching { organizationList ->
                    if (organizationList.isBlank()) {
                        throw throw IllegalArgumentException("Organization list is empty!")
                    }
                    organizationList
                }.onFailure {
                    Log.w(TAG, "Unable to fetch organization list!", it)
                }.getOrThrow()
            }

            val signatureDeferred = async {
                runCatchingCoroutine {
                    createSignatureSingle(listUrl)
                }.mapCatching { signature ->
                    if (signature.isBlank()) {
                        throw throw IllegalArgumentException("Signature of organization list is empty!")
                    }
                    signature
                }.onFailure {
                    Log.w(TAG, "Unable to fetch signature of organization list!", it)
                }.getOrThrow()
            }

            val organizationList = organizationListDeferred.await()
            val signature = signatureDeferred.await()

            try {
                if (withContext(Dispatchers.Default) { securityService.verifyMinisign(organizationList, signature) }) {
                    val organizationListJson = JSONObject(organizationList)
                    serializerService.deserializeOrganizationList(organizationListJson)
                } else {
                    throw InvalidSignatureException("Signature validation failed for organization list!")
                }
            } catch (ex: Exception) {
                Log.w(TAG, "Unable to verify signature", ex)
                throw InvalidSignatureException("Signature validation failed for organization list!")
            }
        }
    }

    private suspend fun createSignatureSingle(signatureRequestUrl: String): String {
        val postfixedUrl = signatureRequestUrl + BuildConfig.SIGNATURE_URL_POSTFIX
        val request = Request.Builder().url(postfixedUrl).build()
        val response = okHttpClient.newCall(request).await()
        val responseBody = response.body
        if (responseBody != null) {
            val result = withContext(Dispatchers.IO) { responseBody.string() }
            responseBody.close()
            return result
        } else {
            throw IOException("Response body is empty!")
        }
    }

    private suspend fun createGetJsonSingle(url: String): String {
        val request = Request.Builder().url(url).build()
        val response = okHttpClient.newCall(request).await()
        val responseBody = response.body
        val responseCode = response.code
        var isGone = false
        for (code in Constants.GONE_HTTP_CODES) {
            if (responseCode == code) {
                isGone = true
            }
        }
        if (isGone) {
            throw OrganizationDeletedException()
        } else if (responseBody != null) {
            val result = withContext(Dispatchers.IO) { responseBody.string() }
            responseBody.close()
            return result
        } else {
            throw IOException("Response body is empty!")
        }
    }

    class OrganizationDeletedException : IllegalStateException()

    companion object {
        private val TAG = OrganizationService::class.java.name
    }
}