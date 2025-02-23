package chess

import scala.math.{ abs, max, min }

opaque type Square = Int
object Square extends OpaqueInt[Square]:
  extension (p: Square)
    inline def down: Option[Square]      = Square.at(file.value, rank.value - 1)
    inline def left: Option[Square]      = Square.at(file.value - 1, rank.value)
    inline def downLeft: Option[Square]  = Square.at(file.value - 1, rank.value - 1)
    inline def downRight: Option[Square] = Square.at(file.value + 1, rank.value - 1)
    inline def up: Option[Square]        = Square.at(file.value, rank.value + 1)
    inline def right: Option[Square]     = Square.at(file.value + 1, rank.value)
    inline def upLeft: Option[Square]    = Square.at(file.value - 1, rank.value + 1)
    inline def upRight: Option[Square]   = Square.at(file.value + 1, rank.value + 1)

    inline def prevRank(color: Color) = color.fold(p.down, p.up)

    def >|(stop: Square => Boolean): List[Square] = |<>|(stop, _.right)
    def |<(stop: Square => Boolean): List[Square] = |<>|(stop, _.left)
    def |<>|(stop: Square => Boolean, dir: Direction): List[Square] =
      dir(p) map { p =>
        p :: (if (stop(p)) Nil else p.|<>|(stop, dir))
      } getOrElse Nil

    inline def ?<(inline other: Square): Boolean = file < other.file
    inline def ?>(inline other: Square): Boolean = file > other.file
    inline def ?+(inline other: Square): Boolean = rank < other.rank
    inline def ?^(inline other: Square): Boolean = rank > other.rank
    inline def ?|(inline other: Square): Boolean = file == other.file
    inline def ?-(inline other: Square): Boolean = rank == other.rank

    def <->(other: Square): Iterable[Square] =
      min(file.value, other.file.value) to max(file.value, other.file.value) flatMap {
        Square.at(_, rank.value)
      }

    inline def onSameDiagonal(other: Square): Boolean =
      file.value - rank.value == other.file.value - other.rank.value || file.value + rank.value == other.file.value + other.rank.value
    inline def onSameLine(other: Square): Boolean = ?-(other) || ?|(other)

    inline def xDist(inline other: Square): Int = abs(p.file.value - other.file.value)
    inline def yDist(inline other: Square): Int = abs(p.rank.value - other.rank.value)

    inline def isLight: Boolean = (file.value + rank.value) % 2 == 1

    inline def file: File = File of p
    inline def rank: Rank = Rank of p

    def asChar: Char =
      if (p <= 25) (97 + p).toChar      // a ...
      else if (p <= 51) (39 + p).toChar // A ...
      else if (p <= 61) (p - 4).toChar  // 0 ...
      else if (p == 62) '!'
      else '?'

    inline def key = s"${p.file.char}${p.rank.char}"

    inline def withRank(inline r: Rank): Square = Square(p.file, r)
    inline def withFile(inline f: File): Square = Square(f, p.rank)

    inline def withRankOf(inline o: Square): Square = withRank(o.rank)
    inline def withFileOf(inline o: Square): Square = withFile(o.file)

  end extension

  inline def apply(inline file: File, inline rank: Rank): Square = file.value + 8 * rank.value

  inline def at(x: Int, y: Int): Option[Square] =
    if (0 <= x && x < 8 && 0 <= y && y < 8) Some(x + 8 * y)
    else None

  inline def at(index: Int): Option[Square] =
    if (0 <= index && index < 64) Some(index)
    else None

  inline def fromKey(inline key: String): Option[Square] = allKeys get key

  inline def fromChar(inline c: Char): Option[Square] = charMap get c

  inline def keyToChar(inline key: String) = fromKey(key).map(_.asChar)

  val A1: Square = 0
  val B1: Square = 1
  val C1: Square = 2
  val D1: Square = 3
  val E1: Square = 4
  val F1: Square = 5
  val G1: Square = 6
  val H1: Square = 7
  val A2: Square = 8
  val B2: Square = 9
  val C2: Square = 10
  val D2: Square = 11
  val E2: Square = 12
  val F2: Square = 13
  val G2: Square = 14
  val H2: Square = 15
  val A3: Square = 16
  val B3: Square = 17
  val C3: Square = 18
  val D3: Square = 19
  val E3: Square = 20
  val F3: Square = 21
  val G3: Square = 22
  val H3: Square = 23
  val A4: Square = 24
  val B4: Square = 25
  val C4: Square = 26
  val D4: Square = 27
  val E4: Square = 28
  val F4: Square = 29
  val G4: Square = 30
  val H4: Square = 31
  val A5: Square = 32
  val B5: Square = 33
  val C5: Square = 34
  val D5: Square = 35
  val E5: Square = 36
  val F5: Square = 37
  val G5: Square = 38
  val H5: Square = 39
  val A6: Square = 40
  val B6: Square = 41
  val C6: Square = 42
  val D6: Square = 43
  val E6: Square = 44
  val F6: Square = 45
  val G6: Square = 46
  val H6: Square = 47
  val A7: Square = 48
  val B7: Square = 49
  val C7: Square = 50
  val D7: Square = 51
  val E7: Square = 52
  val F7: Square = 53
  val G7: Square = 54
  val H7: Square = 55
  val A8: Square = 56
  val B8: Square = 57
  val C8: Square = 58
  val D8: Square = 59
  val E8: Square = 60
  val F8: Square = 61
  val G8: Square = 62
  val H8: Square = 63

  val all: List[Square] = Square from (0 to 63).toList

  val whiteBackrank: List[Square] = (A1 <-> H1).toList
  val blackBackrank: List[Square] = (A8 <-> H8).toList

  val allKeys: Map[String, Square] = all.mapBy(_.key)
  val charMap: Map[Char, Square]   = all.mapBy(square => Square(square).asChar)
