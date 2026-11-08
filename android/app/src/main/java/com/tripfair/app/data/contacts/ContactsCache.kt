package com.tripfair.app.data.contacts

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** The copy of the contact list that lets SOS work without internet. */
interface ContactsCache {
    suspend fun load(): List<EmergencyContact>

    suspend fun save(contacts: List<EmergencyContact>)

    suspend fun clear()
}

/**
 * `files/datastore/emergency_contacts.preferences_pb`, left out of Android backups like the
 * session: the server keeps the list, and the copy is cleared when the user signs out.
 */
val Context.contactsDataStore: DataStore<Preferences> by preferencesDataStore(name = "emergency_contacts")

private val CONTACTS = stringPreferencesKey("contacts")
private val serializer = ListSerializer(EmergencyContact.serializer())

class DataStoreContactsCache(
    private val dataStore: DataStore<Preferences>,
    private val json: Json,
) : ContactsCache {
    override suspend fun load(): List<EmergencyContact> {
        val saved = dataStore.data.first()[CONTACTS] ?: return emptyList()
        return try {
            json.decodeFromString(serializer, saved)
        } catch (_: SerializationException) {
            // Written by an older app version: the next sync replaces it.
            emptyList()
        }
    }

    override suspend fun save(contacts: List<EmergencyContact>) {
        dataStore.edit { it[CONTACTS] = json.encodeToString(serializer, contacts) }
    }

    override suspend fun clear() {
        dataStore.edit { it.clear() }
    }
}
