package com.tripfair.app.ui.screens.sos

import com.tripfair.app.R
import com.tripfair.app.data.contacts.ContactInput
import com.tripfair.app.data.contacts.EmergencyContact
import com.tripfair.app.data.contacts.EmergencyContactsRepository
import com.tripfair.app.data.network.ApiError
import com.tripfair.app.data.network.ApiResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class FakeContacts : EmergencyContactsRepository {
    override val contacts = MutableStateFlow(
        listOf(
            EmergencyContact(1, "Mẹ", phone = "+84905128846", receivesSos = true),
            EmergencyContact(2, "Dũng", phone = "+84986221035", receivesSos = false),
        ),
    )
    var refusal: String? = null
    val savedReceivers = mutableListOf<Map<Int, Boolean>>()

    private fun result(): ApiResult<Unit> = refusal?.let { ApiResult.Failure(ApiError(it)) } ?: ApiResult.Success(Unit)

    override suspend fun sync() = result()
    override suspend fun refresh() = ApiResult.Success(Unit)

    override suspend fun add(input: ContactInput): ApiResult<EmergencyContact> =
        refusal?.let { ApiResult.Failure(ApiError(it)) } ?: ApiResult.Success(EmergencyContact(3, input.name, phone = "+84905128843"))

    override suspend fun update(id: Int, input: ContactInput): ApiResult<EmergencyContact> = add(input)

    override suspend fun setReceivers(receivesSos: Map<Int, Boolean>): ApiResult<Unit> {
        savedReceivers += receivesSos
        return result()
    }

    override suspend fun delete(id: Int): ApiResult<Unit> {
        if (refusal == null) contacts.value = contacts.value.filterNot { it.id == id }
        return result()
    }

    override suspend fun clearLocal() = Unit
}

@OptIn(ExperimentalCoroutinesApi::class)
class EmergencyContactsViewModelTest {
    private val repository = FakeContacts()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `ticking changes the screen only until saved`() {
        val viewModel = EmergencyContactsViewModel(repository)

        viewModel.toggle(2)

        assertTrue(viewModel.contacts.first { it.id == 2 }.receivesSos)
        assertFalse(repository.contacts.value.first { it.id == 2 }.receivesSos)

        viewModel.save()

        assertEquals(listOf(mapOf(2 to true)), repository.savedReceivers)
        assertTrue(viewModel.done)
    }

    @Test
    fun `a failed save stays on the screen with the reason`() {
        repository.refusal = ApiError.NETWORK
        val viewModel = EmergencyContactsViewModel(repository)
        viewModel.selectAll()

        viewModel.save()

        assertFalse(viewModel.done)
        assertEquals(R.string.error_network, viewModel.error)
    }

    @Test
    fun `deleting a contact removes it and closes the form`() {
        val viewModel = ContactFormViewModel(repository)

        viewModel.delete(2)

        assertEquals(listOf(1), repository.contacts.value.map { it.id })
        assertTrue(viewModel.done)
    }

    @Test
    fun `the form puts number problems under the phone field`() {
        val viewModel = ContactFormViewModel(repository)
        val input = ContactInput("Lan", "Gia đình", "0905 128 846", receivesSos = true)

        repository.refusal = "CONTACT_EXISTS"
        viewModel.save(contactId = null, input)
        assertEquals(R.string.contacts_error_exists, viewModel.phoneError)

        repository.refusal = "CONTACT_LIMIT_REACHED"
        viewModel.save(contactId = null, input)
        assertNull(viewModel.phoneError)
        assertEquals(R.string.contacts_error_limit, viewModel.error)

        repository.refusal = null
        viewModel.save(contactId = 1, input)
        assertTrue(viewModel.done)
    }
}
