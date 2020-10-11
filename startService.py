#!/usr/bin/env python3

from cmd import Cmd
from subprocess import PIPE
import subprocess, os, time
import signal
import socket
import sys
import errno
from contextlib import closing


class OrchestrationService(Cmd):

    
    PROXY_PORT = 0
    LOAD_BALANCER_PORT = 0
    URL_SHORTENER_PORT = 0
    CACHE_SIZE = 0

    def __init__(self):
        super(OrchestrationService, self).__init__()
    
        # Read file into an array once.
        self.ports = []
        self.hosts_array = []
        self.hosts_ranges_start = []
        self.hosts_ranges_end = []
        self.range_exists = False
        with open('hosts') as hosts_file:
            i = 0
            for line in hosts_file:
                if (i == 0):
                    port_line = line.rstrip().split(" ")
                    self.PROXY_PORT = (int(port_line[0]))
                    self.LOAD_BALANCER_PORT = (int(port_line[1]))
                    self.URL_SHORTENER_PORT = (int(port_line[2]))
                elif (i == 1):
                    self.CACHE_SIZE = (int(line))
                else:
                    host_range = line.rstrip().split(" ")
                    self.hosts_array.append(host_range[0])
                    self.hosts_ranges_start.append(host_range[1])
                    self.hosts_ranges_end.append(host_range[2])
                    if (host_range[1] == '0' and host_range[2] == '255'):
                        self.range_exists = True
                i += 1
                
        if self.range_exists == False:
            raise Exception('Error: No backup host specified')
        if len(self.hosts_array) == 0:
            raise Exception('Error: No hosts specified in the host file.')

        signal.signal(signal.SIGINT, self.signal_handler)

        
    prompt = '> '
    intro = "Orchestration Service for CSC409. Type ? to list commands"
    

    has_started = False

    #TODO: give error message when servers cannot start properly (e.g. ports busy)
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
            host_range_start = self.hosts_ranges_start[i].rstrip()
            host_range_end = self.hosts_ranges_end[i].rstrip()
            
            argsLB += host + " " + str(self.URL_SHORTENER_PORT) + " " + "1" + " " + host_range_start + " " + host_range_end + " "
            argsProxy += host + " " + str(self.LOAD_BALANCER_PORT) + " " + "1" + " " + host_range_start + " " + host_range_end + " "

        firstHost = self.hosts_array[0]

        #check if ports are available
        self.stop_servers(self.hosts_array, CWD, p=False)

        # Setup Proxy Server
        if (self.check_socket(firstHost, self.PROXY_PORT)):
            print ("Starting up {} proxy server".format(firstHost))
            subprocess.run(["ssh", firstHost, "cd {}/proxy; nohup java MultiThreadedProxy {} 8 {} > out/proxy{}.out 2>out/proxy{}.error < /dev/null &".format(CWD, self.PROXY_PORT, argsProxy, firstHost, firstHost)])
        else:
            print("Cannot start proxy on {}:{} - port may already be in use".format(firstHost, self.PROXY_PORT))
        

        
        n = 1
        for host in self.hosts_array:
            
            host = host.rstrip()
            # TODO: need to catch bad host names
            
            # # Setup Database
            # print("Starting up {} database".format(host))
            # subprocess.run(["ssh", host, "cd {}/dbpackage/; java -classpath '.:../db/sqlite-jdbc-3.32.3.2.jar' MakeDB url{}.db jdbc:sqlite:/virtual/".format(CWD, n)], stdout=subprocess.DEVNULL)
            
            # Setup Load Balancer
            if (self.check_socket(host, self.LOAD_BALANCER_PORT)):
                print("Starting up {} load balancer".format(host))
                subprocess.run(["ssh", host, "cd {}/proxy; nohup java MultiThreadedLB {} 8 {} > out/LB{}.out 2>out/LB{}.error < /dev/null &".format(CWD, self.LOAD_BALANCER_PORT, argsLB, host, host)])
            else: 
                print("Cannot start LB on {}:{} - port may already be in use".format(host, self.LOAD_BALANCER_PORT))
            
            # Setup URL Shortener
            if (self.check_socket(host, self.URL_SHORTENER_PORT)):
                print("Starting up {} URL Shortener service".format(host))
                subprocess.run(["ssh", host, "cd {}/dbpackage; java -classpath '.:../db/sqlite-jdbc-3.32.3.2.jar' URLShortner {} {} url{}.db jdbc:sqlite:/virtual/ > out/shortenerService{}.out 2>out/shortenerService{}.error < /dev/null &".format(CWD, self.URL_SHORTENER_PORT, self.CACHE_SIZE, n, host, host)])
            else: 
                print("Cannot start URL Shortener on {}:{} - port may already be in use".format(host, self.URL_SHORTENER_PORT))
                n += 1
        
        self.has_started = True
    
    def check_socket(self, host, port):
        with closing(socket.socket(socket.AF_INET, socket.SOCK_STREAM)) as sock:
            if sock.connect_ex((host, port)) == 0:
                # port unvailable
                return False
            else:
                # port available
                return True


    def do_monitor(self, input):
        '''# Monitor '''
        if (self.has_started):
            subprocess.run("python3 startMonitoring.py", shell=True)
        else:
            print("Please restart the services before monitoring.\n")

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

    def stop_servers(self, hosts, cwd, p):
        for host in self.hosts_array:
            host = host.rstrip()

            # Kills all java servers
            if p: print("Shutting down servers on {}...".format(host))
            subprocess.run(["ssh", host, "killall java"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL) 

            # Remove db and output files
            if p: 
                print("Shutting down {} database...".format(host))
                subprocess.run(["ssh", host, "cd {}; rm ./dbpackage/out/*".format(cwd)])
                subprocess.run(["ssh", host, "cd {}; rm ./proxy/out/*".format(cwd)])
                subprocess.run(["ssh", host, "cd {}; rm /virtual/*.db".format(cwd)])

    def do_stop(self, input):
        '''# Stop the database, proxy, load balancers and URLShortener service 
        on all hosts defined in the `host` file.'''
        CWD = os.getcwd()
        
        # Stop proxy server
        print("Shutting down all java servers and removing databases...\n")
        
        self.stop_servers(self.hosts_array, CWD, True)
            
        print("Servers successfully shut down.")
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

