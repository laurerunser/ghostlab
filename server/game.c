#include "include/game.h"

// global variables defined in main.h
extern game_data games[];
extern maze_data mazes[];
extern pthread_mutex_t mutex;

// placeholder defined in pregame.c
extern player_data placeholder_player;

void *start_game(void *player) {
    // get the data into easily accessible structs
    player_data *this_player = (player_data *) player;
    game_data *game = &games[this_player->game_number];
    maze_data *maze = game->maze;

    this_player->score = 0; // init the score

    send_welcome_message(this_player, maze, game);
    send_initial_position(this_player, maze);

    handle_game_requests(this_player, game, maze);

    // send [GOBYE] message to signal game has ended
    send_all(this_player->tcp_socket, "GOBYE***", 8);

    // close the sockets (both TCP and UDP)
    close(this_player->tcp_socket);
    close(this_player->udp_socket);

    // if this was the last player, also close the multicast socket
    if (game->nb_players == 0) {
        close(game->multicast_socket);
    }
    return NULL;
}

void handle_game_requests(player_data *this_player, game_data *game, maze_data *maze) {
    char buf[14]; // size of the [SEND?] message without the message part
    // we will use another buffer to get the 'mess' part of the [SEND?] and [MALL?] messages

    // stop reading messages if the game has ended
    // or if the player quit (this_player will be replaced with a placeholder)
    while (!game_has_ended(game) && this_player->is_a_player) {
        // receive header of message
        long res = recv(this_player->tcp_socket, buf, 5, 0);
        if (!isRecvRightLength(res, 5, "Header of a game message")) {
            break;
        }

        // handle the message depending on the header
        if (strncmp("UPMOV", buf, 5) == 0) {
            int steps = receive_move_message("UPMOV", buf, this_player);
            move_up(steps, this_player, maze, game);
        } else if (strncmp("DOMOV", buf, 5) == 0) {
            int steps = receive_move_message("DOMOV", buf, this_player);
            move_down(steps, this_player, maze, game);
        } else if (strncmp("LEMOV", buf, 5) == 0) {
            int steps = receive_move_message("LEMOV", buf, this_player);
            move_left(steps, this_player, maze, game);
        } else if (strncmp("RIMOV", buf, 5) == 0) {
            int steps = receive_move_message("RIMOV", buf, this_player);
            move_right(steps, this_player, maze, game);
        } else if (strncmp("IQUIT", buf, 5) == 0) {
            player_quits(this_player, game);
            // read end of message
            res = recv(this_player->tcp_socket, buf, 3, 0);
        } else if (strncmp("GLIS?", buf, 5) == 0) {
            send_list_of_players_for_game(this_player, game);
            // read end of message
            res = recv(this_player->tcp_socket, buf, 3, 0);
            if (!isRecvRightLength(res, 3, "IQUIT")) {
                break; // ignore incomplete message
            }
        } else if (strncmp("MALL?", buf, 5) == 0) {
            send_message_to_all(this_player, game);
        } else if (strncmp("SEND?", buf, 5) == 0) {
            send_personal_message(this_player, game);
        }
    }
}


void send_welcome_message(player_data *this_player, maze_data *maze, game_data *game) {
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
    memmove(welcome_msg + 15, MULTICAST_ADDR, 15); // NOLINT(bugprone-not-null-terminated-result)
    memmove(welcome_msg + 30, " ", 1); // NOLINT(bugprone-not-null-terminated-result)

    // the UDP port is 4444 + id_of_the_game
    int udp_port = 4444 + this_player->game_number;
    sprintf(welcome_msg + 31, "%04d***", udp_port);

    // send the message
    send_all(this_player->tcp_socket, welcome_msg, 39);
    fprintf(stderr, "fd %d : Sent welcome message, player id=%s\n",
            this_player->tcp_socket, this_player->id);
}

void send_initial_position(player_data *this_player, maze_data *maze) {
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
    send_all(this_player->tcp_socket, message, 25);
    fprintf(stderr, "fd %d : x=%d y=%d, sent position message to player id = %s \n",
            this_player->tcp_socket, this_player->x, this_player->y, this_player->id);
}

int receive_move_message(char *context, char *buf, player_data *this_player) {
    long res = recv(this_player->tcp_socket, &buf[5], 7, 0);
    if (!isRecvRightLength(res, 7, context)) {
        return -1;
    }
    fprintf(stderr, "fd %d : received %s message\n", this_player->tcp_socket, context);
    char *ptr;
    printf("A");
    int d = (int) strtol(&buf[6], &ptr, 10);
    if (ptr == NULL) {
        fprintf(stderr, "Conversion error reading the nb of steps in [%s], string was %s\n",
                context, &buf[6]);
        return -1;
    }
    printf("B");
    return d;
}

void move_up(int steps, player_data *this_player, maze_data *maze, game_data *game) {
    if (steps == -1) {
        return; // the header message wasn't read properly
    }

    bool captured = false;
    pthread_mutex_lock(&mutex); // protect the maze while moving the player

    for (int i = 0; i < steps; i++) {
        if (this_player->y + 1 == maze->height) {
            fprintf(stderr, "fd %d, already at the top of the maze, can't move\n", this_player->tcp_socket);
            break; // this is the top of the maze, can't go up
        } else if (maze->maze[this_player->x][this_player->y + 1] == 0) {
            this_player->y += 1; // empty space, can move up one block
        } else if (maze->maze[this_player->x][this_player->y + 1] == 2) {
            this_player->y += 1; // move up

            // remove the ghost
            captured = true;
            maze->maze[this_player->x][this_player->y] = 0;
            maze->nb_ghosts -= 1;

            printf("g");
            // increase score
            this_player->score += 10;
            printf("h");
            fprintf(stderr, "fd %d : captured a ghost on x=%d, y=%d\n", this_player->tcp_socket, this_player->x,
                    this_player->y);
            printf("i");
            send_score_multicast(this_player, game);

            // if this was the last ghost, this is the end of the game
            // this MUST be here (== at the capture of the ghost) and
            // not in the has_game_ended method because otherwise several
            // threads could send the message at the same time
            if (game->nb_ghosts_left == 0) {
                send_endgame_multicast(game);
            }
        } else { // wall or another player : blocked
            fprintf(stderr, "fd %d : ran into a wall at x=%d y=%d\n", this_player->tcp_socket,
                    this_player->x, this_player->y + 1);
            printf("k");
            break;
        }
    }

    // put the player in their new place in the maze
    maze->maze[this_player->x][this_player->y] = 3;
    pthread_mutex_unlock(&mutex);
    fprintf(stderr, "fd %d : moved %s to x=%d, y=%d\n", this_player->tcp_socket, "UP",
            this_player->x, this_player->y);

    // send message(s) with new position
    if (captured) {
        send_MOVEF(this_player);
    } else {
        send_MOVE(this_player);
    }
}


void move_down(int steps, player_data *this_player, maze_data *maze, game_data *game) {
    if (steps == -1) {
        return; // the header message wasn't read properly
    }

    bool captured = false;
    pthread_mutex_lock(&mutex); // protect the maze while moving the player

    for (int i = 0; i < steps; i++) {
        if (this_player->y - 1 == -1) {
            fprintf(stderr, "fd %d, already at the bottom of the maze, can't move\n", this_player->tcp_socket);
            break; // this is the bottom of the maze, can't go down
        } else if (maze->maze[this_player->x][this_player->y - 1] == 0) {
            this_player->y -= 1; // empty space, can move down one block
        } else if (maze->maze[this_player->x][this_player->y - 1] == 2) {
            this_player->y -= 1; // move up

            // remove the ghost
            captured = true;
            maze->maze[this_player->x][this_player->y] = 0;
            maze->nb_ghosts -= 1;

            // increase score
            this_player->score += 10;
            fprintf(stderr, "fd %d : captured a ghost on x=%d, y=%d\n", this_player->tcp_socket, this_player->x,
                    this_player->y);
            send_score_multicast(this_player, game);

            // if this was the last ghost, this is the end of the game
            // this MUST be here (== at the capture of the ghost) and
            // not in the has_game_ended method because otherwise several
            // threads could send the message at the same time
            if (game->nb_ghosts_left == 0) {
                send_endgame_multicast(game);
            }
        } else { // wall or another player : blocked
            fprintf(stderr, "fd %d : ran into a wall at x=%d y=%d\n", this_player->tcp_socket,
                    this_player->x, this_player->y - 1);
            printf("k");
            break;
        }
    }

    // put the player in their new place in the maze
    maze->maze[this_player->x][this_player->y] = 3;
    pthread_mutex_unlock(&mutex);
    fprintf(stderr, "fd %d : moved %s to x=%d, y=%d\n", this_player->tcp_socket, "DOWN",
            this_player->x, this_player->y);

    // send message(s) with new position
    if (captured) {
        send_MOVEF(this_player);
    } else {
        send_MOVE(this_player);
    }
}

void move_left(int steps, player_data *this_player, maze_data *maze, game_data *game) {
    if (steps == -1) {
        return; // the header message wasn't read properly in the previous method
    }

    bool captured_a_ghost = false;
    pthread_mutex_lock(&mutex); // protect the maze while moving the player
    for (int i = 0; i < steps; i++) {
        if (this_player->x - 1 == -1) {
            fprintf(stderr, "fd %d : already at the left of the maze, can't move\n", this_player->tcp_socket);
            break; // can't go left
        } else if (maze->maze[this_player->x - 1][this_player->y] == 0) { // free space
            this_player->x -= 1; // move
        } else if (maze->maze[this_player->x - 1][this_player->y] == 2) { // ghost
            this_player->x -= 1; // move

            // remove the ghost
            captured_a_ghost = true;
            maze->maze[this_player->x][this_player->y] = 0;
            game->nb_ghosts_left -= 1;

            // increase score
            this_player->score += 10;
            fprintf(stderr, "fd %d : captured a ghost on x=%d, y=%d\n", this_player->tcp_socket, this_player->x,
                    this_player->y);
            send_score_multicast(this_player, game);

            // if this was the last ghost, this is the end of the game
            // this MUST be here (== at the capture of the ghost) and
            // not in the has_game_ended method because otherwise several
            // threads could send the message at the same time
            if (game->nb_ghosts_left == 0) {
                send_endgame_multicast(game);
            }
        } else { // wall or another player : blocked
            fprintf(stderr, "fd %d : ran into a wall at x=%d y=%d\n", this_player->tcp_socket,
                    this_player->x - 1, this_player->y);
            break;
        }
    }

    // put the player in their new place in the maze
    maze->maze[this_player->x][this_player->y] = 3;
    pthread_mutex_unlock(&mutex);
    fprintf(stderr, "fd %d : moved %s to x=%d, y=%d\n", this_player->tcp_socket, "LEFT",
            this_player->x, this_player->y);

    // send message(s) with new position
    if (captured_a_ghost) {
        send_MOVEF(this_player);
    } else {
        send_MOVE(this_player);
    }
}

void move_right(int steps, player_data *this_player, maze_data *maze, game_data *game) {
    if (steps == -1) {
        return; // the header message wasn't read properly in the previous method
    }

    bool captured_a_ghost = false;
    pthread_mutex_lock(&mutex); // protect the maze while moving the player
    for (int i = 0; i < steps; i++) {
        if (this_player->x + 1 == maze->width) {
            fprintf(stderr, "fd %d : already at the right of the maze, can't move\n", this_player->tcp_socket);
            break; // can't go right
        } else if (maze->maze[this_player->x + 1][this_player->y] == 0) { // free space
            this_player->x += 1; // move
        } else if (maze->maze[this_player->x + 1][this_player->y] == 2) { // ghost
            this_player->x += 1; // move

            // remove the ghost
            captured_a_ghost = true;
            maze->maze[this_player->x][this_player->y] = 0;
            game->nb_ghosts_left -= 1;

            // increase score
            this_player->score += 10;
            fprintf(stderr, "fd %d : captured a ghost on x=%d, y=%d\n", this_player->tcp_socket, this_player->x,
                    this_player->y);
            send_score_multicast(this_player, game);

            // if this was the last ghost, this is the end of the game
            // this MUST be here (== at the capture of the ghost) and
            // not in the has_game_ended method because otherwise several
            // threads could send the message at the same time
            if (game->nb_ghosts_left == 0) {
                send_endgame_multicast(game);
            }
        } else { // wall or another player : blocked
            fprintf(stderr, "fd %d : ran into a wall at x=%d y=%d\n", this_player->tcp_socket,
                    this_player->x + 1, this_player->y);
            break;
        }
    }

    // put the player in their new place in the maze
    maze->maze[this_player->x][this_player->y] = 3;
    pthread_mutex_unlock(&mutex);
    fprintf(stderr, "fd %d : moved %s to x=%d, y=%d\n", this_player->tcp_socket, "RIGHT",
            this_player->x, this_player->y);

    // send message(s) with new position
    if (captured_a_ghost) {
        send_MOVEF(this_player);
    } else {
        send_MOVE(this_player);
    }
}


void send_MOVEF(player_data *this_player) {
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
    send_all(this_player->tcp_socket, mess, 21);
    fprintf(stderr, "fd %d : Send MOVEF message\n", this_player->tcp_socket);
}

void send_MOVE(player_data *this_player) {
    char mess[17];
    char *x = int_to_3_bytes(this_player->x);
    char *y_with_stars = int_to_3_bytes_with_stars(this_player->y);
    memmove(mess, "MOVE! ", 6); // NOLINT(bugprone-not-null-terminated-result)
    memmove(mess + 6, x, 3);
    memmove(mess + 9, " ", 1); // NOLINT(bugprone-not-null-terminated-result)
    memmove(mess + 10, y_with_stars, 6);

    free(x);
    free(y_with_stars);

    send_all(this_player->tcp_socket, mess, 16);
    fprintf(stderr, "fd %d : Sent [MOVE!] message\n", this_player->tcp_socket);
}

bool game_has_ended(game_data *game) {
    pthread_mutex_lock(&mutex);
    bool res = game->nb_ghosts_left == 0
               || game->nb_players == 0;
    pthread_mutex_unlock(&mutex);
    return res;
}

void player_quits(player_data *this_player, game_data *game) {
    // remove the player from the game
    pthread_mutex_lock(&mutex);
    game->players[this_player->player_number] = placeholder_player;
    game->nb_players -= 1;
    pthread_mutex_unlock(&mutex);

    // make this_player a placeholder so that the main function
    // will know to exit and kill this thread
    this_player->is_a_player = false;

    fprintf(stderr, "Player %d quits\n", this_player->tcp_socket);

    // no need to send the GOBYE message, it is sent
    // automatically at the end of the main function.
    // The sockets are also closed there.
}

void send_list_of_players_for_game(player_data *this_player, game_data *game) {
    char player_ids[4][8];
    bool is_a_player[4];

    // get the info with a mutex first,
    // then make the messages and send without the mutex
    // to spend the less time possible inside the mutex
    pthread_mutex_lock(&mutex);
    int nb_players = game->nb_players;
    for (int i = 0; i < 4; i++) {
        if (games[this_player->game_number].players[i].is_a_player) {
            memmove(player_ids[i], games[this_player->game_number].players[i].id, 8);
            is_a_player[i] = true;
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
    send_all(this_player->tcp_socket, first_message, 10);

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

            send_all(this_player->tcp_socket, message, 31);
        }
    }
    fprintf(stderr, "fd %d : sent GLIS! and GPLYR messages\n", this_player->tcp_socket);
}

void send_score_multicast(player_data *this_player, game_data *game) {
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
           their_addr, (socklen_t) sizeof(struct sockaddr_in));
    fprintf(stderr, "sent SCORE multicast message for player fd=%d, score=%d\n", this_player->tcp_socket,
            this_player->score);
}

void send_endgame_multicast(game_data *game) {
    char mess[22];

    // find the player with the highest score
    player_data best_player;
    int score_int = 0;
    for (int i = 0; i < 4; i++) {
        if (game->players[i].is_a_player && score_int < game->players[i].score) {
            score_int = game->players[i].score;
            best_player = game->players[i];
        }
    }

    // no need to protect with mutex : this is the end of the game,
    // there is no ghost left so there is no way to change the scores
    char *score = int_to_4_bytes(best_player.score);
    sprintf(mess, "ENDGA %s %s+++", best_player.id, score);
    free(score);

    struct sockaddr_in their_addr;
    sendto(game->multicast_socket, mess, 22, 0, (struct sockaddr *) &their_addr, sizeof(struct sockaddr_in));
    fprintf(stderr, "Sent ENDGA message : player fd=%d won with score %d\n", best_player.tcp_socket, best_player.score);
}

void send_message_to_all(player_data *this_player, game_data *game) {
    // read the message and send the multicast
    struct sockaddr_in addr;
    addr.sin_port = htons(4444 + this_player->game_number);

    inet_pton(AF_INET, MULTICAST_ADDR, &addr.sin_addr);

    printf("c\n");
    long length_of_message = read_and_send_message(game->multicast_socket, addr,
                                                   "MALL?", "MESSA", "MALL!", this_player);
    printf("d\n");
    // recv the complete MALL? message
    char buf[length_of_message];
    long length_to_recv = length_of_message; // stars included in the length
    printf("e\n");
    long res = recv(this_player->tcp_socket, buf, length_to_recv, 0);
    printf("g\n");
    isRecvRightLength(res, length_to_recv, "MALL?");
    printf("h\n");
}

void send_personal_message(player_data *this_player, game_data *game) {
    // read the recipient id (with the space before but not the space after)
    char id[10];
    long res = recv(this_player->tcp_socket, id, 9, 0);
    if (!isRecvRightLength(res, 9, "SEND?")) {
        return;
    }

    // check that the player is in that game
    player_data *recipient = NULL;
    for (int i = 0; i < 4; i++) {
        if (game->players[i].is_a_player
            && strncmp(&id[1], game->players[i].id, 8) == 0) {
            recipient = &game->players[i];
            break;
        }
    }
    // if player isn't in the game
    if (recipient == NULL) {
        send_all(this_player->tcp_socket, "NSEND***", 8);
        fprintf(stderr, "fd %d : Sent NSEND -> recipient id = %s is not in the game\n",
                this_player->tcp_socket, &id[1]);
        return;
    }
    printf("d\n");

    // read the message and send the multicast
    long length_of_message = read_and_send_message(recipient->udp_socket, *recipient->address,
                                                   "SEND?", "MESSP", "SEND!", this_player);
    printf("e\n");
    // recv the complete SEND? message
    char buf[length_of_message]; // stars included in the length
    long length_to_recv = length_of_message;
    printf("f\n");
    res = recv(this_player->tcp_socket, buf, length_to_recv, 0);
    printf("g\n");
    isRecvRightLength(res, length_to_recv, "SEND?");
}

long read_and_send_message(int socketfd, struct sockaddr_in their_addr, char *context, char *header_to_send,
                           char *ack_to_send, player_data *this_player) {
    // read 204 bytes = max size of the message + 1 (space after header) + 3 (the stars)
    // with option MSG_PEEK == don't remove data from the stream
    // => at the end we do a recv of the exact size of the message
    //    so that only the right amount of bytes are taken from the buffer
    //    and there are no additional "cached" request
    char buf[205];
    recv(this_player->tcp_socket, buf, 204, MSG_PEEK);
    buf[204] = '\0'; // to treat it as a string
    // find where the message ends
    char *start_of_stars = strstr(buf, "***");
    if (start_of_stars == NULL) { // didn't find a "***" in the string
        fprintf(stderr, "Reading a [%s] message, can't find *** at the end for player fd = %d\n"
                        "Ignoring the message\n", context, this_player->tcp_socket);
        return -1;
    }
    // the length of the message to send is the difference between the beginning and the end pointers
    long length_of_message = start_of_stars - buf;

    // copy only the relevant portion (== the message to send)
    char *message = malloc(length_of_message);
    memmove(message, buf, length_of_message);

    // make MESSA message
    // length of message + 6 (header+space) + 8 (id) + 1 (space between id and message) + 3 stars
    long length_to_send = length_of_message + 6 + 8 + 1 + 3;
    char complete_message[length_to_send];
    sprintf(complete_message, "%s %s %s+++", header_to_send, this_player->id, message);
    printf("8\n");
    // send multicast message;
    int res = sendto(socketfd, complete_message, length_to_send, 0,
                     (struct sockaddr *) &their_addr, sizeof(struct sockaddr_in));
    printf("%d\n", res);
    fprintf(stderr, "Sent %s message from player fd = %d\n", header_to_send, this_player->tcp_socket);
    printf("9\n");
    // send acknowledgement to initial sender
    char *ack;
    sprintf(ack, "%s***", ack_to_send);
    send_all(this_player->tcp_socket, ack, 8);
    printf("10\n");
    fprintf(stderr, "Sent %s acknowledgement to player fd=%d\n", ack_to_send, this_player->tcp_socket);

    free(message);
    return length_of_message;
}