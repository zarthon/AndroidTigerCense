#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <termios.h>
#include <stdio.h>
#include <unistd.h>
#include <strings.h>
#include <time.h>
#include <sys/ipc.h>
#include <sys/shm.h>


#define BAUDRATE B115200
#define DELAY_SYNC 20000 // Specifies delay between sending of sync byte in Microsecond
#define CEPALDEVICE "/dev/ttyUSB0"



int main()
{
	int fd; // File descriptor for the PORT
	unsigned char readbuff[255]; 
 	struct termios options;
    char *s = "HELLP";

//    int shmid;
//    key_t key;
//    char *shm, *s;

//    key = 5679;
    
/*    if ((shmid = shmget(key, 27, 0666)) < 0) {
        perror("shmget");
        exit(1);
    }
*/
    /*
     * Now we attach the segment to our data space.
     */
/*    if ((shm = shmat(shmid, NULL, 0)) == (char *) -1) {
        perror("shmat");
        exit(1);
    }
*/
    /*
     * Now read what the server put in the memory.
     */
    


	fd = open(CEPALDEVICE, O_RDWR | O_NOCTTY | O_NDELAY );
	if( fd == -1)
	{
		fprintf(stderr ," Cannot open the device %s \n" , CEPALDEVICE);
		return -1;
	}


	bzero(readbuff, 20);


	printf("Reading IO port parameters\n");	
	tcgetattr(fd, &options);
	cfsetispeed(&options, BAUDRATE);

	options.c_cflag |= ( CLOCAL | CREAD);
	options.c_cflag &= ~PARENB;
	options.c_cflag &= ~CSTOPB;
	options.c_cflag &= ~CSIZE;
	options.c_cflag |= CS8;
	options.c_cflag &= ~CRTSCTS;
    options.c_lflag &= ~(ICANON | ECHO | ECHOE | ISIG);

	options.c_cc[VMIN] = 5;
	options.c_cc[VTIME] = 0;

	tcflush(fd, TCIFLUSH);
	tcsetattr(fd, TCSANOW, &options);
    while(1){
        
                write(fd , s, 5);
                usleep(500000);
    }/*
	while(1)
	{
		usleep(10000);
        scanf("%s",readbuff);
        printf("%s",readbuff);

	}
*/

}

