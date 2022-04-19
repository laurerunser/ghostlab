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
