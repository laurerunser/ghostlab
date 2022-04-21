#include "include/pregame.h"

// global variables defined in main.h
extern game_data games[];
extern maze_data mazes[];
extern pthread_mutex_t mutex;

// to remember which game the player is registered in
// at beginning, game_number=-1 to signify this is a placeholder
player_data current_player;
player_data placeholder_player;

// TODO if client recv == 0 -> client disconnected
// -> unregister player from games
// end thread

// TODO : what to do if client recv == -1 ??


void *handle_client_first_connection(void *args_p) {
    struct thread_arguments *args = (struct thread_arguments *) args_p;
    int sock_fd = args->sock_fd;

    // send the list of games
    send_list_of_games(sock_fd);
    fprintf(stderr, "GAMES and OGAME message sent to fd = %d\n", sock_fd);

    // at beginning of loop, current_player is a placeholder
    // game_number = -1 to say the player is not registered in a game
    placeholder_player.game_number = -1;
    current_player = placeholder_player;

    // wait for player's messages
    char buf[24]; // max size of messages is 24 (for REGIS)
    while (!game_has_started()) {
        // receive header of message
        long res = recv(sock_fd, buf, 5, 0);
        if (!isRecvRightLength(res, 5, "Header of a pregame message")) {
            fprintf(stderr, "Ignoring incomplete message\n");
            break;
        }

        // handle message
        // If the message doesn't follow the protocol, ignore it
        if (strncmp("NEWPL", buf, 5) == 0) {
            res = recv(sock_fd, &buf[5], 17, 0);
            if (!isRecvRightLength(res, 17, "NEWPL")) {
                fprintf(stderr, "Ignoring incomplete message\n");
                break;            }
            fprintf(stderr, "received NEWPL message from fd = %d\n", sock_fd);
            create_new_game(sock_fd, buf, args->client_address);
        } else if (strncmp("REGIS", buf, 5) == 0) {
            res = recv(sock_fd,&buf[5], 20, 0);
            if (!isRecvRightLength(res, 20, "REGIS")) {
                fprintf(stderr, "Ignoring incomplete message\n");
                break;            }
            uint8_t game_id = buf[21];
            printf("%s\n", buf);
            fprintf(stderr, "received REGIS message from fd = %d for game id = %d\n", sock_fd, game_id);
            register_player(sock_fd, game_id, buf, args->client_address);
        } else if (strncmp("UNREG", buf, 5) == 0) {
            res = recv(sock_fd, &buf[5], 3, 0);
            if (!isRecvRightLength(res, 3, "UNREG")) {
                fprintf(stderr, "Ignoring incomplete message\n");
                break;
             }
            fprintf(stderr, "received UNREG message from fd = %d\n", sock_fd);
            unregister_player(sock_fd);
        } else if (strncmp("SIZE?", buf, 5) == 0) {
            res = recv(sock_fd, &buf[5], 5, 0);
            if (!isRecvRightLength(res, 5, "SIZE?")) {
                fprintf(stderr, "Ignoring incomplete message\n");
                break;
             }
            uint8_t game_id = buf[6];
            fprintf(stderr, "received SIZE? message from fd = %d "
                            "for game_id = %d\n", sock_fd, game_id);
            send_size_of_maze(sock_fd, game_id);
        } else if (strncmp("LIST?", buf, 5) == 0) {
            res = recv(sock_fd, &buf[5], 5, 0);
            if (!isRecvRightLength(res, 5, "LIST?")) {
                fprintf(stderr, "Ignoring incomplete message\n");
                break;            }
            uint8_t game_id = buf[6];
            fprintf(stderr, "received LIST? message from fd = %d "
                            "for game_id = %d\n", sock_fd, game_id);
            send_list_of_players(sock_fd, game_id);
        } else if (strncmp("GAME?", buf, 5) == 0) {
            res = recv(sock_fd, &buf[5], 3, 0);
            if (!isRecvRightLength(res, 3, "GAME?")) {
                fprintf(stderr, "Ignoring incomplete message\n");
                break;            }
            fprintf(stderr, "received GAME? message from fd = %d\n", sock_fd);
            send_list_of_games(sock_fd);
        } else if (strncmp("START", buf, 5) == 0) {
            res = recv(sock_fd, &buf[5], 3, 0);
            if (!isRecvRightLength(res, 3, "START")) {
                fprintf(stderr, "Ignoring incomplete message\n");
                break;
            }
            fprintf(stderr, "received START message from fd = %d\n", sock_fd);
            bool stop = handle_start_message(sock_fd);
            // player has sent START and cannot send messages anymore for pregame stuff
            // this thread dies (returns NULL) and another one will be spun up
            // by the function that starts game (one new thread for each player)
            if (stop) return NULL;
        }
    }
    return NULL;
}


void send_list_of_games(int sock_fd) {
    // We are going through the 255 games twice because I didn't
    // want the creation and sending of the message to be
    // in the mutex lock.
    // Since it's only 255 loops, it will not add too much time to
    // the program, and I prefer to only do the minimum of work
    // under a mutex lock

    // array to store the number of registered players for each game
    // default value is 5, so that it reads as a "full" game even if the
    // game doesn't exist or has been started.
    int registered_players[256];
    for (int i = 0; i < 256; i++) {
        registered_players[i] = 5;
    }

    // get the number of registered players for each game

    pthread_mutex_lock(&mutex);
    for (int i = 0; i < 256; i++) {
        if (games[i].is_created && !games[i].has_started) {
            registered_players[i] = games[i].nb_players;
        }
    }
    pthread_mutex_unlock(&mutex);


    // go through each registered_players value
    // and make the OGAME message if there is still a spot left in the game
    char *o_game_messages[256];
    int free_message_index = 0;
    uint8_t nb_available_games = 0;
    for (int i = 0; i < 256; i++) {
        if (registered_players[i] < 4) { // game is available
            nb_available_games += 1;

            // make the message [OGAME m s***]
            // - m = the number of the game
            // - s = the number of registered players
            // - [ ] are not included in the message
            o_game_messages[free_message_index] = malloc(12); // 12 bytes in each message
            memmove(o_game_messages[free_message_index], "OGAME ", 6);
            memmove(o_game_messages[free_message_index] + 6, (uint8_t *) &i, 1);
            memmove(o_game_messages[free_message_index] + 7, " ", 1);
            memmove(o_game_messages[free_message_index] + 8, (uint8_t *) &registered_players[i], 1);
            memmove(o_game_messages[free_message_index] + 9, "***", 3);

            free_message_index += 1;
        }
    }

    // Make the [GAMES n***] message
    // - n = the number of available games
    // [ ] not included in the message
    char first_message[10];
    memmove(first_message, "GAMES ", 6); // NOLINT(bugprone-not-null-terminated-result)
    memmove(first_message + 6, &nb_available_games, 1);
    memmove(first_message + 7, "***", 3); // NOLINT(bugprone-not-null-terminated-result)
    fprintf(stderr, "sent GAMES message to fd = %d\n", sock_fd);

    // send the messages
    send_all(sock_fd, first_message, 10);
    for (int i = 0; i < free_message_index; i++) {
        send_all(sock_fd, o_game_messages[i], 12);
        free(o_game_messages[i]);
    }

    fprintf(stderr, "sent OGAME messages to fd = %d\n", sock_fd);
}

void send_list_of_players(int sock_fd, int game_id) {
    if (!games[game_id].is_created) { // game doesn't exist
        send_all(sock_fd, "DUNNO***", 8);
        fprintf(stderr, "sent DUNNO message to fd = %d\n", sock_fd);
    } else { // game exists
        // get number and ids of players
        char player_ids[4][8];
        bool is_a_player[4];
        int nb_players = 0;
        pthread_mutex_lock(&mutex);
        for (int i = 0; i < 4; i++) {
            if (games[game_id].players[i].is_a_player) {
                memmove(player_ids[i], games[game_id].players[i].id, 8);
                is_a_player[i] = true;
                nb_players += 1;
            } else {
                is_a_player[i] = false;
            }
        }
        // unlock mutex
        pthread_mutex_unlock(&mutex);

        // make and send [LIST!...] message
        char first_message[12];
        memmove(first_message, "LIST! ", 6); // NOLINT(bugprone-not-null-terminated-result)
        memmove(first_message + 6, (uint8_t *) &game_id, 1);
        memmove(first_message + 7, " ", 1); // NOLINT(bugprone-not-null-terminated-result)
        memmove(first_message + 8, (uint8_t *) &nb_players, 1);
        memmove(first_message + 9, "***", 3); // NOLINT(bugprone-not-null-terminated-result)
        send_all(sock_fd, first_message, sizeof(first_message));

        // make and send the [PLAYR...] message
        for (int i = 0; i < 4; i++) {
            if (is_a_player[i]) {
                char message[17];
                memmove(message, "PLAYR ", 6); // NOLINT(bugprone-not-null-terminated-result)
                memmove(message + 6, player_ids[i], 8);
                memmove(message + 14, "***", 3); // NOLINT(bugprone-not-null-terminated-result)
                send_all(sock_fd, message, sizeof(message));
            }
        }
        fprintf(stderr, "sent LIST! and PLAYR messages to fd = %d\n", sock_fd);
    }
}

void send_size_of_maze(int sock_fd, uint8_t game_id) {
    // the mutex is unlocked in each branch of the if
    // to minimize locked time (not locked when sending the messages)
    pthread_mutex_lock(&mutex);

    if (!games[game_id].is_created) { // game doesn't exist
        pthread_mutex_unlock(&mutex);
        char message[] = "DUNNO***";
        send_all(sock_fd, message, 8);
        fprintf(stderr, "sent DUNNO message to fd = %d\n", sock_fd);
    } else {
        // we are not testing if the game has been started yet, or
        // if it has too many players for the user to register
        // => a player can see the size of the maze of any game
        //    as long as it exists
        uint16_t width = ntohs(games[game_id].maze->width);
        uint16_t height = ntohs(games[game_id].maze->height);
        pthread_mutex_unlock(&mutex);

        char message[16];
        memmove(message, "SIZE! ", 6); // NOLINT(bugprone-not-null-terminated-result)
        memmove(message + 6, (uint8_t *) &game_id, 1);
        memmove(message + 7, " ", 1); // NOLINT(bugprone-not-null-terminated-result)
        memmove(message + 8, &width, 2);
        memmove(message + 10, " ", 1); // NOLINT(bugprone-not-null-terminated-result)
        memmove(message + 11, &height, 2);
        memmove(message + 13, "***", 3); // NOLINT(bugprone-not-null-terminated-result)

        send_all(sock_fd, message, 16);
        fprintf(stderr, "sent SIZE! message to fd = %d\n", sock_fd);
    }
}

bool handle_start_message(int sock_fd) {
    if (current_player.game_number == -1) { // if player is not registered, just ignore the START message
        fprintf(stderr, "received START message from fd = %d "
                        "but player is not registered into a game\n", sock_fd);
        return false;
    }

    // update player status in the game
    games[current_player.game_number].players[current_player.player_number].sent_start = true;

    // player is registered and ready to go
    fprintf(stderr, "Player fd = %d is ready to start\n", sock_fd);

    // check if the game is ready to start
    bool ready_to_start = true;
    if (games[current_player.game_number].nb_players < 4) {
        ready_to_start = false;
    }
    for (int i = 0; i < 4; i++) {
        if (!games[current_player.game_number].players[i].sent_start) {
            ready_to_start = false;
        }
    }

    if (ready_to_start) {
        fprintf(stderr, "Game %d is ready to start\n", current_player.game_number);

        // create 4 threads to run the game (see code in game.c file)
        player_data *players = games[current_player.game_number].players;
        for (int i = 0; i<4; i++) {
            pthread_t thread_id;
            pthread_create(&thread_id,
                           NULL,
                           start_game,
                           (void *)&players[i]
            );
        }
    }

    return true;
}

bool game_has_started() {
    if (current_player.game_number == -1) {
        return false;
    } else {
        return games[current_player.game_number].has_started;
    }
}

void send_regok_message(int sock_fd, int game_id) {
    char message[10];
    memmove(message, "REGOK ", 6); // NOLINT(bugprone-not-null-terminated-result)
    memmove(message + 6, (uint8_t *) &game_id, 1);
    memmove(message + 7, "***", 3); // NOLINT(bugprone-not-null-terminated-result)

    send_all(sock_fd, message, 10);
}

void create_new_game(int sock_fd, char *buf, struct sockaddr_in* client_address) {
    // check if player is already registered
    if (current_player.game_number != -1) {
        char message[] = "REGNO***";
        send_all(sock_fd, message, 8);
        fprintf(stderr, "Sent [REGNO] to player fd = %d tried to create a new game "
                        "but they are already registered into game number %d\n",
                sock_fd, current_player.game_number);
    }

    // need to lock the entire game array until the game is created so that no-one takes the spot
    // that we want to put the new game in
    pthread_mutex_lock(&mutex);

    // check if there is an empty spot for a new game
    int game_id = -1;
    for (int i = 0; i < 256; i++) {
        if (!games[i].is_created) {
            game_id = i;
            break;
        }
    }
    if (game_id == -1) { // there is no space left to create a new game
        char message[] = "REGNO***";
        send_all(sock_fd, message, 8);
        fprintf(stderr, "Sent [REGNO] to player fd = %d tried to create a new game "
                        "but there is not space left\n", sock_fd);
    }

    // everything is ok, creating the game
    games[game_id].is_created = true;
    games[game_id].has_started = false;
    games[game_id].nb_ghosts_left = 4;

    // create the multicast socket
    char *port;
    sprintf(port, "%d", game_id+4444);
    games[game_id].multicast_socket = create_UDP_socket(*client_address, port);

    // copy of the maze
    int **maze_blocks = malloc(mazes[0].width * sizeof(int *));
    for (int i = 0; i<mazes[0].width; i++) {
        maze_blocks[i] = malloc(mazes[0].height * sizeof(int));
        memcpy(maze_blocks[i], mazes[0].maze[i], mazes[0].height);
    }
    maze_data *maze_tmp = malloc(sizeof(maze_data));
    maze_tmp->height = mazes[0].height;
    maze_tmp->width = mazes[0].width;
    games[game_id].maze = maze_tmp;

    // add players placeholders
    for (int j = 0; j < 4; j++) {
        // init player placeholder values
        games[game_id].players[j].is_a_player = false;
        games[game_id].players[j].sent_start = false;
        games[game_id].players[j].game_number = game_id;
        games[game_id].players[j].player_number = j;
    }

    // add the first player
    add_player_to_game(game_id, 0, buf, sock_fd, client_address);
    pthread_mutex_unlock(&mutex);

    // send the message
    send_regok_message(sock_fd, game_id);
    fprintf(stderr, "Player fd = %d has created the game id = %d\n", sock_fd, game_id);
}

void register_player(int sock_fd, int game_id, char* buf, struct sockaddr_in* client_address) {
    // check if player is already registered in a game
    if (current_player.game_number != -1) {
        send_all(sock_fd, "REGNO***", 8);
        fprintf(stderr, "Sent [REGNO] to player fd = %d tried to register into game number %d "
                        "but they are already registered into game number %d\n",
                sock_fd, game_id, current_player.game_number);
        return;
    }

    pthread_mutex_lock(&mutex);

    // check if game exists
    if (!games[game_id].is_created) {
        send_all(sock_fd, "REGNO***", 8);
        fprintf(stderr, "Sent [REGNO] to player fd = %d tried to register into game number %d "
                        "but game doesn't exist\n",
                sock_fd, game_id);
        return;
    }

    // check that game is not already started
    if (games[game_id].has_started) {
        send_all(sock_fd, "REGNO***", 8);
        fprintf(stderr, "Sent [REGNO] to player fd = %d tried to register into game number %d "
                        "but game has already started\n",
                sock_fd, game_id);
        return;
    }

    // check if the game has a spot left
    int spot_left = -1;
    for (int i = 0; i<4; i++) {
        if (!games[game_id].players[i].is_a_player) {
            spot_left = i;
            break;
        }
    }
    if (spot_left == -1) {
        send_all(sock_fd, "REGNO***", 8);
        fprintf(stderr, "Sent [REGNO[ to player fd = %d tried to register into game number %d "
                        "but there is no spot left\n",
                sock_fd, game_id);
        return;
    }

    // everything is OK, adding player to the game
    add_player_to_game(game_id, spot_left, buf, sock_fd, client_address);

    pthread_mutex_unlock(&mutex);

    // send message
    send_regok_message(sock_fd, game_id);
    fprintf(stderr,"Player fd = %d is registered into game number %d\n",
            sock_fd, game_id);
}

void add_player_to_game(int game_id, int player_index, char *buf, int sock_fd, struct sockaddr_in *client_address) {
    // this method MUST be used inside of a mutex !!

    games[game_id].players[player_index].is_a_player = true;
    memmove(games[game_id].players[0].id, &buf[7], 8); // id starts at position 7
    games[game_id].players[player_index].tcp_socket = sock_fd;

    // get player's udp port
    char udp_port[5];
    memmove(udp_port, &buf[16], 4);
    udp_port[4] = '\0';

    // make the UDP socket
    games[game_id].players[player_index].udp_socket = create_UDP_socket(*client_address, udp_port);
    games[game_id].players[player_index].address = client_address;

    // update current_player
    current_player = games[game_id].players[0];

    // update nb of players in the game
    games[game_id].nb_players += 1;
}

void unregister_player(int sock_fd) {
    if (current_player.game_number == -1) { // player is not registered in a game
        send_all(sock_fd, "DUNNO***", 8);
        return;
    }

    // unregister user
    pthread_mutex_lock(&mutex);
    int game_id = current_player.game_number;
    games[game_id].players[current_player.player_number].is_a_player = false;
    pthread_mutex_unlock(&mutex);

    // update current_player to the placeholder
    current_player = placeholder_player;

    // send message
    char message[10];
    memmove(message, "UNROK ", 6); // NOLINT(bugprone-not-null-terminated-result)
    memmove(message + 6, (uint8_t *)&current_player.game_number, 1);
    memmove(message + 7, "***", 3); // NOLINT(bugprone-not-null-terminated-result)

    send_all(sock_fd, message, 10);
    fprintf(stderr, "Player fd = %d unregistered from games number %d\n", sock_fd, game_id);
}