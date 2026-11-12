package com.woowang.scrollmate.domain

import com.woowang.scrollmate.data.Material
import com.woowang.scrollmate.data.MaterialProgress
import com.woowang.scrollmate.data.MaterialStatus
import com.woowang.scrollmate.data.Subject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MaterialItemsTest {
    private fun material(id: String, name: String, subjectId: Long? = null, status: MaterialStatus = MaterialStatus.READY) =
        Material(id = id, name = name, subjectId = subjectId, status = status)

    @Test
    fun attachesSubjectNameAndProgress() {
        val items = buildMaterialItems(
            materials = listOf(material("m1", "a.pdf", subjectId = 1)),
            subjects = listOf(Subject(id = 1, name = "토익")),
            progress = mapOf("m1" to MaterialProgress(done = 3, total = 10)),
        )

        assertEquals("토익", items.single().subjectName)
        assertEquals(MaterialProgress(3, 10), items.single().progress)
    }

    @Test
    fun materialWithoutSubjectHasNoSubjectName() {
        val items = buildMaterialItems(listOf(material("m1", "a.pdf")), emptyList(), emptyMap())

        assertNull(items.single().subjectName)
        assertNull(items.single().progress)
    }

    @Test
    fun materialWhoseSubjectWasDeletedHasNoSubjectName() {
        val items = buildMaterialItems(listOf(material("m1", "a.pdf", subjectId = 9)), emptyList(), emptyMap())

        assertNull(items.single().subjectName)
    }

    @Test
    fun processingMaterialsComeFirstThenByName() {
        val items = buildMaterialItems(
            materials = listOf(
                material("m1", "다.pdf"),
                material("m2", "나.pdf", status = MaterialStatus.PROCESSING),
                material("m3", "가.pdf"),
                material("m4", "라.pdf", status = MaterialStatus.PROCESSING),
            ),
            subjects = emptyList(),
            progress = emptyMap(),
        )

        assertEquals(listOf("나.pdf", "라.pdf", "가.pdf", "다.pdf"), items.map { it.material.name })
    }

    @Test
    fun emptyInputGivesEmptyList() {
        assertEquals(emptyList<MaterialItem>(), buildMaterialItems(emptyList(), emptyList(), emptyMap()))
    }
}
