#include <stdbool.h>
#include <pthread.h>

#ifndef GHOSTLAB_GAME_H
#define GHOSTLAB_GAME_H

// All the data about a player
typedef struct player_data {
    char id[8];
    bool is_a_player; // true if this is a player, false if this is a placeholder
    bool sent_start; // true if the player sent the [START***] message
    int game_number; // number of the game the player is registered in; -1 if not registered
    int player_number; // the place of this player in the player array (0, 1, 2, or 3)

    int score;

    struct sockaddr_in *address;
    int tcp_socket;
    int udp_socket;
} player_data;

// All the data about a maze
typedef struct maze_data {
    int width;
    int height;
    int nb_ghosts;

    int **maze; // the contents of the maze
    // 0 for empty,
    // 1 for wall,
    // 2 for ghost,
    // 3 for player

    // the starting x and y positions for the players
    int x_start[4];
    int y_start[4];
} maze_data;

// All the data about a game
typedef struct game_data {
    bool is_created;    // false if the game has not been created yet
    maze_data *maze;            // the maze
    player_data players[4];     // info about all the players

    int nb_players;  // how many players are currently registered / playing the game (if player quits -> nb-1)
    bool has_started;
    int nb_ghosts_left;         // the number of ghosts left in the game
} game_data;


// the arguments that the main function from main.c
// gives to the threads from pregame.c
typedef struct thread_arguments {
    int sock_fd;
    struct sockaddr_in *client_address;
} thread_arguments;


#endif //GHOSTLAB_GAME_H
