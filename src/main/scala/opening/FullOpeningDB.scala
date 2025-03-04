package chess
package opening

object FullOpeningDB {

  private def all: Vector[FullOpening] = FullOpeningPart1.db ++ FullOpeningPart2.db

  lazy val byFen = all.map { o =>
    o.fen -> o
  }.toMap

  def findByFen(fen: String) = byFen get fen.split(' ').take(3).mkString(" ")

  val SEARCH_MAX_PLIES = 40

  // assumes standard initial FEN and variant
  def search(moveStrs: List[String]): Option[FullOpening.AtPly] =
    chess.Replay.boards(moveStrs take SEARCH_MAX_PLIES, None, variant.Standard).toOption.flatMap {
      _.zipWithIndex.drop(1).foldRight(none[FullOpening.AtPly]) {
        case ((board, ply), None) =>
          val fen = format.Forsyth.exportStandardPositionTurnCastling(board, ply)
          byFen get fen map (_ atPly ply)
        case (_, found) => found
      }
    }
}
