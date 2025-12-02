package apps.emojionary

import cs214.webapp.*
import ujson.*
import scala.util.Try

object Wire extends AppWire[Event, View]:

  // =======================
  //  Event <-> JSON
  // =======================
  override object eventFormat extends WireFormat[Event]:

    def encode(e: Event): Value = e match
      case Event.StartRound =>
        Obj("type" -> Str("StartRound"))

      case Event.EmojiSelected(emoji) =>
        Obj(
          "type"  -> Str("EmojiSelected"),
          "emoji" -> Str(emoji)
        )

      case Event.GuessSubmitted(text) =>
        Obj(
          "type"  -> Str("GuessSubmitted"),
          "text"  -> Str(text)
        )

      case Event.NextRound =>
        Obj("type" -> Str("NextRound"))

    def decode(json: Value): Try[Event] = Try {
      val obj = json.obj
      val tag = obj("type").str

      tag match
        case "StartRound" =>
          Event.StartRound

        case "EmojiSelected" =>
          val emoji = obj("emoji").str
          Event.EmojiSelected(emoji)

        case "GuessSubmitted" =>
          val text = obj("text").str
          Event.GuessSubmitted(text)

        case "NextRound" =>
          Event.NextRound

        case other =>
          throw new IllegalArgumentException(s"Unknown event type: $other")
    }


  override object viewFormat extends WireFormat[View]:

    private def encodeState(stateView: StateView): Value =
      Obj(
        "uid"   -> Str(stateView.uid),

        // role: Option[Role] -> String ou Null
        "role"  -> (stateView.role match
          case Some(r) => Str(r.toString)
          case None    => Null
        ),

        // phase: Phase -> String
        "phase" -> Str(stateView.phase.toString),

        // emojisHistory: Option[Vector[Emoji]]
        "emojisHistory" ->
          stateView.emojisHistory match
            case Some(vec) => Arr.from(vec.map(Str(_)))
            case None      => Null,

        // guessesHistory: Option[Vector[String]]
        "guessesHistory" ->
          stateView.guessesHistory match
            case Some(vec) => Arr.from(vec.map(Str(_)))
            case None      => Null,

        // secretWord: Option[Word]
        "secretWord" ->
          stateView.secretWord match
            case Some(w) => Str(w)
            case None    => Null
      )

    private def encodeScores(scoresView: ScoresView): Value =
      Obj.from(
        scoresView.map { (user, score) =>
          user -> Num(score)
        }
      )

    def encode(v: View): Value =
      Obj(
        "stateView"  -> encodeState(v.stateView),
        "scoresView" -> encodeScores(v.scoresView)
      )

    def decode(json: Value): Try[View] = Try {
      val obj    = json.obj
      val state  = obj("stateView").obj
      val scores = obj("scoresView").obj

      // ---- scoresView ----
      val scoresView: ScoresView =
        scores.map { (user, scoreVal) =>
          user -> scoreVal.num.toInt
        }.toMap

      // ---- role: Option[Role] ----
      val role: Option[Role] =
        state.get("role") match
          case Some(Null) | None => None
          case Some(v)           => Some(Role.valueOf(v.str))

      // ---- phase: Phase ----
      val phase: Phase =
        Phase.valueOf(state("phase").str)

      // ---- helpers pour Option[Vector[String]] ----
      def readOptVector(field: String): Option[Vector[String]] =
        state.get(field) match
          case Some(Null) | None =>
            None
          case Some(v) =>
            Some(v.arr.toVector.map(_.str))

      val emojisHistory: Option[Vector[Emoji]] =
        readOptVector("emojisHistory").asInstanceOf[Option[Vector[Emoji]]] // Emoji = String, donc OK

      val guessesHistory: Option[Vector[String]] =
        readOptVector("guessesHistory")

      val secretWord: Option[Word] =
        state.get("secretWord") match
          case Some(Null) | None => None
          case Some(v)           => Some(v.str)

      val stateView =
        StateView(
          uid            = state("uid").str,  // UserId ~ String
          role           = role,
          phase          = phase,
          emojisHistory  = emojisHistory,
          guessesHistory = guessesHistory,
          secretWord     = secretWord
        )

      View(
        stateView  = stateView,
        scoresView = scoresView
      )
    }