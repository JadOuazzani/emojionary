package apps.emojionary

import cs214.webapp.UserId


type Emoji = String
type Word  = String
type Score = Map[UserId, Int]

enum Role:
  case Describer, Guesser

enum Phase:
  
  case WaitingForPlayers
  case InRound
  case Result
  case Done


object Rules:
  val MaxEmojisPerRound: Int = 3
  val MaxRounds: Int = 10

final case class State(
  players: Vector[UserId],          
  roles:   Map[UserId, Role],       
  round:   Int,                     
  phase:   Phase,                   
  word:    Option[Word],            // mot secret 
  emojisHistory: Vector[Emoji],       // emojis envoyés (dans l'ordre)
  guesses:     Vector[String],      // historique des guess (dans l'ordre)
  scores:      Score                
):
  lazy val describer: Option[UserId] =
    roles.collectFirst { case (u, Role.Describer) => u }

  lazy val guesser: Option[UserId] =
    roles.collectFirst { case (u, Role.Guesser) => u }

  def gameCanStart: Boolean = players.size >= 2

  def canSendEmoji(uid: UserId): Boolean =
    phase == Phase.InRound &&
      (roles(uid) == (Role.Describer)) &&
      emojisHistory.size < Rules.MaxEmojisPerRound

  def canGuess(uid: UserId): Boolean =
    phase == Phase.InRound &&
      (roles(uid)==(Role.Guesser))

  def reachedHintCap: Boolean =
    emojisHistory.size >= Rules.MaxEmojisPerRound
  
  def hasReachedMaxRounds: Boolean=
    round>=Rules.MaxRounds


enum Event:
  case StartRound                          // quand ???
  case EmojiSelected(emoji: Emoji)         
  case GuessSubmitted(text: String)        
  case NextRound                            // si good guess ou si on atteint nbr de hint max  
  


type ScoresView = Map[UserId, Int]

final case class StateView(
  uid: UserId,                 // l'utilisateur courant A VOIR SI JE GARDE VU QUE YA DEJA ROLE
  role: Option[Role],          // None tant que les rôles ne sont pas assignés
  phase: Phase,                //la phase du jeu
  emojisHistory: Option[Vector[Emoji]],  // historique des emojis visibles que par le guesser (None si c role=describer)
  guessesHistory: Option[Vector[String]],           // historique des guesses visibles que par le guesser (None si c role=describer)
  secretWord: Option[Word]     // Some(word) seulement pour le Describer en InRound/Result
)

final case class View(
  stateView: StateView,
  scoresView: ScoresView
)
