package com.woowang.scrollmate.data

import com.woowang.scrollmate.net.ApiClient
import com.woowang.scrollmate.net.ApiException
import com.woowang.scrollmate.net.UploadFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream

private class FakeMaterialDao : MaterialDao {
    val rows = linkedMapOf<String, Material>()

    override fun observeAll(): Flow<List<Material>> = flowOf(rows.values.toList())
    override suspend fun getAll(): List<Material> = rows.values.toList()
    override suspend fun upsertAll(materials: List<Material>) {
        materials.forEach { rows[it.id] = it }
    }
    override suspend fun assignSubject(materialId: String, subjectId: Long?) {
        rows[materialId] = rows.getValue(materialId).copy(subjectId = subjectId)
    }
    override suspend fun delete(materialId: String) {
        rows.remove(materialId)
    }
}

private class FakeQuestionDao : QuestionDao {
    val rows = linkedMapOf<String, Question>()

    override suspend fun upsertAll(questions: List<Question>) {
        questions.forEach { rows[it.id] = it }
    }
    override suspend fun subjectIdsWithQuestions(): List<Long?> = emptyList()
    override suspend fun randomUnanswered(subjectId: Long?): Question? = null
    override suspend fun randomAny(subjectId: Long?): Question? = null
}

private class FakeSyncState : SyncState {
    override var lastQuestionSync: String? = null
}

class MaterialRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var materials: FakeMaterialDao
    private lateinit var questions: FakeQuestionDao
    private lateinit var syncState: FakeSyncState
    private lateinit var repository: MaterialRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        materials = FakeMaterialDao()
        questions = FakeQuestionDao()
        syncState = FakeSyncState()
        val api = ApiClient(server.url("/").toString(), deviceId = { "device-test-id" })
        repository = MaterialRepository(api, materials, questions, syncState)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun fixture(name: String) =
        javaClass.getResource("/fixtures/$name")!!.readText(Charsets.UTF_8)

    private fun respond(code: Int, body: String? = null) {
        server.enqueue(MockResponse().setResponseCode(code).apply { if (body != null) setBody(body) })
    }

    private fun pdf(name: String) = UploadFile(name, "application/pdf") {
        ByteArrayInputStream("%PDF-1.4 sample".toByteArray())
    }

    @Test
    fun uploadStoresAcceptedMaterialsUnderChosenSubject() = runBlocking {
        respond(202, fixture("upload_accepted.json"))

        repository.upload(listOf(pdf("운영체제.pdf"), pdf("데이터베이스.pdf")), subjectId = 7L)

        assertEquals(setOf("m_010", "m_011"), materials.rows.keys)
        assertTrue(materials.rows.values.all { it.subjectId == 7L })
        assertTrue(materials.rows.values.all { it.status == MaterialStatus.PROCESSING })
    }

    @Test
    fun uploadWithoutSubjectKeepsMaterialsUnclassified() = runBlocking {
        respond(202, fixture("upload_accepted.json"))

        repository.upload(listOf(pdf("운영체제.pdf")), subjectId = null)

        assertTrue(materials.rows.values.all { it.subjectId == null })
    }

    @Test
    fun failedUploadStoresNothing() {
        respond(415, fixture("error_unsupported_file.json"))

        try {
            runBlocking { repository.upload(listOf(pdf("scan.pdf")), subjectId = 1L) }
            fail("ApiException expected")
        } catch (e: ApiException) {
            assertEquals("unsupported_file", e.code)
        }
        assertTrue(materials.rows.isEmpty())
    }

    @Test
    fun refreshKeepsLocalSubjectAndUpdatesProgress() = runBlocking {
        materials.rows["m_001"] = Material("m_001", "운영체제.pdf", subjectId = 3L)
        respond(200, fixture("materials_list.json"))

        repository.refreshMaterials()

        val kept = materials.rows.getValue("m_001")
        assertEquals(3L, kept.subjectId)
        assertEquals(8, kept.questionCount)
        assertNull(materials.rows.getValue("m_002").subjectId)
        assertEquals(MaterialStatus.READY, materials.rows.getValue("m_002").status)
    }

    @Test
    fun refreshRemovesMaterialsGoneFromServer() = runBlocking {
        materials.rows["m_gone"] = Material("m_gone", "삭제됨.pdf")
        respond(200, fixture("materials_list.json"))

        repository.refreshMaterials()

        assertTrue("m_gone" !in materials.rows)
    }

    @Test
    fun syncQuestionsStoresQuestionsAndRemembersServerTime() = runBlocking {
        respond(200, fixture("materials_list.json"))
        respond(200, fixture("questions.json"))

        repository.syncQuestions()

        assertEquals(setOf("q_001", "q_002"), questions.rows.keys)
        assertEquals(QuestionType.SHORT, questions.rows.getValue("q_002").type)
        assertEquals(listOf("페이징", "paging"), questions.rows.getValue("q_002").accepted)
        assertEquals("2026-11-03T21:30:00+09:00", syncState.lastQuestionSync)
    }

    @Test
    fun syncQuestionsUsesLastSyncAsSince() = runBlocking {
        syncState.lastQuestionSync = "2026-11-02T10:00:00+09:00"
        respond(200, fixture("materials_list.json"))
        respond(200, fixture("questions.json"))

        repository.syncQuestions()

        server.takeRequest() // 자료 목록
        val path = server.takeRequest().path!!
        assertTrue(path.startsWith("/api/v1/questions?since="))
    }

    @Test
    fun syncQuestionsSkipsUnknownTypesAndOrphans() = runBlocking {
        respond(200, fixture("materials_list.json"))
        respond(
            200,
            """
            {
              "questions": [
                { "id": "q_ok", "materialId": "m_001", "type": "ox", "question": "a", "answer": "O", "evidence": "e" },
                { "id": "q_new_type", "materialId": "m_001", "type": "choice", "question": "b", "answer": "1", "evidence": "e" },
                { "id": "q_orphan", "materialId": "m_unknown", "type": "ox", "question": "c", "answer": "X", "evidence": "e" }
              ],
              "serverTime": "2026-11-03T22:00:00+09:00"
            }
            """.trimIndent(),
        )

        repository.syncQuestions()

        assertEquals(setOf("q_ok"), questions.rows.keys)
    }

    @Test
    fun deleteRemovesLocalAfterServerConfirms() = runBlocking {
        materials.rows["m_001"] = Material("m_001", "운영체제.pdf")
        respond(204)

        repository.delete("m_001")

        assertTrue(materials.rows.isEmpty())
    }

    @Test
    fun failedDeleteKeepsLocalMaterial() {
        materials.rows["m_001"] = Material("m_001", "운영체제.pdf")
        respond(404, """{ "error": { "code": "not_found", "message": "없는 자료" } }""")

        try {
            runBlocking { repository.delete("m_001") }
            fail("ApiException expected")
        } catch (e: ApiException) {
            assertEquals(404, e.httpStatus)
        }
        assertTrue("m_001" in materials.rows)
    }
}
