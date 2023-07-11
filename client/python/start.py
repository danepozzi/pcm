import os
import time
import socket
import subprocess

scripts = '../scripts/'
hostname = socket.gethostname()
pid = hostname[2:4]
print(pid)

alsa_mic = 100
alsa_out = 100

def setamixer():
    print(['sh', 'amixer', 'set', 'Capture', str(alsa_mic) + '%'])
    print(['sh', 'amixer', 'set', 'Master', str(alsa_out) + '%'])
    subprocess.call(['amixer', 'set', 'Capture', str(alsa_mic) + '%'])
    subprocess.call(['amixer', 'set', 'Master', str(alsa_out) + '%'])
    
#def startjack():
#    subprocess.call(['sh', scripts + 'jack-start.sh'])
 
def startjack():
    os.system("/usr/bin/jackd -P75 -R -dalsa -S -dhw:0 -r48000 -p2048 -n3 &")
 
def stopsound():
    os.system("killall -9 sclang")
    time.sleep(1)
    os.system("killall -9 scsynth")
    time.sleep(1)
    os.system("killall -9 jackd")

def testls():
    os.system("sclang ../scripts/test-ls.scd &")
    #os.system("sclang -h")

def testmic():
    os.system("sclang ../scripts/test-mic.scd &")

setamixer()
stopsound()
startjack()
time.sleep(5)
testls()