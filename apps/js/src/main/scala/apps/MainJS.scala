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

  // --- ÉLÉMENTS PERSISTANTS ---
  
  lazy val secretInput: dom.html.Input = {
    input(
      id := "secret-word-input", 
      tpe := "text", 
      placeholder := "Ex: Pizza...",
      autofocus := true
    ).render
  }

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

  // Fonction pour maintenir le focus lors des mises à jour d'interface
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
    div(cls := "app-container",
      if (view.phase == Phase.WaitingToStart) 
        renderStartButton()
      else if (view.phase == Phase.GameEnd)
        renderEndScreen(view)
      else
        renderActiveGame(view)
    )
  }

  // --- ÉCRANS ---

  def renderStartButton(): Frag = {
    // Reset des champs
    secretInput.value = ""
    guessInput.value = ""
    
    div(
      h2("Bienvenue sur Emojionary !"),
      button(cls := "btn-start", onclick := { () => sendEvent(Event.StartGame) }, "Démarrer la partie 🚀")
    )
  }

  def renderEndScreen(view: View): Frag = {
    div(
      h2("Partie Terminée !"),
      view.winner match {
        case Some(w) => h3(s"🏆 Le gagnant est : $w")
        case None => h3("Personne n'a trouvé...")
      },
      p(s"Le mot était : ${view.secretWord.getOrElse("???")}"),
      button(cls := "btn-start", onclick := { () => sendEvent(Event.StartGame) }, "Rejouer")
    )
  }

  def renderActiveGame(view: View): Frag = {
    div(cls := "game-zone",
      if (view.amIDescriber) 
        frag(h3(cls := "role-title", "Tu fais deviner ! 🤫"), renderDescriberControls(view))
      else 
        frag(h3(cls := "role-title", "Tu dois deviner ! 🤔"), renderGuesserControls(view))
    )
  }

  // --- CONTROLES ---

  def renderDescriberControls(view: View): Frag = {
    div(
      if (view.hints.isEmpty) {
        maintainFocus(secretInput)
        div(cls := "secret-word-section",
          p("Choisis le mot secret + 1er Emoji :"),
          secretInput
        )
      } else {
        div(cls := "secret-reminder", "Mot secret : ", strong(view.secretWord.getOrElse("???").toString))
      },
      div(cls := "emoji-keyboard",
        view.availableEmojis.map { emoji =>
          button(cls := "emoji-key", onclick := { () => onEmojiClicked(view, emoji) }, emoji)
        }
      )
    )
  }

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

  def renderGuesserControls(view: View): Frag = {
    maintainFocus(guessInput)
    
    div(cls := "guesser-controls",
      p("Regarde les indices :"),
      div(cls := "hints-display", 
        if (view.hints.isEmpty) span("En attente...") else view.hints.mkString(" ")
      ),
      div(cls := "input-group",
        guessInput,
        button(cls := "btn-guess", onclick := { () => submitGuess() }, "Deviner")
      )
    )
  }

  def submitGuess(): Unit = {
    val text = guessInput.value.trim
    if (text.nonEmpty) {
      sendEvent(Event.SubmitGuess(text))
      guessInput.value = "" // On vide juste le champ pour la prochaine tentative
    }
  }

  // --- CSS ---
  override def css: String = super.css + """
    body { background-color: #f0f2f5; font-family: 'Segoe UI', sans-serif; display: flex; justify-content: center; height: 100vh; }
    .app-container { background: white; width: 500px; padding: 20px; border-radius: 12px; text-align: center; box-shadow: 0 4px 15px rgba(0,0,0,0.1); margin-top: 20px; }
    .role-title { color: #333; margin-bottom: 20px; }
    .emoji-keyboard { display: grid; grid-template-columns: repeat(auto-fill, minmax(40px, 1fr)); gap: 5px; margin-top: 15px; max-height: 300px; overflow-y: auto; padding: 10px; background: #fafafa; border-radius: 8px; }
    .emoji-key { font-size: 1.5em; background: white; border: 1px solid #ddd; border-radius: 5px; cursor: pointer; padding: 5px; }
    .emoji-key:hover { background: #e3f2fd; transform: scale(1.1); }
    .hints-display { font-size: 3em; margin: 20px 0; background: #ecf0f1; padding: 10px; border-radius: 10px; letter-spacing: 5px; min-height: 60px; }
    #secret-word-input, #guess-input { padding: 10px; font-size: 1.1em; width: 60%; border: 2px solid #ddd; border-radius: 6px; }
    .btn-start { background: #27ae60; color: white; padding: 15px; border: none; border-radius: 8px; font-size: 1.2em; cursor: pointer; margin-top: 20px;}
    .btn-guess { background: #3498db; color: white; padding: 10px 20px; border: none; border-radius: 6px; font-size: 1.1em; margin-left: 5px; cursor: pointer; transition: background 0.3s; }
    .secret-reminder { background: #fff3cd; color: #856404; padding: 10px; border-radius: 5px; display: inline-block; margin-bottom: 10px; }
  """
}