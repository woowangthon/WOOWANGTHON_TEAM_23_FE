package com.woowang.scrollmate.data

import com.woowang.scrollmate.net.ApiClient
import com.woowang.scrollmate.net.ApiException
import com.woowang.scrollmate.net.UploadFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private const val HTTP_NOT_FOUND = 404

// 문제 생성 진행. 끝난 조각 수와 전체 조각 수
data class MaterialProgress(val done: Int, val total: Int)

// 자료 업로드와 서버 동기화. 과목 연결은 기기에만 있으므로 서버에서 받은 자료에도 기존 연결을 유지함
class MaterialRepository(
    private val api: ApiClient,
    private val materials: MaterialDao,
    private val questions: QuestionDao,
    private val syncState: SyncState,
) {
    // 서버가 알려 준 문제 생성 진행(끝난 조각 / 전체 조각). Room 스키마를 바꾸지 않으려고 저장하지 않고,
    // 새로고침할 때마다 메모리에 담음. 앱을 다시 켜면 첫 동기화 때 채워짐
    private val _progress = MutableStateFlow<Map<String, MaterialProgress>>(emptyMap())
    val progress: StateFlow<Map<String, MaterialProgress>> = _progress

    fun observeMaterials(): Flow<List<Material>> = materials.observeAll()

    // 한 번에 올리는 파일은 모두 같은 과목에 넣으므로 응답의 자료 순서와 상관없이 연결할 수 있음
    suspend fun upload(files: List<UploadFile>, subjectId: Long?) {
        val accepted = api.uploadMaterials(files)
        materials.upsertAll(accepted.map { it.toEntity(subjectId) })
    }

    suspend fun refreshMaterials() {
        val remote = api.listMaterials()
        _progress.value = remote.associate { it.id to MaterialProgress(done = it.chunksDone, total = it.chunksTotal) }
        val localById = materials.getAll().associateBy { it.id }
        materials.upsertAll(remote.map { it.toEntity(localById[it.id]?.subjectId) })

        // 서버에서 사라진 자료는 기기에서도 지움
        val remoteIds = remote.map { it.id }.toSet()
        localById.keys.filterNot { it in remoteIds }.forEach { materials.delete(it) }
    }

    suspend fun syncQuestions() {
        // 문제는 자료에 연결되므로 자료 목록을 먼저 최신으로 맞춤
        refreshMaterials()
        val response = api.getQuestions(since = syncState.lastQuestionSync)
        val knownMaterials = materials.getAll().map { it.id }.toSet()
        val (storable, orphans) = response.questions
            .mapNotNull { it.toEntityOrNull() }
            .partition { it.materialId in knownMaterials }
        questions.upsertAll(storable)

        // 자료가 아직 기기에 없어서 담지 못한 문제가 있으면 동기화 시각을 올리지 않음.
        // 올리면 다음 since 조회에 그 문제가 다시 오지 않아 영영 잃게 됨
        if (orphans.isEmpty()) syncState.lastQuestionSync = response.serverTime
    }

    // 서버가 아직 문제를 만드는 중인 자료가 있는지. 있으면 문제가 더 생기므로 다시 받아야 함
    suspend fun hasProcessingMaterials(): Boolean =
        materials.getAll().any { it.status == MaterialStatus.PROCESSING }

    // 서버에서 지운 뒤 기기에서도 지움. 서버에 이미 없는 자료(404)는 기기에만 남은 것이므로 기기에서 지움.
    // 그 밖의 오류는 기기에 그대로 두고 던짐
    suspend fun delete(materialId: String) {
        try {
            api.deleteMaterial(materialId)
        } catch (e: ApiException) {
            if (e.httpStatus != HTTP_NOT_FOUND) throw e
        }
        materials.delete(materialId)
    }
}
