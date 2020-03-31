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

import io.reactivex.Single
import io.reactivex.SingleSource
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import nl.eduvpn.app.BuildConfig
import nl.eduvpn.app.Constants
import nl.eduvpn.app.entity.Instance
import nl.eduvpn.app.entity.Organization
import nl.eduvpn.app.entity.exception.InvalidSignatureException
import nl.eduvpn.app.utils.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.Callable

/**
 * Service which provides the configurations for organization related data model.
 * Created by Daniel Zolnai on 2016-10-07.
 */
class OrganizationService(private val serializerService: SerializerService,
                          private val securityService: SecurityService,
                          private val okHttpClient: OkHttpClient) {

    fun getInstanceListForOrganization(organization: Organization?): Single<List<Instance>> {
        if (organization == null) {
            return Single.just(emptyList())
        }
        val instanceListUrl = BuildConfig.ORGANIZATION_LIST_BASE_URL + organization.serverList
        return Single.zip(
                createGetJsonSingle(instanceListUrl).onErrorReturnItem(""),
                createSignatureSingle(instanceListUrl).onErrorReturnItem(""), BiFunction<String, String, List<Instance>> { serverInfoList, signature ->
            if (serverInfoList.isEmpty() || signature.isEmpty()) {
                Log.w(TAG, "Either server or signature fetch has failed.")
                throw IllegalArgumentException("Either server or signature fetch has failed.")
            }
            try {
                if (securityService.verifyMinisign(serverInfoList, signature)) {
                    val organizationListJson = JSONObject(serverInfoList)
                    return@BiFunction serializerService.deserializeInstancesFromOrganizationServerList(organizationListJson)
                } else {
                    throw InvalidSignatureException("Signature validation failed for organization instance list!")
                }
            } catch (ex: Exception) {
                throw InvalidSignatureException("Signature validation failed for organization instance list!")
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun fetchOrganizations(): Single<List<Organization>> {
        val listUrl = BuildConfig.ORGANIZATION_LIST_BASE_URL + "organization_list.json"
        val organizationListObservable = createGetJsonSingle(listUrl)
        val signatureObservable = createSignatureSingle(listUrl)
        return Single.zip(organizationListObservable, signatureObservable, BiFunction<String, String, List<Organization>> { organizationList: String, signature: String ->
            try {
                if (securityService.verifyMinisign(organizationList, signature)) {
                    val organizationListJson = JSONObject(organizationList)
                    return@BiFunction serializerService.deserializeOrganizationList(organizationListJson)
                } else {
                    throw InvalidSignatureException("Signature validation failed for organization list!")
                }
            } catch (ex: Exception) {
                throw InvalidSignatureException("Signature validation failed for organization list!")
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    private fun createSignatureSingle(signatureRequestUrl: String): Single<String> {
        return Single.defer(Callable<SingleSource<String>> {
            val postfixedUrl = signatureRequestUrl + BuildConfig.SIGNATURE_URL_POSTFIX
            val request = Request.Builder().url(postfixedUrl).build()
            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body
            if (responseBody != null) {
                val result = responseBody.string()
                responseBody.close()
                return@Callable Single.just(result)
            } else {
                return@Callable Single.error<String>(IOException("Response body is empty!"))
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
    }

    private fun createGetJsonSingle(url: String): Single<String> {
        return Single.defer(Callable<SingleSource<String>> {
            val request = Request.Builder().url(url).build()
            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body
            val responseCode = response.code
            var isGone = false
            for (code in Constants.GONE_HTTP_CODES) {
                if (responseCode == code) {
                    isGone = true
                }
            }
            if (isGone) {
                return@Callable Single.error(OrganizationDeletedException())
            } else if (responseBody != null) {
                val result = responseBody.string()
                responseBody.close()
                return@Callable Single.just(result)
            } else {
                return@Callable Single.error(IOException("Response body is empty!"))
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
    }

    class OrganizationDeletedException : IllegalStateException()

    companion object {
        private val TAG = OrganizationService::class.java.name
    }
}