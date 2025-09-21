package com.kikepb.squadfy.domain.exception

class UnauthorizedException: RuntimeException(
    "Missing auth details"
)