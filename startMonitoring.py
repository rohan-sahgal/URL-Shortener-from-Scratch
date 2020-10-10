#!/usr/bin/env python3

from subprocess import PIPE
import subprocess, os, time
import signal
import sys

PROXY_PORT = 8000
LOAD_BALANCER_PORT = 8001
URL_SHORTENER_PORT = 8002

hosts_array = []
hosts_ranges = []

CWD = os.getcwd()



def signal_handler(sig, frame):
    print("\nExiting to startService\n")
    sys.exit(0)


def init_hosts(hosts):

    with open('hosts') as hosts_file:
        for host in hosts_file:
            host_range = host.rstrip().split(" ")
            print(host_range)
            hosts.append(host_range[0])
            hosts_ranges.append(host_range[1])
            
    if len(hosts) == 0:
        raise Exception('Error: No hosts specified in the host file.')

    firstHost = hosts[0]
    


def revive(serviceName, hostName, servicePort):
    print ("\n{} on {}:{} down: restarting...".format(serviceName, hostName, servicePort))
    if serviceName == "Proxy":
        subprocess.run(["ssh", hostName, "cd {}/proxy; nohup java MultiThreadedProxy {} 4 {} > out/Proxy{}.out 2>out/Proxy{}.error < /dev/null &".format(CWD, servicePort, argsProxy, hostName, hostName)])
        print ("Successfully restarted {} on {}:{}".format(serviceName, hostName, servicePort))
    elif serviceName == "Load Balancer":
        subprocess.run(["ssh", hostName, "cd {}/proxy; nohup java MultiThreadedLB {} 4 {} > out/LB{}.out 2>out/LB{}.error < /dev/null &".format(CWD, servicePort, argsLB, hostName, hostName)])
        print ("Successfully restarted {} on {}:{}".format(serviceName, hostName, servicePort))
    elif serviceName == "URL Shortener":
        subprocess.run(["ssh", hostName, "cd {}/dbpackage; java -classpath '.:../db/sqlite-jdbc-3.32.3.2.jar' URLShortner {} url{}.db jdbc:sqlite:/virtual/ > out/shortenerService{}.out 2>out/shortenerService{}.error < /dev/null &".format(CWD, servicePort, hosts_array.index(hostName) + 1, hostName, hostName)])
        print ("Successfully restarted {} on {}:{}".format(serviceName, hostName, servicePort))

def service_status(serviceName, hostName, servicePort, proxyOutput):
    if proxyOutput == b'':
        revive(serviceName, hostName, servicePort)


init_hosts(hosts_array)

argsLB, argsProxy = "", ""
for i in range(len(hosts_array)):
    host = hosts_array[i].rstrip()
    host_range = hosts_ranges[i].rstrip()
    
    argsLB += host + " " + str(URL_SHORTENER_PORT) + " " + "1" + " " + host_range + " "
    argsProxy += host + " " + str(LOAD_BALANCER_PORT) + " " + "1" + " " + host_range + " "

signal.signal(signal.SIGINT, signal_handler)

# Main Loop
print("Monitoring services that go down...")
while True:

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