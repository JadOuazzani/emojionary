package apps

import org.scalajs.dom
import scalatags.JsDom.all.*
import cs214.webapp.*
import cs214.webapp.client.*
import cs214.webapp.client.graphics.WebClientAppInstance
import apps.emojionary.{Wire, Event, View, Phase}

// 1. L'OBJET PRINCIPAL (Config)
// On étend WSClientApp car c'est le contrat officiel de la librairie
object MainJS extends WSClientApp {
  def appId: String = "emojionary"
  def uiId: String = "html"

  // --- LE DÉMARRAGE MANUEL (Comme on n'a pas ApplicationJS) ---
  def main(args: Array[String]): Unit = {
    // 1. On s'enregistre auprès du moteur du framework
    WebClient.register(this)
    // 2. On lance le moteur (il va chercher la div #app-container tout seul)
    WebClient.start()
  }

  def init(userId: UserId, sendMessage: ujson.Value => Unit, target: Target): ClientAppInstance = {
    new EmojionaryUIInstance(userId, sendMessage, target)
  }
}

// 2. L'INSTANCE UI (Le Visuel)
class EmojionaryUIInstance(
    userId: UserId,
    sendMessage: ujson.Value => Unit,
    target: Target
) extends WebClientAppInstance[Event, View](userId, sendMessage, target) {

  override val wire = apps.emojionary.Wire

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
        div(cls := "secret-word-section",
          p("Choisis le mot secret + 1er Emoji :"),
          input(id := "secret-word-input", tpe := "text", placeholder := "Ex: Pizza...")
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
      val input = dom.document.getElementById("secret-word-input")
      // Protection contre le null (si l'élément n'est pas encore rendu)
      if (input != null) {
        val word = input.asInstanceOf[dom.html.Input].value.trim
        if (word.nonEmpty) sendEvent(Event.SendFirstHint(word, emoji))
        else dom.window.alert("Mot secret obligatoire !")
      }
    } else {
      sendEvent(Event.SendHint(emoji))
    }
  }

  def renderGuesserControls(view: View): Frag = {
    div(cls := "guesser-controls",
      p("Regarde les indices :"),
      div(cls := "hints-display", 
        if (view.hints.isEmpty) span("En attente...") else view.hints.mkString(" ")
      ),
      div(cls := "input-group",
        input(id := "guess-input", tpe := "text", placeholder := "Réponse...",
          onkeydown := { (e: dom.KeyboardEvent) => if (e.key == "Enter") submitGuess() }
        ),
        button(cls := "btn-guess", onclick := { () => submitGuess() }, "Deviner")
      )
    )
  }

  def submitGuess(): Unit = {
    val input = dom.document.getElementById("guess-input")
    if (input != null) {
      val field = input.asInstanceOf[dom.html.Input]
      val text = field.value.trim
      if (text.nonEmpty) {
        sendEvent(Event.SubmitGuess(text))
        field.value = ""
      }
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
    .btn-guess { background: #3498db; color: white; padding: 10px 20px; border: none; border-radius: 6px; font-size: 1.1em; margin-left: 5px; cursor: pointer; }
    .secret-reminder { background: #fff3cd; color: #856404; padding: 10px; border-radius: 5px; display: inline-block; margin-bottom: 10px; }
  """
}