package com.kikepb.squadfy.domain.exception

import com.kikepb.squadfy.domain.type.UserId

class ClubParticipantNotFoundException(userId: UserId) : RuntimeException("Club user participant not found for user $userId")
