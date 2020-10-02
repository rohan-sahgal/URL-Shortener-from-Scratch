#!/usr/bin/env python3

from cmd import Cmd
import subprocess, os

class OrchestrationService(Cmd):
    prompt = '> '
    intro = "Orchestration Service for CSC409. Type ? to list commands"
    PROXY_PORT = 8000
    URL_SHORTENER_PORT = 8001
    
    def do_start(self, input):
        '''# Setup/start the database, proxy, load balancers and URLShortener
        service on all hosts defined in the `host` file.'''
        
        CWD = os.getcwd()

        with open('hosts') as hosts_file:
            firstHost = self.get_first_line(hosts_file).rstrip()
            # Setup Proxy Server
            print ("Starting up {} proxy server".format(firstHost))
                            
            subprocess.run(["ssh", firstHost, "cd {}/proxy; nohup java MultiThreadedProxy 8000 4 {} {} 1 > out/proxy{}.out, 2>out/proxyService{}.error < /dev/null &".format(CWD, firstHost, self.PROXY_PORT, firstHost, firstHost)])
            
            
            for host in hosts_file:
                n = 1
                host = host.rstrip()
                # TODO: need to catch bad host names
                
                # Setup Database
                print("Starting up {} database".format(host))
                subprocess.run(["ssh", host, "cd {}/dbpackage/; java -classpath '.:../db/sqlite-jdbc-3.32.3.2.jar' MakeDB url{}.db jdbc:sqlite:/virtual/".format(CWD, n)], stdout=subprocess.DEVNULL)
            
                # Setup URL Shortener
                print("Starting up {} URL Shortener service".format(host))
                subprocess.run(["ssh", host, "cd {}; java -classpath '.:../db/sqlite-jdbc-3.32.3.2.jar' URLShortner {} url{}.db jdbc:sqlite:/virtual/ > out/shortenerService{}.out, 2>out/shortenerService{}.error < /dev/null &".format(CWD, self.URL_SHORTENER_PORT, n, host, host)])
                
                
    def do_stop(self, input):
        '''# Stop the database, proxy, load balancers and URLShortener service 
        on all hosts defined in the `host` file.'''
        CWD = os.getcwd()
        
        with open('hosts') as hosts_file:
            firstHost = self.get_first_line(hosts_file).rstrip()
            # Stop proxy server
            print("Shutting down {} proxy server...".format(firstHost))
            subprocess.run(["ssh", firstHost, "kill $(lsof -i -P | grep {} | cut -d' ' -f5)".format(self.PROXY_PORT)], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL) 

            for host in hosts_file:
                host = host.rstrip()
                
                # Remove *.db file
                print("Shutting down {} database...".format(host))
                subprocess.run(["ssh", host, "cd {}; rm /virtual/*.db".format(CWD)])

                # Stop URL Shortener
                print ("Shutting down {} URL Shortener Service".format(host))
                subprocess.run(["ssh", host, "kill $(lsof -i -P | grep 8080 | cut -d' ' -f5)"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL) 
                
                
    def get_first_line(self, file):
        pos = file.tell()
        line = file.readline()
        file.seek(pos)
        return line

    def do_exit(self, input):
        '''Exit the service.'''
        return True


OrchestrationService().cmdloop()

