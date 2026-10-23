package com.selfspotify.oneum.domain

object DownloadErrors {
    fun shouldRefreshEngine(detail: String): Boolean = listOf(
        "HTTP Error 403", "HTTP Error 410", "Signature extraction failed", "nsig extraction failed", "n challenge solving failed",
    ).any { detail.contains(it, ignoreCase = true) }

    fun message(detail: String): String = when {
        shouldRefreshEngine(detail) -> "유튜브가 파일 요청을 거부했습니다. 엔진을 갱신해 다시 시도했지만 완료하지 못했습니다. 잠시 후 재시도해 주세요."
        detail.contains("engine update failed", true) -> "다운로드 엔진을 업데이트하지 못했습니다. 네트워크 연결을 확인한 뒤 다시 시도해 주세요."
        detail.contains("Requested format", true) -> "선택한 화질의 영상이 없습니다. 다른 화질을 선택해 주세요."
        detail.contains("sign in", true) || detail.contains("bot", true) -> "유튜브에서 요청을 제한했습니다. 잠시 후 다시 시도해 주세요."
        detail.contains("private", true) || detail.contains("unavailable", true) -> "비공개 또는 사용할 수 없는 영상입니다."
        detail.contains("network", true) || detail.contains("resolve", true) || detail.contains("timed out", true) -> "네트워크 연결을 확인해 주세요."
        else -> "다운로드하지 못했습니다. 연결 상태와 링크를 확인한 뒤 다시 시도해 주세요."
    }
}
