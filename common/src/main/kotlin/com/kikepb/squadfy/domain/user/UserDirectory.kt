package com.kikepb.squadfy.domain.user

import com.kikepb.squadfy.domain.type.UserId

/**
 * Read-only port to look up registered users from other modules without querying the user
 * module's tables. Implemented by the user module.
 */
interface UserDirectory {

    fun findUser(userId: UserId): UserSummary?
}

data class UserSummary(
    val userId: UserId,
    val username: String,
    val email: String
)
