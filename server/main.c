#include "main.h"

int main(void) {
    // init the mutex
    pthread_mutex_init(&mutex, NULL);

    // create the main TCP socket that will listen to client's connections
    int sock_fd = create_TCP_socket();

    // main accept() loop for incoming clients
    while (1) {
        socklen_t sin_size = sizeof their_addr;
        struct sockaddr_in their_addr; // connector's address information

        // accept the connection
        int new_fd = accept(sockfd, their_addr, &sin_size);
        if (new_fd == -1) {
            perror("accept");
            continue;
        }
        fprintf(stderr, "accepted a connection : fd = %d, ip = %x\n", new_fd, their_addr.sin_addr.s_addr);

        // create a thread to handle to connection
        pthread_t thread_id;
        pthread_create(&thread_id,
                       attr,
                       handle_client_first_connection,
                       args
        );
    }
    return 0;
}