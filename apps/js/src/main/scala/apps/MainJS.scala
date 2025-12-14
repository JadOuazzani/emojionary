package apps

import org.scalajs.dom
import scalatags.JsDom.all.*
import cs214.webapp.*
import cs214.webapp.client.*
import cs214.webapp.client.graphics.WebClientAppInstance
import apps.emojionary.{Wire, Event, View, Phase}

object MainJS extends WSClientApp {
  def appId: String = "emojionary"
  def uiId: String = "html"

  def main(args: Array[String]): Unit = {
    WebClient.register(this)
    WebClient.start()
  }

  def init(userId: UserId, sendMessage: ujson.Value => Unit, target: Target): ClientAppInstance = {
    new EmojionaryUIInstance(userId, sendMessage, target)
  }
}

class EmojionaryUIInstance(
    userId: UserId,
    sendMessage: ujson.Value => Unit,
    target: Target
) extends WebClientAppInstance[Event, View](userId, sendMessage, target) {

  override val wire = apps.emojionary.Wire

  // Inject Google Fonts into document head on initialization
  locally {
    val link = dom.document.createElement("link").asInstanceOf[dom.html.Link]
    link.rel = "stylesheet"
    link.href = "https://fonts.googleapis.com/css2?family=Nunito:wght@400;600;700;800&family=Fredoka:wght@400;500;600;700&display=swap"
    dom.document.head.appendChild(link)
  }

  lazy val secretInput: dom.html.Input = {
    input(
      id := "secret-word-input", 
      tpe := "text", 
      placeholder := "Ton mot secret...",
      autofocus := true
    ).render
  }

  lazy val guessInput: dom.html.Input = {
    val el = input(
      id := "guess-input", 
      tpe := "text", 
      placeholder := "Ta réponse...",
      autofocus := true
    ).render
    
    el.onkeydown = (e: dom.KeyboardEvent) => {
      if (e.key == "Enter") submitGuess()
    }
    el
  }

  private var lastHintCount: Int = 0
  private var waitingForNextHint: Boolean = false

  def maintainFocus(el: dom.html.Input): Unit = {
    if (dom.document.activeElement == el) {
      val start = el.selectionStart
      val end = el.selectionEnd
      dom.window.setTimeout(() => {
        el.focus()
        el.setSelectionRange(start, end)
      }, 0)
    }
  }

  override def render(userId: UserId, view: View): Frag = {
    div(cls := "emo-app")(
      if (view.phase == Phase.WaitingToStart) 
        renderStartScreen()
      else if (view.phase == Phase.GameEnd)
        renderEndScreen(userId, view)
      else
        renderActiveGame(userId, view)
    )
  }

  def renderTimer(view: View): Frag = {
    val totalMs = 30000L
    val rawSeconds = (view.remainingTimeMs / 1000L).toInt
    val seconds = if (rawSeconds < 0) 0 else rawSeconds
    val progressPercent = ((view.remainingTimeMs.toDouble / totalMs) * 100).toInt.max(0).min(100)
    val isDanger = seconds <= 5

    if (view.phase == Phase.Guessing)
      div(cls := (if (isDanger) "emo-timer danger" else "emo-timer"))(
        div(cls := "timer-bar", style := s"width: $progressPercent%"),
        span(cls := "timer-text")(s"⏱ ${seconds}s")
      )
    else
      frag()
  }

  def renderStartScreen(): Frag = {
    resetLocalState()
    
    div(cls := "emo-start")(
      div(cls := "start-logo")("🎭"),
      h1(cls := "start-title")("Emojionary"),
      p(cls := "start-subtitle")("Fais deviner un mot avec des emojis !"),

      div(cls := "start-rules")(
        div(cls := "rule")("1️⃣ ", span("Choisis un mot secret")),
        div(cls := "rule")("2️⃣ ", span("Envoie des emojis comme indices")),
        div(cls := "rule")("3️⃣ ", span("Les autres devinent")),
        div(cls := "rule")("4️⃣ ", span("Moins d'indices = plus de points !"))
      ),

      button(cls := "btn-play", onclick := { () => sendEvent(Event.StartGame) })(
        "Jouer 🚀"
      )
    )
  }

  def renderEndScreen(currentUser: UserId, view: View): Frag = {
    val shouldAnimate = !endAnimationPlayed
    endAnimationPlayed = true
    
    div(cls := "emo-end")(
      h1(cls := "end-title")("Partie Terminée !"),
      
      view.winner match {
        case Some(w) => 
          div(cls := (if (shouldAnimate) "winner-box animate" else "winner-box"))(
            div(cls := "trophy")("🏆"),
            div(cls := "winner-label")("Gagnant"),
            div(cls := "winner-name")(if (w == currentUser) "TOI !" else shortId(w))
          )
        case None => 
          div(cls := "no-winner")(
            span("😅"),
            span("Personne n'a trouvé...")
          )
      },
      
      div(cls := "reveal-box")(
        span("Le mot était : "),
        strong(view.secretWord.getOrElse("???"))
      ),
      
      renderScoreBoard(currentUser, view),
      
      button(cls := "btn-play", onclick := { () => sendEvent(Event.StartGame) })(
        "Rejouer 🔄"
      )
    )
  }

  private def shortId(uid: UserId): String =
    if (uid.length <= 8) uid else uid.take(8) + "…"

  def renderScoreBoard(currentUser: UserId, view: View): Frag = {
    val ordered = view.allScores.toSeq.sortBy { case (_, score) => -score }

    div(cls := "emo-scores")(
      div(cls := "scores-title")("📊 Scores"),
      if (ordered.isEmpty)
        div(cls := "scores-empty")("Aucun point")
      else
        div(cls := "scores-list")(
          ordered.zipWithIndex.map { case ((uid, score), idx) =>
            val isYou = uid == currentUser
            val medal = idx match {
              case 0 => "🥇"
              case 1 => "🥈"
              case 2 => "🥉"
              case _ => "  "
            }
            div(cls := (if (isYou) "score-row me" else "score-row"))(
              span(cls := "score-medal")(medal),
              span(cls := "score-name")(if (isYou) "Toi" else shortId(uid)),
              span(cls := "score-pts")(score.toString)
            )
          }*
        )
    )
  }

  def renderActiveGame(currentUser: UserId, view: View): Frag = {
    div(cls := "emo-game")(
      div(cls := "game-side")(
        renderScoreBoard(currentUser, view),
        if (!view.amIDescriber) renderGuessHistory(view) else frag()
      ),

      div(cls := "game-main")(
        renderTimer(view),

        if (view.amIDescriber)
          div(cls := "role-box describer")(
            div(cls := "role-badge")("🎨 Tu fais deviner !"),
            renderDescriberControls(view)
          )
        else
          div(cls := "role-box guesser")(
            div(cls := "role-badge")("🔍 À toi de deviner !"),
            renderGuesserControls(view)
          )
      )
    )
  }

  def renderDescriberControls(view: View): Frag = {
    if (view.hints.isEmpty && lastHintCount > 0) {
      secretInput.value = ""
      lastHintCount = 0
    }

    val isWaitingForGuessers = view.phase == Phase.Guessing

    div(cls := "describer-panel")(
      if (view.hints.isEmpty) {
        maintainFocus(secretInput)
        div(cls := "secret-section")(
          div(cls := "section-label")("🎯 Choisis ton mot secret"),
          secretInput,
          div(cls := "section-hint")("Puis clique sur un emoji")
        )
      } else {
        div(cls := "secret-display")(
          span("Mot : "),
          strong(view.secretWord.getOrElse("???"))
        )
      },

      if (isWaitingForGuessers) {
        div(cls := "status-box waiting")(
          span(cls := "status-icon")("⏳"),
          span(cls := "status-text")("Les joueurs réfléchissent...")
        )
      } else {
        frag(
          if (view.hints.nonEmpty)
            div(cls := "status-box ready")(
              span(cls := "status-icon")("👉"),
              span(cls := "status-text")("Envoie un nouvel emoji !")
            )
          else frag(),

          div(cls := "emoji-board")(
            view.availableEmojis.map { emoji =>
              button(cls := "emoji-btn", onclick := { () => onEmojiClicked(view, emoji) })(emoji)
            }*
          )
        )
      }
    )
  }

  def onEmojiClicked(view: View, emoji: String): Unit = {
    if (view.hints.isEmpty) {
      val word = secretInput.value.trim
      if (word.nonEmpty) {
        sendEvent(Event.SendFirstHint(word, emoji))
      } else {
        dom.window.alert("Entre d'abord un mot secret !")
      }
    } else {
      sendEvent(Event.SendHint(emoji))
    }
  }

  def resetLocalState(): Unit = {
    secretInput.value = ""
    guessInput.value = ""
    lastHintCount = 0
    waitingForNextHint = false
    animatedHintCount = 0
    endAnimationPlayed = false
  }

  /** Track how many hints have been animated to avoid re-animating on timer ticks */
  private var animatedHintCount: Int = 0
  
  /** Track if end screen animation has played */
  private var endAnimationPlayed: Boolean = false

  def renderGuesserControls(view: View): Frag = {
    val currentHintCount = view.hints.length

    if (view.hints.isEmpty) {
      lastHintCount = 0
      waitingForNextHint = false
      guessInput.value = ""
      animatedHintCount = 0
    }

    if (currentHintCount > lastHintCount) {
      waitingForNextHint = false
      lastHintCount = currentHintCount
      guessInput.value = ""
      dom.window.setTimeout(() => guessInput.focus(), 0)
    }

    // Update animated count after rendering
    val hintsToAnimate = animatedHintCount
    if (currentHintCount > animatedHintCount) {
      animatedHintCount = currentHintCount
    }

    val isWaitingForHintPhase = view.phase == Phase.WaitingForHint

    div(cls := "guesser-panel")(
      div(cls := "hints-section")(
        div(cls := "hints-label")(s"💡 Indices (${view.hints.length}/5)"),
        div(cls := "hints-box")(
          if (view.hints.isEmpty)
            span(cls := "hints-wait")("En attente...")
          else
            frag(view.hints.zipWithIndex.map { case (emoji, idx) =>
              // Only animate if this hint is new (idx >= hintsToAnimate)
              val shouldAnimate = idx >= hintsToAnimate
              span(
                cls := (if (shouldAnimate) "hint-emoji animate" else "hint-emoji")
              )(emoji)
            }*)
        )
      ),

      if (view.hints.isEmpty) {
        div(cls := "status-box waiting")(
          span(cls := "status-icon")("⏳"),
          span(cls := "status-text")("Le décrypteur choisit...")
        )
      } else if (isWaitingForHintPhase || waitingForNextHint) {
        div(cls := "status-box blocked")(
          span(cls := "status-icon")("🔒"),
          span(cls := "status-text")("Attends le prochain indice...")
        )
      } else {
        maintainFocus(guessInput)
        div(cls := "guess-form")(
          guessInput,
          button(cls := "btn-guess", onclick := { () => submitGuess() })("Deviner →")
        )
      }
    )
  }

  def renderGuessHistory(view: View): Frag = {
    val errors = view.myWrongGuesses

    div(cls := "emo-history")(
      div(cls := "history-title")("📝 Tes essais"),
      if (errors.isEmpty)
        div(cls := "history-empty")("Aucune erreur 👌")
      else
        div(cls := "history-list")(
          errors.distinct.map { g =>
            div(cls := "history-item")(
              span(cls := "history-x")("✗"),
              span(g)
            )
          }*
        )
    )
  }

  def submitGuess(): Unit = {
    val text = guessInput.value.trim
    if (text.nonEmpty) {
      sendEvent(Event.SubmitGuess(text))
      guessInput.value = ""
      waitingForNextHint = true
    }
  }

  override def css: String = """
  * { box-sizing: border-box; margin: 0; padding: 0; }

  body {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    min-height: 100vh;
    padding: 20px;
    font-family: 'Nunito', 'Segoe UI', system-ui, sans-serif;
    display: flex;
    flex-direction: column;
    align-items: center;
  }

  /* =========================
     MAIN CARD (LANDSCAPE)
     ========================= */
  .emo-app {
    width: min(1100px, 96vw);         /* <-- plus large (paysage) */
    margin: 0;                   /* <-- centré */
    background: #fff;
    border-radius: 24px;
    padding: 26px 28px;               /* un peu + d’air */
    box-shadow: 0 20px 60px rgba(0,0,0,0.3);
  }

  /* ===== START SCREEN ===== */
  .emo-start {
    text-align: center;
    padding: 20px 0;
  }

  .start-logo {
    font-size: 80px;
    margin-bottom: 10px;
    animation: bounce 2s infinite;
  }

  @keyframes bounce {
    0%, 100% { transform: translateY(0); }
    50% { transform: translateY(-15px); }
  }

  .start-title {
    font-family: 'Fredoka', 'Comic Sans MS', cursive;
    font-size: 3.5rem;
    background: linear-gradient(135deg, #667eea, #764ba2);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
    margin-bottom: 8px;
  }

  .start-subtitle {
    color: #666;
    font-size: 1.2rem;
    margin-bottom: 30px;
  }

  .start-rules {
    background: linear-gradient(135deg, #f5f7fa 0%, #e4e8ec 100%);
    border-radius: 16px;
    padding: 24px 32px;
    display: inline-block;
    text-align: left;
    margin-bottom: 30px;
  }

  .rule {
    padding: 8px 0;
    font-size: 1.1rem;
    color: #444;
  }

  .rule span { margin-left: 8px; }

  .btn-play {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: #fff;
    border: none;
    padding: 18px 50px;
    font-size: 1.4rem;
    font-family: 'Fredoka', 'Comic Sans MS', cursive;
    font-weight: 600;
    border-radius: 50px;
    cursor: pointer;
    box-shadow: 0 8px 30px rgba(102, 126, 234, 0.5);
    transition: all 0.3s ease;
  }

  .btn-play:hover {
    transform: translateY(-4px) scale(1.05);
    box-shadow: 0 12px 40px rgba(102, 126, 234, 0.6);
  }

  .btn-play:active {
    transform: translateY(0) scale(0.98);
  }

  /* =========================
     GAME LAYOUT (LANDSCAPE)
     ========================= */
  .emo-game {
    display: grid;
    grid-template-columns: 260px 1fr;   /* <-- sidebar plus large */
    gap: 24px;
    align-items: start;
  }

  .game-side {
    display: flex;
    flex-direction: column;
    gap: 16px;
    min-width: 0;
  }

  .game-main {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 12px;
    min-width: 0;
    overflow: visible;
    width: 100%;
  }

  /* ===== TIMER ===== */
  .emo-timer {
    position: relative;
    width: 160px;         /* un poil plus large */
    height: 40px;
    background: #f0f0f0;
    border-radius: 20px;
    overflow: hidden;
    border: 3px solid #667eea;
    flex-shrink: 0;
  }

  .timer-bar {
    position: absolute;
    left: 0;
    top: 0;
    bottom: 0;
    background: linear-gradient(90deg, #4ade80, #fbbf24);
    transition: width 1s linear;
  }

  .timer-text {
    position: absolute;
    inset: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    font-weight: 700;
    font-size: 1rem;
    color: #333;
    z-index: 1;
  }

  .emo-timer.danger {
    border-color: #ef4444;
    animation: shake 0.5s ease-in-out infinite;
  }

  .emo-timer.danger .timer-bar {
    background: #ef4444;
  }

  .emo-timer.danger .timer-text {
    color: #ef4444;
  }

  @keyframes shake {
    0%, 100% { transform: translateX(0); }
    25% { transform: translateX(-4px); }
    75% { transform: translateX(4px); }
  }

  /* ===== ROLE BOX ===== */
  .role-box {
    width: 100%;
    max-width: 620px;   /* <-- beaucoup plus large (évite l’effet carré) */
    display: flex;
    flex-direction: column;
    align-items: center;
  }

  .role-badge {
    display: inline-block;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: #fff;
    padding: 10px 20px;
    border-radius: 50px;
    font-family: 'Fredoka', 'Comic Sans MS', cursive;
    font-size: 1rem;
    font-weight: 600;
    margin-bottom: 16px;
    text-align: center;
  }

  /* ===== DESCRIBER ===== */
  .describer-panel {
    display: flex;
    flex-direction: column;
    gap: 20px;
    width: 100%;
  }

  .secret-section {
    background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
    border-radius: 16px;
    padding: 20px;
    border: 2px solid #f59e0b;
    width: 100%;
  }

  .section-label {
    font-weight: 700;
    font-size: 1.1rem;
    margin-bottom: 12px;
    color: #92400e;
  }

  .section-hint {
    font-size: 0.9rem;
    color: #a16207;
    margin-top: 8px;
  }

  #secret-word-input, #guess-input {
    width: 100%;
    padding: 14px 18px;
    font-size: 1.1rem;
    border: 2px solid #ddd;
    border-radius: 12px;
    font-family: inherit;
    transition: all 0.2s;
  }

  #secret-word-input:focus, #guess-input:focus {
    outline: none;
    border-color: #667eea;
    box-shadow: 0 0 0 4px rgba(102, 126, 234, 0.2);
  }

  .secret-display {
    background: #fef3c7;
    padding: 12px 20px;
    border-radius: 50px;
    display: inline-block;
    color: #92400e;
    font-size: 1.1rem;
  }

  .secret-display strong {
    color: #78350f;
    margin-left: 4px;
  }

  /* ===== STATUS BOX ===== */
  .status-box {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 16px 24px;
    border-radius: 12px;
    font-size: 1.1rem;
    width: 100%;
  }

  .status-box.waiting {
    background: linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%);
    color: #1e40af;
  }

  .status-box.ready {
    background: linear-gradient(135deg, #d1fae5 0%, #a7f3d0 100%);
    color: #065f46;
  }

  .status-box.blocked {
    background: linear-gradient(135deg, #fed7aa 0%, #fdba74 100%);
    color: #9a3412;
  }

  .status-icon { font-size: 1.5rem; }
  .status-text { font-weight: 600; }

  /* ===== EMOJI BOARD ===== */
  .emoji-board {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(44px, 1fr)); /* un poil + grand */
    gap: 8px;
    background: #f8fafc;
    padding: 14px;
    border-radius: 16px;
    max-height: 320px;  /* <-- + haut */
    overflow-y: auto;
    border: 1px solid #e2e8f0;
    width: 100%;
  }

  .emoji-btn {
    font-size: 1.5rem;
    background: #fff;
    border: 1px solid #e2e8f0;
    border-radius: 10px;
    padding: 7px;
    cursor: pointer;
    transition: all 0.2s cubic-bezier(0.34, 1.56, 0.64, 1);
  }

  .emoji-btn:hover {
    transform: scale(1.25);
    border-color: #667eea;
    background: #f0f4ff;
    z-index: 10;
    box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
  }

  /* ===== GUESSER ===== */
  .guesser-panel {
    display: flex;
    flex-direction: column;
    gap: 16px;
    width: 100%;
    align-items: center;
  }

  .hints-section {
    text-align: center;
    width: 100%;
  }

  .hints-label {
    font-weight: 700;
    color: #64748b;
    margin-bottom: 12px;
    font-size: 1rem;
  }

  .hints-box {
    background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
    border: 3px solid #e2e8f0;
    border-radius: 16px;
    padding: 16px;
    min-height: 70px;
    display: flex;
    flex-wrap: wrap;
    justify-content: center;
    align-items: center;
    gap: 6px;
    max-width: 520px;   /* <-- plus large */
    margin: 0 auto;
  }

  .hints-wait {
    color: #94a3b8;
    font-style: italic;
    font-size: 1rem;
    text-align: center;
    width: 100%;
  }

  .hint-emoji {
    font-size: 2rem;
    display: flex;
    align-items: center;
    justify-content: center;
    width: 52px;
    height: 52px;
    flex-shrink: 0;
  }

  .hint-emoji.animate {
    animation: dropIn 0.5s cubic-bezier(0.34, 1.56, 0.64, 1) backwards;
  }

  @keyframes dropIn {
    from {
      opacity: 0;
      transform: translateY(-20px) scale(1.3) rotate(10deg);
    }
    to {
      opacity: 1;
      transform: translateY(0) scale(1) rotate(0);
    }
  }

  .guess-form {
    display: flex;
    flex-direction: row;
    gap: 10px;
    justify-content: center;
    align-items: center;
    margin-top: 8px;
    width: 100%;
    max-width: 520px;   /* <-- plus large */
  }

  .guess-form #guess-input {
    flex: 1;
    min-width: 0;
    padding: 10px 14px;
    font-size: 1rem;
  }

  .btn-guess {
    background: linear-gradient(135deg, #10b981 0%, #059669 100%);
    color: #fff;
    border: none;
    padding: 10px 16px;
    font-size: 0.95rem;
    font-weight: 700;
    border-radius: 50px;
    cursor: pointer;
    box-shadow: 0 4px 15px rgba(16, 185, 129, 0.4);
    transition: all 0.2s;
    white-space: nowrap;
    flex-shrink: 0;
  }

  .btn-guess:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 20px rgba(16, 185, 129, 0.5);
  }

  /* ===== SCORES ===== */
  .emo-scores {
    background: #fff;
    border-radius: 16px;
    padding: 16px;
    border: 2px solid #e2e8f0;
  }

  .scores-title {
    font-family: 'Fredoka', 'Comic Sans MS', cursive;
    font-weight: 600;
    font-size: 1.1rem;
    padding-bottom: 12px;
    border-bottom: 2px solid #f1f5f9;
    margin-bottom: 12px;
  }

  .scores-empty {
    color: #94a3b8;
    text-align: center;
    font-size: 0.9rem;
    padding: 8px 0;
  }

  .scores-list {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  .score-row {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 10px 12px;
    background: #f8fafc;
    border-radius: 10px;
  }

  .score-row.me {
    background: linear-gradient(135deg, #d1fae5 0%, #a7f3d0 100%);
    font-weight: 700;
  }

  .score-medal { font-size: 1.1rem; width: 24px; }
  .score-name { flex: 1; color: #334155; }

  .score-pts {
    font-family: 'Fredoka', 'Comic Sans MS', cursive;
    font-weight: 700;
    font-size: 1.2rem;
    color: #667eea;
  }

  /* ===== HISTORY ===== */
  .emo-history {
    background: #fff;
    border-radius: 16px;
    padding: 16px;
    border: 2px solid #e2e8f0;
  }

  .history-title {
    font-family: 'Fredoka', 'Comic Sans MS', cursive;
    font-weight: 600;
    font-size: 1rem;
    padding-bottom: 10px;
    border-bottom: 2px solid #f1f5f9;
    margin-bottom: 10px;
  }

  .history-empty {
    color: #94a3b8;
    font-size: 0.85rem;
  }

  .history-list {
    display: flex;
    flex-direction: column;
    gap: 6px;
    max-height: 120px;
    overflow-y: auto;
  }

  .history-item {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 10px;
    background: #fef2f2;
    border-radius: 8px;
    font-size: 0.9rem;
  }

  .history-x {
    color: #ef4444;
    font-weight: 700;
  }

  /* ===== END SCREEN ===== */
  .emo-end {
    text-align: center;
    padding: 20px 0;
  }

  .end-title {
    font-family: 'Fredoka', 'Comic Sans MS', cursive;
    font-size: 2.5rem;
    color: #1e293b;
    margin-bottom: 24px;
  }

  .winner-box {
    display: inline-flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;
    background: linear-gradient(135deg, #fef08a 0%, #fde047 100%);
    padding: 28px 48px;
    border-radius: 20px;
    box-shadow: 0 8px 30px rgba(253, 224, 71, 0.5);
    margin-bottom: 24px;
  }

  .winner-box.animate {
    animation: pop 0.6s cubic-bezier(0.34, 1.56, 0.64, 1);
  }

  @keyframes pop {
    0% { transform: scale(0) rotate(-10deg); }
    100% { transform: scale(1) rotate(0); }
  }

  .trophy { font-size: 3.5rem; }

  .winner-label {
    font-size: 0.9rem;
    color: #a16207;
    text-transform: uppercase;
    letter-spacing: 2px;
    font-weight: 600;
  }

  .winner-name {
    font-family: 'Fredoka', 'Comic Sans MS', cursive;
    font-size: 1.8rem;
    font-weight: 700;
    color: #78350f;
  }

  .no-winner {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;
    color: #64748b;
    font-size: 1.2rem;
    margin-bottom: 24px;
  }

  .no-winner span:first-child { font-size: 3rem; }

  .reveal-box {
    background: #f1f5f9;
    padding: 12px 24px;
    border-radius: 50px;
    display: inline-block;
    font-size: 1.1rem;
    color: #475569;
    margin-bottom: 24px;
  }

  .reveal-box strong {
    color: #667eea;
    font-size: 1.3rem;
    margin-left: 6px;
  }

  .emo-end .emo-scores {
    display: inline-block;
    min-width: 280px;
    margin-bottom: 24px;
  }

  /* ===== RESPONSIVE ===== */
  @media (max-width: 700px) {
    body { padding: 0; }

    .emo-app {
      width: 100%;
      border-radius: 0;
      padding: 20px 16px;
      min-height: 100vh;
    }

    .emo-game {
      grid-template-columns: 1fr;
    }

    .game-side {
      order: 2;
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 12px;
    }

    .game-main { order: 1; }

    .start-title { font-size: 2.5rem; }
    .start-logo { font-size: 60px; }

    .hints-box {
      max-width: 280px;
    }

    .hint-emoji { 
      font-size: 1.6rem; 
      width: 44px;
      height: 44px;
    }

    .guess-form {
      flex-direction: column;
      max-width: 280px;
    }

    .guess-form #guess-input {
      width: 100%;
    }

    .btn-guess { 
      width: 100%; 
    }

    .emoji-board {
      grid-template-columns: repeat(auto-fill, minmax(44px, 1fr));
    }
  }
"""
}