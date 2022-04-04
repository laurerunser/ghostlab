#include "include/main.h"

// gives default values to the games and players structures
void init_data_structures() {
    // no need for mutex because there are no concurrent threads yet
    for (int i = 0; i<256; i++) {
        games[i].is_created = false;
        games[i].maze = &maze[0]; // only one maze to choose from
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
}
// TODO : this data init should for the most part be done
// when creating the game
// Only the part about setting the is_created to false should stay here


int main(void) {
    // init the data
    //init_data_structures();
    init_test_1();

    // init the mutex
    pthread_mutex_init(&mutex, NULL);

    // create the main TCP socket that will listen to client's connections
    int sock_fd = create_TCP_socket();

    // main accept() loop for incoming clients
#pragma clang diagnostic push
#pragma ide diagnostic ignored "EndlessLoop"
    while (1) {
        // client's address
        struct sockaddr_in* their_addr = malloc(sizeof (struct sockaddr_in));
        socklen_t sin_size = sizeof(struct sockaddr_in);

        // accept the connection
        int *new_fd = (int *)malloc(sizeof(int));
        *new_fd = accept(sock_fd, (struct sockaddr *)&their_addr, &sin_size);
        if (*new_fd == -1) {
            perror("accept");
            continue;
        }
        fprintf(stderr, "accepted a connection : fd = %d\n", *new_fd);

        struct thread_arguments args;
        args.sock_fd = *new_fd;
        args.client_address = their_addr;

        // create a thread to handle to connection
        pthread_t thread_id;
        pthread_create(&thread_id,
                       NULL,
                       handle_client_first_connection,
                       (void *)&args
        );
    }
#pragma clang diagnostic pop
}
