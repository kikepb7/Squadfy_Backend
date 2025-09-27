package com.kikepb.squadfy.api.util

import com.kikepb.squadfy.domain.exception.UnauthorizedException
import com.kikepb.squadfy.domain.type.UserId
import org.springframework.security.core.context.SecurityContextHolder

val requestUserId: UserId
    get() = SecurityContextHolder.getContext().authentication?.principal as? UserId
        ?: throw UnauthorizedException()