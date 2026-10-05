package com.kikepb.squadfy.service

import com.kikepb.squadfy.domain.model.MatchEventType
import com.kikepb.squadfy.domain.model.MatchModel
import com.kikepb.squadfy.domain.model.PlayerRatingCalculator
import com.kikepb.squadfy.domain.model.PlayerRatingCalculator.RatedPlayer
import com.kikepb.squadfy.domain.model.PlayerRatingModel
import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.ClubMemberId
import com.kikepb.squadfy.domain.type.MatchId
import com.kikepb.squadfy.domain.type.UserId
import com.kikepb.squadfy.infrastructure.database.entities.PlayerRatingChangeEntity
import com.kikepb.squadfy.infrastructure.database.entities.PlayerRatingEntity
import com.kikepb.squadfy.infrastructure.database.repositories.PlayerRatingChangeRepository
import com.kikepb.squadfy.infrastructure.database.repositories.PlayerRatingRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PlayerRatingService(
    private val playerRatingRepository: PlayerRatingRepository,
    private val playerRatingChangeRepository: PlayerRatingChangeRepository,
    private val clubAccessGuard: ClubAccessGuard
) {

    fun getMyRating(clubId: ClubId, userId: UserId): PlayerRatingModel {
        val memberId = clubAccessGuard.requireMember(clubId = clubId, userId = userId).memberId
        val stored = playerRatingRepository.findAllByClubIdAndClubMemberIdIn(clubId = clubId, clubMemberIds = listOf(memberId))
            .firstOrNull()
        return PlayerRatingModel(
            clubId = clubId,
            clubMemberId = memberId,
            rating = stored?.rating ?: PlayerRatingCalculator.INITIAL_RATING,
            matchesRated = stored?.matchesRated ?: 0
        )
    }

    /** Current rating of each member; members without completed matches get the initial rating. */
    fun ratingsFor(clubId: ClubId, memberIds: Collection<ClubMemberId>): Map<ClubMemberId, Double> {
        if (memberIds.isEmpty()) return emptyMap()
        val stored = playerRatingRepository.findAllByClubIdAndClubMemberIdIn(clubId = clubId, clubMemberIds = memberIds)
            .associate { it.clubMemberId to it.rating }
        return memberIds.associateWith { stored[it] ?: PlayerRatingCalculator.INITIAL_RATING }
    }

    @Transactional
    fun applyCompletedMatch(match: MatchModel) {
        val ratings = loadOrCreate(clubId = match.clubId, memberIds = match.teamA + match.teamB)

        fun ratedPlayer(memberId: ClubMemberId): RatedPlayer {
            val events = match.events.filter { it.clubMemberId == memberId }
            val rating = ratings.getValue(memberId)
            return RatedPlayer(
                memberId = memberId,
                rating = rating.rating,
                matchesRated = rating.matchesRated,
                goals = events.count { it.type == MatchEventType.GOAL },
                assists = events.count { it.type == MatchEventType.ASSIST },
                yellowCards = events.count { it.type == MatchEventType.YELLOW_CARD },
                redCards = events.count { it.type == MatchEventType.RED_CARD }
            )
        }

        val deltas = PlayerRatingCalculator.calculateDeltas(
            teamA = match.teamA.map(::ratedPlayer),
            teamB = match.teamB.map(::ratedPlayer),
            goalsA = match.teamAScore,
            goalsB = match.teamBScore
        )

        deltas.forEach { (memberId, delta) ->
            ratings.getValue(memberId).apply {
                rating += delta
                matchesRated += 1
            }
        }
        playerRatingRepository.saveAll(ratings.values)
        playerRatingChangeRepository.saveAll(
            deltas.map { (memberId, delta) -> PlayerRatingChangeEntity(matchId = match.id, clubMemberId = memberId, delta = delta) }
        )
    }

    @Transactional
    fun revertMatch(clubId: ClubId, matchId: MatchId) {
        val changes = playerRatingChangeRepository.findAllByMatchId(matchId = matchId)
        if (changes.isEmpty()) return

        val ratings = loadOrCreate(clubId = clubId, memberIds = changes.map { it.clubMemberId })
        changes.forEach { change ->
            ratings.getValue(change.clubMemberId).apply {
                rating -= change.delta
                matchesRated = (matchesRated - 1).coerceAtLeast(0)
            }
        }
        playerRatingRepository.saveAll(ratings.values)
        playerRatingChangeRepository.deleteAllByMatchIdInBulk(matchId = matchId)
    }

    private fun loadOrCreate(clubId: ClubId, memberIds: Collection<ClubMemberId>): Map<ClubMemberId, PlayerRatingEntity> {
        val existing = playerRatingRepository.findAllByClubIdAndClubMemberIdIn(clubId = clubId, clubMemberIds = memberIds)
            .associateBy { it.clubMemberId }
        return memberIds.distinct().associateWith { existing[it] ?: PlayerRatingEntity(clubId = clubId, clubMemberId = it) }
    }
}
