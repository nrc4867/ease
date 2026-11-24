package dev.chieppa.ease.build.sources

/** Every rule `checkSources` runs. */
val sourceRules: List<SourceRule> =
  listOf(
    FileLengthRule(),
    MarkdownLengthRule(),
    WaitRule(),
    TestReflectionRule(),
    HistoryWordsRule(),
    GoldenFileRule(),
  )

/** The findings of every rule in [rules] over every file in [files]. */
fun checkAll(files: List<SourceFile>, rules: List<SourceRule> = sourceRules): List<Finding> =
  files.flatMap { file ->
    rules.flatMap { rule -> rule.check(file) }
  }
