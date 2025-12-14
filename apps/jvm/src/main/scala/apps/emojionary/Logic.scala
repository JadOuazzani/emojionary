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
    "😀","😃","😄","😁","😆","😅","🤣","😂","🙂","🙃","😉","😊","😇","🥰","😍","🤩","😘","😗","😚","😙",
    "😋","😛","😜","🤪","😝","🤑","🤗","🤭","🤔","🤐","🤨","😐","😑","😶","🫥","😶‍🌫️","🙄","😏",
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
  )


  // ============ INIT ============
  
  override def init(clients: Seq[UserId]): State =
    require(clients.nonEmpty, "Cannot start game with no players")
    require(clients.distinct.size == clients.size, "Duplicate player IDs not allowed")
    
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
    ).ensuring { s =>
      s.playerOrder.size == clients.size &&
      s.scores.size == clients.size &&
      s.scores.values.forall(_ == 0) &&
      s.phase == Phase.WaitingToStart &&
      s.hints.isEmpty &&
      s.guesses.isEmpty &&
      s.secretWord.isEmpty &&
      s.winner.isEmpty
    }

  // ============ PROJECT ============
  
  override def project(state: State)(userId: UserId): View =
    require(state.playerOrder.contains(userId), s"Unknown player: $userId")
    
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
    ).ensuring { v =>
      v.remainingTimeMs >= 0 &&
      v.hints.size <= Rules.MaxHints &&
      v.allScores.values.forall(_ >= 0) &&
      (v.amIDescriber || v.availableEmojis.isEmpty) &&
      v.myWrongGuesses.forall(g => !state.secretWord.contains(g.toLowerCase))
    }

  // ============ TRANSITION ============
  
  override def transition(state: State)(userId: UserId, event: Either[Tick, Event]): Try[Seq[Action[State]]] =
    event match
      case Left(tick) => 
        // Tick events come from system clock, userId is not a player
        handleTick(state, tick)
      case Right(evt) => 
        require(state.playerOrder.contains(userId), s"Unknown player: $userId")
        handleEvent(state, userId, evt)

  // ============ TICK HANDLER ============
  
  private def handleTick(state: State, tick: Tick): Try[Seq[Action[State]]] =
    require(tick.systemMillis >= 0, "Invalid tick timestamp")
    
    state.phase match
      case Phase.Guessing if tick.systemMillis >= state.timerEndMs =>
        // Timer expired
        if state.hintCount >= Rules.MaxHints then
          // Max hints reached, game over with no winner
          val newState = state.copy(
            phase = Phase.GameEnd,
            timerEndMs = tick.systemMillis + Rules.GameEndDurationMs
          ).ensuring(s => s.phase == Phase.GameEnd && s.winner.isEmpty)
          Success(Seq(Action.Render(newState)))
        else
          // Go to next hint
          val newState = state.copy(phase = Phase.WaitingForHint)
            .ensuring(_.phase == Phase.WaitingForHint)
          Success(Seq(Action.Render(newState)))
      
      case Phase.GameEnd if tick.systemMillis >= state.timerEndMs =>
        // Auto-continue to new game (Timer-based)
        val newState = startNewRound(state)
        Success(Seq(Action.Render(newState)))
      
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
    if state.phase == Phase.WaitingToStart then
        if !state.canStart then Failure(IllegalStateException("Need at least 2 players"))
        else
            val newState = state.copy(phase = Phase.WaitingForHint)
              .ensuring(_.phase == Phase.WaitingForHint)
            Success(Seq(Action.Render(newState)))
    else if state.phase == Phase.GameEnd then
        // Redémarrage manuel via bouton "Rejouer" avant la fin du timer
        val newState = startNewRound(state)
        Success(Seq(Action.Render(newState)))
    else
        Failure(IllegalStateException("Game already started"))

  // Helper pour démarrer un nouveau round (utilisé par timer et bouton manuel)
  private def startNewRound(state: State): State =
    require(state.playerOrder.nonEmpty, "Cannot start round with no players")
    
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
    ).ensuring { s =>
      s.playerOrder == state.playerOrder &&
      s.scores == state.scores &&
      s.phase == Phase.WaitingForHint &&
      s.hints.isEmpty &&
      s.guesses.isEmpty &&
      s.secretWord.isEmpty &&
      s.winner.isEmpty &&
      s.describerIndex >= 0 &&
      s.describerIndex < s.playerOrder.size
    }

  private def handleSendFirstHint(state: State, userId: UserId, secretWord: Word, emoji: Emoji): Try[Seq[Action[State]]] =
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
      val newState = state.copy(
        phase = Phase.Guessing,
        secretWord = Some(secretWord.trim.toLowerCase),
        hints = List(emoji),
        currentHintGuessers = Set.empty,
        timerEndMs = now + Rules.HintDurationMs
      ).ensuring { s =>
        s.phase == Phase.Guessing &&
        s.hints.size == 1 &&
        s.secretWord.isDefined &&
        s.currentHintGuessers.isEmpty
      }
      Success(Seq(Action.Render(newState)))

  private def handleSendHint(state: State, userId: UserId, emoji: Emoji): Try[Seq[Action[State]]] =
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
      val newState = state.copy(
        phase = Phase.Guessing,
        hints = state.hints :+ emoji,
        currentHintGuessers = Set.empty,
        timerEndMs = now + Rules.HintDurationMs
      ).ensuring { s =>
        s.phase == Phase.Guessing &&
        s.hints.size == state.hints.size + 1 &&
        s.hints.last == emoji &&
        s.currentHintGuessers.isEmpty
      }
      Success(Seq(Action.Render(newState)))

  private def handleSubmitGuess(state: State, userId: UserId, text: String): Try[Seq[Action[State]]] =
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
        val newScores = state.scores.updated(userId, state.scores.getOrElse(userId, 0) + points)
        val now = System.currentTimeMillis()
        val newState = state.copy(
          phase = Phase.GameEnd,
          guesses = state.guesses :+ entry,
          scores = newScores,
          winner = Some(userId),
          timerEndMs = now + Rules.GameEndDurationMs
        ).ensuring { s =>
          s.phase == Phase.GameEnd &&
          s.winner.contains(userId) &&
          s.scores(userId) > state.scores(userId) &&
          s.guesses.size == state.guesses.size + 1
        }
        Success(Seq(Action.Render(newState)))
      else
        val allGuessers: Set[UserId] = 
          state.playerOrder.filterNot(uid => state.isDescriber(uid)).toSet

        val updatedGuessers: Set[UserId] = 
          state.currentHintGuessers + userId

        // tous les guessers ont tenté leur chance ?
        val everyoneAnswered = updatedGuessers == allGuessers

        val now = System.currentTimeMillis()

        val newState = 
          if everyoneAnswered then
            // si on est déjà au nombre max d'indices -> fin de partie sans gagnant
            if state.hintCount >= Rules.MaxHints then
              state.copy(
                phase = Phase.GameEnd,
                guesses = state.guesses :+ entry,
                currentHintGuessers = updatedGuessers,
                winner = None,
                timerEndMs = now + Rules.GameEndDurationMs
              ).ensuring { s =>
                s.phase == Phase.GameEnd &&
                s.winner.isEmpty &&
                s.guesses.size == state.guesses.size + 1
              }
            else
              // sinon, on repasse en WaitingForHint pour permettre un nouvel emoji
              state.copy(
                phase = Phase.WaitingForHint,
                guesses = state.guesses :+ entry,
                currentHintGuessers = updatedGuessers,
                timerEndMs = 0L
              ).ensuring { s =>
                s.phase == Phase.WaitingForHint &&
                s.guesses.size == state.guesses.size + 1
              }
          else
            // il reste encore des joueurs qui n'ont pas répondu → on reste en Guessing
            state.copy(
              guesses = state.guesses :+ entry,
              currentHintGuessers = updatedGuessers
            ).ensuring { s =>
              s.phase == Phase.Guessing &&
              s.guesses.size == state.guesses.size + 1 &&
              s.currentHintGuessers.contains(userId)
            }
        Success(Seq(Action.Render(newState)))

  // ============ HELPERS ============
  
  private def availableEmojis(state: State): List[Emoji] =
    require(state.hints != null, "Hints list cannot be null")
    AllEmojis.filterNot(state.hints.contains).distinct.toList
      .ensuring(result => result.forall(e => !state.hints.contains(e)))
  
  private def nextDescriberIndex(state: State, winner: Option[UserId]): Int =
    require(state.playerOrder.nonEmpty, "Player order cannot be empty")
    
    val result = winner match
      case Some(winnerId) =>
        val idx = state.playerOrder.indexOf(winnerId)
        require(idx >= 0, s"Winner $winnerId not found in player order")
        idx
      case None =>
        (state.describerIndex + 1) % state.playerOrder.size
    result.ensuring(idx => idx >= 0 && idx < state.playerOrder.size)
  
  private def calculatePoints(hintCount: Int): Int =
    require(hintCount >= 1, s"Hint count must be at least 1, got $hintCount")
    require(hintCount <= Rules.MaxHints, s"Hint count cannot exceed ${Rules.MaxHints}, got $hintCount")
    (Rules.MaxHints - hintCount + 1).ensuring(pts => pts >= 1 && pts <= Rules.MaxHints)