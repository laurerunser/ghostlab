#include "include/pregame.h"

// global variables defined in main.h
extern game_data games[];
extern pthread_mutex_t mutex;

void *handle_client_first_connection(void *args_p) {
    struct thread_arguments *args = (struct thread_arguments *)args_p;
    int sock_fd = args->sock_fd;

    // send the list of games
    send_list_of_games(sock_fd);
    fprintf(stderr, "GAMES and OGAME message sent to fd = %d", sock_fd);

    return NULL; // return NULL at the end
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
    for (int i = 0; i<256; i++) {
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
            memmove(o_game_messages[free_message_index] + 6, (uint8_t * ) & i, 1);
            memmove(o_game_messages[free_message_index] + 7, " ", 1);
            memmove(o_game_messages[free_message_index] + 8, (uint8_t *)&registered_players[i], 1);
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

}
