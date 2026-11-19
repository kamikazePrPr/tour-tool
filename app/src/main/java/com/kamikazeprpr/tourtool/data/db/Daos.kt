package com.kamikazeprpr.tourtool.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TeamDao {
    @Query("SELECT * FROM team ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Team>>

    @Query("SELECT * FROM team WHERE id = :id")
    suspend fun getById(id: Long): Team?

    @Query("SELECT * FROM team WHERE id = :id")
    fun observeById(id: Long): Flow<Team?>

    @Insert
    suspend fun insert(team: Team): Long

    @Update
    suspend fun update(team: Team)

    @Delete
    suspend fun delete(team: Team)

    @Query("SELECT COUNT(*) FROM member WHERE teamId = :teamId")
    fun observeMemberCount(teamId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM member WHERE teamId = :teamId AND status = :status")
    fun observeCountByStatus(teamId: Long, status: String): Flow<Int>
}

@Dao
interface MemberDao {
    @Query("SELECT * FROM member WHERE teamId = :teamId ORDER BY seq ASC, id ASC")
    fun observeByTeam(teamId: Long): Flow<List<Member>>

    @Query("SELECT * FROM member WHERE teamId = :teamId AND status != 'AUTO_OK' AND status != 'MANUAL_OK' ORDER BY seq ASC, id ASC")
    fun observePending(teamId: Long): Flow<List<Member>>

    @Query("SELECT * FROM member WHERE id = :id")
    suspend fun getById(id: Long): Member?

    @Query("SELECT COUNT(*) FROM member WHERE teamId = :teamId")
    suspend fun countByTeam(teamId: Long): Int

    @Query("SELECT * FROM member WHERE teamId = :teamId AND idNumber = :idNumber AND idNumber != '' LIMIT 1")
    suspend fun findSameNumber(teamId: Long, idNumber: String): Member?

    @Insert
    suspend fun insert(member: Member): Long

    @Insert
    suspend fun insertAll(members: List<Member>): List<Long>

    @Update
    suspend fun update(member: Member)

    @Delete
    suspend fun delete(member: Member)

    @Query("DELETE FROM member WHERE teamId = :teamId")
    suspend fun deleteByTeam(teamId: Long)
}

@Dao
interface TagDao {
    @Query("SELECT * FROM tag ORDER BY sortOrder ASC, id ASC")
    fun observeAll(): Flow<List<Tag>>

    @Query("SELECT * FROM tag ORDER BY sortOrder ASC, id ASC")
    suspend fun getAll(): List<Tag>

    @Query("DELETE FROM member_tag WHERE memberId = :memberId")
    suspend fun unlinkAll(memberId: Long)

    @Query("DELETE FROM member_tag WHERE memberId IN (SELECT id FROM member WHERE teamId = :teamId)")
    suspend fun clearTeamTags(teamId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(tag: Tag): Long

    @Delete
    suspend fun delete(tag: Tag)

    @Query("SELECT * FROM tag WHERE label = :label LIMIT 1")
    suspend fun findByLabel(label: String): Tag?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun link(memberTag: MemberTag)

    @Query("DELETE FROM member_tag WHERE memberId = :memberId AND tagId = :tagId")
    suspend fun unlink(memberId: Long, tagId: Long)

    @Query("SELECT t.* FROM tag t INNER JOIN member_tag mt ON t.id = mt.tagId WHERE mt.memberId = :memberId ORDER BY t.sortOrder")
    fun observeTagsOf(memberId: Long): Flow<List<Tag>>

    @Query(
        """SELECT mt.memberId AS memberId, t.label AS label FROM member_tag mt
           INNER JOIN tag t ON t.id = mt.tagId
           INNER JOIN member m ON m.id = mt.memberId
           WHERE m.teamId = :teamId ORDER BY t.sortOrder"""
    )
    suspend fun tagsOfTeam(teamId: Long): List<MemberTagLabel>

    @Query(
        """SELECT mt.memberId AS memberId,
                  t.id AS id, t.label AS label, t.colorIndex AS colorIndex, t.sortOrder AS sortOrder
           FROM member_tag mt
           INNER JOIN tag t ON t.id = mt.tagId
           INNER JOIN member m ON m.id = mt.memberId
           WHERE m.teamId = :teamId ORDER BY t.sortOrder"""
    )
    suspend fun tagsOfTeamFull(teamId: Long): List<MemberTagFull>

    @Query("SELECT * FROM tag WHERE id = :id")
    suspend fun getById(id: Long): Tag?
}

/** 列表页显示标签用：带颜色下标，一次把整团捞出来 */
data class MemberTagFull(
    val memberId: Long,
    val id: Long,
    val label: String,
    val colorIndex: Int,
    val sortOrder: Int,
)

/** 导出用：只要名字 */
data class MemberTagLabel(
    val memberId: Long,
    val label: String,
)
