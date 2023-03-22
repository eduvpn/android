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
import nl.eduvpn.app.utils.charset
import nl.eduvpn.app.utils.runCatchingCoroutine
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.nio.charset.Charset

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
                    getSignature(serverListUrl)
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
                    getJsonBytes(serverListUrl)
                }.onFailure { it ->
                    Log.w(TAG, "Unable to fetch signature of server list!", it)
                }.getOrThrow()
            }

            val (serverListBytes, serverListCharset) = serverListDeferred.await()
            val signature = signatureDeferred.await()

            try {
                if (!securityService.verifyMinisign(serverListBytes, signature)) {
                    throw InvalidSignatureException("Signature validation failed for server list!")
                }
            } catch (ex: Exception) {
                Log.w(TAG, "Unable to verify signature", ex)
                throw InvalidSignatureException("Signature validation failed for server list!")
            }

            val serverListString = serverListBytes.toString(serverListCharset)
            if (serverListString.isBlank()) {
                Log.w(TAG, "Server list is empty")
                throw IllegalArgumentException("Server list is empty!")
            }
            serializerService.deserializeServerList(serverListString)
        }
    }

    suspend fun fetchOrganizations(): OrganizationList {
        return coroutineScope {
            val listUrl = BuildConfig.ORGANIZATION_LIST_BASE_URL + "organization_list.json"

            val organizationListDeferred = async {
                runCatchingCoroutine {
                    getJsonBytes(listUrl)
                }.onFailure {
                    Log.w(TAG, "Unable to fetch organization list!", it)
                }.getOrThrow()
            }

            val signatureDeferred = async {
                runCatchingCoroutine {
                    getSignature(listUrl)
                }.mapCatching { signature ->
                    if (signature.isBlank()) {
                        throw throw IllegalArgumentException("Signature of organization list is empty!")
                    }
                    signature
                }.onFailure {
                    Log.w(TAG, "Unable to fetch signature of organization list!", it)
                }.getOrThrow()
            }

            val (organizationListBytes, charset) = organizationListDeferred.await()
            val signature = signatureDeferred.await()

            try {
                if (!securityService.verifyMinisign(organizationListBytes, signature)) {
                    throw InvalidSignatureException("Signature validation failed for organization list!")
                }
            } catch (ex: Exception) {
                Log.w(TAG, "Unable to verify signature", ex)
                throw InvalidSignatureException("Signature validation failed for organization list!")
            }

            val organizationListString = organizationListBytes.toString(charset)
            if (organizationListString.isBlank()) {
                Log.w(TAG, "Organization list is empty!")
                throw IllegalArgumentException("Organization list is empty!")
            }
            val organizationListJson = JSONObject(organizationListString)
            serializerService.deserializeOrganizationList(organizationListJson)
        }
    }

    private suspend fun getSignature(signatureRequestUrl: String): String {
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

    private suspend fun getJsonBytes(url: String): Pair<ByteArray, Charset> {
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
            val charset = responseBody.charset()
            val result = withContext(Dispatchers.IO) { responseBody.bytes() }
            responseBody.close()
            return Pair(result, charset)
        } else {
            throw IOException("Response body is empty!")
        }
    }

    class OrganizationDeletedException : IllegalStateException()

    companion object {
        private val TAG = OrganizationService::class.java.name
    }
}
