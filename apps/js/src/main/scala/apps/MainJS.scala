package apps

import org.scalajs.dom
import scalatags.JsDom.all.*
import cs214.webapp.*
import cs214.webapp.client.*
import cs214.webapp.client.graphics.WebClientAppInstance
import apps.emojionary.{Wire, Event, Phase}

object MainJS extends WSClientApp {
  def appId: String = "emojionary"
  def uiId: String = "html"
  def init(userId: UserId, sendMessage: ujson.Value => Unit, target: Target): ClientAppInstance = {
    new EmojionaryUIInstance(userId, sendMessage, target)
  }
}

class EmojionaryUIInstance(
    userId: UserId,
    sendMessage: ujson.Value => Unit,
    target: Target
) extends WebClientAppInstance[Wire.Event, Wire.View](userId, sendMessage, target) {

  override val wire = apps.emojionary.Wire

  override def render(userId: UserId, view: Wire.View): Frag = {
    div(cls := "app-container",
      if (view.phase == Phase.WaitingToStart) 
        renderStartButton()
      else if (view.phase == Phase.GameEnd)
        div("Partie terminée ! (Sara affichera les scores ici)")
      else
        renderActiveGame(view)
    )
  }

  def renderStartButton(): Frag = {
    button(cls := "btn-start", onclick := { () => sendEvent(Event.StartGame) }, "Démarrer la partie 🚀")
  }

  def renderActiveGame(view: Wire.View): Frag = {
    div(cls := "game-zone",
      if (view.amIDescriber) 
        frag(h3(cls := "role-title", "Tu fais deviner ! 🤫"), renderDescriberControls(view))
      else 
        frag(h3(cls := "role-title", "Tu dois deviner ! 🤔"), renderGuesserControls(view))
    )
  }

  def renderDescriberControls(view: Wire.View): Frag = {
    div(
      if (view.hints.isEmpty) {
        div(cls := "secret-word-section",
          p("Choisis le mot secret, puis clique sur le premier Emoji :"),
          input(id := "secret-word-input", tpe := "text", placeholder := "Ex: Pizza, Chat, Avion...")
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

  def onEmojiClicked(view: Wire.View, emoji: String): Unit = {
    if (view.hints.isEmpty) {
      val wordInput = dom.document.getElementById("secret-word-input").asInstanceOf[dom.html.Input]
      val word = wordInput.value.trim
      if (word.nonEmpty) sendEvent(Event.SendFirstHint(word, emoji))
      else dom.window.alert("Il faut écrire un mot secret avant d'envoyer un emoji !")
    } else {
      sendEvent(Event.SendHint(emoji))
    }
  }

  def renderGuesserControls(view: Wire.View): Frag = {
    div(cls := "guesser-controls",
      p("Regarde les emojis et devine le mot :"),
      div(cls := "input-group",
        input(id := "guess-input", tpe := "text", placeholder := "Tape ta réponse ici...",
          onkeydown := { (e: dom.KeyboardEvent) => if (e.key == "Enter") submitGuess() }
        ),
        button(cls := "btn-guess", onclick := { () => submitGuess() }, "Deviner !")
      )
    )
  }

  def submitGuess(): Unit = {
    val input = dom.document.getElementById("guess-input").asInstanceOf[dom.html.Input]
    val text = input.value.trim
    if (text.nonEmpty) {
      sendEvent(Event.SubmitGuess(text))
      input.value = ""
    }
  }

  override def css: String = super.css + """
    body { 
     background-color: #f0f2f5;
     font-family: 'Segoe UI', sans-serif; 
     display: flex; 
     justify-content: center;
     height: 100vh; }
    .app-container { 
     background: white; 
     width: 500px; 
     padding: 20px;
     border-radius: 12px; 
     text-align: center; 
     box-shadow: 0 4px 15px rgba(0,0,0,0.1); 
     margin-top: 20px; }
    .role-title { 
     color: #333; 
     margin-bottom: 20px; }
    .emoji-keyboard { 
     display: grid; 
     grid-template-columns: repeat(auto-fill, minmax(40px, 1fr)); 
     gap: 5px; 
     margin-top: 15px; 
     max-height: 300px; 
     overflow-y: auto; 
     padding: 10px; 
     background: #fafafa; 
     border-radius: 8px; }
    .emoji-key { 
     font-size: 1.5em; 
     background: white; 
     border: 1px solid #ddd; 
     border-radius: 5px; 
     cursor: pointer; 
     padding: 5px; }
    .emoji-key:hover { 
     background: #e3f2fd; 
     transform: scale(1.1); }
    #secret-word-input, 
    #guess-input { 
     padding: 10px; 
     font-size: 1.1em; 
     width: 70%; 
     border: 2px solid #ddd; 
     border-radius: 6px; }
    #secret-word-input:focus, 
    #guess-input:focus { 
     border-color: #3498db; 
     outline: none; }
    .btn-start { 
     background: #27ae60; 
     color: white; 
     padding: 15px; 
     border: none; 
     border-radius: 8px; 
     font-size: 1.2em; 
     cursor: pointer; }
    .btn-guess { 
     background: #3498db; 
     color: white; 
     padding: 10px 20px; 
     border: none; 
     border-radius: 6px; 
     font-size: 1.1em; 
     margin-left: 5px; 
     cursor: pointer; }
    .secret-reminder { 
     background: #fff3cd; 
     color: #856404; 
     padding: 10px; 
     border-radius: 5px; 
     display: inline-block; 
     margin-bottom: 10px; }
  """
}