package com.tripfair.app.data.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

/**
 * The file is `files/datastore/session.preferences_pb`; res/xml/backup_rules.xml and
 * data_extraction_rules.xml keep it out of Android backups, since the Keystore key that
 * decrypts it never leaves the device anyway.
 */
val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "session")

private val ACCESS = stringPreferencesKey("access_token")
private val REFRESH = stringPreferencesKey("refresh_token")

/** Saves the tokens encrypted with [cipher]; unreadable values count as signed out. */
class DataStoreSessionStore(
    private val dataStore: DataStore<Preferences>,
    private val cipher: TokenCipher,
) : SessionStore {
    override suspend fun load(): SessionTokens? {
        val prefs = dataStore.data.first()
        val access = prefs[ACCESS]?.let(cipher::decrypt)
        val refresh = prefs[REFRESH]?.let(cipher::decrypt)
        return if (access != null && refresh != null) SessionTokens(access, refresh) else null
    }

    override suspend fun save(tokens: SessionTokens) {
        dataStore.edit {
            it[ACCESS] = cipher.encrypt(tokens.accessToken)
            it[REFRESH] = cipher.encrypt(tokens.refreshToken)
        }
    }

    override suspend fun clear() {
        dataStore.edit { it.clear() }
    }
}
