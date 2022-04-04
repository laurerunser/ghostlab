#include "include/pregame.h"

// global variables defined in main.h
extern game_data games[];
extern pthread_mutex_t mutex;

void *handle_client_first_connection(void *args_p) {
    struct thread_arguments *args = (struct thread_arguments *) args_p;
    int sock_fd = args->sock_fd;

    // send the list of games
    send_list_of_games(sock_fd);
    fprintf(stderr, "GAMES and OGAME message sent to fd = %d\n", sock_fd);

    // wait for player's messages
    char buf[23]; // max size of messages is 23 (for REGIS)
    while (1) {
        recv(sock_fd, buf, 23, 0);

        if (strncmp("NEWPL", buf, 5) == 0) {

        } else if (strncmp("REGIS", buf, 5) == 0) {

        } else if (strncmp("SIZE?", buf, 5) == 0) {
            uint8_t game_id = buf[7];
            fprintf(stderr, "received SIZE? message from fd = %d for game_id = %d\n", sock_fd, game_id);
            send_size_of_maze(sock_fd, game_id);
        } else if (strncmp("LIST?", buf, 5) == 0) {
            uint8_t game_id = buf[7];
            fprintf(stderr, "received LIST? message from fd = %d for game_id = %d\n", sock_fd, game_id);
            send_list_of_players(sock_fd, game_id);
        } else if (strncmp("GAME?", buf, 5) == 0) {
            fprintf(stderr, "received GAME? message from fd = %d\n", sock_fd);
            send_list_of_games(sock_fd);
        } else if (strncmp("START", buf, 5) == 0) {

        }
        return NULL;
    }
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
        if (games->is_created && !games[i].has_started) {
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

    // send the messages
    send_all(sock_fd, first_message, 10);
    for (int i = 0; i < free_message_index; i++) {
        send_all(sock_fd, o_game_messages[i], 12);
    }

    fprintf(stderr, "sent GAME and OGAME messages to fd = %d\n", sock_fd);
}

void send_list_of_players(int sock_fd, int game_id) {

    if (!games[game_id].is_created) { // game doesn't exist
        send_all(sock_fd, "DUNNO***", 8);
        fprintf(stderr, "sent DUNNO message to fd = %d\n", sock_fd);
    } else { // game exists
        // get number and ids of players
        char player_ids[4][8];
        int nb_players = 0;
        pthread_mutex_lock(&mutex);
        for (int i = 0; i < 4; i++) {
            if (games[game_id].players[i].is_a_player) {
                memmove(player_ids[i], games[game_id].players[i].id, 8);
                nb_players += 1;
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
        send_all(sock_fd, first_message, 12);

        // make and send the [PLAYR...] message
        for (int i = 0; i < 4; i++) {
            if (player_ids[i] != NULL) {
                char message[17];
                sprintf(message, "PLAYR %s***", player_ids[i]);
                send_all(sock_fd, message, 17);
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