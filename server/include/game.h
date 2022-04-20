#include <stdio.h>
#include <stdlib.h>
#include <malloc.h>
#include <string.h>

#include "game_data.h"
#include "socket_connections.h"

#define BROADCAST_IP "255.255.255.255"

void send_welcome_message();
void send_initial_position();

void *start_game(void *player);
void handle_game_requests();

bool game_has_ended();

/*
 * Receives the end of a UP/DO/LE/RIMOV message
 * (== the space, the nb on 3 bytes and the ***).
 * Returns the number read from the string.
 * Displays an error message if :
 * - message is of incorrect length
 * - the number cannot be parsed from the string
 * In case of error, returns -1
 */
int receive_move_message(char *context, char *buf);

/*
 * steps : the max number of steps to take
 * context : "UP" or "DOWN" // "LEFT" or "RIGHT"
 * direction : 1 to go up/right, -1 to go down/left
 */
void move_vertical(int steps, char* context, int direction);
void move_horizontal(int steps, char* context, int direction);


void send_MOVEF();
void send_MOVE();

void player_quits();

void send_list_of_players_for_game();

void send_score_multicast();
void send_endgame_multicast();