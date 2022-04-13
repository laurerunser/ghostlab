#include "test.h"
extern game_data games[];
extern maze_data mazes[];

void init_test_1() {
    // no need for mutex because there are no concurrent threads yet

    // init the maze
    // => there MUST be at least one game defined at the beginning
    mazes[0].width = 30;
    mazes[0].height = 34;
    mazes[0].maze = malloc(mazes[0].width * sizeof(int *));
    for(int i = 0; i<mazes[0].width; i++) {
        mazes[0].maze[i] = malloc(mazes[0].height * sizeof(int));
        for (int j = 0; j<mazes[0].height; j++) {
            mazes[0].maze[i][j] = 0;
        }
    }

    // init some of the games
    for (int i = 0; i<256; i++) {
        games[i].is_created = false;
        games[i].maze = &mazes[0]; // only one maze to choose from
        games[i].has_started = false;
        games[i].nb_players = 0;
        games[i].nb_ghosts_left = 4;

        for (int j = 0; j < 4; j++) {
            // init player placeholder values
            games[i].players[j].is_a_player = false;
            games[i].players[j].sent_start = false;
            games[i].players[j].game_number = i;
        }
    }

    for (int i = 0; i<4; i++) {
        games[i].is_created = true;
        games[i].nb_players = i;
        for (int j = 0; j<i; j++) {
            games[i].players[j].is_a_player = true;
            memmove(games[i].players[j].id, "ID345678", 8);
        }
    }

    for (int i = 8; i<12; i++) {
        games[i].is_created = true;
        games[i].has_started = true;
    }

    for (int i = 45; i<76; i++) {
        games[i].is_created = true;
        games[i].nb_players = 4;
        for (int j = 0; j<4; j++) {
            games[i].players[j].is_a_player = true;
            memmove(games[i].players[j].id, "ID345678", 8);
        }
    }
}