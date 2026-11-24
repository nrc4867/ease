package dev.chieppa.ease.commands.wire

import kotlinx.serialization.SerializationException

/** Wire bits a [WireDecoder] refused, with what was wrong. */
internal class WireRefused(detail: String) : SerializationException(detail)
