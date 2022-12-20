package chess
package variant

case object Standard
    extends Variant(
      id = Variant.Id(1),
      key = Variant.Key("standard"),
      uciKey = "chess",
      name = "Standard",
      shortName = "Std",
      title = "Standard rules of chess (FIDE)",
      standardInitialPosition = true
    ):

  val pieces: Map[Pos, Piece] = Variant.symmetricRank(backRank)
