package com.fieldcrm.shared.api

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import com.fieldcrm.shared.model.BorrowerModel
import com.fieldcrm.shared.model.LoanApplicationModel

class FieldCRMClient(private val baseUrl: String) {
    private var accessToken: String? = null

    val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }

    fun setToken(token: String) {
        accessToken = token
    }

    private fun HttpRequestBuilder.secureHeaders() {
        accessToken?.let {
            header(HttpHeaders.Authorization, "Bearer $it")
        }
    }

    suspend fun createBorrower(borrower: BorrowerModel): HttpResponse {
        return client.post("$baseUrl/api/v1/borrowers/") {
            contentType(ContentType.Application.Json)
            secureHeaders()
            setBody(borrower)
        }
    }

    suspend fun createApplication(app: LoanApplicationModel): HttpResponse {
        return client.post("$baseUrl/api/v1/applications/") {
            contentType(ContentType.Application.Json)
            secureHeaders()
            setBody(app)
        }
    }

    suspend fun fetchBorrowers(): List<BorrowerModel> {
        val response = client.get("$baseUrl/api/v1/borrowers/") {
            secureHeaders()
        }
        return if (response.status == HttpStatusCode.OK) {
            Json.decodeFromString(response.bodyAsText())
        } else {
            emptyList()
        }
    }

    suspend fun fetchApplications(): List<LoanApplicationModel> {
        val response = client.get("$baseUrl/api/v1/applications/") {
            secureHeaders()
        }
        return if (response.status == HttpStatusCode.OK) {
            Json.decodeFromString(response.bodyAsText())
        } else {
            emptyList()
        }
    }
}
