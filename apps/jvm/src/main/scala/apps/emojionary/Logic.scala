package apps.emojionary

import cs214.webapp.*
import cs214.webapp.server.ClockDrivenStateMachine
import scala.util.{Try, Success, Failure}

class Logic extends ClockDrivenStateMachine[Event, State, View]:

  // ============ CONFIGURATION ============
  
  override val clockPeriodMs: Int = 1000
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
    "😀","😃","😄","😁","😆","😅","🤣","😂","🙂","🙃","😊","😇","🥰","😍","🤩","😘","😗","😚","😙",
    "😋","🤪","🤑","🤗","🤭","🤔","🤨","😐","😑","😶","🫥","😶‍🌫️","🙄","😏",
    "😣","😥","😮","🤐","😯","😪","😫","🥱","😴","😌","😛","😜","😝","🤤","😓","😔","😕","🫤","🙃",
    "😖","😞","😟","😤","😢","😭","😦","😧","😨","😩","😬","😰","😱","🥵","🥶","😳","🤯","😵","😵‍💫",
    "🥴","😲","🤠","🥳","😎","🤓","🧐","😒","😡","😠","🤬","😷","🤒","🤕","🤢","🤮","🤧","😇","🤥",
    "😈","👿","💀","☠️","👻","👽","🤖","💩",
    // 👋 Gestures / hands
    "👋","🤚","✋","🖐️","👌","🤌","🤏","✌️","🤞","🤟","🤘","🤙","👈","👉","👆","👇","☝️","👍","👎",
    "✊","👊","🤛","🤜","👏","🙌","👐","🤲","🤝","🙏",
    // 🧍 People & roles
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
    "🍲","🥣","🥗","🍿","🧈","🧂","🥫","🍝","🍜","🍛","🍣","🍱","🥟","🍤","🍙","🍚","🍘","🍥",
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
    // 🧰 Objects
    "⌛","⏳","⏰","⏱️","📱","💻","🖥️","⌨️","🖱️","🖨️","📷","📸","🎥","📺","📡","🔋","🔌","💡","🔦",
    "🕯️","🧯","🧲","🧪","🧫","🧬","🔬","🔭","📡","📘","📗","📙","📕","📓","📒","📔","📚","📖","🔖",
    "📝","✏️","🖊️","🖋️","✒️","🖍️","📐","📏","🧮","📎","🖇️","📌","📍","✂️","🗃️","🗂️","🗄️",
    "🗑️","🔒","🔓","🔑","🗝️","🔨","🪓","⛏️","🧰","🔧","🪛","🔩","⚙️","🧱","🪜","🧹","🧺","🧻",
    "🧼","🪥","🧽","🛁","🚪","🪟","🪑","🛏️",
    // ❤️ Symbols
    "❤️","🧡","💛","💚","💙","💜","🖤","🤍","🤎","💔","❤️‍🔥","❤️‍🩹","💖","💗","💓","💞","💕","💘",
    "💯","✅","☑️","❌","❗","❓","‼️","⁉️","➕","➖","✖️","➗","🔁","🔄","🎵","🎶","🔔","🔕","📢","📣",
    "🟢","🟡","🟠","🔴","⚫","⚪","🟣","🟤","🔺","🔻","⭐","✨"
  ).distinct

  // Validate emoji list at class load time
  require(AllEmojis.nonEmpty, "AllEmojis must not be empty")
  require(AllEmojis.distinct == AllEmojis, "AllEmojis must not contain duplicates")
  require(AllEmojis.forall(_.nonEmpty), "All emojis must be non-empty strings")

  // ============ INIT ============
  
  override def init(clients: Seq[UserId]): State =
    require(clients.nonEmpty, "Must have at least one client")
    require(clients.distinct == clients, "Clients must be unique")
    require(clients.forall(_.nonEmpty), "Client IDs must not be empty")
    
    State(
      playerOrder = clients.toVector,
      describerIndex = 0,
      scores = clients.map(id => id -> 0).toMap,
      phase = Phase.WaitingToStart,
      secretWord = None,
      hints = List.empty,
      guesses = List.empty,
      currentHintGuessers = Set.empty,
      timerEndMs = 0L,
      winner = None
    ).ensuring(s => 
      s.playerOrder == clients.toVector &&
      s.phase == Phase.WaitingToStart &&
      s.scores.values.forall(_ == 0),
      "Initial state must have correct player order, WaitingToStart phase, and zero scores"
    )

  // ============ PROJECT ============
  
  override def project(state: State)(userId: UserId): View =
    require(state.playerOrder.contains(userId), 
      s"User $userId must be a player in the game")
    
    val now = System.currentTimeMillis()
    val remaining = math.max(0L, state.timerEndMs - now)
    val amIDescriber = state.isDescriber(userId)
  
    // Describer sees word during game, everyone sees at GameEnd
    val visibleWord = state.phase match
      case Phase.WaitingForHint | Phase.Guessing if amIDescriber => state.secretWord
      case Phase.GameEnd => state.secretWord
      case _ => None
  
    // Only Describer sees available emojis, and only when picking
    val visibleEmojis = state.phase match
      case Phase.WaitingForHint if amIDescriber => availableEmojis(state)
      case _ => List.empty

    val allGuessesText: List[String] = state.guesses.map(_.text)

    // Errors history of THIS player only
    val myWrongGuesses: List[String] =
      state.guesses.collect {
        case GuessEntry(player, text, correct)
            if player == userId && !correct =>
          text
      }
  
    View(
      phase = state.phase,
      amIDescriber = amIDescriber,
      allScores = state.scores,
      hints = state.hints,
      guesses = allGuessesText,
      remainingTimeMs = remaining,
      secretWord = visibleWord,
      winner = state.winner,
      availableEmojis = visibleEmojis,
      myWrongGuesses = myWrongGuesses
    ).ensuring(v =>
      v.phase == state.phase &&
      v.amIDescriber == state.isDescriber(userId) &&
      v.hints == state.hints &&
      v.remainingTimeMs >= 0,
      "View must accurately reflect state for this user"
    )

  // ============ TRANSITION ============
  
  override def transition(state: State)(userId: UserId, event: Either[Tick, Event]): Try[Seq[Action[State]]] =
    require(state.playerOrder.contains(userId) || event.isLeft, 
      s"User $userId must be a player (unless tick event)")
    
    event match
      case Left(tick) => handleTick(state, tick)
      case Right(evt) => handleEvent(state, userId, evt)

  // ============ TICK HANDLER ============
  
  private def handleTick(state: State, tick: Tick): Try[Seq[Action[State]]] =
    require(tick.systemMillis >= 0, "Tick time must be non-negative")
    
    state.phase match
      case Phase.Guessing if tick.systemMillis >= state.timerEndMs =>
        // Timer expired
        if state.hintCount >= Rules.MaxHints then
          // Max hints reached, game over with no winner
          val newState = state.copy(
            phase = Phase.GameEnd,
            timerEndMs = tick.systemMillis + Rules.GameEndDurationMs
          )
          Success(Seq(Action.Render(newState)))
            .ensuring(_.get.nonEmpty, "Must produce at least one action")
        else
          // Go to next hint
          val newState = state.copy(phase = Phase.WaitingForHint)
          Success(Seq(Action.Render(newState)))
            .ensuring(_.get.head match
              case Action.Render(s) => s.phase == Phase.WaitingForHint
              case _ => false
            , "Must transition to WaitingForHint phase")
      
      case Phase.GameEnd if tick.systemMillis >= state.timerEndMs =>
        // Auto-continue to new game (Timer-based)
        val newState = startNewRound(state)
        Success(Seq(Action.Render(newState)))
          .ensuring(_.get.head match
            case Action.Render(s) => s.phase == Phase.WaitingForHint
            case _ => false
          , "New round must start in WaitingForHint phase")
      
      case _ =>
        Success(Seq(Action.Render(state)))

  // ============ EVENT HANDLERS ============
  
  private def handleEvent(state: State, userId: UserId, event: Event): Try[Seq[Action[State]]] =
    require(state.playerOrder.contains(userId), s"User $userId must be a player")
    
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
    require(state.playerOrder.contains(userId), s"User $userId must be a player")
    
    if state.phase == Phase.WaitingToStart then
      if !state.canStart then 
        Failure(IllegalStateException("Need at least 2 players"))
      else
        val newState = state.copy(phase = Phase.WaitingForHint)
        Success(Seq(Action.Render(newState)))
          .ensuring(_.get.head match
            case Action.Render(s) => s.phase == Phase.WaitingForHint
            case _ => false
          , "Game must transition to WaitingForHint")
    else if state.phase == Phase.GameEnd then
      // Manual restart via "Rejouer" button before timer ends
      val newState = startNewRound(state)
      Success(Seq(Action.Render(newState)))
        .ensuring(_.get.head match
          case Action.Render(s) => 
            s.phase == Phase.WaitingForHint && 
            s.hints.isEmpty && 
            s.secretWord.isEmpty
          case _ => false
        , "New round must reset game state")
    else
      Failure(IllegalStateException("Game already started"))

  // Helper to start a new round (used by timer and manual button)
  private def startNewRound(state: State): State =
    require(state.phase == Phase.GameEnd || state.phase == Phase.WaitingToStart,
      "Can only start new round from GameEnd or WaitingToStart")
    
    val newDescriberIdx = nextDescriberIndex(state, state.winner)
    
    State(
      playerOrder = state.playerOrder,
      describerIndex = newDescriberIdx,
      scores = state.scores,
      phase = Phase.WaitingForHint,
      secretWord = None,
      hints = List.empty,
      guesses = List.empty,
      currentHintGuessers = Set.empty,
      timerEndMs = 0L,
      winner = None
    ).ensuring(s =>
      s.playerOrder == state.playerOrder &&
      s.scores == state.scores &&
      s.phase == Phase.WaitingForHint &&
      s.hints.isEmpty &&
      s.secretWord.isEmpty &&
      s.winner.isEmpty,
      "New round must preserve players and scores, reset everything else"
    )

  private def handleSendFirstHint(state: State, userId: UserId, secretWord: Word, emoji: Emoji): Try[Seq[Action[State]]] =
    require(state.playerOrder.contains(userId), s"User $userId must be a player")
    require(secretWord != null, "Secret word cannot be null")
    require(emoji != null, "Emoji cannot be null")
    
    if state.phase != Phase.WaitingForHint then
      Failure(IllegalStateException("Not waiting for hint"))
    else if !state.isDescriber(userId) then
      Failure(IllegalStateException("Only Describer can send hints"))
    else if state.hints.nonEmpty then
      Failure(IllegalStateException("Use SendHint for subsequent hints"))
    else if !AllEmojis.contains(emoji) then
      Failure(IllegalArgumentException("Invalid emoji"))
    else if secretWord.trim.isEmpty then
      Failure(IllegalArgumentException("Secret word cannot be empty"))
    else
      val now = System.currentTimeMillis()
      val normalizedWord = secretWord.trim.toLowerCase
      val newState = state.copy(
        phase = Phase.Guessing,
        secretWord = Some(normalizedWord),
        hints = List(emoji),
        currentHintGuessers = Set.empty,
        timerEndMs = now + Rules.HintDurationMs
      )
      Success(Seq(Action.Render(newState)))
        .ensuring(_.get.head match
          case Action.Render(s) => 
            s.phase == Phase.Guessing &&
            s.secretWord.contains(normalizedWord) &&
            s.hints == List(emoji) &&
            s.hintCount == 1
          case _ => false
        , "First hint must set up guessing phase correctly")

  private def handleSendHint(state: State, userId: UserId, emoji: Emoji): Try[Seq[Action[State]]] =
    require(state.playerOrder.contains(userId), s"User $userId must be a player")
    require(emoji != null, "Emoji cannot be null")
    
    if state.phase != Phase.WaitingForHint then
      Failure(IllegalStateException("Not waiting for hint"))
    else if !state.isDescriber(userId) then
      Failure(IllegalStateException("Only Describer can send hints"))
    else if state.hints.isEmpty then
      Failure(IllegalStateException("Use SendFirstHint for first hint"))
    else if !AllEmojis.contains(emoji) then
      Failure(IllegalArgumentException("Invalid emoji"))
    else if state.hints.contains(emoji) then
      Failure(IllegalArgumentException("Emoji already used"))
    else
      val now = System.currentTimeMillis()
      val previousHintCount = state.hintCount
      val newState = state.copy(
        phase = Phase.Guessing,
        hints = state.hints :+ emoji,
        currentHintGuessers = Set.empty,
        timerEndMs = now + Rules.HintDurationMs
      )
      Success(Seq(Action.Render(newState)))
        .ensuring(_.get.head match
          case Action.Render(s) => 
            s.phase == Phase.Guessing &&
            s.hintCount == previousHintCount + 1 &&
            s.hints.last == emoji
          case _ => false
        , "Additional hint must increment hint count and append emoji")

  private def handleSubmitGuess(state: State, userId: UserId, text: String): Try[Seq[Action[State]]] =
    require(state.playerOrder.contains(userId), s"User $userId must be a player")
    require(text != null, "Guess text cannot be null")
    
    if state.phase != Phase.Guessing then
      Failure(IllegalStateException("Not in guessing phase"))
    else if !state.isGuesser(userId) then
      Failure(IllegalStateException("Describer cannot guess"))
    else
      val normalizedGuess = text.trim.toLowerCase
      val isCorrect = state.secretWord.exists(_ == normalizedGuess)
      
      val entry = GuessEntry(
        player = userId,
        text = text.trim,
        correct = isCorrect
      )

      if isCorrect then
        val points = calculatePoints(state.hintCount)
        val previousScore = state.scores.getOrElse(userId, 0)
        val newScores = state.scores.updated(userId, previousScore + points)
        val now = System.currentTimeMillis()
        val newState = state.copy(
          phase = Phase.GameEnd,
          guesses = state.guesses :+ entry,
          scores = newScores,
          winner = Some(userId),
          timerEndMs = now + Rules.GameEndDurationMs
        )
        Success(Seq(Action.Render(newState)))
          .ensuring(_.get.head match
            case Action.Render(s) => 
              s.phase == Phase.GameEnd &&
              s.winner.contains(userId) &&
              s.scores(userId) == previousScore + points
            case _ => false
          , "Correct guess must end game with winner and updated score")
      else
        val allGuessers: Set[UserId] = 
          state.playerOrder.filterNot(uid => state.isDescriber(uid)).toSet

        val updatedGuessers: Set[UserId] = 
          state.currentHintGuessers + userId

        // All guessers have tried?
        val everyoneAnswered = updatedGuessers == allGuessers

        val now = System.currentTimeMillis()

        val newState = 
          if everyoneAnswered then
            // If already at max hints -> game over with no winner
            if state.hintCount >= Rules.MaxHints then
              state.copy(
                phase = Phase.GameEnd,
                guesses = state.guesses :+ entry,
                currentHintGuessers = updatedGuessers,
                winner = None,
                timerEndMs = now + Rules.GameEndDurationMs
              )
            else
              // Otherwise, go back to WaitingForHint for new emoji
              state.copy(
                phase = Phase.WaitingForHint,
                guesses = state.guesses :+ entry,
                currentHintGuessers = updatedGuessers,
                timerEndMs = 0L
              )
          else
            // Still players who haven't answered → stay in Guessing
            state.copy(
              guesses = state.guesses :+ entry,
              currentHintGuessers = updatedGuessers
            )
        
        Success(Seq(Action.Render(newState)))
          .ensuring(_.get.head match
            case Action.Render(s) => 
              s.guesses.size == state.guesses.size + 1 &&
              s.currentHintGuessers.contains(userId)
            case _ => false
          , "Wrong guess must be recorded and guesser must be tracked")

  // ============ HELPERS ============
  
  private def availableEmojis(state: State): List[Emoji] =
    AllEmojis.filterNot(state.hints.contains).toList
      .ensuring(result => 
        result.intersect(state.hints).isEmpty,
        "Available emojis must not include already used hints"
      )
  
  private def nextDescriberIndex(state: State, winner: Option[UserId]): Int =
    require(state.playerOrder.nonEmpty, "Player order must not be empty")
    
    val result = winner match
      case Some(winnerId) =>
        val idx = state.playerOrder.indexOf(winnerId)
        require(idx >= 0, s"Winner $winnerId must be in player order")
        idx
      case None =>
        (state.describerIndex + 1) % state.playerOrder.size
    
    result.ensuring(r => 
      r >= 0 && r < state.playerOrder.size,
      s"Next describer index $result must be valid"
    )
  
  private def calculatePoints(hintCount: Int): Int =
    require(hintCount >= 1 && hintCount <= Rules.MaxHints, 
      s"Hint count $hintCount must be between 1 and ${Rules.MaxHints}")
    
    val points = Rules.MaxHints - hintCount + 1
    
    points.ensuring(p => 
      p >= 1 && p <= Rules.MaxHints
    )