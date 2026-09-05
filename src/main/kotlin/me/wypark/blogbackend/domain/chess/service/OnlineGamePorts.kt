package me.wypark.blogbackend.domain.chess.service

import me.wypark.blogbackend.domain.chess.entity.OnlineGame
import me.wypark.blogbackend.domain.chess.entity.OnlineInvite
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

/** 진행 중인 온라인 대국과 초대 코드의 저장소. 재시작 후에도 대국이 이어지도록 외부 저장소에 둔다. */
interface OnlineGameStore {
    fun save(game: OnlineGame)
    fun find(gameId: String): OnlineGame?
    fun activeGameIds(): Set<String>
    fun saveInvite(invite: OnlineInvite)
    fun findInvite(code: String): OnlineInvite?
    fun deleteInvite(code: String)
}

/** 온라인 대국의 영구 기록. 시작할 때 만들고 끝날 때 갱신한다. */
interface OnlineGameHistoryStore {
    fun save(game: OnlineGame)
    fun find(gameId: String): OnlineGame?
    fun findAllByMemberId(memberId: Long, pageable: Pageable): Page<OnlineGame>
}

/** 회원에게 실시간 메시지를 보내는 통로. 구현은 WebSocket 세션 레지스트리다. */
interface OnlineGameNotifier {
    fun send(memberId: Long, message: Any)
    fun isConnected(memberId: Long): Boolean
}
