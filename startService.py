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
    has_started = False

    prompt = '> '
    intro = "Orchestration Service for CSC409. Type ? to list commands"

    def __init__(self):
        super(OrchestrationService, self).__init__()
    
        # Read file into an array once.
        self.ports = []
        self.hosts_array = []
        self.hosts_ranges_start = []
        self.hosts_ranges_end = []
        self.range_exists = False
        with open('config') as config_file:
            i = 0
            for line in config_file:
                if (i == 0):
                    port_line = line.rstrip().split(" ")
                    self.PROXY_PORT = (int(port_line[0]))
                    self.LOAD_BALANCER_PORT = (int(port_line[1]))
                    self.URL_SHORTENER_PORT = (int(port_line[2]))
                elif (i == 1):
                    self.CACHE_SIZE = (int(line))
                else:
                    host_range = line.rstrip().split(" ")
                    self.hosts_array.append(host_range[0].rstrip())
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

    def do_start(self, input):
        '''# Cleanup all existing logging and database files for the 
        proxy/LBs/URL Shorteners. 
        Setup/start the database, proxy, load balancers and URLShortener
        service on all hosts defined in the `config` file.'''
        
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

        # Stop servers started by this user and cleanup all outputs
        self.stop_servers(self.hosts_array, CWD, startUp=True)

        # Setup Proxy Server
        if (self.check_socket(firstHost, self.PROXY_PORT)):
            print ("Starting up {} proxy server".format(firstHost))
            subprocess.run(["ssh", firstHost, "cd {}/proxy; nohup java MultiThreadedProxy {} 8 {} > out/proxy{}.out 2>out/proxy{}.error < /dev/null &".format(CWD, self.PROXY_PORT, argsProxy, firstHost, firstHost)])
        else:
            print("Cannot start proxy on {}:{} - port may already be in use".format(firstHost, self.PROXY_PORT))
        
        n = 1
        for host in self.hosts_array:

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

        subprocess.run("nohup python3 -u startMonitoring.py > out/monitoring.out 2>out/monitoring.error < /dev/null &", shell=True)
        print("Started Monitoring Service")

        self.has_started = True
           
    def check_socket(self, host, port):
        ''' Check if port is available for use'''
        with closing(socket.socket(socket.AF_INET, socket.SOCK_STREAM)) as sock:
            return not sock.connect_ex((host, port)) == 0

    def do_monitor(self, input):
        '''# Tails the monitoring file and outputs to the orchestration output.
        Service must be running to run this command.
        '''
        if self.has_started:
            subprocess.run(["tail", "-f", "out/monitoring.out"])
        else:
            print("Please restart the services before monitoring.\n")

    def do_status(self, input):
        '''# Print status of the proxy, load balancer and URLShortener service.
        '''
        proxyBuilder = ["\nProxy Status:\n"]
        lbBuilder = ["\nLoad Balancer Status:\n"]
        urlBuilder = ["\nURL Shortener Status:\n"]

        proxyOutput = subprocess.run(["ssh", self.hosts_array[0], "lsof -i -P | grep {} | cut -d' ' -f5".format(self.PROXY_PORT)], stdout=PIPE, stderr=PIPE)
        self.service_status("Proxy", self.hosts_array[0], self.PROXY_PORT, proxyOutput.stdout, proxyBuilder)

        for host in self.hosts_array:
            lbOutput = subprocess.run(["ssh", host, "lsof -i -P | grep {} | cut -d' ' -f5".format(self.LOAD_BALANCER_PORT)], stdout=PIPE, stderr=PIPE) 
            self.service_status("Load Balancer", host, self.LOAD_BALANCER_PORT, lbOutput.stdout, lbBuilder)

            urlOutput = subprocess.run(["ssh", host, "lsof -i -P | grep {} | cut -d' ' -f5".format(self.URL_SHORTENER_PORT)], stdout=PIPE, stderr=PIPE) 
            self.service_status("URL Shortener", host, self.URL_SHORTENER_PORT, urlOutput.stdout, urlBuilder)
                
        print(''.join(str(x) for x in (proxyBuilder + lbBuilder + urlBuilder)))

    def stop_servers(self, hosts, cwd, startUp):
        print("Shutting down existing running services...")
        
        if startUp: print("Cleaning up output logs and databases")

        subprocess.run("pkill -9 -f startMonitoring.py", shell=True)
        for host in self.hosts_array:
            subprocess.run(["ssh", host, "killall java"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL) 

            if startUp:
                subprocess.run(["ssh", host, "cd {}; rm dbpackage/out/*.* > /dev/null".format(cwd)], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
                subprocess.run(["ssh", host, "cd {}; rm proxy/out/*.* > /dev/null".format(cwd)], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
                subprocess.run(["ssh", host, "cd {}; rm /virtual/*.db > /dev/null".format(cwd)], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)

    def do_stop(self, input):
        '''# Stop the proxy and all running load balancers/URL shorteners
        on all the hosts defined in the `config` file.'''
        CWD = os.getcwd()
        
        self.stop_servers(self.hosts_array, CWD, startUp=False)

        self.has_started = False
        print("Servers successfully shut down.")

    def service_status(self, serviceName, hostName, servicePort, proxyOutput, outputBuilder):
        ''' Helper for do_status '''
        if proxyOutput == b'':
            outputBuilder.append("{:<20}{}:{:<5} \t DOWN \n".format(serviceName, hostName, servicePort))
        else:
            outputBuilder.append("{:<20}{}:{:<5} \t UP \n".format(serviceName, hostName, servicePort))

    def compile_java_files(self):
        print ("Compiling java executables")
        subprocess.run("./compileJava", shell=True)
    
    def make_output_directories(self):
        MONITOR_OUT_DIR = "out"
        PROXY_OUT_DIR = "proxy/out"
        DBPACKAGE_OUT_DIR = "dbpackage/out"
          
        try:
            if not os.path.exists(MONITOR_OUT_DIR):
                os.mkdir(MONITOR_OUT_DIR)
            
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
