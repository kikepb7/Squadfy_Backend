package com.kikepb.squadfy.domain.exception

class InvalidSearchQueryException(minLength: Int) : RuntimeException("The search needs at least $minLength characters")
