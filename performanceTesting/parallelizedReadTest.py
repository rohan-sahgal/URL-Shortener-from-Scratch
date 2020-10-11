#!/usr/bin/python3
import random, string, subprocess
import time
import urllib.request

from multiprocessing.pool import ThreadPool as Pool

pool_size = 8

def worker(i):
    try:
        request="http://dh2010pc30:8008/{}".format(i)
        subprocess.run(["curl", "-X", "GET", request], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)

    except Exception as e:
        print(e)

pool = Pool(pool_size)

t0 = time.time()

for i in range(1000):
    pool.apply_async(worker, (i,))


pool.close()
pool.join()

t1 = time.time()

print(t1-t0)

print((t1-t0)/1000)

#also test for variable number of hosts
#reverse reads and writes

#TESTING VARIABLE NUMBER OF WORKERS
# 1 worker
# 1000 writes: 15.38 19.58 17.23 16.97
# 1000 reads:   4.61  4.31  4.11  4.11

# 2 workers
# 1000 writes: 9.05 8.14 8.14
# 1000 reads:  2.40 2.11 2.12 2.00

# 4 workers
# 1000 writes: 5.52 5.12 5.02 4.92 4.82 
# 1000 reads:  1.91 1.81 1.71 1.81 1.81

# 8 workers
# 1000 writes: 4.73 4.62 4.62
# 1000 reads:  1.91 1.81 2.41 2.51 2.21 2.62 2.21 1.71


# VARIABLE NUMBER OF WRITES/READS
# n = 10
# writes: 0.20 0.10 0.10 0.10 0.10
# reads:  0.10 0.10 0.10 0.10 0.10 

# n = 100
# writes: 0.60 0.60 0.50 0.60 0.50
# reads:  0.30 0.30 0.20 0.30 0.20

# n = 1000
# writes: 5.02 4.72 4.52 4.62
# reads:  1.82 1.71 1.81 1.71

# n = 10000
# writes: 46.01 44.90
# reads:  24.79 20.92 

