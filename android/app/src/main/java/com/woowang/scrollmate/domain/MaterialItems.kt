package com.woowang.scrollmate.domain

import com.woowang.scrollmate.data.Material
import com.woowang.scrollmate.data.MaterialProgress
import com.woowang.scrollmate.data.MaterialStatus
import com.woowang.scrollmate.data.Subject

// 자료 목록 화면의 한 줄. subjectName이 null이면 학습 주제가 없는 자료(미분류)
data class MaterialItem(
    val material: Material,
    val subjectName: String?,
    // 서버가 알려 준 문제 생성 진행. 아직 받지 못했으면 null
    val progress: MaterialProgress?,
)

// 문제를 만드는 중인 자료를 먼저, 그다음 이름순으로 보여 줌
fun buildMaterialItems(
    materials: List<Material>,
    subjects: List<Subject>,
    progress: Map<String, MaterialProgress>,
): List<MaterialItem> {
    val subjectNames = subjects.associate { it.id to it.name }
    return materials
        .map { MaterialItem(it, it.subjectId?.let(subjectNames::get), progress[it.id]) }
        .sortedWith(
            compareBy<MaterialItem> { if (it.material.status == MaterialStatus.PROCESSING) 0 else 1 }
                .thenBy { it.material.name },
        )
}
