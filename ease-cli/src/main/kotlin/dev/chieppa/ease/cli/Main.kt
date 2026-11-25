package dev.chieppa.ease.cli

import kotlin.system.exitProcess

/** The `ease` command: runs the command named by the first argument and exits with its status. */
public fun main(args: Array<String>) {
  exitProcess(EaseCli.standard().run(args.toList()))
}
