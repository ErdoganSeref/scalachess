package chess
package variant

import chess.format.Uci
import cats.syntax.option.*
import cats.data.Validated
import chess.format.EpdFen
import bitboard.Bitboard

case object Crazyhouse
    extends Variant(
      id = Variant.Id(10),
      key = Variant.LilaKey("crazyhouse"),
      uciKey = Variant.UciKey("crazyhouse"),
      name = "Crazyhouse",
      shortName = "Crazy",
      title = "Captured pieces can be dropped back on the board instead of moving a piece.",
      standardInitialPosition = true
    ):

  def pieces = Standard.pieces

  override val initialFen = EpdFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR/ w KQkq - 0 1")

  def validMoves(situation: Situation): List[Move] =
    Standard.validMoves(situation)

  override def valid(board: Board, strict: Boolean) =
    (Color.all forall validSide(board, false)) &&
      (!strict || (board.board.byRole(Pawn).count <= 16 && board.nbPieces <= 32))

  private def canDropPawnOn(pos: Pos) = pos.rank != Rank.First && pos.rank != Rank.Eighth

  override def drop(situation: Situation, role: Role, pos: Pos): Validated[ErrorStr, Drop] =
    for
      d1 <- situation.board.crazyData toValid ErrorStr("Board has no crazyhouse data")
      _ <-
        if (role != Pawn || canDropPawnOn(pos)) Validated.valid(d1)
        else Validated.invalid(ErrorStr(s"Can't drop $role on $pos"))
      piece = Piece(situation.color, role)
      d2     <- d1.drop(piece) toValid ErrorStr(s"No $piece to drop on $pos")
      board1 <- situation.board.place(piece, pos) toValid ErrorStr(s"Can't drop $role on $pos, it's occupied")
      _ <-
        if board1.checkOf(situation.color).no then Validated.valid(board1)
        else Validated.invalid(ErrorStr(s"Dropping $role on $pos doesn't uncheck the king"))
    yield Drop(
      piece = piece,
      pos = pos,
      situationBefore = situation,
      after = board1 withCrazyData d2
    )

  override def fiftyMoves(history: History): Boolean = false

  override def isIrreversible(move: Move): Boolean = move.castles

  override def finalizeBoard(board: Board, uci: Uci, capture: Option[Piece]): Board =
    uci match
      case Uci.Move(orig, dest, promOption) =>
        board.crazyData.fold(board) { data =>
          val d1 = capture.fold(data) { data.store(_, dest) }
          val d2 = promOption.fold(d1.move(orig, dest)) { _ =>
            d1 promote dest
          }
          board withCrazyData d2
        }
      case _ => board

  private def canDropStuff(situation: Situation) =
    situation.board.crazyData.exists { (data: Data) =>
      val roles = data.pockets(situation.color).roles
      roles.nonEmpty && possibleDrops(situation).fold(true) { squares =>
        squares.nonEmpty && {
          squares.exists(canDropPawnOn) || roles.exists(chess.Pawn !=)
        }
      }
    }

  override def staleMate(situation: Situation) =
    super.staleMate(situation) && !canDropStuff(situation)

  override def checkmate(situation: Situation) =
    super.checkmate(situation) && !canDropStuff(situation)

  // there is always sufficient mating material in Crazyhouse
  override def opponentHasInsufficientMaterial(situation: Situation) = false
  override def isInsufficientMaterial(board: Board)                  = false

  // if the king is not in check, all drops are possible, we just return None
  // king is in single check, we return the squares between the king and the checker
  // king is in double check, no drop is possible
  def possibleDrops(situation: Situation): Option[List[Pos]] =
    situation.ourKing.flatMap(king =>
      val checkers = situation.board.board.attackers(king, !situation.color)
      if checkers.moreThanOne then Some(Nil)
      else checkers.first.map(checker => Bitboard.between(king, checker).occupiedSquares)
    )

  // all legal moves and drops
  // this function is used in perfts only
  def legalMoves(situation: Situation): List[MoveOrDrop] =
    legalDrops(situation) ::: situation.legalMoves.filterNot(m =>
      m.castle.exists(c => c.isStandard && m.dest != c.rook)
    )

  // if the king is not in check, all empty squares are possible drop
  // king is in single check, return the squares between the king and the checker
  // king is in double check, no drop is possible
  // this function is used in perfts only
  private def legalDropSquares(situation: Situation): Bitboard =
    import bitboard.Bitboard
    situation.ourKing
      .map(king =>
        val checkers = situation.board.board.attackers(king, !situation.color)
        if checkers.isEmpty then ~situation.board.occupied
        else checkers.singleSquare.map(Bitboard.between(king, _)).getOrElse(Bitboard.empty)
      )
      .getOrElse(Bitboard.empty)

  // generate all legal drops
  // this function is used in perfts only
  private def legalDrops(situation: Situation): List[Drop] =
    val targets = legalDropSquares(situation)
    if targets.isEmpty then Nil
    else
      situation.board.crazyData.fold(List.empty[Drop]) { data =>
        val roles = data.pockets(situation.color).roles
        val dropsWithoutPawn =
          for
            role <- List(Knight, Bishop, Rook, Queen)
            if roles contains role
            to <- targets.occupiedSquares
            piece = Piece(situation.color, role)
            after = situation.board.place(piece, to).get // this is safe, we checked the target squares
            d2    = data.drop(piece).get                 // this is safe, we checked the pocket
          yield Drop(piece, to, situation, after withCrazyData d2)
        val dropWithPawn =
          if roles contains Pawn then
            for
              to <- (targets & ~Bitboard.firstRank & ~Bitboard.lastRank).occupiedSquares
              piece = Piece(situation.color, Pawn)
              after = situation.board.place(piece, to).get // this is safe, we checked the target squares
              d2    = data.drop(piece).get                 // this is safe, we checked the pocket
            yield Drop(piece, to, situation, after withCrazyData d2)
          else Nil
        dropsWithoutPawn ::: dropWithPawn
      }

  val storableRoles = List(Pawn, Knight, Bishop, Rook, Queen)

  case class Data(
      pockets: Pockets,
      // in crazyhouse, a promoted piece becomes a pawn
      // when captured and put in the pocket.
      // there we need to remember which pieces are issued from promotions.
      // we do that by tracking their positions on the board.
      promoted: Set[Pos]
  ):

    def drop(piece: Piece): Option[Data] =
      pockets take piece map { nps =>
        copy(pockets = nps)
      }

    def store(piece: Piece, from: Pos) =
      copy(
        pockets = pockets store {
          if (promoted(from)) piece.color.pawn else piece
        },
        promoted = promoted - from
      )

    def promote(pos: Pos) = copy(promoted = promoted + pos)

    def move(orig: Pos, dest: Pos) =
      copy(
        promoted = if (promoted(orig)) promoted - orig + dest else promoted
      )

  object Data:
    val init = Data(Pockets(Pocket(Nil), Pocket(Nil)), Set.empty)

  case class Pockets(white: Pocket, black: Pocket):

    def apply(color: Color) = color.fold(white, black)

    def take(piece: Piece): Option[Pockets] =
      piece.color.fold(
        white take piece.role map { np =>
          copy(white = np)
        },
        black take piece.role map { np =>
          copy(black = np)
        }
      )

    def store(piece: Piece) =
      piece.color.fold(
        copy(black = black store piece.role),
        copy(white = white store piece.role)
      )

  case class Pocket(roles: List[Role]):

    def take(role: Role) =
      if (roles contains role) Option(copy(roles = roles diff List(role)))
      else None

    def store(role: Role) =
      if (storableRoles contains role) copy(roles = role :: roles)
      else this
