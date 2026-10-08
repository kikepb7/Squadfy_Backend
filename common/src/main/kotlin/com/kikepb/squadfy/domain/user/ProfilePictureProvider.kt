package com.kikepb.squadfy.domain.user

import com.kikepb.squadfy.domain.type.UserId

/** Current profile picture of users (spec 012 RN-C3). Implemented by the chat module, which owns them. */
fun interface ProfilePictureProvider {

    /** Picture of each user that has one; users without a picture are left out. */
    fun findProfilePictures(userIds: Collection<UserId>): Map<UserId, String>
}
