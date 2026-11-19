package com.kamikazeprpr.tourtool.data

import com.kamikazeprpr.tourtool.data.db.Member
import com.kamikazeprpr.tourtool.data.db.Team
import com.kamikazeprpr.tourtool.data.db.TourDatabase
import kotlinx.coroutines.flow.Flow

/**
 * 数据入口。界面层只跟这里打交道，不直接碰 DAO。
 */
class TourRepository(private val db: TourDatabase) {

    fun teams(): Flow<List<Team>> = db.teamDao().observeAll()

    fun team(teamId: Long): Flow<Team?> = db.teamDao().observeById(teamId)

    suspend fun createTeam(name: String): Long =
        db.teamDao().insert(Team(name = name.trim()))

    suspend fun renameTeam(team: Team, newName: String) =
        db.teamDao().update(team.copy(name = newName.trim()))

    suspend fun deleteTeam(team: Team) = db.teamDao().delete(team)

    fun memberCount(teamId: Long): Flow<Int> = db.teamDao().observeMemberCount(teamId)

    /** 「要你看一眼」的人数：识别失败、待核对、未验对，都算。 */
    fun needReviewCount(teamId: Long): Flow<Int> =
        db.teamDao().observeCountByStatus(teamId, Member.STATUS_NEED_REVIEW)

    fun members(teamId: Long): Flow<List<Member>> = db.memberDao().observeByTeam(teamId)

    suspend fun addMember(
        teamId: Long,
        name: String,
        gender: String,
        idNumber: String,
        birthDate: String?,
        status: String,
        failReasons: String?,
        sourceUri: String?,
        uriPermissionHeld: Boolean,
    ): Long {
        val seq = db.memberDao().countByTeam(teamId) + 1
        return db.memberDao().insert(
            Member(
                teamId = teamId,
                seq = seq,
                name = name,
                gender = gender,
                idNumber = idNumber,
                birthDate = birthDate,
                status = status,
                failReasons = failReasons,
                sourceUri = sourceUri,
                uriPermissionHeld = uriPermissionHeld,
            )
        )
    }
}
