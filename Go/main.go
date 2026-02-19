//Umesh Dhakal
//MSCS632
//Assingment 6
package main

import (
	"bufio"
	"fmt"
	"os"
	"strconv"
	"strings"
	"sync"
	"time"
)
// data type
type Record struct {
	id   int
	line string 
}

// Shared results (in memory + file)
type Results struct {
	mu     sync.Mutex
	lines  []string
	fileOK bool
	f      *os.File
	w      *bufio.Writer
}

func NewResults(filename string) *Results {
	r := &Results{
		lines:  make([]string, 0),
		fileOK: false,
	}

	f, err := os.Create(filename)
	if err != nil {
		logMsg("MAIN", "ERR", "cannot create results.txt (still saving in memory)")
		return r
	}

	r.f = f
	r.w = bufio.NewWriter(f)
	r.fileOK = true
	return r
}

func (r *Results) Save(s string) {
	r.mu.Lock()
	defer r.mu.Unlock()

	r.lines = append(r.lines, s)

	if !r.fileOK {
		return
	}

	_, err := r.w.WriteString(s + "\n")
	if err != nil {
		logMsg("MAIN", "ERR", "write failed (stopping file writes)")
		r.fileOK = false
		return
	}
	_ = r.w.Flush()
}

func (r *Results) Count() int {
	r.mu.Lock()
	defer r.mu.Unlock()
	return len(r.lines)
}

func (r *Results) Close() {
	r.mu.Lock()
	defer r.mu.Unlock()

	if r.fileOK && r.w != nil {
		_ = r.w.Flush()
	}
	if r.f != nil {
		_ = r.f.Close()
	}
}

// log style
func logMsg(who, tag, msg string) {
	fmt.Printf("%s [%s] %s: %s\n", time.Now().Format("15:04:05"), tag, who, msg)
}
//grade
func grade(score int) string {
	switch {
	case score >= 90:
		return "A"
	case score >= 80:
		return "B"
	case score >= 70:
		return "C"
	case score >= 60:
		return "D"
	default:
		return "F"
	}
}

func worker(id int, jobs <-chan Record, out *Results, wg *sync.WaitGroup) {
	defer wg.Done()

	who := fmt.Sprintf("W%d", id)
	logMsg(who, "START", "worker started")

	for rec := range jobs {
		// simulate work time
		time.Sleep(time.Millisecond * time.Duration(150+time.Now().UnixNano()%350))

		// parse "Name,Score"
		parts := strings.Split(rec.line, ",")
		if len(parts) != 2 {
			logMsg(who, "ERR", fmt.Sprintf("record %d bad format: %q", rec.id, rec.line))
			out.Save(fmt.Sprintf("Record-%d FAIL | line=%q | reason=bad_format", rec.id, rec.line))
			continue
		}

		name := strings.TrimSpace(parts[0])
		scoreStr := strings.TrimSpace(parts[1])

		score, err := strconv.Atoi(scoreStr)
		if err != nil {
			logMsg(who, "ERR", fmt.Sprintf("record %d score not number: %q", rec.id, scoreStr))
			out.Save(fmt.Sprintf("Record-%d FAIL | name=%s | score=%q | reason=not_number", rec.id, name, scoreStr))
			continue
		}

		g := grade(score)

		// Results.txt
		outLine := fmt.Sprintf("Record-%d OK | by=%s | name=%s | score=%d | grade=%s",
			rec.id, who, name, score, g)

		out.Save(outLine)
		logMsg(who, "OK", fmt.Sprintf("record %d saved", rec.id))
	}

	logMsg(who, "END", "worker finished")
}

func main() {
	workerCount := 4
	totalRecords := 20

	jobs := make(chan Record, totalRecords)
	out := NewResults("results.txt")
	defer out.Close()

	var wg sync.WaitGroup

	// start workers
	for i := 1; i <= workerCount; i++ {
		wg.Add(1)
		go worker(i, jobs, out, &wg)
	}

	// create records
	for i := 1; i <= totalRecords; i++ {
	    // example scores
		line := fmt.Sprintf("S%d,%d", i, 50+i*2) 

		if i == 7 {
		    // bad format
			line = "badrecord" 
		}
		if i == 15 {
		    // score not a number
			line = "a,b" 
		}

		jobs <- Record{id: i, line: line}
	}
    // tells workers “no more jobs”
	close(jobs)
	wg.Wait()

	logMsg("MAIN", "DONE", fmt.Sprintf("workers=%d records=%d saved=%d file=results.txt",
		workerCount, totalRecords, out.Count()))
}

