#include <stdio.h>
#include <stdlib.h>
#include <malloc.h>
#include <string.h>

#include "game_data.h"
#include "socket_connections.h"

#define BROADCAST_IP "255.255.255.255"

void send_welcome_message(player_data *this_player, maze_data *maze, game_data *game);

void send_initial_position(player_data *this_player, maze_data *maze);

void *start_game(void *player);

void handle_game_requests(player_data *this_player, game_data *game, maze_data *maze);

bool game_has_ended(game_data *game);

/*
 * Receives the end of a UP/DO/LE/RIMOV message
 * (== the space, the nb on 3 bytes and the ***).
 * Returns the number read from the string.
 * Displays an error message if :
 * - message is of incorrect length
 * - the number cannot be parsed from the string
 * In case of error, returns -1
 */
int receive_move_message(char *context, char *buf, player_data *this_player);

/*
 * steps : the max number of steps to take
*/
void move_up(int steps, player_data *this_player, maze_data *maze, game_data *game);

void move_down(int steps, player_data *this_player, maze_data *maze, game_data *game);

void move_left(int steps, player_data *this_player, maze_data *maze, game_data *game);

void move_right(int steps, player_data *this_player, maze_data *maze, game_data *game);

void send_MOVEF(player_data *this_player);

void send_MOVE(player_data *this_player);

void player_quits(player_data *this_player, game_data *game);

void send_list_of_players_for_game(player_data *this_player, game_data *game);

void send_score_multicast(player_data *this_player, game_data *game);

void send_endgame_multicast(game_data *game);

void send_message_to_all(player_data *this_player, game_data *game);

void send_personal_message(player_data *this_player, game_data *game);

/*
 * Returns the length of the message that was read (with the *** at the end)
 * context : "MALL?" or "SEND?" -> for error messages
 * header_to_send : "MESSA" or "MESSP" -> multicast header
 * ack : "MALL!" or "SEND!" -> TCP acknowledgement header
 */
long read_and_send_message(int socketfd, struct sockaddr_in their_addr, char *context, char *header_to_send,
                           char *ack_to_send, player_data *this_player);