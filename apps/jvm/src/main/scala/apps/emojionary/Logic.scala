package apps.emojionary

import cs214.webapp.*
import cs214.webapp.server.ClockDrivenStateMachine
import scala.util.{Try, Success, Failure}
class Logic extends ClockDrivenStateMachine[Event, State, View]:

  // ============ CONFIGURATION ============
  
  override val clockPeriodMs: Int = 100
  override val clockDrivenWire: AppWire[Event, View] = Wire
  
  override val appInfo: AppInfo = AppInfo(
    id = "emojionary",
    name = "Emojionary",
    description = "Guess the word using emojis!",
    year = 2025
  )

  // ============ EMOJI LIST ============
  
  val AllEmojis: Vector[Emoji] = Vector(
    // 😀 Faces & emotions
    "😀","😃","😄","😁","😆","😅","🤣","😂","🙂","🙃","😉","😊","😇","🥰","😍","🤩","😘","😗","😚","😙",
    "😋","😛","😜","🤪","😝","🤑","🤗","🤭","🤫","🤔","🤐","🤨","😐","😑","😶","🫥","😶‍🌫️","🙄","😏",
    "😣","😥","😮","🤐","😯","😪","😫","🥱","😴","😌","😛","😜","😝","🤤","😓","😔","😕","🫤","🙃",
    "😖","😞","😟","😤","😢","😭","😦","😧","😨","😩","😬","😰","😱","🥵","🥶","😳","🤯","😵","😵‍💫",
    "🥴","😲","🤠","🥳","😎","🤓","🧐","😒","😡","😠","🤬","😷","🤒","🤕","🤢","🤮","🤧","😇","🤥",
    "😈","👿","💀","☠️","👻","👽","🤖","💩",

    // 👋 Gestures / hands (sans tons de peau)
    "👋","🤚","✋","🖐️","👌","🤌","🤏","✌️","🤞","🤟","🤘","🤙","👈","👉","👆","👇","☝️","👍","👎",
    "✊","👊","🤛","🤜","👏","🙌","👐","🤲","🤝","🙏",

    // 🧍 People & roles (neutre)
    "🧑","👧","👦","👩","👨","🧓","👶","🧒","🧑‍🎓","🧑‍🏫","🧑‍💻","🧑‍🔬","🧑‍🎨","🧑‍🚀","🧑‍🍳",
    "🧑‍⚕️","🧑‍🌾","🧑‍🔧","🧑‍🎤","🧑‍🎮","🧑‍✈️","🧑‍🚒","🧑‍⚖️","🕵️","💂","👷","🧑‍🏭",
    "🦸","🦹","🧙","🧝","🧛","🧟","🧞","🧜",

    // 🐶 Animals
    "🐶","🐱","🐭","🐹","🐰","🦊","🐻","🐼","🐻‍❄️","🐨","🐯","🦁","🐮","🐷","🐽","🐸","🐵","🙈","🙉","🙊",
    "🐔","🐧","🐦","🐤","🐣","🐥","🦆","🦅","🦉","🦇","🐺","🐗","🐴","🦄","🐝","🪲","🐛","🦋","🐌",
    "🐞","🦂","🕷️","🕸️","🐢","🐍","🦎","🐙","🦑","🦐","🦞","🦀","🐠","🐟","🐡","🦈","🐬","🐳","🐋",
    "🦭","🐊","🦧","🦍","🐘","🦣","🦏","🦛","🦓","🦒","🦘","🐪","🐫","🦙","🦌","🐄","🐂","🐃","🐎",
    "🐖","🐏","🐑","🦝","🦨","🦡","🦫","🦦","🦥","🐁","🐀","🐿️","🦔","🐾",

    // 🌿 Nature & weather
    "🌵","🌴","🌲","🌳","🌱","🌿","☘️","🍀","🎍","🎋","🍃","🍂","🍁","🪴","🌸","🌼","🌻","🌺","🌹",
    "🥀","🌷","🌾","🌙","🌚","🌛","⭐","🌟","✨","⚡","☀️","🌤️","⛅","🌥️","☁️","🌦️","🌧️","⛈️","🌩️",
    "🌨️","❄️","☃️","⛄","💨","🌪️","🌫️","🌈","☔","💧","💦","🔥","🌊","🫧",

    // 🍎 Food & drink
    "🍏","🍎","🍐","🍊","🍋","🍌","🍉","🍇","🍓","🫐","🍈","🍒","🍑","🥭","🍍","🥥","🥝","🍅","🍆",
    "🥑","🥦","🥬","🥒","🌶️","🫑","🌽","🥕","🧄","🧅","🥔","🍠","🫘","🥜","🌰","🍞","🥐","🥖","🫓","🥨",
    "🥯","🥞","🧇","🧀","🍖","🍗","🥩","🥓","🍔","🍟","🍕","🌭","🥪","🌮","🌯","🫔","🥙","🥚","🍳","🥘",
    "🍲","🥣","🥗","🍿","🧈","🧂","🥫","🍝","🍜","🍜","🍛","🍣","🍱","🥟","🍤","🍙","🍚","🍘","🍥",
    "🦪","🍦","🍧","🍨","🍩","🍪","🎂","🍰","🧁","🥧","🍫","🍬","🍭","🍮","🍯",
    "☕","🫖","🍵","🥛","🧃","🥤","🧋","🍶","🍺","🍻","🥂","🍷","🍸","🍹","🧉","🍾",

    // ⚽ Sports & activities
    "⚽","🏀","🏈","⚾","🥎","🎾","🏐","🏉","🥏","🎱","🏓","🏸","🏒","🏑","🥍","🏏","⛳","🥊","🥋",
    "🎽","🛹","🛼","⛸️","🥌","🛷","🎿","🏂","🏋️","🤼","🤸","⛹️","🤺","🏊","🚣","🧗","🚴","🚵","🏇",
    "🧘","🏄","🏆","🥇","🥈","🥉","🎯","🪀","🪁","🎳","🎮","🕹️","🎲","♟️","🧩","🎨","🎭","🎤","🎧",
    "🎼","🎹","🥁","🪘","🎷","🎺","🎸","🪕","🎻",

    // 🚗 Travel & places
    "🚗","🚕","🚙","🚌","🚎","🏎️","🚓","🚑","🚒","🚐","🛻","🚚","🚛","🚜","🛵","🏍️","🚲","🛴","🚨",
    "🚁","✈️","🛫","🛬","🛩️","🚀","🛸","🚂","🚆","🚄","🚅","🚈","🚇","🚊","🚉","🛶","⛵","🛥️","🚤",
    "🛳️","⚓","🗺️","🗽","🗼","🏰","🏯","🏟️","🏛️","⛪","🕌","🛕","🕍","⛩️","🗻","🏔️","⛰️","🌋","🏜️",
    "🏖️","🏝️","🏞️","🌅","🌄","🌆","🌇","🌃","🌉","🏙️",

    // 🧰 Objects (utiles en UI)
    "⌛","⏳","⏰","⏱️","📱","💻","🖥️","⌨️","🖱️","🖨️","📷","📸","🎥","📺","📡","🔋","🔌","💡","🔦",
    "🕯️","🧯","🧲","🧪","🧫","🧬","🔬","🔭","📡","📘","📗","📙","📕","📓","📒","📔","📚","📖","🔖",
    "📝","✏️","🖊️","🖋️","✒️","🖍️","📐","📏","🧮","📎","🖇️","📌","📍","✂️","🗃️","🗂️","🗄️",
    "🗑️","🔒","🔓","🔑","🗝️","🔨","🪓","⛏️","🧰","🔧","🪛","🔩","⚙️","🧱","🪜","🧹","🧺","🧻",
    "🧼","🪥","🧽","🛁","🚪","🪟","🪑","🛏️",

    // ❤️ Symbols
    "❤️","🧡","💛","💚","💙","💜","🖤","🤍","🤎","💔","❤️‍🔥","❤️‍🩹","💖","💗","💓","💞","💕","💘",
    "💯","✅","☑️","❌","❗","❓","‼️","⁉️","➕","➖","✖️","➗","🔁","🔄","🎵","🎶","🔔","🔕","📢","📣",
    "🟢","🟡","🟠","🔴","⚫","⚪","🟣","🟤","🔺","🔻","⭐","✨"
)


  // ============ INIT ============
  
  override def init(clients: Seq[UserId]): State =
    State(
      playerOrder = clients.toVector,
      describerIndex = 0,
      scores = clients.map(id => id -> 0).toMap,
      phase = Phase.WaitingToStart,
      secretWord = None,
      hints = List.empty,
      guesses = List.empty,
      timerEndMs = 0L,
      winner = None
    )

  // ============ PROJECT ============
  
  override def project(state: State)(userId: UserId): View =
    ???

  // ============ TRANSITION ============
  
  override def transition(state: State)(userId: UserId, event: Either[Tick, Event]): Try[Seq[Action[State]]] =
    event match
      case Left(tick) => handleTick(state, tick)
      case Right(evt) => handleEvent(state, userId, evt)

  // ============ TICK HANDLER ============
  
  private def handleTick(state: State, tick: Tick): Try[Seq[Action[State]]] =
    state.phase match
      case Phase.Guessing =>
        ???  // Check if timer expired → WaitingForHint or GameEnd
      
      case Phase.GameEnd =>
        ???  // Check if timer expired → start new game
      
      case _ =>
        Success(Seq(Action.Render(state)))

  // ============ EVENT HANDLERS ============
  
  private def handleEvent(state: State, userId: UserId, event: Event): Try[Seq[Action[State]]] =
    event match
      case Event.StartGame =>
        handleStartGame(state, userId)
      case Event.SendFirstHint(secretWord, emoji) =>
        handleSendFirstHint(state, userId, secretWord, emoji)
      case Event.SendHint(emoji) =>
        handleSendHint(state, userId, emoji)
      case Event.SubmitGuess(text) =>
        handleSubmitGuess(state, userId, text)

  private def handleStartGame(state: State, userId: UserId): Try[Seq[Action[State]]] =
    ???

  private def handleSendFirstHint(state: State, userId: UserId, secretWord: Word, emoji: Emoji): Try[Seq[Action[State]]] =
    ???

  private def handleSendHint(state: State, userId: UserId, emoji: Emoji): Try[Seq[Action[State]]] =
    ???

  private def handleSubmitGuess(state: State, userId: UserId, text: String): Try[Seq[Action[State]]] =
    ???

  // ============ HELPERS ============
  
  private def availableEmojis(state: State): Vector[Emoji] =
    AllEmojis.filterNot(state.hints.contains)
  
  private def nextDescriberIndex(state: State, winner: Option[UserId]): Int =
    ???  // winner's index, or round-robin
  
  private def calculatePoints(hintCount: Int): Int =
    ???  // MaxHints - hintCount + 1