#include <stdio.h>
#include <malloc.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <netinet/in.h>
#include <string.h>

#include "game_data.h"
#include "socket_connections.h"

#ifndef GHOSTLAB_PREGAME_H
#define GHOSTLAB_PREGAME_H

// Main thread function that will handle the client's message
// for the pregame part
void *handle_client_first_connection(void *args);

// sends the [GAMES...] and [OGAME...] messages
void send_list_of_games(int sockfd);

// sends the [DUNNO...] or [LIST!...] and [PLAYR...] messages
void send_list_of_players(int sockfd, int game_id);

// sends the [DUNNO...] or [SIZE! m h w***] message
void send_size_of_maze(int sock_fd, uint8_t game_id);

#endif //GHOSTLAB_PREGAME_H
