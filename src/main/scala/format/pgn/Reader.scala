package chess
package format.pgn

import util.chaining.scalaUtilChainingOps
import cats.data.Validated

object Reader:

  sealed trait Result:
    def valid: Validated[ErrorStr, Replay]

  object Result:
    case class Complete(replay: Replay) extends Result:
      def valid = Validated.valid(replay)
    case class Incomplete(replay: Replay, failure: ErrorStr) extends Result:
      def valid = Validated.invalid(failure)

  def full(pgn: PgnStr, tags: Tags = Tags.empty): Validated[ErrorStr, Result] =
    fullWithSans(pgn, identity, tags)

  def moves(sans: Iterable[SanStr], tags: Tags): Validated[ErrorStr, Result] =
    movesWithSans(sans, identity, tags)

  def fullWithSans(pgn: PgnStr, op: Sans => Sans, tags: Tags = Tags.empty): Validated[ErrorStr, Result] =
    Parser
      .full(pgn)
      .map: parsed =>
        makeReplay(makeGame(parsed.tags ++ tags), op(Sans(parsed.mainline)))

  def fullWithSans(parsed: ParsedPgn, op: Sans => Sans): Result =
    makeReplay(makeGame(parsed.tags), op(Sans(parsed.mainline)))

  def movesWithSans(sans: Iterable[SanStr], op: Sans => Sans, tags: Tags): Validated[ErrorStr, Result] =
    Parser
      .moves(sans)
      .map: moves =>
        makeReplay(makeGame(tags), op(moves))

  private def makeReplay(game: Game, sans: Sans): Result =
    sans.value.foldLeft[Result](Result.Complete(Replay(game))):
      case (Result.Complete(replay), san) =>
        san(replay.state.situation).fold(
          err => Result.Incomplete(replay, err),
          move => Result.Complete(replay addMove move)
        )
      case (r: Result.Incomplete, _) => r

  private def makeGame(tags: Tags) =
    Game(variantOption = tags.variant, fen = tags.fen).pipe(self =>
      self.copy(startedAtPly = self.ply, clock = tags.clockConfig.map(Clock.apply))
    )
