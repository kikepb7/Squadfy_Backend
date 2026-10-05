package com.kikepb.squadfy.service

import com.kikepb.squadfy.domain.exception.InvalidTeamGenerationRequestException
import com.kikepb.squadfy.domain.exception.MatchNotFoundException
import com.kikepb.squadfy.domain.model.MatchModel
import com.kikepb.squadfy.domain.model.TeamSideModel.TEAM_A
import com.kikepb.squadfy.domain.model.TeamSideModel.TEAM_B
import com.kikepb.squadfy.domain.type.ClubMemberId
import com.kikepb.squadfy.domain.type.MatchId
import com.kikepb.squadfy.infrastructure.database.entities.MatchTeamPlayerEntity
import com.kikepb.squadfy.infrastructure.database.mappers.toEntitySide
import com.kikepb.squadfy.infrastructure.database.mappers.toMatchModel
import com.kikepb.squadfy.infrastructure.database.repositories.MatchEventRepository
import com.kikepb.squadfy.infrastructure.database.repositories.MatchRepository
import com.kikepb.squadfy.infrastructure.database.repositories.MatchTeamPlayerRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import kotlin.random.Random

@Service
class MatchTeamService(
    private val matchRepository: MatchRepository,
    private val matchTeamPlayerRepository: MatchTeamPlayerRepository,
    private val matchEventRepository: MatchEventRepository,
    private val matchAnnouncementService: MatchAnnouncementService
) {

    @Transactional
    fun generateTeams(
        matchId: MatchId,
        mode: TeamGenerationMode,
        manualTeamA: List<ClubMemberId>?,
        manualTeamB: List<ClubMemberId>?
    ): MatchModel {
        val match = matchRepository.findByIdOrNull(matchId)
            ?: throw MatchNotFoundException()

        val assignment = when (mode) {
            TeamGenerationMode.AUTO -> {
                val enrolled = matchAnnouncementService.getEnrolledPlayersByMatch(matchId)
                if (enrolled.size < 2) throw InvalidTeamGenerationRequestException(
                    "At least 2 enrolled players are required for AUTO mode"
                )
                autoAssignTeams(enrolled)
            }
            TeamGenerationMode.MANUAL -> manualAssignTeams(
                teamA = manualTeamA,
                teamB = manualTeamB
            )
        }

        matchTeamPlayerRepository.deleteByMatchId(matchId)
        matchTeamPlayerRepository.saveAll(
            assignment.first.map {
                MatchTeamPlayerEntity(matchId = matchId, clubMemberId = it, teamSide = TEAM_A.toEntitySide())
            } + assignment.second.map {
                MatchTeamPlayerEntity(matchId = matchId, clubMemberId = it, teamSide = TEAM_B.toEntitySide())
            }
        )

        val savedPlayers = matchTeamPlayerRepository.findAllByMatchId(matchId)
        val events = matchEventRepository.findAllByMatchId(matchId)
        val enrolledPlayers = matchAnnouncementService.getEnrolledPlayersByMatch(matchId)
        return match.toMatchModel(players = savedPlayers, events = events, enrolledPlayers = enrolledPlayers)
    }

    private fun autoAssignTeams(memberIds: List<ClubMemberId>): Pair<List<ClubMemberId>, List<ClubMemberId>> {
        val shuffled = memberIds.shuffled(Random(Instant.now().toEpochMilli()))
        val teamA = shuffled.take(shuffled.size / 2 + shuffled.size % 2)
        val teamB = shuffled.drop(teamA.size)
        return teamA to teamB
    }

    private fun manualAssignTeams(
        teamA: List<ClubMemberId>?,
        teamB: List<ClubMemberId>?
    ): Pair<List<ClubMemberId>, List<ClubMemberId>> {
        val safeTeamA = teamA?.distinct() ?: emptyList()
        val safeTeamB = teamB?.distinct() ?: emptyList()

        if (safeTeamA.isEmpty() || safeTeamB.isEmpty()) {
            throw InvalidTeamGenerationRequestException("Manual mode requires non-empty teamA and teamB")
        }
        val all = safeTeamA + safeTeamB
        if (all.size != all.toSet().size) {
            throw InvalidTeamGenerationRequestException("A player cannot be in both teams")
        }
        if (kotlin.math.abs(safeTeamA.size - safeTeamB.size) > 1) {
            throw InvalidTeamGenerationRequestException("Team sizes must be balanced (difference ≤ 1)")
        }

        return safeTeamA to safeTeamB
    }

    enum class TeamGenerationMode {
        AUTO,
        MANUAL
    }
}
