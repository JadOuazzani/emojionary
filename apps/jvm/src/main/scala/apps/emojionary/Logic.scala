package apps.emojionary

import cs214.webapp.*
import cs214.webapp.server.StateMachine
import cs214.webapp.Action
import scala.util.{Try, Success}

class Logic extends StateMachine[Event, State, View]:

  override val appInfo: AppInfo = AppInfo(
    id = "emojionary",
    name = "Emojionary",
    description = "Guess the word using emojis.",
    year = 2025
  )

  override val wire = Wire

  override def init(clients: Seq[UserId]): State =
    State()

  override def project(state: State)(userId: UserId): View =
    View("Hello from Emojionary 👋")

  override def transition(state: State)(userId: UserId, event: Event): Try[Seq[Action[State]]] =
    Success(Seq(Action.Render(state)))
