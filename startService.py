#!/usr/bin/env python3

from cmd import Cmd
import subprocess, os

class OrchestrationService(Cmd):
    prompt = '> '
    intro = "Orchestration Service for CSC409. Type ? to list commands"
    port = 8000
    
    def do_start(self, input):
        # Start Service
        CWD = os.getcwd()

        with open('hosts') as hosts_file:
            for host in hosts_file:
                n = 1
                host = host.rstrip()
                # TODO: need to catch bad host names

                # Setup Database
                # subprocess.run(f"ssh {host} \"cd {CWD}/dbpackage/; java -classpath '.:../db/sqlite-jdbc-3.32.3.2.jar' MakeDB url{n}.db jdbc:sqlite:/virtual/\"", shell=True)
                
                # Setup Proxy
                subprocess.run(["ssh", host, "cd {}/proxy; nohup java MultiThreadedProxy 8000 4 {} {} 1".format(CWD, host, self.port)], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
                
                break
                
    def do_stop(self, input):
        # Stop Database, proxy, and URL Shortening Services
        CWD = os.getcwd()
        
        # TODO: Not working (although the rm /virtual*.db works when running in a regular shell)
        with open('hosts') as hosts_file:
            print("Shutting down databases...")
            for host in hosts_file:
                subprocess.run(f"ssh {host} \"rm /virtual/*.db;\"", shell=True)
                break
        
        # Stop proxy server
        # kill $(lsof -i -P | grep '8000' | cut -d" " -f5)


    def do_exit(self, input):
        '''Exit the service.'''
        return True


OrchestrationService().cmdloop()
