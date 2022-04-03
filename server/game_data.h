#ifndef GHOSTLAB_GAME_H
#define GHOSTLAB_GAME_H

// All the data about a player
typedef {
    char[8] id;
    bool sent_start; // true if the player sent the [START***] message
    bool registered; // true if the player is currently registered for a game
    int game_number; // number of the game the player is registered in; -1 if not registered

    int tcp_socket;
    int udp_socket;
} player_data;

// All the data about a maze
typedef {
    int width;
    int height;
    int nb_ghosts;

    int[][] maze; // the contents of the maze
    // 0 for empty,
    // 1 for wall,
    // 2 for ghost,
    // 3 for player
} maze_data;

// All the data about a game
typedef {
    maze_data *maze;            // the maze
    player_data[4] players;     // info about all the players

    int nb_ghosts_left;         // the number of ghosts left in the game
} game_data;

#endif //GHOSTLAB_GAME_H
