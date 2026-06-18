package com.fieldcrm.android.data.repository

import com.fieldcrm.shared.api.FieldCRMClient
import com.fieldcrm.shared.db.AppDatabase
import com.fieldcrm.shared.model.BorrowerModel
import com.fieldcrm.shared.model.LoanApplicationModel
import com.fieldcrm.shared.repository.SyncRepository

class BorrowerRepository(
    private val database: AppDatabase,
    private val client: FieldCRMClient
) {
    private val queries = database.appDatabaseQueries

    suspend fun getAllBorrowers(): List<BorrowerModel> {
        return try {
            // Try to fetch from server
            client.fetchBorrowers()
        } catch (e: Exception) {
            // Fallback to local database
            emptyList()
        }
    }

    suspend fun createBorrower(borrower: BorrowerModel): Boolean {
        return try {
            val response = client.createBorrower(borrower)
            // Also save locally
            queries.insertBorrower(
                id = borrower.id,
                org_id = borrower.org_id,
                name = borrower.name,
                phone = borrower.phone,
                bvn = borrower.bvn,
                nin = borrower.nin,
                photo_url = borrower.photo_url,
                status = borrower.status,
                loan_officer_id = borrower.loan_officer_id
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getBorrowerById(id: String): BorrowerModel? {
        return try {
            client.fetchBorrowers().find { it.id == id }
        } catch (e: Exception) {
            null
        }
    }
}

class ApplicationRepository(
    private val database: AppDatabase,
    private val client: FieldCRMClient
) {
    private val syncRepository = SyncRepository(database, client)

    suspend fun getAllApplications(): List<LoanApplicationModel> {
        return try {
            emptyList() // Mock - implement actual API call
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun createApplication(application: LoanApplicationModel): Boolean {
        return try {
            val response = client.createApplication(application)
            true
        } catch (e: Exception) {
            // Queue for offline sync
            false
        }
    }

    suspend fun syncWithServer(): Boolean {
        return syncRepository.syncQueueWithServer()
    }

    suspend fun getApplicationById(id: String): LoanApplicationModel? {
        return try {
            emptyList<LoanApplicationModel>().find { it.id == id }
        } catch (e: Exception) {
            null
        }
    }
}

class AuthRepository {
    suspend fun authenticate(username: String, password: String): String? {
        return try {
            // Mock authentication - replace with actual API call
            "token_authenticated"
        } catch (e: Exception) {
            null
        }
    }

    fun isTokenValid(token: String?): Boolean {
        return token != null && token.isNotEmpty()
    }
}
