package com.woowang.scrollmate.sync

import com.woowang.scrollmate.data.Material
import com.woowang.scrollmate.data.MaterialDao
import com.woowang.scrollmate.data.MaterialRepository
import com.woowang.scrollmate.data.Question
import com.woowang.scrollmate.data.QuestionDao
import com.woowang.scrollmate.data.SyncState
import com.woowang.scrollmate.net.ApiClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

private class MemoryMaterials : MaterialDao {
    val rows = linkedMapOf<String, Material>()

    override fun observeAll(): Flow<List<Material>> = flowOf(rows.values.toList())
    override suspend fun getAll(): List<Material> = rows.values.toList()
    override suspend fun upsertAll(materials: List<Material>) {
        materials.forEach { rows[it.id] = it }
    }
    override suspend fun assignSubject(materialId: String, subjectId: Long?) = Unit
    override suspend fun delete(materialId: String) {
        rows.remove(materialId)
    }
}

private class MemoryQuestions : QuestionDao {
    val rows = linkedMapOf<String, Question>()

    override suspend fun upsertAll(questions: List<Question>) {
        questions.forEach { rows[it.id] = it }
    }
    override suspend fun subjectIdsWithQuestions(): List<Long?> = emptyList()
    override suspend fun questionsOfSubject(subjectId: Long?): List<Question> = emptyList()
}

private class MemorySync : SyncState {
    override var lastQuestionSync: String? = null
}

class SyncExecutorTest {
    private lateinit var server: MockWebServer
    private lateinit var questions: MemoryQuestions
    private lateinit var syncState: MemorySync
    private lateinit var executor: SyncExecutor

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        questions = MemoryQuestions()
        syncState = MemorySync()
        val api = ApiClient(server.url("/").toString(), deviceId = { "device-test-id" })
        executor = SyncExecutor(MaterialRepository(api, MemoryMaterials(), questions, syncState))
    }

    @After
    fun tearDown() {
        runCatching { server.shutdown() }
    }

    private fun fixture(name: String) =
        javaClass.getResource("/fixtures/$name")!!.readText(Charsets.UTF_8)

    private fun respond(code: Int, body: String = "") {
        server.enqueue(MockResponse().setResponseCode(code).setBody(body))
    }

    @Test
    fun syncStoresNewQuestions() = runBlocking {
        respond(200, fixture("materials_list.json"))
        respond(200, fixture("questions.json"))

        assertEquals(SyncOutcome.Success, executor.run())
        assertEquals(setOf("q_001", "q_002"), questions.rows.keys)
    }

    @Test
    fun syncRemembersServerTimeForNextRequest() = runBlocking {
        respond(200, fixture("materials_list.json"))
        respond(200, fixture("questions.json"))

        executor.run()

        assertEquals("2026-11-03T21:30:00+09:00", syncState.lastQuestionSync)
    }

    @Test
    fun serverErrorIsRetried() = runBlocking {
        respond(502, fixture("error_llm_unavailable.json"))

        assertEquals(SyncOutcome.Retry, executor.run())
        assertEquals(emptySet<String>(), questions.rows.keys)
    }

    @Test
    fun rateLimitIsRetried() = runBlocking {
        respond(429, fixture("error_rate_limited.json"))

        assertEquals(SyncOutcome.Retry, executor.run())
    }

    @Test
    fun serverDownIsRetried() = runBlocking {
        server.shutdown()

        assertEquals(SyncOutcome.Retry, executor.run())
    }

    private fun materialsJson(status: String) = """
        {"materials":[{"id":"m_001","name":"a.pdf","status":"$status","chunksTotal":2,"chunksDone":1,"questionCount":1}]}
    """.trimIndent()

    private fun questionsJson(id: String) = """
        {"questions":[{"id":"$id","materialId":"m_001","type":"ox","question":"문장","answer":"O","accepted":[],"evidence":"근거"}],
         "serverTime":"2026-11-09T12:00:00+09:00"}
    """.trimIndent()

    @Test
    fun processingMaterialsAreFetchedAgainUntilReady() = runBlocking {
        respond(200, materialsJson("processing"))
        respond(200, questionsJson("q_001"))
        respond(200, materialsJson("ready"))
        respond(200, questionsJson("q_002"))

        val outcome = executor.runWhileProcessing(maxPolls = 5, waitMillis = 0)

        assertEquals(SyncOutcome.Success, outcome)
        assertEquals(setOf("q_001", "q_002"), questions.rows.keys)
        assertEquals(4, server.requestCount)
    }

    @Test
    fun readyMaterialsAreFetchedOnlyOnce() = runBlocking {
        respond(200, materialsJson("ready"))
        respond(200, questionsJson("q_001"))

        executor.runWhileProcessing(maxPolls = 5, waitMillis = 0)

        assertEquals(2, server.requestCount)
    }

    @Test
    fun pollingStopsAtMaxPollsWhenServerKeepsProcessing() = runBlocking {
        repeat(3) {
            respond(200, materialsJson("processing"))
            respond(200, questionsJson("q_001"))
        }

        val outcome = executor.runWhileProcessing(maxPolls = 2, waitMillis = 0)

        assertEquals(SyncOutcome.Success, outcome)
        assertEquals(6, server.requestCount)
    }

    @Test
    fun pollingReturnsTheFailureWhenARepeatFails() = runBlocking {
        respond(200, materialsJson("processing"))
        respond(200, questionsJson("q_001"))
        respond(502, fixture("error_llm_unavailable.json"))

        val outcome = executor.runWhileProcessing(maxPolls = 5, waitMillis = 0)

        assertEquals(SyncOutcome.Retry, outcome)
    }

    @Test
    fun clientErrorFailsWithoutRetry() = runBlocking {
        respond(400, """{"error":{"code":"invalid_request","message":"잘못된 요청"}}""")

        assertEquals(SyncOutcome.Failed("invalid_request"), executor.run())
    }
}
