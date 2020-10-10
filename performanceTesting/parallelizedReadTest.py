#!/usr/bin/python3
import random, string, subprocess
import time
import urllib.request

from multiprocessing.pool import ThreadPool as Pool

pool_size = 8

def worker(i):
    try:
        request="http://localhost:8000/{}".format(i)
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
