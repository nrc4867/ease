package dev.chieppa.ease.viewport.spike

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.chieppa.stretch.devtools.DevTools
import dev.chieppa.stretch.frame.Viewport
import dev.chieppa.stretch.gpu.GpuSurface
import dev.chieppa.stretch.theme.StretchTheme
import dev.chieppa.stretch.theme.StretchThemes
import java.util.Locale

/** Translucent backgrounds, so the triangle shows through what Compose draws over it. */
private val markerBackground = Color(0x80000000)
private val readoutBackground = Color(0xB0101318)

private val readoutStyle = TextStyle(color = Color.White, fontSize = 13.sp)

/**
 * The viewport spike: a Stretch viewport whose `GpuSurface` shows the engine's triangle, with
 * Compose drawing over it, a marker box across the triangle's middle and readouts in the corners.
 */
public fun main() {
  DevTools.startIfRequested()
  application {
    Window(
      onCloseRequest = ::exitApplication,
      title = "Ease viewport spike",
      state = rememberWindowState(width = 1280.dp, height = 800.dp),
    ) {
      StretchTheme(StretchThemes.Ink) { Spike() }
    }
  }
}

@Composable
private fun Spike() {
  val engine = remember { FrameRate() }
  val ui = remember { FrameRate() }
  var gpu by remember { mutableStateOf("starting") }
  LaunchedEffect(ui) {
    while (true) withFrameNanos { ui.tick() }
  }
  Viewport(
    grid = false,
    topStart = { Readout("Engine on $gpu, drawn on Stretch's device") },
    bottomStart = {
      Readout("engine %.0f fps · UI %.0f fps".format(Locale.ROOT, engine.perSecond, ui.perSecond))
    },
  ) {
    GpuSurface(
      state = Unit,
      modifier = Modifier.fillMaxSize(),
      onReady = { gpu = it },
      onFailure = { it.printStackTrace() },
    ) { context ->
      EngineScene(context, engine)
    }
    Box(
      Modifier.align(Alignment.Center)
        .size(120.dp, 40.dp)
        .background(markerBackground)
        .border(2.dp, Color.White),
      contentAlignment = Alignment.Center,
    ) {
      Readout("Compose over it")
    }
  }
}

@Composable
private fun Readout(text: String) {
  BasicText(
    text,
    Modifier.background(readoutBackground).padding(horizontal = 8.dp, vertical = 4.dp),
    style = readoutStyle,
  )
}
