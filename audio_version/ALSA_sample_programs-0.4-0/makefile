all: microphone speaker

microphone: microphone.c
	gcc -lasound -o microphone microphone.c

speaker: speaker.c
	gcc -lasound -o speaker speaker.c

clean:
	rm -f microphone
	rm -f speaker
