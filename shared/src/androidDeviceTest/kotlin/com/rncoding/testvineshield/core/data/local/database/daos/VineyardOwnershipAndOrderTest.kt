package com.rncoding.testvineshield.core.data.local.database.daos

import androidx.room.Room
import androidx.room.useWriterConnection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rncoding.testvineshield.core.data.local.database.VineshieldDatabase
import com.rncoding.testvineshield.core.data.local.database.buildVineshieldDatabase
import com.rncoding.testvineshield.core.data.local.database.entities.UserEntity
import com.rncoding.testvineshield.core.data.local.database.entities.VineyardEntity
import com.rncoding.testvineshield.core.domain.error.DatabaseError
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.repository.VineyardRepository
import com.rncoding.testvineshield.di.commonModule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import java.util.UUID
import kotlin.test.*

@RunWith(AndroidJUnit4::class)
class VineyardOwnershipAndOrderTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val filename = "r03a-vineyard-test-${UUID.randomUUID()}.db"
    private lateinit var database: VineshieldDatabase
    private var owner = 0L
    private var otherOwner = 0L
    private var first = 0L
    private var second = 0L
    private var foreign = 0L
    private val date = LocalDate(2026, 11, 4)

    private fun open(): VineshieldDatabase = buildVineshieldDatabase(
        Room.databaseBuilder<VineshieldDatabase>(context, filename)
    )
    private fun vineyard(userId: Long, name: String, order: Int) = VineyardEntity(
        userId = userId, name = name, size = 1.5, country = "Bulgaria", city = "Plovdiv",
        latitude = 42.1, longitude = 24.7, timeZone = "Europe/Sofia", elevation = 200.0,
        sortOrder = order, createdAt = date, updatedAt = date
    )

    @Before fun setUp(): Unit = runBlocking {
        database = open()
        owner = database.userDao().insertUser(UserEntity(email = "a@example.test", passwordHash = "fixture", passwordSalt = "fixture"))
        otherOwner = database.userDao().insertUser(UserEntity(email = "b@example.test", passwordHash = "fixture", passwordSalt = "fixture"))
        first = database.vineyardDao().insertVineyard(vineyard(owner, "First", 0))
        second = database.vineyardDao().insertVineyard(vineyard(owner, "Second", 1))
        foreign = database.vineyardDao().insertVineyard(vineyard(otherOwner, "Other owner", 0))
    }

    @After fun tearDown() {
        if (::database.isInitialized) database.close()
        context.deleteDatabase(filename)
    }

    private suspend fun order() = database.vineyardDao().observeVineyardsForUser(owner).first()
        .map { it.vineyardId to it.sortOrder }

    @Test fun readsDoNotRevealAnotherOwnersVineyard(): Unit = runBlocking {
        val dao = database.vineyardDao()
        assertNull(dao.getVineyard(owner, foreign))
        assertNull(dao.getVineyard(owner, Long.MAX_VALUE))
        assertNull(dao.observeVineyard(owner, foreign).first())
        assertEquals(listOf(first, second), dao.getVineyardIdsForUser(owner))
        assertEquals(listOf(foreign), dao.getVineyardIdsForUser(otherOwner))
    }

    @Test fun missingAndForeignLookupHaveTheSameDomainError(): Unit = runBlocking {
        val application = koinApplication {
            modules(commonModule, module { single<VineshieldDatabase> { database } })
        }
        try {
            val repository = application.koin.get<VineyardRepository>()
            for (id in listOf(foreign, Long.MAX_VALUE)) {
                val error = assertIs<Result.Error<*>>(repository.getVineyard(owner, id))
                assertEquals(DatabaseError.NotFound("Vineyard"), error.error)
            }
        } finally { application.close() }
    }

    @Test fun foreignUpdateAndDeleteAffectNoRows(): Unit = runBlocking {
        val dao = database.vineyardDao()
        val original = assertNotNull(dao.getVineyard(otherOwner, foreign))
        assertEquals(0, dao.updateVineyard(
            vineyardId = foreign, userId = owner, name = "Intrusion", size = original.size,
            country = original.country, city = original.city, latitude = original.latitude,
            longitude = original.longitude, timeZone = original.timeZone, elevation = original.elevation,
            updatedAt = date
        ))
        assertEquals(0, dao.deleteVineyard(owner, foreign))
        assertEquals(original, dao.getVineyard(otherOwner, foreign))
    }

    @Test fun completeOwnedReorderPersistsAndLeavesOtherOwnerUnchanged(): Unit = runBlocking {
        database.vineyardDao().reorderVineyards(owner, listOf(second, first))
        assertEquals(listOf(second to 0, first to 1), order())
        assertEquals(0, assertNotNull(database.vineyardDao().getVineyard(otherOwner, foreign)).sortOrder)
    }

    @Test fun duplicateMissingForeignAndStaleListsChangeNothing(): Unit = runBlocking {
        val before = order()
        val invalid = listOf(
            listOf(first, first), listOf(first), listOf(first, foreign),
            listOf(first, Long.MAX_VALUE), listOf(second, first, foreign), emptyList(), listOf(0L, first)
        )
        for (ids in invalid) {
            assertFailsWith<IllegalArgumentException> { database.vineyardDao().reorderVineyards(owner, ids) }
            assertEquals(before, order())
        }
        val third = database.vineyardDao().insertVineyard(vineyard(owner, "Newly created", 2))
        assertFailsWith<IllegalArgumentException> { database.vineyardDao().reorderVineyards(owner, listOf(second, first)) }
        assertEquals(listOf(first to 0, second to 1, third to 2), order())
    }

    @Test fun anEmptyOwnerCanSubmitAnEmptyOrder(): Unit = runBlocking {
        val emptyOwner = database.userDao().insertUser(UserEntity(email = "empty@example.test", passwordHash = "fixture", passwordSalt = "fixture"))
        database.vineyardDao().reorderVineyards(emptyOwner, emptyList())
        assertEquals(listOf(first to 0, second to 1), order())
    }

    @Test fun failureOnSecondUpdateRollsBackFirstUpdate(): Unit = runBlocking {
        val before = order()
        // The first requested row changes successfully; a real SQLite trigger aborts the second.
        database.useWriterConnection { connection ->
            connection.usePrepared("""
                CREATE TRIGGER r03a_abort_order BEFORE UPDATE OF sort_order ON vineyard
                WHEN NEW.vineyard_id = $first
                BEGIN SELECT RAISE(ABORT, 'injected test failure'); END
            """.trimIndent()) { it.step() }
        }
        var failure: Exception? = null
        try { database.vineyardDao().reorderVineyards(owner, listOf(second, first)) }
        catch (exception: Exception) { failure = exception }
        assertNotNull(failure, "The injected database failure must propagate.")
        assertEquals(before, order(), "No sort-order value may survive a failed transaction.")
    }

    @Test fun closeAndReopenPreservesRowsAndOrderUsingProductionBuilder(): Unit = runBlocking {
        val original = assertNotNull(database.vineyardDao().getVineyard(owner, first))
        database.vineyardDao().reorderVineyards(owner, listOf(second, first))
        database.close()
        database = open()
        assertEquals(original.copy(sortOrder = 1), database.vineyardDao().getVineyard(owner, first))
        assertEquals(listOf(second to 0, first to 1), order())
        assertNotNull(database.vineyardDao().getVineyard(otherOwner, foreign))
    }
}
