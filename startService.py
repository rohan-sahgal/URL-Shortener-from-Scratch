#!/usr/bin/env python3

from cmd import Cmd
from subprocess import PIPE
import subprocess, os, time
import signal
import sys

class OrchestrationService(Cmd):

    def __init__(self):
        super(OrchestrationService, self).__init__()
    
        # Read file into an array once.
        self.hosts_array = []
        self.hosts_ranges = []
        with open('hosts') as hosts_file:
            for host in hosts_file:
                host_range = host.rstrip().split(" ")
                self.hosts_array.append(host_range[0])
                self.hosts_ranges.append(host_range[1])
                
        if len(self.hosts_array) == 0:
            raise Exception('Error: No hosts specified in the host file.')

        signal.signal(signal.SIGINT, self.signal_handler)

        
    prompt = '> '
    intro = "Orchestration Service for CSC409. Type ? to list commands"
    
    PROXY_PORT = 8000
    LOAD_BALANCER_PORT = 8001
    URL_SHORTENER_PORT = 8002

    has_started = False

    def do_start(self, input):
        '''# Setup/start the database, proxy, load balancers and URLShortener
        service on all hosts defined in the `host` file.'''
        
        # Create out directories
        self.make_output_directories()
        
        # Compile java files
        self.compile_java_files()
        
        CWD = os.getcwd()
        
        argsLB, argsProxy = "", ""
        for i in range(len(self.hosts_array)):
            host = self.hosts_array[i].rstrip()
            host_range = self.hosts_ranges[i].rstrip()
            
            argsLB += host + " " + str(self.URL_SHORTENER_PORT) + " " + "1" + " " + host_range + " "
            argsProxy += host + " " + str(self.LOAD_BALANCER_PORT) + " " + "1" + " " + host_range + " "

        firstHost = self.hosts_array[0]
        
        # Setup Proxy Server
        print ("Starting up {} proxy server".format(firstHost))
        subprocess.run(["ssh", firstHost, "cd {}/proxy; nohup java MultiThreadedProxy {} 4 {} > out/proxy{}.out 2>out/proxy{}.error < /dev/null &".format(CWD, self.PROXY_PORT, argsProxy, firstHost, firstHost)])
        
        n = 1
        for host in self.hosts_array:
            
            host = host.rstrip()
            # TODO: need to catch bad host names
            
            # # Setup Database
            # print("Starting up {} database".format(host))
            # subprocess.run(["ssh", host, "cd {}/dbpackage/; java -classpath '.:../db/sqlite-jdbc-3.32.3.2.jar' MakeDB url{}.db jdbc:sqlite:/virtual/".format(CWD, n)], stdout=subprocess.DEVNULL)
            
            # Setup Load Balancer
            print("Starting up {} load balancer".format(host))
            subprocess.run(["ssh", host, "cd {}/proxy; nohup java MultiThreadedLB {} 4 {} > out/LB{}.out 2>out/LB{}.error < /dev/null &".format(CWD, self.LOAD_BALANCER_PORT, argsLB, host, host)])
        
            # Setup URL Shortener
            print("Starting up {} URL Shortener service".format(host))
            subprocess.run(["ssh", host, "cd {}/dbpackage; java -classpath '.:../db/sqlite-jdbc-3.32.3.2.jar' URLShortner {} url{}.db jdbc:sqlite:/virtual/ > out/shortenerService{}.out 2>out/shortenerService{}.error < /dev/null &".format(CWD, self.URL_SHORTENER_PORT, n, host, host)])
                
            n += 1
        
        self.has_started = True
    
    def do_monitor(self, input):
        '''# Monitor '''
        if (self.has_started):
            subprocess.run("python3 startMonitoring.py", shell=True)
        else:
            print("Please start the service before monitoring\n")

    def do_status(self, input):
        '''# Monitor the proxy, database, load balancer and URLShortener service
        '''
        proxyBuilder = ["\nProxy Status:\n"]
        lbBuilder = ["\nLoad Balancer Status:\n"]
        urlBuilder = ["\nURL Shortener Status:\n"]
        dbBuilder = ["\nDatabase Status:\n"]

        proxyOutput = subprocess.run(["ssh", self.hosts_array[0], "lsof -i -P | grep {} | cut -d' ' -f5".format(self.PROXY_PORT)], stdout=PIPE, stderr=PIPE)
        self.service_status("Proxy", self.hosts_array[0], self.PROXY_PORT, proxyOutput.stdout, proxyBuilder)

        for host in self.hosts_array:
            lbOutput = subprocess.run(["ssh", host, "lsof -i -P | grep {} | cut -d' ' -f5".format(self.LOAD_BALANCER_PORT)], stdout=PIPE, stderr=PIPE) 
            self.service_status("Load Balancer", host, self.LOAD_BALANCER_PORT, lbOutput.stdout, lbBuilder)

            urlOutput = subprocess.run(["ssh", host, "lsof -i -P | grep {} | cut -d' ' -f5".format(self.URL_SHORTENER_PORT)], stdout=PIPE, stderr=PIPE) 
            self.service_status("URL Shortener", host, self.URL_SHORTENER_PORT, urlOutput.stdout, urlBuilder)

            #figure out how to see state of database
            #idea: try a put and remove, see if successful?
            
        
        print(''.join(str(x) for x in (proxyBuilder + lbBuilder + urlBuilder)))



    def do_stop(self, input):
        '''# Stop the database, proxy, load balancers and URLShortener service 
        on all hosts defined in the `host` file.'''
        CWD = os.getcwd()
        
        firstHost = self.hosts_array[0]
        # Stop proxy server
        print("Shutting down {} proxy server...".format(firstHost))
        subprocess.run(["ssh", firstHost, "kill $(lsof -i -P | grep {} | cut -d' ' -f5)".format(self.PROXY_PORT)], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL) 
        
        for host in self.hosts_array:
            host = host.rstrip()
            subprocess.run(["ssh", host, "killall java"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL) 

            # Remove *.db file
            print("Shutting down {} database...".format(host))
            subprocess.run(["ssh", host, "cd {}; rm /virtual/*.db".format(CWD)])

            # Stop URL Shortener
            print ("Shutting down {} URL Shortener Service".format(host))
            subprocess.run(["ssh", host, "kill $(lsof -i -P | grep {} | cut -d' ' -f5)".format(self.URL_SHORTENER_PORT)], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL) 
            
            # # Stop Load Balancer
            print ("Shutting down {} Load Balancer".format(host))
            subprocess.run(["ssh", host, "kill $(lsof -i -P | grep {} | cut -d' ' -f5)".format(self.LOAD_BALANCER_PORT)], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL) 
    
        self.has_started = False

    def service_status(self, serviceName, hostName, servicePort, proxyOutput, outputBuilder):
        if proxyOutput == b'':
            outputBuilder.append("{:<20}{}:{:<5} \t DOWN \n".format(serviceName, hostName, servicePort))
        else:
            outputBuilder.append("{:<20}{}:{:<5} \t UP \n".format(serviceName, hostName, servicePort))

    def compile_java_files(self):
        print ("Compiling java executables")
        subprocess.run("./compileJava", shell=True)
    
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

    def signal_handler(self, sig, frame):
        ''' Signal handler (captures SIGINT/ctrl + c) for the monitor command'''
        pass
                
    def do_exit(self, input):
        '''Exit the service.'''
        return True



OrchestrationService().cmdloop()

