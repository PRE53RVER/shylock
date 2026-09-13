package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.model.LendingContact
import com.example.data.model.LendingEntry
import com.example.data.model.Transaction
import com.example.data.model.displayName
import com.example.data.repository.CategoryRepository
import com.example.data.repository.LendingRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LendingTest {

    private lateinit var db: AppDatabase
    private lateinit var lendingRepository: LendingRepository
    private lateinit var categoryRepository: CategoryRepository

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        lendingRepository = LendingRepository(db.lendingDao())
        categoryRepository = CategoryRepository(
            db.categoryDao(),
            db.subcategoryDao(),
            db.transactionDao()
        )
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testLendingBalanceMath() = runBlocking {
        // Example: lend 500 -> outstanding 500. Lend 100 more -> 600. They repay 50 -> 550.
        val contactId = lendingRepository.insertContact(
            LendingContact(name = "Alice", note = "Roommate", colorHex = "#4D96FF", iconName = "person")
        ).toInt()

        // 1. Lend 500
        lendingRepository.insertEntry(
            LendingEntry(
                contactId = contactId,
                amount = 500.0,
                direction = "LENT",
                description = "Dinner",
                timestamp = 1000L
            )
        )

        var entries = lendingRepository.getEntriesForContact(contactId).first()
        var outstanding = entries.fold(0.0) { acc, e ->
            if (e.direction == "LENT") acc + e.amount else acc - e.amount
        }
        assertEquals(500.0, outstanding, 0.001)

        // 2. Lend 100 more -> 600
        lendingRepository.insertEntry(
            LendingEntry(
                contactId = contactId,
                amount = 100.0,
                direction = "LENT",
                description = "Taxi",
                timestamp = 2000L
            )
        )

        entries = lendingRepository.getEntriesForContact(contactId).first()
        outstanding = entries.fold(0.0) { acc, e ->
            if (e.direction == "LENT") acc + e.amount else acc - e.amount
        }
        assertEquals(600.0, outstanding, 0.001)

        // 3. Repay 50 -> 550
        lendingRepository.insertEntry(
            LendingEntry(
                contactId = contactId,
                amount = 50.0,
                direction = "REPAID",
                description = "Partial UPI transfer",
                timestamp = 3000L
            )
        )

        entries = lendingRepository.getEntriesForContact(contactId).first()
        outstanding = entries.fold(0.0) { acc, e ->
            if (e.direction == "LENT") acc + e.amount else acc - e.amount
        }
        assertEquals(550.0, outstanding, 0.001)

        // 4. Repay 550 -> 0 (fully settled)
        lendingRepository.insertEntry(
            LendingEntry(
                contactId = contactId,
                amount = 550.0,
                direction = "REPAID",
                description = "Final settlement",
                timestamp = 4000L
            )
        )

        entries = lendingRepository.getEntriesForContact(contactId).first()
        outstanding = entries.fold(0.0) { acc, e ->
            if (e.direction == "LENT") acc + e.amount else acc - e.amount
        }
        assertEquals(0.0, outstanding, 0.001)
    }

    @Test
    fun testCascadeDeleteContact() = runBlocking {
        val contactId = lendingRepository.insertContact(
            LendingContact(name = "Bob", note = "Friend")
        ).toInt()

        lendingRepository.insertEntry(
            LendingEntry(
                contactId = contactId,
                amount = 250.0,
                direction = "LENT",
                description = "Movie tickets"
            )
        )

        val contact = lendingRepository.getContactById(contactId)
        assertTrue(contact != null)

        val entriesBefore = lendingRepository.getEntriesForContact(contactId).first()
        assertEquals(1, entriesBefore.size)

        // Deleting contact should cascade delete its entries
        lendingRepository.deleteContact(contact!!)

        val entriesAfter = lendingRepository.getEntriesForContact(contactId).first()
        assertEquals(0, entriesAfter.size)
        val contactAfter = lendingRepository.getContactById(contactId)
        assertTrue(contactAfter == null)
    }

    @Test
    fun testSystemCategoriesCreation() = runBlocking {
        categoryRepository.ensureSystemCategories()

        val lendingCat = categoryRepository.getCategoryByName("Lending")
        assertTrue(lendingCat != null)
        assertEquals("EXPENSE", lendingCat!!.type)
        assertEquals("Lending", lendingCat.displayName)

        val repayCat = categoryRepository.getCategoryByName("Loan Repayment")
        assertTrue(repayCat != null)
        assertEquals("INCOME", repayCat!!.type)
        assertEquals("Loan Pay Back", repayCat.displayName)
    }

    @Test
    fun testLinkedLendingTransactions() = runBlocking {
        categoryRepository.ensureSystemCategories()
        val lendingCat = categoryRepository.getCategoryByName("Lending")!!

        val contactId = lendingRepository.insertContact(
            LendingContact(name = "Charlie", note = "Colleague")
        ).toInt()

        val entryId = lendingRepository.insertEntry(
            LendingEntry(
                contactId = contactId,
                amount = 1200.0,
                direction = "LENT",
                description = "Lunch share",
                timestamp = 5000L
            )
        ).toInt()

        // Insert linked transaction
        val txId = categoryRepository.insertTransaction(
            Transaction(
                categoryId = lendingCat.id,
                subcategoryId = null,
                amount = 1200.0,
                description = "Lent to Charlie - Lunch share",
                timestamp = 5000L,
                type = "EXPENSE",
                lendingEntryId = entryId
            )
        ).toInt()

        val tx = categoryRepository.getTransactionByLendingEntryId(entryId)
        assertTrue(tx != null)
        assertEquals(txId, tx!!.id)
        assertEquals(entryId, tx.lendingEntryId)
        assertEquals("EXPENSE", tx.type)
        assertEquals(1200.0, tx.amount, 0.001)

        // Test delete by lendingEntryId
        categoryRepository.deleteTransactionByLendingEntryId(entryId)
        val txAfter = categoryRepository.getTransactionByLendingEntryId(entryId)
        assertTrue(txAfter == null)
    }
}
