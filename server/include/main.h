#include "game_data.h"
#include "socket_connections.h"
#include "pregame.h"
#include "test.h"

#ifndef GHOSTLAB_MAIN_H
#define GHOSTLAB_MAIN_H

// array of all the games
game_data games[256];

// array of all the available mazes
maze_data mazes[1];

// mutex to protect the data
pthread_mutex_t mutex;


#endif //GHOSTLAB_MAIN_H
