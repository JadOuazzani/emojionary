# Emojionary - Design Proposal

## User Stories
As a "Describer", when a new round starts, I want to receive a secret target word and send a sequence of emojis to my partner so that I can help them guess the word without using text.
As a "Guesser", when I receive a new emoji hint, I want to view the history of sent emojis and type a guess into a text field to check if I have found the correct word.
As a player, when the round ends successfully, I want to see the score update based on the number of attempts and switch roles with my partner so that the game remains fair and engaging.

## Requirements
The application will simulate a real-time guessing game inspired by Pictionary, but using emojis instead of drawings. 
Upon launching the app, users will be able to create a lobby or join an existing one to pair up with another player. 
Once the game begins, the system will access a local or cloud-based dictionary to assign a secret word to Player A (the Describer). Player A will have access to a custom emoji picker or the system keyboard to send hints one by one, which will appear instantly on Player B's (the Guesser) screen via a real-time database connection.
The core logic will handle the validation of guesses. Player B will type their answer into an input field; the app will normalize the text (case-insensitive) and compare it to the secret word. If the guess is incorrect, the UI will notify both players, prompting Player A to send another hint. If the guess is correct, the round terminates, points are calculated (inversely proportional to the number of emojis used), and the roles are automatically swapped for the next round. The app will maintain a persistent score counter throughout the session.

## Mock-up
_

## Roles
Jad will be responsible for the UI design and activity layouts; 
Riad will handle the real-time database integration (e.g., Firebase) for message syncing; 
Lilya will implement the game logic (word randomization, answer validation, scoring); 
Sara will manage user authentication and the lobby/matchmaking system.