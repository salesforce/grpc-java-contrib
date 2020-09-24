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
	"path"
	"runtime"
	"strings"
	"syscall"
)

func main() {
	jarName := os.Args[0]
	jarArgs := os.Args[1:]

	binary, err := exec.LookPath("java")
	if err != nil {
		javaHome, javaHomeSet := os.LookupEnv("JAVA_HOME")
		if !javaHomeSet {
			log.Fatalf("Java not found in PATH and JAVA_HOME not set: %v", err)
		}

		stat, err := os.Stat(javaHome)
		if err != nil {
			log.Fatalf("Java not found in JAVA_HOME: %v", err)
		} else if !stat.IsDir() {
			log.Fatalf("JAVA_HOME is not a directory: %v", err)
		} else {
			binary = path.Join(javaHome, "bin", "java")
		}
	}

	args := []string{"-jar", strings.TrimPrefix(jarName, "./")}
	args = append(args, jarArgs...)

	if runtime.GOOS == "windows" {
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
	} else {
		err = syscall.Exec(binary, append([]string{"java"}, args...), os.Environ())
		if err != nil {
			log.Fatalf("Bootstrap execution error: %v", err)
		}
	}
}
