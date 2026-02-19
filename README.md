# Multi-threaded Data Processing System

**Umesh Dhakal**  
**MSCS632**  
**Dr. Jay Thom**  
**Assignment 6**

---

## About the project

This project is a Multi-threaded Data Processing System that simulates multiple workers processing data in parallel.  
Each worker pulls a task from a shared queue, processes it, and saves the result to a shared output (`results.txt`).  
Both versions include safe concurrency, clean shutdown, logging, and error handling.

---

## Java Version 

### Tools I Used
- IntelliJ IDEA
- JDK 21 (or any JDK 16+)

### How to Run 
1. Open IntelliJ and open your project folder after you clone.
2. Run the main file `MSCS632A6_MT.java.`
3. You can chose terminal to if you prefer, but I prefer Intellij

### Output
- Console shows workers starting, completing tasks, errors if any, and DONE
- `results.txt` contains processed task lines and FAIL lines

---

## Go Version

### Tools I Used
- OnlineGDB Go Compiler (browser)


### How to Run (OnlineGDB)
1. Open OnlineGDB and select **Language: Go**
2. Paste your Go code into `main.go`
3. Click **Run**

### Output
- Console shows workers starting, completing tasks, errors if any, and DONE
- `results.txt` contains processed task lines and FAIL lines


## Notes
- Java uses threads and `synchronized`/`wait()`/`notifyAll()` to safely share the queue.
- Go uses goroutines and channels to safely pass work to workers.
- Both implementations handle errors and shut down safely after work is finished.
