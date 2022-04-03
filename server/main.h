#include "game_data.h"
#include "socket_connections.h"
#include "pregame.h"

#ifndef GHOSTLAB_MAIN_H
#define GHOSTLAB_MAIN_H

// array of all the games
game_data[256] games;

// array of all the available mazes
maze_data[1] maze;

// mutex to protect the data
pthread_mutex_t mutex;

// the arguments that the main function gives to the threads.
typedef struct thread_arguments {
    int sock_fd;
    struct sockaddr_in client_address;
} thread_arguments;

#endif //GHOSTLAB_MAIN_H
