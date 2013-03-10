/*************************************************************************************
This sample program was made on Mar 2005 by:

Aquiles Yáñez C.

Under the design guidance of:

Agustin González V.

*************************************************************************************/
//Needed libraries
#include <stdio.h>
#include <stdlib.h>
#include <alsa/asoundlib.h>
#include <getopt.h>

//Enum needed to choose the type of I/O loop 
typedef enum {
    METHOD_DIRECT_RW,   //method with direct use of read/write functions
    METHOD_DIRECT_MMAP, //method with direct use of memory mapping
    METHOD_ASYNC_MMAP,  //method with async use of memory mapping
    METHOD_ASYNC_RW,    //method with async use of read/write functions
    METHOD_RW_AND_POLL, //method with use of read/write functions and pool
    METHOD_DIRECT_RW_NI //method with direct use of read/write and noninterleaved format (not implemented)
} enum_io_method;

//struct that defines one I/O method
struct io_method {
    enum_io_method method;   //I/O loop type
    snd_pcm_access_t access; //PCM access type
    int open_mode;           //open function flags
};

//array of the available I/O methods defined for capture
static struct io_method methods[] = {
    { METHOD_DIRECT_RW, SND_PCM_ACCESS_RW_INTERLEAVED, 0 },
    { METHOD_DIRECT_MMAP, SND_PCM_ACCESS_MMAP_INTERLEAVED, 0 },
    { METHOD_ASYNC_RW,SND_PCM_ACCESS_RW_INTERLEAVED, 0 },
    { METHOD_ASYNC_MMAP,SND_PCM_ACCESS_MMAP_INTERLEAVED, 0 },
    { METHOD_RW_AND_POLL,SND_PCM_ACCESS_RW_INTERLEAVED, 0 },
    { METHOD_DIRECT_RW_NI,SND_PCM_ACCESS_RW_NONINTERLEAVED, 0 }//not implemented
    //SND_PCM_ACCESS_RW_NONINTERLEAVED not supported by the most kind of cards
};

//general configuration parameters of the device
struct device_parameters {
    snd_pcm_sframes_t buffer_size;      //buffer size in frames
    snd_pcm_sframes_t period_size;      //period size in frames
    unsigned int buffer_time;           //length of the circular buffer in usec
    unsigned int period_time;           //length of one period in usec 
    int n_channels;                     //number of channels
    unsigned int sample_rate;           //frame rate
    snd_pcm_format_t sample_format;     //format of the samples
    snd_pcm_access_t access_type;       //PCM access type
};

//recovery callback in case of error
static int xrun_recovery(snd_pcm_t *handle, int error)
{
    switch(error)
    {
        case -EPIPE:    // Buffer Over-run   
            fprintf(stderr,"microphone: \"Buffer Overrun\" \n");
            if (error = snd_pcm_prepare(handle)< 0)
                fprintf(stderr,"microphone: Buffer overrrun cannot be recovered, snd_pcm_prepare fail: %s\n", snd_strerror(error));
            return 0;
            break;
                
        case -ESTRPIPE: //suspend event occurred
            fprintf(stderr,"microphone: Error ESTRPIPE\n");
			//EAGAIN means that the request cannot be processed immediately
            while ((error = snd_pcm_resume(handle)) == -EAGAIN) 
                sleep(1);// wait until the suspend flag is clear

            if (error < 0) // error case
            {
                if (error = snd_pcm_prepare(handle) < 0)
                    fprintf(stderr,"microphone: Suspend cannot be recovered, snd_pcm_prepare fail: %s\n", snd_strerror(error));
            }
            return 0;
            break;
            
        case -EBADFD://Error PCM descriptor is wrong
            fprintf(stderr,"microphone: Error EBADFD\n");
            break;
            
        default:
            fprintf(stderr,"microphone: Error unknown, error = %d\n",error);
            break;
    }
    return error;
}

//shows the help when is needed 
static void help(void)
{
    int k;
    printf(
"Usage: microphone [OPTIONS]\n"
"-h,--help      show this usage help\n"
"-d,--device    device of capture\n"
"-r,--rate      sample rate in hz\n"
"-c,--channels  channels number\n"
"-m,--method    I/O method\n"
"-p,--period    period size in samples\n"
"-b,--buffer    circular buffer size in samples\n"
"\n");
    printf("The I/O methods are:\n");
    printf("(0) DIRECT_RW\n");
    printf("(1) DIRECT_MMAP\n");
    printf("(2) ASYNC_RW\n");
    printf("(3) ASYNC_MMAP\n");
    printf("(4) RW_AND_POLL\n");
}

/*******************************************************************************/
/********************* case: direct with r/w functions *************************/
/*******************************************************************************/
//This case only uses a main loop
static int direct_rw(snd_pcm_t *device, struct device_parameters cap_dev_params)
{
    int error;
    snd_pcm_sframes_t period_size = cap_dev_params.period_size;
    int n_channels = cap_dev_params.n_channels;
    short buf[n_channels*period_size];//audio samples buffer
    signed short *ptr;
    int cptr;
    
    while(1)
    {
        ptr = buf; //aux pointer in buff
        cptr = period_size; //aux ptr needed to calculate remained frames
        //in the most cases readi only uses one bucle
        while(cptr > 0) {//wait until buf is full with period_size frames
            error = snd_pcm_readi (device, ptr, cptr);
            if (error < 0)
            {
                if (xrun_recovery(device, error) < 0) {
                    fprintf(stderr,"microphone: Write error: %s\n", snd_strerror(error));
                    exit(EXIT_FAILURE);
                }
                break; //discard current period       
            }
            ptr += error * n_channels;
            cptr -= error;
        }
        //write to standard output
        write(STDOUT_FILENO, buf, sizeof(short)*period_size*n_channels);
    }
}

/*******************************************************************************/
/********************** case: direct with memory mapping ***********************/
/*******************************************************************************/
//this case also uses one main loop 
static int direct_mmap(snd_pcm_t *device, struct device_parameters cap_dev_params)
{
    int error, state;
    snd_pcm_sframes_t period_size = cap_dev_params.period_size;//period size in frames
    int n_channels = cap_dev_params.n_channels;//number of channels
    const snd_pcm_channel_area_t *my_areas;//mapped memory area info
    snd_pcm_uframes_t offset, frames, size;//aux for frames count
    snd_pcm_sframes_t avail, commitres;//aux for frames count
    int first=1; //first == 1  => first period of the stream is processed now
  
    while(1) //main loop
    {
        state = snd_pcm_state(device); //needed for descriptor check
        switch(state)
        {
            case SND_PCM_STATE_XRUN://buffer over-run
                //fprintf(stderr,"microphone: SND_PCM_STATE_XRUN\n");
                if (error = xrun_recovery(device, -EPIPE) < 0) 
                {
                    fprintf(stderr,"microphone: XRUN recovery failed: %s\n", snd_strerror(error));
                    return error;
                }
				//stream is restarted
                first = 1;
                break;
                
            case SND_PCM_STATE_SUSPENDED://PCM is suspended
            //fprintf(stderr,"microphone: SND_PCM_STATE_SUSPENDED\n");
                if (error = xrun_recovery(device, -ESTRPIPE) < 0) 
                {
                    fprintf(stderr,"microphone: SUSPEND recovery failed: %s\n", snd_strerror(error));
                    return error;
                }
                break;
        }

		//checks how many frames are ready to read or write
        avail = snd_pcm_avail_update(device);
        if (avail < 0) 
        {
            if (error = xrun_recovery(device, avail) < 0) {
                fprintf(stderr,"microphone: SUSPEND recovery failed: %s\n", snd_strerror(error));
                return error;
            }
            first = 1;
            continue;   
        }   
        if (avail < period_size)//checks if one period is ready to process
        {         
            switch(first) 
            {
                case 1:
					//if the capture from PCM is started (first=1) and one period is ready to process,
					//the stream must start 
                    first = 0;
                    if (error = snd_pcm_start(device) < 0) {
                        fprintf(stderr,"microphone: Start error: %s\n", snd_strerror(error));
                        exit(EXIT_FAILURE);
                    }
                    break;
                    
                case 0:
                    //wait for pcm to become ready
                    if (error = snd_pcm_wait(device, -1)< 0) {
                        if ((error = xrun_recovery(device, error)) < 0) {
                            fprintf(stderr,"microphone: snd_pcm_wait error: %s\n", snd_strerror(error));
                            exit(EXIT_FAILURE);
                        }
                        first = 1;
                    }
            } 
            continue;
        }
        size = period_size;
        while (size > 0) //wait until we have period_size frames (in the most cases only one loop is needed)
        {
            frames = size;//expected frames number to be processed
			//frames is a bidirectional variable, this means that the real number of frames processed is written 
			//to this variable by the function.
            if ((error = snd_pcm_mmap_begin (device, &my_areas, &offset, &frames)) < 0) {
                if ((error = xrun_recovery(device, error)) < 0) {
                    fprintf(stderr,"microphone: MMAP begin avail error: %s\n", snd_strerror(error));
                    exit(EXIT_FAILURE);
                }
                first = 1;
            } 
            //write to standard output
            write(STDOUT_FILENO, (my_areas[0].addr)+(offset*sizeof(short)*n_channels), frames*sizeof(short)*n_channels);
            commitres = snd_pcm_mmap_commit(device, offset, frames);
            if (commitres < 0 || (snd_pcm_uframes_t)commitres != frames) {
                if ((error = xrun_recovery(device, commitres >= 0 ? commitres : -EPIPE)) < 0) {
                    fprintf(stderr,"microphone: MMAP commit error: %s\n", snd_strerror(error));
                    exit(EXIT_FAILURE);
                }
                first = 1;
            }
            size -= frames;//needed in the condition of the while loop to check if period is filled
        }
    }
}

/*****************************************************************************************/
/*********************  Case: Async with read/write functions  ***************************/
/*****************************************************************************************/
//this case uses a sync callback and the read function
struct async_private_data {
        int n_channels;
        snd_pcm_sframes_t period_size;
};

//In every call to async_rw_callback one period is processed

//Size of one period (bytes) = n_channels * sizeof(short) * period_size
void async_rw_callback(snd_async_handler_t *ahandler)
{
    int error;
    snd_pcm_t *device = snd_async_handler_get_pcm(ahandler);
    struct async_private_data *data = snd_async_handler_get_callback_private(ahandler);
    int n_channels = data->n_channels;
    snd_pcm_sframes_t period_size = data->period_size;
    short buf[n_channels*period_size];//audio frames buffer 
    signed short *ptr;
    int cptr;

    ptr = buf; //aux pointer in buff
    cptr = period_size; //indicates how many frames are still unread

	//in the most cases one loop is enough to fill the buffer
    while(cptr > 0) {//wait until the buffer have period_size frames
		//trying to read one entire period
        if ((error = snd_pcm_readi (device, ptr, cptr)) < 0)
        {
            if (xrun_recovery(device, error) < 0) {
                fprintf(stderr,"microphone: Write error: %s\n", snd_strerror(error));
                exit(EXIT_FAILURE);
            }
            continue;       
        }
        ptr += error * n_channels;
        cptr -= error;
    }
    //writing to standard output
    write(STDOUT_FILENO, buf, sizeof(short)*period_size*n_channels);
}

void async_rw(snd_pcm_t *device, struct device_parameters cap_dev_params)
{
    snd_pcm_sframes_t period_size = cap_dev_params.period_size;
    int n_channels = cap_dev_params.n_channels;
    struct async_private_data data;//private data passed to the async routine 
    data.n_channels = n_channels;
    data.period_size = cap_dev_params.period_size;//period size
    snd_async_handler_t *ahandler;//async handler
    int error;
    short buf[n_channels*period_size];//audio frames buffer
    
	//adding async handler for PCM with private data data and callback async_rw_callback
    if (error = snd_async_add_pcm_handler(&ahandler, device, async_rw_callback, &data) < 0)
    {
        fprintf(stderr,"microphone: Unable to register async handler\n");
        exit(EXIT_FAILURE);
    }
	//async_rw_callback is called every time that the period is filled
    
	//starting to read data from PCM
    if ((error = snd_pcm_readi (device, buf, period_size)) < 0)
    {
        if (xrun_recovery(device, error)) {
            fprintf(stderr,"microphone: Write error: %s\n", snd_strerror(error));
            exit(EXIT_FAILURE);
        }       
    }

	//writing one period to standard output
    write(STDOUT_FILENO, buf, sizeof(short)*period_size*n_channels);

	//the remainder work is made by the handler and the callback
    while (1) {
        sleep(1);
    }
}

/*******************************************************************************/
/************************ case async with memory mapping ***********************/
/*******************************************************************************/
//This case uses an async callback and memory mapping
void async_mmap_callback(snd_async_handler_t *ahandler)//async callback
{  
    int error, state;
    snd_pcm_t *device = snd_async_handler_get_pcm(ahandler);
    struct async_private_data *data = snd_async_handler_get_callback_private(ahandler);
    int n_channels = data->n_channels;
    snd_pcm_sframes_t period_size = data->period_size;
    const snd_pcm_channel_area_t *my_areas;//memory area info
    snd_pcm_uframes_t offset, frames, size;
    snd_pcm_sframes_t avail, commitres;
    int first = 0;
    
    while(1)
    {   
        state = snd_pcm_state(device);//checks the PCM descriptor state
        switch(state)
        {
            case SND_PCM_STATE_XRUN://checks if the buffer is in a wrong state
                //fprintf(stderr,"microphone: SND_PCM_STATE_XRUN\n");
                if (error = xrun_recovery(device, -EPIPE) < 0) 
                {
                    fprintf(stderr,"microphone: XRUN recovery failed: %s\n", snd_strerror(error));
                    exit(EXIT_FAILURE);
                }
                first = 1;
                break;
                
            case SND_PCM_STATE_SUSPENDED://checks if PCM is in a suspend state
                //fprintf(stderr,"microphone: SND_PCM_STATE_SUSPENDED\n");
                if (error = xrun_recovery(device, -ESTRPIPE) < 0) 
                {
                    fprintf(stderr,"microphone: SUSPEND recovery failed: %s\n", snd_strerror(error));
                    exit(EXIT_FAILURE);
                }
                break;
        }
        avail = snd_pcm_avail_update(device);
        if (avail < 0) //error
        {
            if (error = xrun_recovery(device, avail) < 0) {
                fprintf(stderr,"microphone: Recovery fail: %s\n", snd_strerror(error));
                exit(error);
            }
            first = 1;
            continue;   
        }     
        if (avail < period_size)
        {         
            switch(first) 
            {
                case 1://initializing PCM
                    fprintf(stderr,"microphone: Restarting PCM \n");
                    first = 0;
                    if (error = snd_pcm_start(device) < 0) {
                        fprintf(stderr,"microphone: Start error: %s\n", snd_strerror(error));
                        exit(EXIT_FAILURE);
                    }
                    break;
                    
                case 0:                   
                    return;
            } 
            continue;//we don't have enough data for one period
        }
        size = period_size;
        while (size > 0)//wait until we have period_size frames
        {
            frames = size;//expected frames number to be processed
			//frames is a bidirectional variable, that means the real number of frames processed is written 
			//to this variable by the function.
            
			//sending request for the start of the data reading by the application
            if ((error = snd_pcm_mmap_begin (device, &my_areas, &offset, &frames)) < 0) {
                if ((error = xrun_recovery(device, error)) < 0) {
                    fprintf(stderr,"microphone: MMAP begin avail error: %s\n", snd_strerror(error));
                    exit(EXIT_FAILURE);
                }
                first = 1;
            } 
            //writing data to standard output
            write(STDOUT_FILENO, (my_areas[0].addr)+(offset*sizeof(short)*n_channels), frames*sizeof(short)*n_channels);

			//sending signal for the end of the data reading by the application
            commitres = snd_pcm_mmap_commit(device, offset, frames);
            if (commitres < 0 || (snd_pcm_uframes_t)commitres != frames) {
                if ((error = xrun_recovery(device, commitres >= 0 ? commitres : -EPIPE)) < 0) {
                    fprintf(stderr,"microphone: MMAP commit error: %s\n", snd_strerror(error));
                    exit(EXIT_FAILURE);
                }
                first = 1;
            }
            size -= frames;//needed for the condition of the while loop (size == 0 means end of reading)
        }
    }   
}

void async_mmap(snd_pcm_t *device, struct device_parameters cap_dev_params)
{
    snd_async_handler_t *ahandler;// async handler
    struct async_private_data data;// private data passed to the async callback
    snd_pcm_sframes_t period_size = cap_dev_params.period_size;
    int n_channels = cap_dev_params.n_channels;
    data.n_channels = n_channels;
    data.period_size = cap_dev_params.period_size;
    int error;
    snd_pcm_uframes_t offset, frames;
    snd_pcm_sframes_t avail, commitres;
    const snd_pcm_channel_area_t *my_areas;//memory area info
    
	//adding async handler for PCM
    if (error = snd_async_add_pcm_handler(&ahandler, device, async_mmap_callback, &data) < 0)
    {
        fprintf(stderr,"microphone: Unable to register async handler\n");
        exit(EXIT_FAILURE);
    }
    //starting PCM
    if ((error = snd_pcm_start(device)) < 0) {
        fprintf (stderr, "microphone: Unable to start PCM (%s)\n",snd_strerror (error));
        exit (1);
    }
 
	//sending request for the start of the data reading by the application
    if ((error = snd_pcm_mmap_begin (device, &my_areas, &offset, &frames))<0) {
        fprintf (stderr, "microphone: Memory mapping cannot be started (%s)\n",snd_strerror (error));
        exit (1);
    }
 
	//writing data to standard output
    write(STDOUT_FILENO, (my_areas[0].addr)+(offset*sizeof(short)*n_channels), frames*sizeof(short)*n_channels);
	//sending signal for the end of the data reading by the application
    commitres = snd_pcm_mmap_commit(device, offset, frames);
            if (commitres < 0 || (snd_pcm_uframes_t)commitres != frames) {
                if ((error = xrun_recovery(device, commitres >= 0 ? commitres : -EPIPE)) < 0) {
                    fprintf(stderr,"microphone: MMAP commit error: %s\n", snd_strerror(error));
                    exit(EXIT_FAILURE);
                }
            }
	//the remainder work is made by the handler and the callback
    while (1) {
        sleep(1);
    }
}

/*******************************************************************************/
/************************ case: async with poll function ***********************/
/*******************************************************************************/
//uses the pool function to read the data when is available 

//waits for data in buffer using poll
static int wait_for_poll(snd_pcm_t *device, struct pollfd *ufds, unsigned int count)
{   
    unsigned short revents;
    
    while (1) {
        //checking file descriptors activity
        poll(ufds, count, -1);//-1 means block
        //processing data returned by pool
        snd_pcm_poll_descriptors_revents(device, ufds, count, &revents);
        if (revents & POLLERR)
            return -EIO;
        if (revents & POLLIN)//we have data waiting for read
            return 0;
        }
}

static int rw_and_poll_loop(snd_pcm_t *device, struct device_parameters cap_dev_params)                     
{
    int count;
    int error;
    struct pollfd *ufds;//file descriptor array used by pool
    snd_pcm_sframes_t period_size = cap_dev_params.period_size;
    int n_channels = cap_dev_params.n_channels;
    signed short *ptr;//pointer in buffer
    int cptr;//captured frames counter
    short buf[n_channels*period_size];//audio frames buffer
    int init;
    count = snd_pcm_poll_descriptors_count (device);
    if (count <= 0) {
                fprintf(stderr,"microphone: Invalid poll descriptors count\n");
                return count;
    }
    ufds = malloc(sizeof(struct pollfd) * count);    
    if ((error = snd_pcm_poll_descriptors(device, ufds, count)) < 0) {
                fprintf(stderr,"microphone: Unable to obtain poll descriptors for capture: %s\n", snd_strerror(error));
                return error;
    }
    init = 1; 
    while(1)
    {
        if (!init)
        {
            error = wait_for_poll(device, ufds, count);
            if (error < 0) { //try to recover from error
                if (snd_pcm_state(device) == SND_PCM_STATE_XRUN ||snd_pcm_state(device) == SND_PCM_STATE_SUSPENDED) 
                {
                    error = snd_pcm_state(device) == SND_PCM_STATE_XRUN ? -EPIPE : -ESTRPIPE;
                    if (xrun_recovery(device, error) < 0) {
                        fprintf(stderr,"microphone: Write error: %s\n", snd_strerror(error));
                        exit(EXIT_FAILURE);
                    }
                    init = 1;
                } else {
                    fprintf(stderr,"microphone: Wait for poll failed\n");
                    return error;
                }
            }
        }
        ptr = buf;
        cptr = period_size;

        while(cptr > 0) //waits until buff is filled with period_size frames
        {   
            if ((error = snd_pcm_readi (device, buf, period_size)) < 0)
            {
                if (xrun_recovery(device, error)) {
                    fprintf(stderr,"microphone: Write error: %s\n", snd_strerror(error));
                    exit(EXIT_FAILURE);
                }
                init=1;
                continue;       
            }
            if (snd_pcm_state(device) == SND_PCM_STATE_RUNNING)
                init = 0;
            ptr += error * n_channels;
            cptr -= error;
            if (cptr == 0)//exits if the read of the period is done
                break;

			//if period is not totally filled, another while loop is needed
            error = wait_for_poll(device, ufds, count);
            if (error < 0) { //tries to recover from error
                if (snd_pcm_state(device) == SND_PCM_STATE_XRUN ||snd_pcm_state(device) == SND_PCM_STATE_SUSPENDED) 
                {
                    error = snd_pcm_state(device) == SND_PCM_STATE_XRUN ? -EPIPE : -ESTRPIPE;
                    if (xrun_recovery(device, error) < 0) {
                        fprintf(stderr,"microphone: Write error: %s\n", snd_strerror(error));
                        exit(EXIT_FAILURE);
                    }
                    init = 1;
                } else {
                    fprintf(stderr,"microphone: Wait for poll failed\n");
                    return error;
                }
            }
        }
        //writes to standard output
        write(STDOUT_FILENO, buf, sizeof(short)*period_size*n_channels);
    }
}

/*****************************************************************************************/
/********************************** main function ****************************************/
/*****************************************************************************************/
int main (int argc, char *argv[])
{
   
    int error,dir;
    unsigned int sample_rate = 48000;//expected frame rate
    unsigned int real_sample_rate;//real frame rate 
    int n_channels = 2;//expected number of channels
    unsigned int real_n_channels;//real number of channels
    char * device_name = "hw:0,0";
    snd_pcm_t *device;//capture device
    snd_pcm_hw_params_t *hw_params;//hardware configuration structure
    int access = 1;
    snd_pcm_sframes_t buffer_size = 2048;//expected buffer size in frames
    snd_pcm_uframes_t period_size = 8;//expected period size in frames
    snd_pcm_sframes_t real_buffer_size;//real buffer size in frames
    snd_pcm_uframes_t real_period_size;//real period size in frames
    unsigned int buffer_time;//length of the circular buffer in us
    unsigned int period_time;//length of one period in us
    snd_pcm_format_t real_sample_format;
    snd_pcm_access_t real_access;
	struct device_parameters capture_device_params;
    struct option long_option[] =
    {
               
        {"device", 1, NULL, 'd'},
        {"rate", 1, NULL, 'r'},
        {"channels", 1, NULL, 'c'},
        {"method", 1, NULL, 'm'},
        {"buffer", 1, NULL, 'b'},
        {"period", 1, NULL, 'p'},
        {"help", 0, NULL, 'h'},
        {NULL, 0, NULL, 0},
    };//needed for getopt_long
    
 /************************** processing command line parameters ******************************/
    while (1) {
        int c;
        if ((c = getopt_long(argc, argv, "d:r:c:m:b:p:h", long_option, NULL)) < 0)
            break;
        switch (c) 
        {
            case 'd':
                device_name = strdup(optarg);
                break;
            case 'r':
                sample_rate = atoi(optarg);
                sample_rate = sample_rate < 4000 ? 4000 : sample_rate;
                sample_rate = sample_rate > 196000 ? 196000 : sample_rate;
                break;
            case 'c':
                n_channels = atoi(optarg);
                n_channels = n_channels < 1 ? 1 : n_channels;
                n_channels = n_channels > 1024 ? 1024 : n_channels;
                break;
            case 'm':
                access = atoi(optarg);
                break;
            case 'b':
                buffer_size = atoi(optarg);
                break;
                        //  buffer_time(us) = 0.001 * buffer_size(frames) / rate(khz) 
            case 'p':
                period_size = atoi(optarg);
                break;
            case 'h':
                help();
                exit(1);
                break;
        }                           
    }

/************************************** opens the device *****************************************/

    if ((error = snd_pcm_open (&device, device_name, SND_PCM_STREAM_CAPTURE, methods[access].open_mode)) < 0) {
        fprintf (stderr, "microphone: Device cannot be opened  %s (%s)\n", 
             argv[1],
             snd_strerror (error));
        exit (1);
    }
    fprintf (stderr, "microphone: Device: %s open_mode = %d\n", device_name, methods[access].open_mode);

	//allocating the hardware configuration structure
    if ((error = snd_pcm_hw_params_malloc (&hw_params)) < 0) {
        fprintf (stderr, "microphone: Hardware configuration structure cannot be allocated (%s)\n",
             snd_strerror (error));
        exit (1);
    }

    //assigning the hw configuration structure to the device
    if ((error = snd_pcm_hw_params_any (device, hw_params)) < 0) {
        fprintf (stderr, "microphone: Hardware configuration structure cannot be assigned to device (%s)\n",
             snd_strerror (error));
        exit (1);
    }

/*********************************** shows the audio capture method ****************************/
    
    switch(methods[access].method)
    {
        case METHOD_DIRECT_RW:
            fprintf (stderr, "microphone: capture method: METHOD_DIRECT_RW (m = 0) \n");
            break;
        case METHOD_DIRECT_MMAP:
            fprintf (stderr, "microphone: capture method: METHOD_DIRECT_MMAP (m = 1)\n");
            break;
        case METHOD_ASYNC_MMAP:
            fprintf (stderr, "microphone: capture method: METHOD_ASYNC_MMAP (m = 2)\n");
            break;
        case METHOD_ASYNC_RW:
            fprintf (stderr, "microphone: capture method: METHOD_ASYNC_RW (m = 3)\n");
            break;
        case METHOD_RW_AND_POLL:
            fprintf (stderr, "microphone: capture method: METHOD_RW_AND_POLL (m = 4)\n");
            break;
        case METHOD_DIRECT_RW_NI://not implemented
            fprintf (stderr, "microphone: capture method: METHOD_DIRECT_RW_NI (m = 5)\n");
            break;
    }
    
/*************************** configures access method ******************************************/  
    //sets the configuration method
    fprintf (stderr, "microphone: Access method: %d\n",methods[access].access);
    if ((error = snd_pcm_hw_params_set_access (device, hw_params, methods[access].access)) < 0) {
        fprintf (stderr, "microphone: Access method cannot be configured (%s)\n",
             snd_strerror (error));
        exit (1);
    }
    //checks the access method
    if ((error = snd_pcm_hw_params_get_access (hw_params, &real_access)) < 0) {
        fprintf (stderr, "microphone: Access method cannot be obtained (%s)\n",
             snd_strerror (error));
        exit (1);
    }  
    //shows the access method
    switch(real_access)
    {
    case SND_PCM_ACCESS_MMAP_INTERLEAVED:
        fprintf (stderr, "microphone: PCM access method: SND_PCM_ACCESS_MMAP_INTERLEAVED \n");
        break;
    case SND_PCM_ACCESS_MMAP_NONINTERLEAVED:
        fprintf (stderr, "microphone: PCM access method: SND_PCM_ACCESS_MMAP_NONINTERLEAVED \n");
        break;
    case SND_PCM_ACCESS_MMAP_COMPLEX:
        fprintf (stderr, "microphone: PCM access method: SND_PCM_ACCESS_MMAP_COMPLEX \n");
        break;
    case SND_PCM_ACCESS_RW_INTERLEAVED:
        fprintf (stderr, "microphone: PCM access method: SND_PCM_ACCESS_RW_INTERLEAVED \n");
        break;
    case SND_PCM_ACCESS_RW_NONINTERLEAVED:
        fprintf (stderr, "microphone: PCM access method: SND_PCM_ACCESS_RW_NONINTERLEAVED \n");
        break;
    }
/****************************  configures the capture format *******************************/   
    //SND_PCM_FORMAT_S16_LE => 16 bit signed little endian
    if ((error = snd_pcm_hw_params_set_format (device, hw_params, SND_PCM_FORMAT_S16_LE)) < 0)
    {
        fprintf (stderr, "microphone: Capture format cannot be configured (%s)\n",
             snd_strerror (error));
        exit (1);
    }
    //checks capture format
    if ((error = snd_pcm_hw_params_get_format (hw_params, &real_sample_format)) < 0)
    {
        fprintf (stderr, "microphone: Capture sample format cannot be obtained (%s)\n",
             snd_strerror (error));
        exit (1);
    }
    //just shows the capture format in a human readable way
    switch(real_sample_format)
    {
    case SND_PCM_FORMAT_S16_LE:
        fprintf (stderr, "microphone: PCM capture sample format: SND_PCM_FORMAT_S16_LE \n");
        break;
    default:
        fprintf (stderr, "microphone: PCM capture sample format = %d\n", real_sample_format);
    }    
/*************************** configures the sample rate  ***************************/    
	//sets the sample rate
    if ((error = snd_pcm_hw_params_set_rate (device, hw_params, sample_rate, 0)) < 0) {
        fprintf (stderr, "microphone: Sample rate cannot be configured (%s)\n",
             snd_strerror (error));
        exit (1);
    }
    //checks sample rate
    if ((error = snd_pcm_hw_params_get_rate (hw_params, &real_sample_rate, 0)) < 0) {
        fprintf (stderr, "microphone: Sample rate cannot be obtained (%s)\n",
             snd_strerror (error));
        exit (1);
    }
    fprintf (stderr, "microphone: Sample_rate_real = %d\n", real_sample_rate);
    
/**************************** configures the number of channels ********************************/    
    //sets the number of channels
    if ((error = snd_pcm_hw_params_set_channels (device, hw_params, n_channels)) < 0) {
        fprintf (stderr, "microphone: Number of channels cannot be configured (%s)\n",
             snd_strerror (error));
        exit (1);
    }
    //checks the number of channels
    if ((error = snd_pcm_hw_params_get_channels (hw_params,& real_n_channels)) < 0) {
        fprintf (stderr, "microphone: Number of channels cannot be obtained (%s)\n",
             snd_strerror (error));
        exit (1);
    }
    fprintf (stderr, "microphone: real_n_channels = %d\n", real_n_channels);
    
/***************************** configures the buffer size *************************************/
    //sets the buffer size
    if (error = snd_pcm_hw_params_set_buffer_size(device, hw_params, buffer_size) < 0) {
        fprintf (stderr, "microphone: Buffer size cannot be configured (%s)\n",
             snd_strerror (error));
        exit (1);
    }
    //checks the value of the buffer size
    if (error = snd_pcm_hw_params_get_buffer_size(hw_params, &real_buffer_size) < 0) {
    fprintf (stderr, "microphone: Buffer size cannot be obtained (%s)\n",
             snd_strerror (error));
        exit (1);
    }
    fprintf (stderr, "microphone: Buffer size = %d [frames]\n", (int)real_buffer_size);
/***************************** configures period size *************************************/
    dir=0; //dir=0  =>  period size must be equal to period_size 
    //sets the period size
    if (error = snd_pcm_hw_params_set_period_size(device, hw_params, period_size, dir) < 0) {
        fprintf (stderr, "microphone: Period size cannot be configured (%s)\n",
             snd_strerror (error));
        exit (1);
    }
    //checks the value of period size
    if (error = snd_pcm_hw_params_get_period_size(hw_params, &real_period_size, &dir) < 0) {
    fprintf (stderr, "microphone: Period size cannot be obtained (%s)\n",
             snd_strerror (error));
        exit (1);
    }
    fprintf (stderr, "microphone: Period size = %d [frames]\n", (int)real_period_size);
/************************* applies the hardware configuration  ******************************/

    if ((error = snd_pcm_hw_params (device, hw_params)) < 0) {
        fprintf (stderr, "microphone: Hardware parameters cannot be configured (%s)\n",
             snd_strerror (error));
        exit (1);
    }
    //frees the structure of hardware configuration
    snd_pcm_hw_params_free (hw_params);

/*********************************** filling capture_device_params *************************************/

    capture_device_params.access_type = real_access;           //real access method
    capture_device_params.buffer_size = real_buffer_size;      //real buffer size
    capture_device_params.period_size = real_period_size;      //real period size
    capture_device_params.buffer_time = buffer_time;           //real buffer time
    capture_device_params.period_time = period_time;           //real period time
    capture_device_params.sample_format = real_sample_format;  //real samples format
    capture_device_params.sample_rate = real_sample_rate;      //real sample rate 
    capture_device_params.n_channels = n_channels;             //real number of channels
    
/********************** selects the appropriate access method for audio capture *******************/
    
	switch(methods[access].method)
	{
		case METHOD_DIRECT_RW:
			direct_rw(device, capture_device_params);
		break;
		
		case METHOD_DIRECT_MMAP:
			direct_mmap(device, capture_device_params);
		break;
		
		case METHOD_ASYNC_MMAP:
			async_mmap(device, capture_device_params);
		break;
		
		case METHOD_ASYNC_RW:
			async_rw(device, capture_device_params);            
		break;
		
		case METHOD_RW_AND_POLL:
			rw_and_poll_loop(device, capture_device_params);
		break;
		
		case METHOD_DIRECT_RW_NI://not implemented
			//direct_rw_ni(dev_captura, parm_dev_captura);
			fprintf (stderr, "microphone: Method not supported\n");
		break;
	}
    
    fprintf (stderr, "microphone: BYE BYE\n");
    //closes the device
    snd_pcm_close (device);
    exit (0);
}
