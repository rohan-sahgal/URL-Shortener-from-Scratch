#!/usr/bin/env python3

from cmd import Cmd
import subprocess, os

class OrchestrationService(Cmd):
    prompt = '> '
    intro = "Orchestration Service for CSC409. Type ? to list commands"
    PROXY_PORT = 8000
    URL_SHORTENER_PORT = 8001
    LOAD_BALANCER_PORT = 8002
    
    # TODO: need to mkdir the corresponding out folders or add the out folders to the repo
    def do_start(self, input):
        '''# Setup/start the database, proxy, load balancers and URLShortener
        service on all hosts defined in the `host` file.'''
        
        # Create out directories
        self.make_output_directories()
        
        CWD = os.getcwd()
        
        # Read file into an array once.
        hosts_array = []
        with open('hosts') as hosts_file:
            for host in hosts_file:
                hosts_array.append(host.rstrip())
        
        if len(hosts_array) == 0:
            raise Exception('Error: No hosts specified in the host file.')
        
        argsLB, argsProxy = "", ""
        for host in hosts_array:
            host = host.rstrip()
            
            argsLB += host + " " + str(self.URL_SHORTENER_PORT) + " " + "1" + " "
            argsProxy += host + " " + str(self.LOAD_BALANCER_PORT) + " " + "1" + " "

        firstHost = hosts_array[0]
        
        # Setup Proxy Server
        print ("Starting up {} proxy server".format(firstHost))
        subprocess.run(["ssh", firstHost, "cd {}/proxy; nohup java MultiThreadedProxy {} 4 {} > out/proxy{}.out 2>out/proxy{}.error < /dev/null &".format(CWD, self.PROXY_PORT, argsProxy, firstHost, firstHost)])
        
        for host in hosts_array:
            n = 1
            host = host.rstrip()
            # TODO: need to catch bad host names
            
            # Setup Database
            print("Starting up {} database".format(host))
            subprocess.run(["ssh", host, "cd {}/dbpackage/; java -classpath '.:../db/sqlite-jdbc-3.32.3.2.jar' MakeDB url{}.db jdbc:sqlite:/virtual/".format(CWD, n)], stdout=subprocess.DEVNULL)
            
            # Setup Load Balancer
            print("Starting up {} load balancer".format(host))
            subprocess.run(["ssh", host, "cd {}/proxy; nohup java MultiThreadedLB {} 4 {} > out/LB{}.out 2>out/LB{}.error < /dev/null &".format(CWD, self.LOAD_BALANCER_PORT, argsLB, host, host)])
        
            # Setup URL Shortener
            print("Starting up {} URL Shortener service".format(host))
            subprocess.run(["ssh", host, "cd {}/dbpackage; java -classpath '.:../db/sqlite-jdbc-3.32.3.2.jar' URLShortner {} url{}.db jdbc:sqlite:/virtual/ > out/shortenerService{}.out 2>out/shortenerService{}.error < /dev/null &".format(CWD, self.URL_SHORTENER_PORT, n, host, host)])
                
            n += 1

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
                subprocess.run(["ssh", host, "kill $(lsof -i -P | grep {} | cut -d' ' -f5)".format(self.URL_SHORTENER_PORT)], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL) 
                
                # Stop Load Balancer
                print ("Shutting down {} Load Balancer".format(host))
                subprocess.run(["ssh", host, "kill $(lsof -i -P | grep {} | cut -d' ' -f5)".format(self.LOAD_BALANCER_PORT)], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL) 
    
    def make_output_directories(self):
        PROXY_OUT_DIR = "proxy/out"
        DBPACKAGE_OUT_DIR = "dbpackage/out"
          
        try:
          if not os.path.exists(PROXY_OUT_DIR):
              os.mkdir(PROXY_OUT_DIR)
          
          if not os.path.exists(DBPACKAGE_OUT_DIR):
              os.mkdir(DBPACKAGE_OUT_DIR)
        except OSError:
          print("Creation of the directories failed")                    
                
    def get_first_line(self, file):
        pos = file.tell()
        line = file.readline()
        file.seek(pos)
        return line

    def do_exit(self, input):
        '''Exit the service.'''
        return True


OrchestrationService().cmdloop()

