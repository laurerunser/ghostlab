#include "include/game.h"

// global variables defined in main.h
extern game_data games[];
extern maze_data mazes[];
extern pthread_mutex_t mutex;

// the player this thread talks to + their socket
player_data *this_player;
int sock_fd_tcp;

// the game we are using
// !! Both the game and the maze MUST be protected by the
// mutex !!
game_data *game;
maze_data *maze;

void *start_game(void *player) {
    // get the data into easily accessible structs
    this_player = (player_data *) player;
    sock_fd_tcp = this_player->tcp_socket;
    game = &games[this_player->game_number];
    maze = game->maze;

    send_welcome_message();
    send_initial_position();
}

void send_welcome_message() {
    char welcome_msg[39];
    memmove(welcome_msg, "WELCO ", 6); // NOLINT(bugprone-not-null-terminated-result)
    memmove(welcome_msg + 6, &this_player->game_number, 1); // game number
    memmove(welcome_msg + 7, " ", 1); // NOLINT(bugprone-not-null-terminated-result)

    // no need to protect the reading of the height and width
    // because they are constants (never changed during the game)
    memmove(welcome_msg + 8, &maze->height, 2);
    memmove(welcome_msg + 10, " ", 1); // NOLINT(bugprone-not-null-terminated-result)
    memmove(welcome_msg + 11, &maze->width, 2);
    memmove(welcome_msg + 13, " ", 1); // NOLINT(bugprone-not-null-terminated-result)

    // protect the number of ghosts just in case
    pthread_mutex_lock(&mutex);
    memmove(welcome_msg + 14, &game->nb_ghosts_left, 1);
    pthread_mutex_unlock(&mutex);

    // BROADCAST_IP is defined in the game.h header file
    memmove(welcome_msg + 15, BROADCAST_IP, 15); // NOLINT(bugprone-not-null-terminated-result)
    memmove(welcome_msg + 30, " ", 1); // NOLINT(bugprone-not-null-terminated-result)

    // the UDP port is 4444 + id_of_the_game
    int udp_port = 4444 + this_player->game_number;
    sprintf(welcome_msg + 31, "%04d***", udp_port);

    // send the message
    send_all(sock_fd_tcp, welcome_msg, 39);
    fprintf(stderr, "Sent welcome message to player id=%s, sockfd = %d\n",
            this_player->id, sock_fd_tcp);
}

void send_initial_position() {
    char message[25];
    sprintf(message, "POSIT %s ", this_player->id);

    // get initial position for this player
    // no need to protect with mutex, this information doesn't change
    int x = maze->x_start[this_player->player_number];
    int y = maze->y_start[this_player->player_number];

    // add position to message
    char *x_str = int_to_3_bytes(x);
    char *y_str = int_to_3_bytes_with_stars(y);
    memmove(message + 15, x_str, 3);
    memmove(message + 18, " ", 1); // NOLINT(bugprone-not-null-terminated-result)
    memmove(message + 19, y_str, 6);
    free(x_str);
    free(y_str);

    // send message
    send_all(sock_fd_tcp, message, 25);
    fprintf(stderr, "Sent position message to player id = %s, sockfd = %d\n"
            "Position is x=%d y=%d", this_player->id, sock_fd_tcp, x, y);

}