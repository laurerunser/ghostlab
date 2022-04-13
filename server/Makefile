#### HOW TO USE ####
# To add a file to cassini :
# - add the dir/name.o to the line OBJ_FILES
# - don't forget the .o instead of .c => the object files are created
#   automatically from the source files
#
# Don't touch anything else, the variables are here to do the work for you

CC ?= gcc
CLFAGS ?= -Wall

# tell the linker where to find the .h files
LIBINCLUDE = include
CFLAGS += -I$(LIBINCLUDE)

# list of the object files to compile
OBJ_FILES = main.o socket_connections.o pregame.o test.o

%.o: %.c $(DEPS)
	$(CC) -c -o $@ $< $(CFLAGS)

# compile the server
all: server

# the server
server: $(OBJ_FILES)
	$(CC) $(CFLAGS) -o server $(OBJ_FILES) -lpthread

# to remove the compiled files
.PHONY: clean
clean:
	rm server $(OBJ_FILES)
