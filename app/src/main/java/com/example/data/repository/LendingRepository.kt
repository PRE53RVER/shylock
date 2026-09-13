package com.example.data.repository

import com.example.data.db.LendingDao
import com.example.data.model.LendingContact
import com.example.data.model.LendingEntry
import kotlinx.coroutines.flow.Flow

class LendingRepository(
    private val lendingDao: LendingDao
) {
    val allContacts: Flow<List<LendingContact>> = lendingDao.getAllContacts()
    val allEntries: Flow<List<LendingEntry>> = lendingDao.getAllEntries()

    fun getEntriesForContact(contactId: Int): Flow<List<LendingEntry>> {
        return lendingDao.getEntriesForContact(contactId)
    }

    suspend fun getEntriesListForContact(contactId: Int): List<LendingEntry> {
        return lendingDao.getEntriesListForContact(contactId)
    }

    suspend fun getAllEntriesList(): List<LendingEntry> {
        return lendingDao.getAllEntriesList()
    }

    suspend fun getContactById(id: Int): LendingContact? {
        return lendingDao.getContactById(id)
    }

    suspend fun insertContact(contact: LendingContact): Long {
        return lendingDao.insertContact(contact)
    }

    suspend fun updateContact(contact: LendingContact) {
        lendingDao.updateContact(contact)
    }

    suspend fun deleteContact(contact: LendingContact) {
        // Cascade delete all lending entries for this contact
        lendingDao.deleteEntriesForContact(contact.id)
        lendingDao.deleteContact(contact)
    }

    suspend fun insertEntry(entry: LendingEntry): Long {
        return lendingDao.insertEntry(entry)
    }

    suspend fun updateEntry(entry: LendingEntry) {
        lendingDao.updateEntry(entry)
    }

    suspend fun deleteEntry(entry: LendingEntry) {
        lendingDao.deleteEntry(entry)
    }

    suspend fun deleteEntryById(id: Int) {
        lendingDao.deleteEntryById(id)
    }
}
