package chess
package format.pgn

case class ParsedPgn(tags: List[Tag], sans: List[San]) {

  def tag(name: String): Option[String] = {
    val tagType = (Tag tagType name)
    tags.find(_.name == tagType).map(_.value)
  }
}

// Standard Algebraic Notation
sealed trait San {

  def apply(situation: Situation): Valid[MoveOrDrop]
}

case class Std(
    dest: Pos,
    role: Role,
    capture: Boolean = false,
    file: Option[Int] = None,
    rank: Option[Int] = None,
    check: Boolean = false,
    checkmate: Boolean = false,
    promotion: Option[PromotableRole] = None) extends San {

  def withSuffixes(s: Suffixes) = copy(
    check = s.check,
    checkmate = s.checkmate,
    promotion = s.promotion)

  def apply(situation: Situation) = move(situation) map Left.apply

  def move(situation: Situation): Valid[chess.Move] =
    situation.board.pieces.foldLeft(Option.empty[chess.Move]) {
      case (None, (pos, piece)) if piece.color == situation.color && piece.role == role && compare(file, pos.x) && compare(rank, pos.y) && piece.eyesMovable(pos, dest) =>
        val a = Actor(piece, pos, situation.board)
        a trustedMoves false find { m =>
          m.dest == dest && a.board.variant.kingSafety(a, m)
        }
      case (m, _) => m
    } match {
      case None       => failure(s"No move found: $this\n$situation")
      case Some(move) => (move withPromotion promotion) match {
        case Some(move) => success(move)
        case None => failure("Wrong promotion")
      }
    }

  private def compare[A](a: Option[A], b: A) = a.fold(true)(b==)
}

case class Drop(
    role: Role,
    pos: Pos,
    check: Boolean = false,
    checkmate: Boolean = false) extends San {

  def withSuffixes(s: Suffixes) = copy(
    check = s.check,
    checkmate = s.checkmate)

  def apply(situation: Situation) = drop(situation) map Right.apply

  def drop(situation: Situation): Valid[chess.Drop] =
    situation.drop(role, pos)
}

case class Suffixes(
  check: Boolean,
  checkmate: Boolean,
  promotion: Option[PromotableRole])

case class Castle(
    side: Side,
    check: Boolean = false,
    checkmate: Boolean = false) extends San {

  def withSuffixes(s: Suffixes) = copy(
    check = s.check,
    checkmate = s.checkmate)

  def apply(situation: Situation) = move(situation) map Left.apply

  def move(situation: Situation): Valid[chess.Move] = for {
    kingPos ← ((situation.board kingPosOf situation.color) match {
      case Some(kingPos) => success(kingPos)
      case None => failure("No king found")
    })
    actor ← ((situation.board actorAt kingPos) match {
      case Some(actor) => success(actor)
      case None => failure("No actor found")
    })
    move ← ((actor.castleOn(side).headOption) match {
      case Some(move) => success(move)
      case None => failure("Cannot castle / variant is " + situation.board.variant)
    })
  } yield move
}
