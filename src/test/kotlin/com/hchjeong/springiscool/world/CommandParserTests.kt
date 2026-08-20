package com.hchjeong.springiscool.world

import kotlin.test.Test
import kotlin.test.assertEquals

class CommandParserTests {
    private val parser = CommandParser()

    @Test
    fun `parses command aliases`() {
        assertEquals(WorldCommand.Look, parser.parse("l"))
        assertEquals(WorldCommand.Help, parser.parse("?"))
        assertEquals(WorldCommand.Quit, parser.parse("exit"))
    }

    @Test
    fun `preserves unknown command text`() {
        assertEquals(WorldCommand.Unknown("open drawer"), parser.parse(" open drawer "))
    }
}
