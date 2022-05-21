#include "include/test.h"
extern game_data games[];
extern maze_data mazes[];

void add_walls_and_default_pos() {
    int** m = mazes[0].maze;

    int tab[7][6]={
        {1, 1, 1, 0, 1, 1},
        {0, 0, 0, 0, 0, 1},
        {1, 1, 1, 1, 0, 1},
        {1, 0, 0, 0, 0, 1},
        {1, 0, 0, 0, 0, 1},
        {1, 0, 1, 1, 0, 0},
        {1, 0, 1, 1, 1, 1}};
    
    for (int i = 0; i<7; i++) {
        for (int j = 0; j<6; j++) {
            m[i][j]= tab[i][j];
        }
    }

    
/*
    // add walls
    for (int i = 0; i<6; i++) {
        m[i][0] = 1;
        m[i][2] = 1;
    }
    m[3][0] = 0;
    m[4][3] = 0;
    m[1][5] = 1;

    for (int i = 3; i < 7; i++) {
        m[0][i] = 1;
        m[2][i] = 1;
        m[5][i] = 1;
    }
    m[2][4] = 0;
    m[3][5] = 1;
    m[3][6] = 1;
    m[4][6] = 1;
    */

    // add players positions
    int *x = mazes[0].x_start;
    x[0] = 0;
    x[1] = 1;
    x[2] = 5;
    x[3] = 5;
    int *y = mazes[0].y_start;
    y[0] = 2;
    y[1] = 5;
    y[2] = 4;
    y[3] = 1;

    // add players to the maze
    m[0][2] = 3;
    m[1][5] = 3;
    m[5][4] = 3;
    m[5][1] = 3;

    // add ghosts
    m[1][2] = 2;
    m[3][2] = 2;
    m[3][3] = 2;
    m[3][4] = 3;
}

void init_test_1() {
    // no need for mutex because there are no concurrent threads yet

    // init the maze
    // => there MUST be at least one game defined at the beginning
    mazes[0].width = 7; // columns 0 to 6 == 7 cols
    mazes[0].height = 6; // lines 0 to 5 == 6 lines

    // malloc the maze and init all blocks to 0
    mazes[0].maze = malloc(mazes[0].width * sizeof(int *));
    for(int i = 0; i<mazes[0].width; i++) {
        mazes[0].maze[i] = malloc(mazes[0].height * sizeof(int));
        for (int j = 0; j<mazes[0].height; j++) {
            mazes[0].maze[i][j] = 0;
        }
    }

    // add the walls (the maze on the teacher's pdf)
    // and the default starting positions for the players
    add_walls_and_default_pos();


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