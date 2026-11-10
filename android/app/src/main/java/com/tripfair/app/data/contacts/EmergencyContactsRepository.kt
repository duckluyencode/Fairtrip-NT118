package com.tripfair.app.data.contacts

import com.tripfair.app.data.network.ApiClient
import com.tripfair.app.data.network.ApiResult
import com.tripfair.app.data.network.apiCall
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

/**
 * The emergency contacts of the signed-in user. [contacts] starts from the copy on the
 * phone, so the SOS screen has its recipients offline; each change goes to the server first
 * and then updates the list and the copy.
 */
interface EmergencyContactsRepository {
    val contacts: StateFlow<List<EmergencyContact>>

    /** After sign-in and at app start: the copy first, then the server's list. */
    suspend fun sync(): ApiResult<Unit>

    suspend fun refresh(): ApiResult<Unit>

    suspend fun add(input: ContactInput): ApiResult<EmergencyContact>

    suspend fun update(id: Int, input: ContactInput): ApiResult<EmergencyContact>

    /**
     * Saves who receives the SOS text, one request per changed contact. Stops at the first
     * failure; the contacts saved before it stay saved.
     */
    suspend fun setReceivers(receivesSos: Map<Int, Boolean>): ApiResult<Unit>

    suspend fun delete(id: Int): ApiResult<Unit>

    /** On sign-out: the next account on this phone must not see these contacts. */
    suspend fun clearLocal()
}

class RemoteEmergencyContactsRepository(
    private val api: EmergencyContactsApi,
    private val cache: ContactsCache,
    private val json: Json = ApiClient.json,
) : EmergencyContactsRepository {
    private val _contacts = MutableStateFlow<List<EmergencyContact>>(emptyList())
    override val contacts: StateFlow<List<EmergencyContact>> = _contacts.asStateFlow()

    override suspend fun sync(): ApiResult<Unit> {
        if (_contacts.value.isEmpty()) _contacts.value = cache.load()
        return refresh()
    }

    override suspend fun refresh(): ApiResult<Unit> =
        when (val result = apiCall(json) { api.list() }) {
            is ApiResult.Success -> {
                publish(result.value.items)
                ApiResult.Success(Unit)
            }
            is ApiResult.Failure -> result
        }

    override suspend fun add(input: ContactInput): ApiResult<EmergencyContact> =
        apiCall(json) { api.create(input) }.also { result ->
            if (result is ApiResult.Success) publish(_contacts.value + result.value)
        }

    override suspend fun update(id: Int, input: ContactInput): ApiResult<EmergencyContact> {
        val patch = ContactPatch(input.name, input.relation, input.phone, input.receivesSos)
        return apiCall(json) { api.update(id, patch) }.also { result ->
            if (result is ApiResult.Success) replace(result.value)
        }
    }

    override suspend fun setReceivers(receivesSos: Map<Int, Boolean>): ApiResult<Unit> {
        val changed = _contacts.value.filter { contact ->
            receivesSos[contact.id]?.let { it != contact.receivesSos } ?: false
        }
        for (contact in changed) {
            when (val result = apiCall(json) { api.update(contact.id, ContactPatch(receivesSos = !contact.receivesSos)) }) {
                is ApiResult.Success -> replace(result.value)
                is ApiResult.Failure -> return result
            }
        }
        return ApiResult.Success(Unit)
    }

    override suspend fun delete(id: Int): ApiResult<Unit> =
        apiCall(json) { api.delete(id) }.also { result ->
            if (result is ApiResult.Success) publish(_contacts.value.filterNot { it.id == id })
        }

    override suspend fun clearLocal() {
        _contacts.value = emptyList()
        cache.clear()
    }

    private suspend fun replace(contact: EmergencyContact) {
        publish(_contacts.value.map { if (it.id == contact.id) contact else it })
    }

    private suspend fun publish(contacts: List<EmergencyContact>) {
        _contacts.value = contacts
        cache.save(contacts)
    }
}
