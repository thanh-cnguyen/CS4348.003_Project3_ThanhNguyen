The project includes:

	Driver.java
		Performs simulation of schedulign algorithm:
		Capture input file names
		Initiate approriate algorithms based on schedule file (.sf)
		Obtain inputs from process file (.pf)

	SheduleAlgorithm.java
		Provides algorithm to operate processes:
		First come first serve
		Virtual round robin
		Shortest remaining time
		Highest response ratio time

	Process.java
		Performs communication between logger and Encrypter:
		Writes input to Logger
		Writes input to Encrypter
		Reads outputs from the Encrypter and writes them to Logger

	Makefile
		Provides options to run, compile, and inspect source code:
		make all - compile the program
		make run - to run the program
		make clr - to remove runnable files
		make cat - to open the source of ScheduleAlgorithm.java

	WriteUp_Project3_cs4348.003_ThanhNguyen.pdf
		has the progress of approaches
		provides explanations of problems encountered
		guide to run the project

	process_files:
		folder contains all input examples

	schedule_files:
		folder contains all scheduling algorithms