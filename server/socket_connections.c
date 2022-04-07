#include "include/socket_connections.h"

/* Sends a TCP message. Makes sure all the message is sent through,
 * even if the message is very big.
 * - sock_fd : the socket
 * - buf : contains the message to send
 * - len : the length of the message to send
*/
void send_all(int sock_fd, char *buf, int len) {
    int total = 0; // how many bytes we've sent
    int bytes_left = len; // how many we have left to send
    int n;
    while (total < len) { // keep sending until all the message went through
        n = send(sock_fd, buf + total, bytes_left, 0);
        if (n == -1) {
            break;
        }
        total += n;
        bytes_left -= n;
    }
}


/*
 * Casts the sockaddr into the sockaddr_in(6) depending on whether it is
 * a IPv4 or IPv6 address
*/
void *get_in_addr(struct sockaddr *sa) {
    if (sa->sa_family == AF_INET) {
        return &(((struct sockaddr_in *)sa)->sin_addr);
    } else {
        return &(((struct sockaddr_in6 *)sa)->sin6_addr);
    }
}

/*
 * Creates a TCP socket and returns the file descriptor
 */
int create_TCP_socket(void) {
    int sockfd;
    struct addrinfo hints, *servinfo, *p;

    memset(&hints, 0, sizeof hints);
    hints.ai_family = AF_UNSPEC;
    hints.ai_socktype = SOCK_STREAM; // or another type of socket
    hints.ai_flags = AI_PASSIVE; // use my IP

    // try to get the info. If != 0 there was an error.
    // the NULL value in getaddrinfo + the AI_PASSIVE flag above
    // will create a socket on localhost, suitable for bind and accept
    int rv; // to remember the error code if needed
    if ((rv = getaddrinfo(NULL, TCP_PORT, &hints, &servinfo)) != 0) {
        fprintf(stderr, "getaddrinfo: %s\n", gai_strerror(rv));
        return EXIT_FAILURE;
    }


    // loop through all the results and bind to the first we can
    for (p = servinfo; p != NULL; p = p->ai_next) {
        // try to create the socket
        if ((sockfd = socket(p->ai_family, p->ai_socktype, p->ai_protocol)) == -1) {
            perror("socket creation");
            continue;
        }

        // try to set options
        // SOL_SOCKET is to set the options at socket level
        // SO_REUSEADDR allows to reuse local addresses and ports
        // => we set it to 1 to activate it
        // (same procedure with any other option)
        int yes = 1;
        if (setsockopt(sockfd, SOL_SOCKET, SO_REUSEADDR, &yes, sizeof(int)) == -1) {
            perror("setsockopt");
            exit(EXIT_FAILURE);
        }

        // try to bind
        if (bind(sockfd, p->ai_addr, p->ai_addrlen) == -1) {
            close(sockfd);
            perror("bind");
            continue;
        }

        break; // break once one valid address was found
    }

    freeaddrinfo(servinfo); // we are done with this structure

    // make sure we are really bound
    if (p == NULL) {
        fprintf(stderr, "failed to bind\n");
        exit(EXIT_FAILURE);
    }

    // start listening
    if (listen(sockfd, BACKLOG) == -1) {
        perror("listen");
        exit(EXIT_FAILURE);
    }

    fprintf(stderr, "server TCP socket ready to listen to connections...\n");
    return sockfd;
}