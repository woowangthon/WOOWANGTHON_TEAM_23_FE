package com.woowang.scrollmate.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import okio.source
import java.io.IOException
import java.io.InputStream

private const val API_PREFIX = "/api/v1"
private const val HEADER_DEVICE_ID = "X-Device-Id"
private const val FIELD_FILES = "files"
private const val JSON_MEDIA_TYPE = "application/json; charset=utf-8"

val ApiJson = Json { ignoreUnknownKeys = true }

// 올릴 파일 하나. 공유로 받은 content:// 파일도 스트림으로만 다루기 위해 열기 함수를 받음
class UploadFile(
    val name: String,
    val mimeType: String,
    val open: () -> InputStream,
)

// FE ↔ BE 호출. 형식은 docs의 api.md를 따름
class ApiClient(
    baseUrl: String,
    private val deviceId: () -> String,
    private val http: OkHttpClient = OkHttpClient(),
    private val json: Json = ApiJson,
) {
    private val base = baseUrl.trimEnd('/')

    suspend fun uploadMaterials(files: List<UploadFile>): List<MaterialDto> {
        val body = MultipartBody.Builder().setType(MultipartBody.FORM).apply {
            files.forEach { addFormDataPart(FIELD_FILES, it.name, it.asRequestBody()) }
        }.build()
        val request = requestBuilder("$base$API_PREFIX/materials").post(body).build()
        return execute(request) { json.decodeFromString<MaterialsResponse>(it).materials }
    }

    suspend fun listMaterials(): List<MaterialDto> {
        val request = requestBuilder("$base$API_PREFIX/materials").get().build()
        return execute(request) { json.decodeFromString<MaterialsResponse>(it).materials }
    }

    suspend fun deleteMaterial(id: String) {
        val request = requestBuilder("$base$API_PREFIX/materials/$id").delete().build()
        execute(request) { }
    }

    suspend fun getQuestions(since: String? = null): QuestionsResponse {
        val url = "$base$API_PREFIX/questions".toHttpUrl().newBuilder().apply {
            if (since != null) addQueryParameter("since", since)
        }.build()
        val request = requestBuilder(url.toString()).get().build()
        return execute(request) { json.decodeFromString<QuestionsResponse>(it) }
    }

    suspend fun postAnswers(answers: List<AnswerDto>): Int {
        val body = json.encodeToString(AnswersRequest(answers))
            .toRequestBody(JSON_MEDIA_TYPE.toMediaType())
        val request = requestBuilder("$base$API_PREFIX/answers").post(body).build()
        return execute(request) { json.decodeFromString<RecordedResponse>(it).recorded }
    }

    private fun requestBuilder(url: String) =
        Request.Builder().url(url).header(HEADER_DEVICE_ID, deviceId())

    private suspend fun <T> execute(request: Request, parse: (String) -> T): T =
        withContext(Dispatchers.IO) {
            val response = try {
                http.newCall(request).execute()
            } catch (e: IOException) {
                throw NetworkException(e)
            }
            response.use {
                val text = it.body.string()
                if (!it.isSuccessful) throw toApiException(it.code, text)
                parse(text)
            }
        }

    private fun toApiException(status: Int, text: String): ApiException {
        val error = runCatching { json.decodeFromString<ErrorResponse>(text).error }.getOrNull()
        return ApiException(
            httpStatus = status,
            code = error?.code ?: ApiException.CODE_UNKNOWN,
            message = error?.message ?: "HTTP $status",
        )
    }

    private fun UploadFile.asRequestBody() = object : RequestBody() {
        override fun contentType() = mimeType.toMediaType()

        override fun writeTo(sink: BufferedSink) {
            open().use { sink.writeAll(it.source()) }
        }
    }
}
