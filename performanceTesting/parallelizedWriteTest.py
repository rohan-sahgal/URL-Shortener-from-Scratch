#!/usr/bin/python3
import random, string, subprocess
import time
from multiprocessing.pool import ThreadPool as Pool

pool_size = 8

def worker(i):
    try:
        longResource = "http://"+''.join(random.choice(string.ascii_uppercase + string.digits) for _ in range(100))
        # shortResource = ''.join(random.choice(string.ascii_uppercase + string.digits) for _ in range(20))

        request="http://dh2010pc30:8008/?short={}&long={}".format(i, longResource)
        subprocess.call(["curl", "-X", "PUT", request], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    
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

