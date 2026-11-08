package com.tripfair.app.ui.screens.sos

import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tripfair.app.R
import com.tripfair.app.data.contacts.ContactInput
import com.tripfair.app.data.contacts.EmergencyContact
import com.tripfair.app.data.contacts.EmergencyContactsRepository
import com.tripfair.app.data.network.ApiError
import com.tripfair.app.data.network.ApiResult
import kotlinx.coroutines.launch

@StringRes
fun contactErrorMessage(error: ApiError): Int = when (error.code) {
    ApiError.NETWORK -> R.string.error_network
    "CONTACT_LIMIT_REACHED" -> R.string.contacts_error_limit
    "CONTACT_NOT_FOUND" -> R.string.contacts_error_not_found
    else -> R.string.error_unexpected
}

/**
 * "Liên hệ khẩn cấp": ticking a card only changes the choice on screen; "Lưu" sends the
 * changes, and leaving without saving drops them.
 */
class EmergencyContactsViewModel(private val repository: EmergencyContactsRepository) : ViewModel() {
    private var saved by mutableStateOf<List<EmergencyContact>>(emptyList())
    private val choices = mutableStateMapOf<Int, Boolean>()

    var saving by mutableStateOf(false)
        private set

    @get:StringRes
    var error by mutableStateOf<Int?>(null)
        private set

    var done by mutableStateOf(false)
        private set

    /** The saved list with the choices made on this screen. */
    val contacts: List<EmergencyContact>
        get() = saved.map { contact -> choices[contact.id]?.let { contact.copy(receivesSos = it) } ?: contact }

    init {
        viewModelScope.launch { repository.contacts.collect { saved = it } }
        // Someone may have changed the list on another phone.
        viewModelScope.launch { repository.refresh() }
    }

    fun toggle(id: Int) {
        val contact = contacts.firstOrNull { it.id == id } ?: return
        choices[id] = !contact.receivesSos
        error = null
    }

    fun selectAll() {
        saved.forEach { choices[it.id] = true }
        error = null
    }

    fun save() {
        if (saving) return
        viewModelScope.launch {
            saving = true
            when (val result = repository.setReceivers(choices.toMap())) {
                is ApiResult.Success -> done = true
                is ApiResult.Failure -> error = contactErrorMessage(result.error)
            }
            saving = false
        }
    }
}

/** Adds a contact, or edits the one with [contactId]. */
class ContactFormViewModel(private val repository: EmergencyContactsRepository) : ViewModel() {
    var saving by mutableStateOf(false)
        private set

    @get:StringRes
    var phoneError by mutableStateOf<Int?>(null)
        private set

    @get:StringRes
    var error by mutableStateOf<Int?>(null)
        private set

    var done by mutableStateOf(false)
        private set

    fun contact(id: Int?): EmergencyContact? = id?.let { repository.contacts.value.firstOrNull { it.id == id } }

    fun save(contactId: Int?, input: ContactInput) {
        if (saving) return
        viewModelScope.launch {
            saving = true
            phoneError = null
            error = null
            val result = if (contactId == null) repository.add(input) else repository.update(contactId, input)
            when (result) {
                is ApiResult.Success -> done = true
                is ApiResult.Failure -> when (result.error.code) {
                    // The backend checks the number's format; the name only has to be filled in.
                    "VALIDATION_FAILED" -> phoneError = R.string.contacts_error_phone
                    "CONTACT_EXISTS" -> phoneError = R.string.contacts_error_exists
                    else -> error = contactErrorMessage(result.error)
                }
            }
            saving = false
        }
    }
}
