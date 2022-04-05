#include <arpa/inet.h>
#include <bits/pthreadtypes.h>
#include <errno.h>
#include <netdb.h>
#include <netinet/in.h>
#include <poll.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <time.h>
#include <unistd.h>
#include <pthread.h>

#ifndef GHOSTLAB_SOCKET_CONNECTIONS_H
#define GHOSTLAB_SOCKET_CONNECTIONS_H

#define TCP_PORT "3490" // the port users will be connecting to
#define BACKLOG 0 // how many pending connections queue will hold; 0 for system default

/* Sends a TCP message. Makes sure all the message is sent through,
 * even if the message is very big.
 * - sock_fd : the socket
 * - buf : contains the message to send
 * - len : the length of the message to send
*/
void send_all(int, char*, int);

/*
 * Creates a TCP socket and returns the file descriptor
 */
int create_TCP_socket(void);

#endif //GHOSTLAB_SOCKET_CONNECTIONS_H