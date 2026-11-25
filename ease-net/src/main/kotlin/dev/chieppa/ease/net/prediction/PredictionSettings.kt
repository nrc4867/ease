package dev.chieppa.ease.net.prediction

import dev.chieppa.ease.ecs.prediction.CorrectionOffsets
import dev.chieppa.ease.ecs.prediction.InterpolatedEntities
import dev.chieppa.ease.ecs.prediction.PredictedEntities

/**
 * How a client predicts, reconciles and interpolates: its [clock]'s lead, the [historyTicks] of
 * prediction and input it keeps to replay, the [interpolationDelayTicks] it draws others behind by
 * and the [interpolationStates] it keeps of each, and how corrections are smoothed: each tick an
 * offset keeps [smoothingKeep] of itself, and one past [snapDistance] world units snaps.
 */
public class PredictionSettings(
  public val clock: ClockSettings = ClockSettings(),
  public val historyTicks: Int = PredictedEntities.DEFAULT_HISTORY_TICKS,
  public val interpolationDelayTicks: Int = InterpolationClock.DEFAULT_DELAY_TICKS,
  public val interpolationStates: Int = InterpolatedEntities.DEFAULT_CAPACITY,
  public val smoothingKeep: Float = CorrectionOffsets.DEFAULT_KEEP,
  public val snapDistance: Float = CorrectionOffsets.DEFAULT_SNAP_DISTANCE,
)
