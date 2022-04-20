#include "include/game.h"

// global variables defined in main.h
extern game_data games[];
extern maze_data mazes[];
extern pthread_mutex_t mutex;

// placeholder defined in pregame.c
extern player_data placeholder_player;

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

    this_player->score = 0; // init the score

    send_welcome_message();
    send_initial_position();

    handle_game_requests();

    // send [GOBYE] message to signal game has ended
    send_all(sock_fd_tcp, "GOBYE***", 8);

    // close the sockets (both TCP and UDP)
    close(sock_fd_tcp);
    close(this_player->udp_port);

    return NULL;
}

void handle_game_requests() {
    char buf[14]; // size of the [SEND?] message without the message part
    // we will use another buffer to get the 'mess' part of the [SEND?] and [MALL?] messages

    // stop reading messages if the game has ended
    // or if the player quit (this_player will be replaced with a placeholder)
    while (!game_has_ended() && this_player->is_a_player) {
        // receive header of message
        long res = recv(sock_fd_tcp, buf, 5, 0);
        if (!isRecvRightLength(res, 5, "Header of a game message")) {
            break;
        }

        // handle the message depending on the header
        if (strncmp("UPMOV", buf, 5) == 0) {
            int steps = receive_move_message("UPMOV", buf);
            move_vertical(steps, "UP", 1);
        } else if (strncmp("DOMOV", buf, 5) == 0) {
            int steps = receive_move_message("DOMOV", buf);
            move_vertical(steps, "DOWN", -1);
        } else if (strncmp("LEMOV", buf, 5) == 0) {
            int steps = receive_move_message("LEMOV", buf);
            move_horizontal(steps, "LEFT", -1);
        } else if (strncmp("RIMOV", buf, 5) == 0) {
            int steps = receive_move_message("RIMOV", buf);
            move_horizontal(steps, "RIGHT", 1);

        } else if (strncmp("IQUIT", buf, 5) == 0) {
            player_quits();
        } else if (strncmp("GLIS?", buf, 5) == 0) {
            send_list_of_players_for_game();
        } else if (strncmp("MALL?", buf, 5) == 0) {
        } else if (strncmp("SEND?", buf, 5) == 0) {
        }
    }
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
    this_player->x = maze->x_start[this_player->player_number];
    this_player->y = maze->y_start[this_player->player_number];

    // add position to message
    char *x_str = int_to_3_bytes(this_player->x);
    char *y_str = int_to_3_bytes_with_stars(this_player->y);
    memmove(message + 15, x_str, 3);
    memmove(message + 18, " ", 1); // NOLINT(bugprone-not-null-terminated-result)
    memmove(message + 19, y_str, 6);
    free(x_str);
    free(y_str);

    // send message
    send_all(sock_fd_tcp, message, 25);
    fprintf(stderr, "Sent position message to player id = %s, sockfd = %d\n"
                    "Position is x=%d y=%d\n", this_player->id, sock_fd_tcp, this_player->x, this_player->y);
}

int receive_move_message(char *context, char *buf) {
    long res = recv(sock_fd_tcp, &buf[5], 7, 0);
    if (!isRecvRightLength(res, 7, context)) {
        return -1;
    }
    fprintf(stderr, "received %s message from fd = %d\n", context, sock_fd_tcp);
    char *ptr;
    int d = (int) strtol(&buf[6], &ptr, 10);
    if (ptr == NULL) {
        fprintf(stderr, "Conversion error reading the nb of steps in [%s], string was %s\n",
                context, &buf[6]);
        return -1;
    }
    return d;
}


// TODO refactor into one method
// or at least refactor the bits that are the same
void move_vertical(int steps, char *context, int direction) {
    bool captured_a_ghost = false;
    pthread_mutex_lock(&mutex); // protect the maze while moving the player
    for (int i = 0; i < steps; i++) {
        if (direction == 1 && this_player->y + 1 >= maze->height) {
            break; // can't go up
        }
        if (direction == -1 && this_player->y - 1 < 0) {
            break; // can't go down
        } else if (maze->maze[this_player->x][this_player->y + 1 * direction] == 0) { // free space
            this_player->y += 1; // move
        } else if (maze->maze[this_player->x][this_player->y + 1 * direction] == 2) { // ghost
            this_player->y += 1; // move
            // remove the ghost
            captured_a_ghost = true;
            maze->maze[this_player->x][this_player->y] = 0;
            maze->nb_ghosts -= 1;
            // increase score
            this_player->score += 10;
            fprintf(stderr, "Player fd = %d captured a ghost on x=%d, y=%d\n", sock_fd_tcp, this_player->x, this_player->y);
            send_score_multicast();
        } else { // wall or another player : blocked
            fprintf(stderr, "Player fd=%d ran into a wall at x=%d y=%d\n", sock_fd_tcp, this_player->x, this_player->y);
            break;
        }
    }

    // put the player in their new place in the maze
    maze->maze[this_player->x][this_player->y] = 3;
    pthread_mutex_unlock(&mutex);
    fprintf(stderr, "Player fd=%d moved %s to x=%d, y=%d\n", context, sock_fd_tcp, this_player->x, this_player->y);

    // send message(s) with new position
    if (captured_a_ghost) {
        send_MOVEF();
    } else {
        send_MOVE();
    }
}

void move_horizontal(int steps, char *context, int direction) {
    bool captured_a_ghost = false;
    pthread_mutex_lock(&mutex); // protect the maze while moving the player
    for (int i = 0; i < steps; i++) {
        if (direction == 1 && this_player->x + 1 >= maze->width) {
            break; // can't go up
        }
        if (direction == -1 && this_player->x - 1 < 0) {
            break; // can't go down
        } else if (maze->maze[this_player->x + 1 * direction][this_player->y] == 0) { // free space
            this_player->x += 1; // move
        } else if (maze->maze[this_player->x + 1 * direction][this_player->y] == 2) { // ghost
            this_player->x += 1; // move
            // remove the ghost
            captured_a_ghost = true;
            maze->maze[this_player->x][this_player->y] = 0;
            maze->nb_ghosts -= 1;
            // increase score
            this_player->score += 10;
            fprintf(stderr, "Player fd = %d captured a ghost on x=%d, y=%d\n", sock_fd_tcp, this_player->x, this_player->y);
            send_score_multicast();
        } else { // wall or another player : blocked
            fprintf(stderr, "Player fd=%d ran into a wall at x=%d y=%d\n", sock_fd_tcp, this_player->x, this_player->y);
            break;
        }
    }

    // put the player in their new place in the maze
    maze->maze[this_player->x][this_player->y] = 3;
    pthread_mutex_unlock(&mutex);
    fprintf(stderr, "Player fd=%d moved %s to x=%d, y=%d\n", context, sock_fd_tcp, this_player->x, this_player->y);

    // send message(s) with new position
    if (captured_a_ghost) {
        send_MOVEF();
    } else {
        send_MOVE();
    }
}


void send_MOVEF() {
    // the SCORE multicast message is sent during the `move` function
    // because we need the exact coordinates at which the ghost was captured

    // make MOVEF message
    char mess[22];
    char *x = int_to_3_bytes(this_player->x);
    char *y = int_to_3_bytes(this_player->y);
    char *score_with_stars = int_to_4_bytes_with_stars(this_player->score);
    memmove(mess, "MOVEF ", 6); // NOLINT(bugprone-not-null-terminated-result)
    memmove(mess + 6, x, 3);
    memmove(mess + 9, " ", 1); // NOLINT(bugprone-not-null-terminated-result)
    memmove(mess + 10, y, 3);
    memmove(mess + 13, " ", 1); // NOLINT(bugprone-not-null-terminated-result)
    memmove(mess + 14, score_with_stars, 7);

    free(x);
    free(y);
    free(score_with_stars);

    // send MOVEF message
    send_all(sock_fd_tcp, mess, 21);
    fprintf(stderr, "Send MOVEF message to player fd=%d\n", sock_fd_tcp);
}

void send_MOVE() {
    char mess[17];
    char *x = int_to_3_bytes(this_player->x);
    char *y_with_stars = int_to_3_bytes_with_stars(this_player->y);
    memmove(mess, "MOVE! ", 6); // NOLINT(bugprone-not-null-terminated-result)
    memmove(mess + 6, x, 3);
    memmove(mess + 9, " ", 1); // NOLINT(bugprone-not-null-terminated-result)
    memmove(mess + 10, y_with_stars, 6);

    free(x);
    free(y_with_stars);

    send_all(sock_fd_tcp, mess, 16);
    fprintf(stderr, "Send [MOVE!] to player fd=%d\n", sock_fd_tcp);
}

bool game_has_ended() {
    pthread_mutex_lock(&mutex);
    bool res = game->nb_ghosts_left == 0
               || game->nb_players == 0;
    pthread_mutex_unlock(&mutex);
    return res;
}

void player_quits() {
    // read the *** at the end of the message
    char buf[3];
    int res = recv(sock_fd_tcp, buf, 3, 0);
    if (!isRecvRightLength(res, 3, "IQUIT")) {
        return; // ignore incomplete message
    }

    // remove the player from the game
    pthread_mutex_lock(&mutex);
    game->players[this_player->player_number] = placeholder_player;
    game->nb_players -= 1;
    pthread_mutex_unlock(&mutex);

    // make this_player a placeholder so that the main function
    // will know to exit and kill this thread
    this_player->is_a_player = false;

    // no need to send the GOBYE message, it is sent
    // automatically at the end of the main function.
    // The sockets are also closed there.
}

void send_list_of_players_for_game() {
    char player_ids[4][8];
    bool is_a_player[4];
    int nb_players = game->nb_players;

    // get the info with a mutex first,
    // then make the messages and send without the mutex
    // to spend the less time possible inside the mutex
    pthread_mutex_lock(&mutex);
    for (int i = 0; i < 4; i++) {
        if (games[this_player->game_number].players[i].is_a_player) {
            memmove(player_ids[i], games[this_player->game_number].players[i].id, 8);
            is_a_player[i] = true;
            nb_players += 1;
        } else {
            is_a_player[i] = false;
        }
    }
    pthread_mutex_unlock(&mutex);

    // make and send [GLIS!...] message
    char first_message[10];
    memmove(first_message, "GLIS! ", 6); // NOLINT(bugprone-not-null-terminated-result)
    memmove(first_message + 6, (uint8_t *) &nb_players, 1);
    memmove(first_message + 7, "***", 3); // NOLINT(bugprone-not-null-terminated-result)
    send_all(sock_fd_tcp, first_message, 10);

    // make and send the [GPLYR...] messages
    for (int i = 0; i < 4; i++) {
        if (is_a_player[i]) {
            char message[31];
            char *x = int_to_3_bytes(game->players[i].x);
            char *y = int_to_3_bytes(game->players[i].y);
            char *score_with_stars = int_to_4_bytes_with_stars(game->players[i].score);
            memmove(message, "GPLYR ", 6); // NOLINT(bugprone-not-null-terminated-result)
            memmove(message + 6, player_ids[i], 8);
            memmove(message + 14, " ", 1); // NOLINT(bugprone-not-null-terminated-result)
            memmove(message + 15, x, 3);
            memmove(message + 18, " ", 1); // NOLINT(bugprone-not-null-terminated-result)
            memmove(message + 19, y, 3);
            memmove(message + 22, " ", 1); // NOLINT(bugprone-not-null-terminated-result)
            memmove(message + 23, score_with_stars, 8);

            free(x);
            free(y);
            free(score_with_stars);

            send_all(sock_fd_tcp, message, 31);
        }
    }
    fprintf(stderr, "sent GLIS! and GPLYR messages to fd = %d\n", sock_fd_tcp);
}

void send_score_multicast() {
    char mess[30];
    char *score = int_to_4_bytes(this_player->score);
    char *x = int_to_3_bytes(this_player->x);
    char *y = int_to_3_bytes(this_player->y);

    sprintf(mess, "SCORE %s %s %s %s+++", this_player->id, score, x, y);
    free(score);
    free(x);
    free(y);

    struct sockaddr *their_addr;
    sendto(game->multicast_socket, mess, strlen(mess), 0,
           their_addr, (socklen_t)sizeof(struct sockaddr_in));
    fprintf(stderr, "Sent SCORE multicast message for player fd=%d, score=%d\n", sock_fd_tcp, this_player->score);
}
