package dev.chieppa.ease.cli

import java.io.PrintStream

/** Where a command writes: [out] for results, [err] for problems. */
public class CliOutput(public val out: PrintStream, public val err: PrintStream)
