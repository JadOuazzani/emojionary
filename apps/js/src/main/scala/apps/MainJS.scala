package apps

import org.scalajs.dom
import scalatags.JsDom.all.*
import cs214.webapp.*
import cs214.webapp.client.*
import cs214.webapp.client.graphics.WebClientAppInstance
import apps.emojionary.{Wire, Event, View, Phase}

/**
 * The main entry point for the Emojionary client application.
 * It registers the application with the WebClient framework.
 */
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

/**
 * The specific UI instance for Emojionary.
 * Handles rendering the view based on the game state and processing user interactions.
 *
 * @param userId      The ID of the current user.
 * @param sendMessage Function to send events to the server.
 * @param target      The rendering target (container).
 */
class EmojionaryUIInstance(
    userId: UserId,
    sendMessage: ujson.Value => Unit,
    target: Target
) extends WebClientAppInstance[Event, View](userId, sendMessage, target) {

  override val wire = apps.emojionary.Wire

  /**
   * Persistent input element for the secret word.
   * Defined as a lazy val to ensure the DOM element remains the same instance
   * across multiple `render` calls, preventing focus loss.
   */
  lazy val secretInput: dom.html.Input = {
    input(
      id := "secret-word-input", 
      tpe := "text", 
      placeholder := "Ex: Pizza...",
      autofocus := true
    ).render
  }

  /**
   * Persistent input element for guessing.
   * Kept persistent to maintain text and focus during UI updates (e.g., timer ticks).
   */
  lazy val guessInput: dom.html.Input = {
    val el = input(
      id := "guess-input", 
      tpe := "text", 
      placeholder := "Réponse...",
      autofocus := true
    ).render
    
    el.onkeydown = (e: dom.KeyboardEvent) => {
      if (e.key == "Enter") submitGuess()
    }
    el
  }

  /** Tracks the number of hints seen to manage local UI blocking logic. */
  private var lastHintCount: Int = 0
  
  /** Local flag to optimistically block the guesser while waiting for the next hint. */
  private var waitingForNextHint: Boolean = false

  /**
   * Helper function to forcibly maintain focus on a persistent input element.
   * Uses a timeout to re-apply focus after the DOM has been updated by the framework.
   *
   * @param el The input element to focus.
   */
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

  /**
   * Main render function that dispatches to specific screen renderers based on the game phase.
   */
  override def render(userId: UserId, view: View): Frag = {
    div(cls := "app-container",
      if (view.phase == Phase.WaitingToStart) 
        renderStartButton()
      else if (view.phase == Phase.GameEnd)
        renderEndScreen(userId, view)
      else
        renderActiveGame(userId, view)
    )
  }

  /**
   * Renders the countdown timer if the game is in the guessing phase.
   */
  def renderTimer(view: View): Frag = {
    val rawSeconds = (view.remainingTimeMs / 1000L).toInt
    val seconds = if (rawSeconds < 0) 0 else rawSeconds

    if (view.phase == Phase.Guessing)
      div(cls := "timer",
        span(cls := "timer-label", "Temps restant : "),
        span(
          cls := (if seconds <= 5 then "timer-value danger" else "timer-value"),
          s"${seconds}s"
        )
      )
    else
      frag() 
  }

  /**
   * Renders the initial start screen with rules and a start button.
   */
  def renderStartButton(): Frag = {
    resetLocalState()
    
    div(cls := "start-screen",
      h1(cls := "app-title", "Emojionary"),
      p(cls := "app-subtitle", "Fais deviner un mot en n'utilisant que des emojis ✨"),

      div(cls := "rules-card",
        h3("Règles du jeu"),
        ul(cls := "rules-list",
          li("Un joueur choisit un mot secret."),
          li("Il envoie une suite d'emojis comme indices."),
          li("Les autres joueurs essaient de deviner le mot."),
          li("Plus tu trouves vite, plus tu marques de points !")
        )
      ),

      button(
        cls := "btn-start",
        onclick := { () => sendEvent(Event.StartGame) },
        "Démarrer la partie 🚀"
      )
    )
  }

  /**
   * Renders the end screen showing the winner, the secret word, and the final scores.
   */
  def renderEndScreen(currentUser: UserId, view: View): Frag = {
    div(
      h2("Partie Terminée !"),
      view.winner match {
        case Some(w) => h3(s"🏆 Le gagnant est : $w")
        case None => h3("Personne n'a trouvé...")
      },
      p(s"Le mot était : ${view.secretWord.getOrElse("???")}"),
      renderScoreBoard(currentUser, view),
      button(cls := "btn-start", onclick := { () => sendEvent(Event.StartGame) }, "Rejouer tout de suite")
    )
  }

  /**
   * Formatting helper to shorten long User IDs.
   */
  private def shortId(uid: UserId): String =
    if uid.length <= 6 then uid else uid.take(6) + "…"

  /**
   * Renders the scoreboard, sorted by score in descending order.
   */
  def renderScoreBoard(currentUser: UserId, view: View): Frag = {
    val ordered = view.allScores.toSeq.sortBy { case (_, score) => -score }

    div(cls := "scoreboard",
      h3("Scores"),
      if (ordered.isEmpty)
        p(cls := "empty-scores", "Aucun point pour l'instant.")
      else
        table(
          thead(tr(th("Joueur"), th("Points"))),
          tbody(
            ordered.map { case (uid, score) =>
              val isYou = uid == currentUser
              tr(cls := (if isYou then "score-row you" else "score-row"),
                td(if isYou then s"Toi (${shortId(uid)})" else shortId(uid)),
                td(score.toString)
              )
            }*
          )
        )
    )
  }

  /**
   * Renders the main active game layout, split into sidebar and main panel.
   */
  def renderActiveGame(currentUser: UserId, view: View): Frag = {
    div(cls := "game-layout",
      div(cls := "sidebar",
        renderScoreBoard(currentUser, view),
        if (!view.amIDescriber) renderGuessHistory(view) else frag()
      ),

      div(cls := "main-panel",
        renderTimer(view),

        if (view.amIDescriber)
          frag(
            h3(cls := "role-title", "Tu fais deviner ! 🤫"),
            renderDescriberControls(view)
          )
        else
          frag(
            h3(cls := "role-title", "Tu dois deviner ! 🤔"),
            renderGuesserControls(view)
          )
      )
    )
  }

  /**
   * Renders the controls for the player who is describing the word.
   * Includes the secret word input (initially) and the emoji keyboard.
   */
  def renderDescriberControls(view: View): Frag = {
    // Automatically reset input if the round changes
    if (view.hints.isEmpty && lastHintCount > 0) {
        secretInput.value = ""
        lastHintCount = 0
    }

    // Check if the describer is waiting for guessers (Guessing phase)
    val isWaitingForGuessers = view.phase == Phase.Guessing

    div(
      // Top section: Secret Word input or reminder
      if (view.hints.isEmpty) {
        maintainFocus(secretInput)
        div(cls := "secret-word-section",
          h4(style := "margin-bottom: 10px; color: #2c3e50;", "🎯 À toi de faire deviner !"),
          p("Choisis le mot secret + 1er Emoji :"),
          secretInput
        )
      } else {
        div(cls := "secret-reminder", "Mot secret : ", strong(view.secretWord.getOrElse("???").toString))
      },

      // Bottom section: Status message or Emoji Keyboard
      if (isWaitingForGuessers) {
        div(cls := "status-message",
          p(style := "color: #3498db; font-weight: bold;", "⏳ Les joueurs réfléchissent..."),
          p("Tu ne peux pas envoyer d'indice pour le moment.")
        )
      } else {
        frag(
          if (view.hints.nonEmpty) 
            div(cls := "status-message", style := "background: #eafaf1; color: #27ae60;",
               p(style := "font-weight: bold;", "👉 C'est à toi !"),
               p("Choisis un nouvel emoji pour les aider.")
            )
          else frag(),

          div(cls := "emoji-keyboard",
            view.availableEmojis.map { emoji =>
              button(cls := "emoji-key", onclick := { () => onEmojiClicked(view, emoji) }, emoji)
            }
          )
        )
      }
    )
  }

  /**
   * Handles emoji clicks. Sends the first hint (with secret word) or subsequent hints.
   */
  def onEmojiClicked(view: View, emoji: String): Unit = {
    if (view.hints.isEmpty) {
      val word = secretInput.value.trim
      if (word.nonEmpty) {
        sendEvent(Event.SendFirstHint(word, emoji))
      } else {
        dom.window.alert("Mot secret obligatoire !")
      }
    } else {
      sendEvent(Event.SendHint(emoji))
    }
  }

  /**
   * Resets the local UI state variables.
   */
  def resetLocalState(): Unit = {
    secretInput.value = ""
    guessInput.value = ""
    lastHintCount = 0
    waitingForNextHint = false
  }

  /**
   * Renders the controls for the players guessing the word.
   */
  def renderGuesserControls(view: View): Frag = {
    val currentHintCount = view.hints.length

    // Reset state if it's the start of a new round
    if (view.hints.isEmpty) {
       lastHintCount = 0
       waitingForNextHint = false
       guessInput.value = "" 
    }

    // Unlock input if a new hint has arrived
    if (currentHintCount > lastHintCount) {
      waitingForNextHint = false
      lastHintCount = currentHintCount
      guessInput.value = "" 
      dom.window.setTimeout(() => guessInput.focus(), 0)
    }

    val isWaitingForHintPhase = view.phase == Phase.WaitingForHint

    div(cls := "guesser-controls",
      p("Regarde les indices :"),
      div(
        cls := "hints-display",
        if (view.hints.isEmpty) span("En attente...")
        else view.hints.mkString(" ")
      ),

      if (view.hints.isEmpty) {
        div(cls := "status-message", "⏳ Le décrypteur choisit le premier indice...")
      }
      else if (isWaitingForHintPhase || waitingForNextHint) {
        div(
          cls := "status-message",
          p(style := "color: #e67e22; font-weight: bold;", "⏳ En attente du prochain indice..."),
          p("Tu ne peux plus proposer pour l'indice actuel.")
        )
      }
      else {
        maintainFocus(guessInput)
        div(cls := "input-group",
          guessInput,
          button(cls := "btn-guess", onclick := { () => submitGuess() }, "Deviner")
        )
      }
    )
  }

  /**
   * Renders the history of incorrect guesses for the current user.
   */
  def renderGuessHistory(view: View): Frag = {
    val errors = view.myWrongGuesses

    div(cls := "guess-history",
      h3("Tes erreurs"),
      if (errors.isEmpty)
        p(cls := "empty-history", "Tu n'as encore fait aucune erreur 👌")
      else
        ul(
          errors.distinct.map { g =>
            li("❌ ", g)
          }*
        )
    )
  }

  /**
   * Submits the current guess to the server and locks the input locally.
   */
  def submitGuess(): Unit = {
    val text = guessInput.value.trim
    if (text.nonEmpty) {
      sendEvent(Event.SubmitGuess(text))
      guessInput.value = "" 
      waitingForNextHint = true
    }
  }

  /**
   * CSS styles for the application.
   */
  override def css: String = super.css + """
    body { background-color: #f0f2f5; font-family: 'Segoe UI', sans-serif; display: flex; justify-content: center; height: 100vh; }
    .app-container {
      background: white;
      width: 900px;              
      padding: 24px;
      border-radius: 20px;
      text-align: left;
      box-shadow: 0 4px 18px rgba(0,0,0,0.08);
      margin-top: 20px;
    }    
    .role-title { color: #333; margin-bottom: 20px; }
    .emoji-keyboard { display: grid; grid-template-columns: repeat(auto-fill, minmax(40px, 1fr)); gap: 5px; margin-top: 15px; max-height: 300px; overflow-y: auto; padding: 10px; background: #fafafa; border-radius: 8px; }
    .emoji-key { font-size: 1.5em; background: white; border: 1px solid #ddd; border-radius: 5px; cursor: pointer; padding: 5px; }
    .emoji-key:hover { background: #e3f2fd; transform: scale(1.1); }
    .hints-display { font-size: 3em; margin: 20px 0; background: #ecf0f1; padding: 10px; border-radius: 10px; letter-spacing: 5px; min-height: 60px; }
    #secret-word-input, #guess-input { padding: 10px; font-size: 1.1em; width: 60%; border: 2px solid #ddd; border-radius: 6px; }
    .btn-start { background: #27ae60; color: white; padding: 15px; border: none; border-radius: 8px; font-size: 1.2em; cursor: pointer; margin-top: 20px;}
    .btn-guess { background: #3498db; color: white; padding: 10px 20px; border: none; border-radius: 6px; font-size: 1.1em; margin-left: 5px; cursor: pointer; transition: background 0.3s; }
    .secret-reminder { background: #fff3cd; color: #856404; padding: 10px; border-radius: 5px; display: inline-block; margin-bottom: 10px; }
    .status-message { padding: 20px; background: #f8f9fa; border-radius: 8px; color: #7f8c8d; font-size: 1.1em; animation: fadeIn 0.3s; }
    
    .game-layout { display: grid; grid-template-columns: 280px 1fr; gap: 24px; align-items: flex-start; }
    .sidebar { display: flex; flex-direction: column; gap: 12px; }
    .main-panel { text-align: center; }
    .scoreboard { background: #f7f9fb; border-radius: 12px; padding: 10px 14px; font-size: 0.9em; border: 1px solid #e3e9f0; }
    .scoreboard h3 { margin-top: 0; margin-bottom: 6px; font-size: 1em; }
    .scoreboard table { width: 100%; border-collapse: collapse; }
    .scoreboard th, .scoreboard td { padding: 4px 6px; border-bottom: 1px solid #edf1f5; }
    .score-row.you { background: #eafaf1; font-weight: 600; }
    .guess-history { background: #fdfefe; border-radius: 12px; padding: 8px 10px; font-size: 0.9em; text-align: left; border: 1px solid #e3e9f0; }
    .guess-history h3 { margin-top: 0; margin-bottom: 6px; font-size: 1em; }
    .guess-history ul { list-style: none; padding-left: 0; margin: 4px 0 0 0; max-height: 120px; overflow-y: auto; }
    .guess-history li { padding: 2px 0; }
    .empty-history { font-size: 0.85em; color: #7f8c8d; margin: 4px 0 0 0; font-style: italic; }
    .start-screen { display: flex; flex-direction: column; gap: 16px; align-items: center; text-align: center; padding: 10px 20px; }
    .app-title { font-size: 2rem; margin: 0; color: #2c3e50; }
    .app-subtitle { margin: 4px 0 8px 0; color: #7f8c8d; }
    .rules-card { background: #f7f9fb; border-radius: 14px; padding: 14px 18px; border: 1px solid #e3e9f0; text-align: left; max-width: 520px; }
    .rules-list { margin: 0; padding-left: 18px; font-size: 0.95em; color: #555; }
    .timer { margin: 10px auto 0 auto; display: inline-flex; align-items: center; gap: 6px; padding: 6px 12px; border-radius: 999px; background: #fdf6e3; border: 1px solid #f1c40f33; font-size: 0.95em; color: #7d6608; }
    .timer-label { font-weight: 500; }
    .timer-value { font-weight: 600; }
    .timer-value.danger { color: #c0392b; }
    @keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
  """
}