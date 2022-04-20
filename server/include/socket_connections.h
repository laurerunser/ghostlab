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
#include <stdbool.h>

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

/*
 * Returns the int as a char[3]; with leading zeroes if needed
 * Don't forget to FREE the pointer after !
 */
char* int_to_3_bytes(int);

/*
 * Same with, but returns a char[6] with the "***" after the number
 */
char* int_to_3_bytes_with_stars(int);

/*
 * Returns true if received=expected, false otherwise.
 * If false, also prints an error message to stderr,
 * where [context] is the header of the message that was being read.
 */
void isRecvRightLength(long received, long expected, char* context);

#endif //GHOSTLAB_SOCKET_CONNECTIONS_H
