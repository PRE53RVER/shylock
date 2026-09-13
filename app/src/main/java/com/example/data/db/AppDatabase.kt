package com.example.data.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.Category
import com.example.data.model.DetectedPayment
import com.example.data.model.LendingContact
import com.example.data.model.LendingEntry
import com.example.data.model.Subcategory
import com.example.data.model.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE type = :type ORDER BY name ASC")
    fun getCategoriesByType(type: String): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun getCategoryById(id: Int): Category?

    @Query("SELECT * FROM categories WHERE colorHex = :colorHex LIMIT 1")
    suspend fun getCategoryByColor(colorHex: String): Category?

    @Query("SELECT * FROM categories WHERE name = :name LIMIT 1")
    suspend fun getCategoryByName(name: String): Category?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category): Long

    @Update
    suspend fun updateCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)
}

@Dao
interface SubcategoryDao {
    @Query("SELECT * FROM subcategories ORDER BY name ASC")
    fun getAllSubcategories(): Flow<List<Subcategory>>

    @Query("SELECT * FROM subcategories WHERE parentCategoryId = :parentId ORDER BY name ASC")
    fun getSubcategoriesForCategory(parentId: Int): Flow<List<Subcategory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubcategory(subcategory: Subcategory): Long

    @Update
    suspend fun updateSubcategory(subcategory: Subcategory)

    @Delete
    suspend fun deleteSubcategory(subcategory: Subcategory)

    @Query("DELETE FROM subcategories WHERE parentCategoryId = :parentId")
    suspend fun deleteByParentCategory(parentId: Int)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction): Long

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("DELETE FROM transactions WHERE categoryId = :categoryId")
    suspend fun deleteByCategoryId(categoryId: Int)

    @Query("SELECT * FROM transactions WHERE lendingEntryId = :lendingEntryId LIMIT 1")
    suspend fun getTransactionByLendingEntryId(lendingEntryId: Int): Transaction?

    @Query("DELETE FROM transactions WHERE lendingEntryId = :lendingEntryId")
    suspend fun deleteByLendingEntryId(lendingEntryId: Int)

    @Query("DELETE FROM transactions WHERE lendingEntryId IN (:lendingEntryIds)")
    suspend fun deleteByLendingEntryIds(lendingEntryIds: List<Int>)
}

@Dao
interface LendingDao {
    @Query("SELECT * FROM lending_contacts ORDER BY name ASC")
    fun getAllContacts(): Flow<List<LendingContact>>

    @Query("SELECT * FROM lending_contacts WHERE id = :id LIMIT 1")
    suspend fun getContactById(id: Int): LendingContact?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: LendingContact): Long

    @Update
    suspend fun updateContact(contact: LendingContact)

    @Delete
    suspend fun deleteContact(contact: LendingContact)

    @Query("DELETE FROM lending_entries WHERE contactId = :contactId")
    suspend fun deleteEntriesForContact(contactId: Int)

    @Query("SELECT * FROM lending_entries ORDER BY timestamp DESC")
    fun getAllEntries(): Flow<List<LendingEntry>>

    @Query("SELECT * FROM lending_entries WHERE contactId = :contactId ORDER BY timestamp ASC")
    fun getEntriesForContact(contactId: Int): Flow<List<LendingEntry>>

    @Query("SELECT * FROM lending_entries WHERE contactId = :contactId ORDER BY timestamp ASC")
    suspend fun getEntriesListForContact(contactId: Int): List<LendingEntry>

    @Query("SELECT * FROM lending_entries WHERE id = :id LIMIT 1")
    suspend fun getEntryById(id: Int): LendingEntry?

    @Query("SELECT * FROM lending_entries")
    suspend fun getAllEntriesList(): List<LendingEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: LendingEntry): Long

    @Update
    suspend fun updateEntry(entry: LendingEntry)

    @Delete
    suspend fun deleteEntry(entry: LendingEntry)

    @Query("DELETE FROM lending_entries WHERE id = :id")
    suspend fun deleteEntryById(id: Int)
}

@Dao
interface DetectedPaymentDao {
    @Query("SELECT * FROM detected_payments WHERE status = 'PENDING' ORDER BY timestamp DESC")
    fun getPendingPayments(): Flow<List<DetectedPayment>>

    @Query("SELECT COUNT(*) FROM detected_payments WHERE status = 'PENDING'")
    fun getPendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM detected_payments WHERE status = 'PENDING' AND timestamp >= :since")
    suspend fun countPendingSince(since: Long): Int

    @Query("SELECT * FROM detected_payments WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): DetectedPayment?

    @Query("SELECT * FROM detected_payments WHERE fingerprint = :fingerprint LIMIT 1")
    suspend fun getByFingerprint(fingerprint: String): DetectedPayment?

    // Same amount + direction + counterparty from the same app inside a short window is the
    // same payment re-posted (banks and UPI apps often both notify), regardless of wording
    @Query(
        """
        SELECT COUNT(*) FROM detected_payments
        WHERE amount = :amount AND direction = :direction
          AND timestamp BETWEEN :from AND :to
          AND ((:counterparty IS NULL AND counterparty IS NULL) OR counterparty = :counterparty)
        """
    )
    suspend fun countSimilar(amount: Double, direction: String, counterparty: String?, from: Long, to: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(payment: DetectedPayment): Long

    @Query("UPDATE detected_payments SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Int, status: String)

    @Query("DELETE FROM detected_payments WHERE status != 'PENDING' AND timestamp < :before")
    suspend fun pruneResolvedBefore(before: Long)

    @Query("DELETE FROM detected_payments")
    suspend fun deleteAll()
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE categories ADD COLUMN type TEXT NOT NULL DEFAULT 'EXPENSE'")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `lending_contacts` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `note` TEXT,
                `colorHex` TEXT,
                `iconName` TEXT
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `lending_entries` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `contactId` INTEGER NOT NULL,
                `amount` REAL NOT NULL,
                `direction` TEXT NOT NULL,
                `description` TEXT NOT NULL,
                `timestamp` INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE transactions ADD COLUMN lendingEntryId INTEGER DEFAULT NULL")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `detected_payments` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `amount` REAL NOT NULL,
                `direction` TEXT NOT NULL,
                `counterparty` TEXT,
                `source` TEXT NOT NULL,
                `sourcePackage` TEXT NOT NULL,
                `rawText` TEXT NOT NULL,
                `fingerprint` TEXT NOT NULL,
                `timestamp` INTEGER NOT NULL,
                `status` TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_detected_payments_fingerprint` ON `detected_payments` (`fingerprint`)")
    }
}

@Database(
    entities = [Category::class, Subcategory::class, Transaction::class, LendingContact::class, LendingEntry::class, DetectedPayment::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun subcategoryDao(): SubcategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun lendingDao(): LendingDao
    abstract fun detectedPaymentDao(): DetectedPaymentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "color_manager_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
