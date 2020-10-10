#!/usr/bin/env python3

from subprocess import PIPE
import subprocess, os, time
import signal
import sys

PROXY_PORT = 8003
LOAD_BALANCER_PORT = 8004
URL_SHORTENER_PORT = 8005

hosts_array = []

CWD = os.getcwd()



def signal_handler(sig, frame):
    print("\nExiting to startService\n")
    sys.exit(0)


def init_hosts(hosts_array):

    with open('hosts') as hosts_file:
        for host in hosts_file:
            hosts_array.append(host.rstrip())
            
    if len(hosts_array) == 0:
        raise Exception('Error: No hosts specified in the host file.')

    firstHost = hosts_array[0]
    


def revive(serviceName, hostName, servicePort):

    if serviceName == "Proxy":
        print ("Starting up {} proxy server".format(hostName))
        # subprocess.run(["ssh", hostName, "cd {}/proxy; nohup java MultiThreadedProxy {} 4 {} > out/proxy{}.out 2>out/proxy{}.error < /dev/null &".format(CWD, servicePort, argsProxy, hostName, hostName)])
        subprocess.run(["ssh", hostName, "cd {}/proxy; nohup java MultiThreadedProxy {} 4 {} > out/Proxy{}.out 2>out/Proxy{}.error < /dev/null &".format(CWD, servicePort, argsProxy, hostName, hostName)])

    if serviceName == "Load Balancer":
        print("Starting up {} load balancer".format(hostName))
        subprocess.run(["ssh", hostName, "cd {}/proxy; nohup java MultiThreadedLB {} 4 {} > out/LB{}.out 2>out/LB{}.error < /dev/null &".format(CWD, servicePort, argsLB, hostName, hostName)])
    
    if serviceName == "URL Shortener":
        print("Starting up {} URL Shortener service".format(hostName))
        subprocess.run(["ssh", hostName, "cd {}/dbpackage; java -classpath '.:../db/sqlite-jdbc-3.32.3.2.jar' URLShortner {} url{}.db jdbc:sqlite:/virtual/ > out/shortenerService{}.out 2>out/shortenerService{}.error < /dev/null &".format(CWD, servicePort, hosts_array.index(hostName) + 1, hostName, hostName)])
                

def service_status(serviceName, hostName, servicePort, proxyOutput):
    if proxyOutput == b'':
        revive(serviceName, hostName, servicePort)


init_hosts(hosts_array)

argsLB, argsProxy = "", ""
for host in hosts_array:
    host = host.rstrip()
    
    argsLB += host + " " + str(URL_SHORTENER_PORT) + " " + "1" + " "
    argsProxy += host + " " + str(LOAD_BALANCER_PORT) + " " + "1" + " "

signal.signal(signal.SIGINT, signal_handler)

while True:

    # def do_status(self, input):
    #     '''# Monitor the proxy, database, load balancer and URLShortener service
    #     '''


    proxyOutput = subprocess.run(["ssh", hosts_array[0], "lsof -i -P | grep {} | cut -d' ' -f5".format(PROXY_PORT)], stdout=PIPE, stderr=PIPE)
    service_status("Proxy", hosts_array[0], PROXY_PORT, proxyOutput.stdout)

    for host in hosts_array:
        lbOutput = subprocess.run(["ssh", host, "lsof -i -P | grep {} | cut -d' ' -f5".format(LOAD_BALANCER_PORT)], stdout=PIPE, stderr=PIPE) 
        service_status("Load Balancer", host, LOAD_BALANCER_PORT, lbOutput.stdout)

        urlOutput = subprocess.run(["ssh", host, "lsof -i -P | grep {} | cut -d' ' -f5".format(URL_SHORTENER_PORT)], stdout=PIPE, stderr=PIPE) 
        service_status("URL Shortener", host, URL_SHORTENER_PORT, urlOutput.stdout)

        #figure out how to see state of database
        #idea: try a put and remove, see if successful?
        
    



    time.sleep(1)