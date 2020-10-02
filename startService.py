#!/usr/bin/env python3

from cmd import Cmd
import subprocess, os

class OrchestrationService(Cmd):
    prompt = '> '
    intro = "Orchestration Service for CSC409. Type ? to list commands"
    
    def do_start(self, input):
        # Start Service
        CWD = os.getcwd()

        with open('hosts') as hosts_file:
            for host in hosts_file:
                n = 1
                host = host.rstrip()
                
                print(CWD)

                # Setup Database
                subprocess.run(f"ssh {host} \"cd {CWD}/dbpackage/; java -classpath '.:../db/sqlite-jdbc-3.32.3.2.jar' MakeDB url{n}.db jdbc:sqlite:/virtual/\"", shell=True)
                break
                
    def do_stop(self, input):
        # Stop Database, proxy, and URL Shortening Services
        CWD = os.getcwd()

        with open('hosts') as hosts_file:
            print("Shutting down databases...")
            for host in hosts_file:
                subprocess.run(f"ssh {host} \"rm /virtual/*.db\"", shell=True)
                break

    def do_exit(self):
        '''Exit the service.'''
        return True


OrchestrationService().cmdloop()
