/*
 *  Copyright (c) 2019, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package main

import (
	"log"
	"os"
	"os/exec"
	"strings"
	"syscall"
)

func main() {
	jarName := os.Args[0]
	jarArgs := os.Args[1:]

	binary, err := exec.LookPath("java")
	if err != nil {
		log.Fatalf("Java execution error: %v", err)
	}

	args := []string{"-jar", strings.TrimPrefix(jarName, "./")}
	args = append(args, jarArgs...)

	cmd := exec.Command(binary, args...)
	cmd.Stdout = os.Stdout
	cmd.Stderr = os.Stderr
	cmd.Stdin = os.Stdin

	if err := cmd.Run(); err != nil {
		if err, ok := err.(*exec.ExitError); ok {
			if status, ok := err.Sys().(syscall.WaitStatus); ok {
				// Exit with the same code as the Java program
				os.Exit(status.ExitStatus())
			}
		} else {
			log.Fatalf("Bootstrap execution error: %v", err)
		}
	}
}