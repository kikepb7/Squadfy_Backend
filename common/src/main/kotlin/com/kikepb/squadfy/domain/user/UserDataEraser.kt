package com.kikepb.squadfy.domain.user

import com.kikepb.squadfy.domain.type.UserId

/**
 * Removes or anonymizes what a module stores about a user when they delete their account
 * (spec 010 RN-A). Every module that keeps user data implements it; the user module calls all of
 * them in the same transaction as the deletion, ordered with `@Order` (club before match).
 */
interface UserDataEraser {

    fun eraseUserData(userId: UserId)
}
