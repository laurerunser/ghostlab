#include <stdio.h>
#include <malloc.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <netinet/in.h>
#include <string.h>

#include "game_data.h"
#include "socket_connections.h"
#include "game.h"


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

// checks if the player is registered into a game
// if so, changes the player's status, and checks to see if the game should start (and then starts it)
// otherwise, ignores the message
// Returns true if the START message is accepted, false if it is ignored
bool handle_start_message(int sock_fd, player_data current_player);

// returns true if the game the player is registered in has started
bool game_has_started(player_data current_player);

// tries to create a new game and register the player in it.
// Sends the [REGOK...] or [REGNO...] message
player_data create_new_game(int sock_fd, char *buf, struct sockaddr_in *client_address, player_data current_player);

// registers a player into a game that already exists
// Sends the [REGOK...] or [REGNO...] message
player_data
register_player(int sock_fd, int game_id, char *buf, struct sockaddr_in *client_address, player_data current_player);

/*
 * Adds a player to a game.
 * The method MUST be used INSIDE A MUTEX
 * - game_id : the id of the game
 * - player_index : the index where to put the player in the game->players[] array
 * - buf : the buffer from which to read the udp port
 * - sock_fd : the tcp socket of the player
 * - client_address : the address of the player
 */
player_data
add_player_to_game(int game_id, int player_index, char *buf, int sock_fd, struct sockaddr_in *client_address);

// unregisters the player from the game
// Sends [UNROK...] or [DUNNO...] messages
void unregister_player(int sock_fd, player_data current_player);
#endif //GHOSTLAB_PREGAME_H
