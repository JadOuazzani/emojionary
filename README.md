# Emojionary - Design Proposal

## User Stories
As a player, when I open the app, I can create a lobby (share a code) or join a lobby so that I can pair with exactly one other player.
As the Describer, when a round starts, I receive a secret word and can send emoji hints (one at a time) so that my partner can guess without me using text.
As the Guesser, when I receive new hints, I can see the full hint history and submit guesses; when I guess correctly, both players see the updated score and roles swap for the next round.

## Requirements
The app supports two-player sessions. Players meet in a lobby identified by a short code. Once two players are present, either player can start a round.

At the start of each round, the server chooses a word from a local built-in dictionary and assigns roles: one Describer, one Guesser. The Describer can only send emoji hints; the Guesser can only submit guesses via text input. The server maintains the authoritative game state: hint history, guess count, and round status.

When the Guesser submits a guess, the server normalizes it (case-insensitive, trimmed). If incorrect, the round continues. If correct, the server ends the round, computes points (e.g., fewer hints / fewer guesses ⇒ more points), updates the persistent session score, and swaps roles before the next round.

## Mock-up
_

## Roles
	•	Jad — UI design and activity layouts
	•	Riad — Real-time database integration (e.g., Firebase)
	•	Lilya — Game logic (word selection, validation, scoring)
	•	Sara — User authentication and lobby/matchmaking system