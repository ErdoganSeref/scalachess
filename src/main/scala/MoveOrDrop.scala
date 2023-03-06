package chess

import chess.format.Uci

type MoveOrDrop = Move | Drop

object MoveOrDrop:
  extension (md: MoveOrDrop)
    def isMove = md.isInstanceOf[Move]
    def isDrop = md.isInstanceOf[Drop]

    inline def fold[A](move: Move => A, drop: Drop => A): A =
      md match
        case m: Move => move(m)
        case d: Drop => drop(d)

    def move: Option[Move] = md.fold(Some(_), _ => None)
    def drop: Option[Drop] = md.fold(_ => None, Some(_))

    inline def applyVariantEffect: MoveOrDrop =
      md match
        case m: Move => m.applyVariantEffect
        case d: Drop => d

    inline def toUci: Uci =
      md match
        case m: Move => m.toUci
        case d: Drop => d.toUci

    inline def situationAfter: Situation =
      md match
        case m: Move => m.situationAfter
        case d: Drop => d.situationAfter
