package com.kikepb.squadfy.domain.exception

class InvalidChatSizeException: RuntimeException(
    "There must be at least 2 unique participants to create a chat."
)