package com.kamikazeprpr.tourtool.data

import com.kamikazeprpr.tourtool.data.db.Member
import com.kamikazeprpr.tourtool.data.db.MemberTag
import com.kamikazeprpr.tourtool.data.db.MemberTagFull
import com.kamikazeprpr.tourtool.data.db.Tag
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

    suspend fun updateTeam(team: Team) = db.teamDao().update(team)

    suspend fun deleteTeam(team: Team) = db.teamDao().delete(team)

    fun memberCount(teamId: Long): Flow<Int> = db.teamDao().observeMemberCount(teamId)

    /** 首页一次拿全所有团的人数 */
    fun memberCounts(): Flow<List<com.kamikazeprpr.tourtool.data.db.TeamMemberCount>> =
        db.teamDao().observeCountsByTeam()

    /** 「要你看一眼」的人数：识别失败、待核对、未验对，都算。 */
    fun needReviewCount(teamId: Long): Flow<Int> =
        db.teamDao().observeCountByStatus(teamId, Member.STATUS_NEED_REVIEW)

    fun members(teamId: Long): Flow<List<Member>> = db.memberDao().observeByTeam(teamId)

    /** 一次把这个团所有人的标签捞出来：成员 id → 标签名列表 */
    suspend fun tagsOfTeam(teamId: Long): Map<Long, List<String>> =
        db.tagDao().tagsOfTeam(teamId).groupBy({ it.memberId }, { it.label })

    /** 列表页显示彩色标签用，带颜色下标 */
    suspend fun tagsByMember(teamId: Long): Map<Long, List<MemberTagFull>> =
        db.tagDao().tagsOfTeamFull(teamId).groupBy { it.memberId }

    suspend fun allTags(): List<Tag> = db.tagDao().getAll()

    suspend fun findTag(label: String): Tag? = db.tagDao().findByLabel(label)

    /** 新建标签：颜色下标取当前标签数，天然跟已有的错开 */
    suspend fun addTag(label: String): Long {
        val existing = db.tagDao().findByLabel(label)
        if (existing != null) return existing.id
        val order = db.tagDao().getAll().size
        return db.tagDao().insert(Tag(label = label, colorIndex = order, sortOrder = order))
    }

    /** 整个成员标签集合一次替换：先清空再按勾选写入 */
    suspend fun setMemberTags(memberId: Long, tagIds: List<Long>) {
        db.tagDao().unlinkAll(memberId)
        tagIds.forEach { db.tagDao().link(MemberTag(memberId, it)) }
    }

    suspend fun removeMemberTag(memberId: Long, tagId: Long) = db.tagDao().unlink(memberId, tagId)

    /** 清掉这个团所有人身上的标签，标签本身还留在库里 */
    suspend fun clearTeamTags(teamId: Long) = db.tagDao().clearTeamTags(teamId)

    suspend fun updateMember(member: Member) = db.memberDao().update(member)

    suspend fun deleteMember(member: Member) = db.memberDao().delete(member)

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
        phone: String = "",
    ): Long {
        val seq = db.memberDao().countByTeam(teamId) + 1
        return db.memberDao().insert(
            Member(
                teamId = teamId,
                seq = seq,
                name = name,
                gender = gender,
                idNumber = idNumber,
                phone = phone,
                birthDate = birthDate,
                status = status,
                failReasons = failReasons,
                sourceUri = sourceUri,
                uriPermissionHeld = uriPermissionHeld,
            )
        )
    }
}
