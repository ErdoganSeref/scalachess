package chess
package format.pgn

import cats.data.Validated
import cats.syntax.option.*

opaque type Sans = List[San]
object Sans:
  val empty                        = Nil
  def apply(sans: List[San]): Sans = sans

  extension (sans: Sans) def value: List[San] = sans

case class ParsedPgn(
    initialPosition: InitialPosition,
    tags: Tags,
    sans: Sans
)

// Standard Algebraic Notation
sealed trait San:

  def apply(situation: Situation): Validated[ErrorStr, MoveOrDrop]

  def metas: Metas

  def withMetas(m: Metas): San

  def withSuffixes(s: Suffixes): San = withMetas(metas withSuffixes s)

  def withComments(s: List[String]): San = withMetas(metas withComments s)

  def withVariations(s: List[Sans]): San = withMetas(metas withVariations s)

  def mergeGlyphs(glyphs: Glyphs): San =
    withMetas(
      metas.withGlyphs(metas.glyphs merge glyphs)
    )

case class Std(
    dest: Pos,
    role: Role,
    capture: Boolean = false,
    file: Option[File] = None,
    rank: Option[Rank] = None,
    promotion: Option[PromotableRole] = None,
    metas: Metas = Metas.empty
) extends San:

  def apply(situation: Situation) = move(situation)

  override def withSuffixes(s: Suffixes) =
    copy(
      metas = metas withSuffixes s,
      promotion = s.promotion
    )

  def withMetas(m: Metas) = copy(metas = m)

  def move(situation: Situation): Validated[ErrorStr, chess.Move] =
    val pieces = situation.board.board.byRole(role) & situation.us
    pieces.first { pos =>
      if compare(file, pos.file.index + 1) &&
        compare(rank, pos.rank.index + 1)
      then situation.generateMovesAt(pos) find { _.dest == dest }
      else None
    } match
      case None       => Validated invalid ErrorStr(s"No move found: $this\n$situation")
      case Some(move) => move withPromotion promotion toValid ErrorStr("Wrong promotion")

  override def toString = s"$role ${dest.key}"

  private inline def compare[A](a: Option[A], b: A) = a.fold(true)(b ==)

case class Drop(
    role: Role,
    pos: Pos,
    metas: Metas = Metas.empty
) extends San:

  def apply(situation: Situation) = drop(situation)

  def withMetas(m: Metas) = copy(metas = m)

  def drop(situation: Situation): Validated[ErrorStr, chess.Drop] =
    situation.drop(role, pos)

case class InitialPosition(
    comments: List[String]
)

case class Metas(
    check: Boolean,
    checkmate: Boolean,
    comments: List[String],
    glyphs: Glyphs,
    variations: List[Sans]
):

  def withSuffixes(s: Suffixes) =
    copy(
      check = s.check,
      checkmate = s.checkmate,
      glyphs = s.glyphs
    )

  def withGlyphs(g: Glyphs) = copy(glyphs = g)

  def withComments(c: List[String]) = copy(comments = c)

  def withVariations(v: List[Sans]) = copy(variations = v)

object Metas:
  val empty = Metas(check = false, checkmate = false, Nil, Glyphs.empty, Nil)

case class Castle(
    side: Side,
    metas: Metas = Metas.empty
) extends San:

  def apply(situation: Situation) = move(situation)

  def withMetas(m: Metas) = copy(metas = m)

  def move(situation: Situation): Validated[ErrorStr, chess.Move] =
    import situation.{ genCastling, ourKing, variant }
    ourKing.flatMap(k =>
      variant
        .applyVariantEffect(genCastling(k))
        .filter(variant.kingSafety)
        .find(_.castle.exists(_.side == side))
    ) toValid ErrorStr(s"Cannot castle / variant is $variant")

case class Suffixes(
    check: Boolean,
    checkmate: Boolean,
    promotion: Option[PromotableRole],
    glyphs: Glyphs
)
