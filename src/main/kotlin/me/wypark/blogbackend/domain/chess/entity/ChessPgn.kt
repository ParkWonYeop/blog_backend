package me.wypark.blogbackend.domain.chess.entity

private val RESULT_HEADER = Regex("""\[Result\s+"[^"]*"]""")
private val TRAILING_RESULT = Regex("""(1-0|0-1|1/2-1/2|\*)\s*$""")

/** 엔진이 만든 PGN의 Result 헤더와 말미 결과 토큰을 바꾼다. 기권·시간패처럼 보드 밖에서 끝난 결과를 반영할 때 쓴다. */
fun String.withPgnResult(result: String): String {
    val withHeader = if (RESULT_HEADER.containsMatchIn(this)) {
        RESULT_HEADER.replace(this, """[Result "$result"]""")
    } else {
        """[Result "$result"]""" + "\n" + this
    }
    return if (TRAILING_RESULT.containsMatchIn(withHeader)) {
        TRAILING_RESULT.replace(withHeader, result)
    } else {
        "${withHeader.trimEnd()} $result"
    }
}
