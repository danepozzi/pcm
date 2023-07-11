import os
import time
import socket
import subprocess
import time
import board
import RPi.GPIO as GPIO
from datetime import datetime
from datetime import date
from os.path import exists
from adafruit_lc709203f import LC709203F

scripts = '../scripts/'
hostname = socket.gethostname()
pid = hostname[2:4]

pcm = True
playDC = False
threshold = 3.7
alsa_mic = 127
alsa_out = 127

logPath = '/home/pi/src/python/log/'

volts = [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0]
suns = [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0]
samplingInterval = 2
meanInterval = 30
length = meanInterval / samplingInterval

def setamixer():
    print(['sh', 'amixer', 'set', 'Capture', str(alsa_mic) + '%'])
    print(['sh', 'amixer', 'set', 'Master', str(alsa_out) + '%'])
    subprocess.call(['amixer', 'set', 'Capture', str(alsa_mic) + '%'])
    subprocess.call(['amixer', 'set', 'Master', str(alsa_out) + '%'])
    
def startjack():
    print("starting jack")
    os.system("/usr/bin/jackd -P75 -R -dalsa -S -dhw:0 -r48000 -p2048 -n3 &")
 
def stopsound():
    print("killing all audio")
    os.system("killall -9 sclang")
    time.sleep(1)
    os.system("killall -9 scsynth")
    time.sleep(1)
    os.system("killall -9 jackd")
    
def stop_tropos():
    print("stopping tropos")
    os.system("killall -9 sclang")
    time.sleep(1)
    os.system("killall -9 scsynth")
    
def start_tropos():
    print("starting tropos")
    os.system("export DISPLAY=:0.0 && sclang /home/pi/src/tropos/main01.scd &")

def logDay():
    day =  date.today()
    day = day.strftime("%y%m%d")    
    return day

def logTime():
    now = datetime.now()
    current_time = now.strftime("%H:%M:%S")    
    return current_time

def isDayTime():
    now = datetime.now()
    hour = int(now.strftime("%H"))
    if hour > 10:
        if hour < 18:
            day = 1
        else:
            day = 0
    else:
        day = 0
    return day

def log():
    if exists(logPath+logDay()+'.txt'):
        with open(logPath+logDay()+'.txt', "a") as f:
            f.write("-----------------------------------------------\n")
    else:
        print("Generating log")
        with open(logPath+logDay()+'.txt', 'w') as f:
            f.write("-----------------------------------------------\n")
    if pcm:
        with open(logPath+logDay()+'.txt', "a") as f:
            f.write('System booted wiht PCM ON (shutdown is True)\n')
            f.write("-----------------------------------------------\n")
            f.write("TIME")
            f.write('\t')
            f.write('\t')
            f.write("VOLT")
            f.write('\t')
            f.write("SUN")
            f.write('\t')
            f.write('\t')
            f.write("STATE")
            f.write('\n')
    else:
        with open(logPath+logDay()+'.txt', "a") as f:
            f.write('System booted wiht PCM OFF (shutdown is False)\n')
            f.write("-----------------------------------------------\n")
            f.write("TIME")
            f.write('\t')
            f.write('\t')
            f.write("VOLT")
            f.write('\t')
            f.write("SUN")
            f.write('\t')
            f.write('\t')
            f.write("STATE")
            f.write('\n')
            
def append(volt, sun, state):
    with open(logPath+logDay()+'.txt', "a") as f:
        f.write(logTime())
        f.write('\t')
        f.write(str(format(volt, '.3f')))
        f.write('\t')
        f.write(str(sun))
        f.write('\t')
        f.write(state)
        f.write('\n')
        msg = (str("\""+logTime()+'\t'+str(format(volt, '.3f'))+'\t'+str(sun)+'\t'+state+'\n\"'));
        print(8080, msg);
        msg2 = (str("\""+pid+' '+str(format(volt, '.3f'))+' '+str(sun)+' '+state+'\n\"'));
        netcat(8081, msg2)

GPIO.setmode(GPIO.BCM)
sunPin = 13
GPIO.setup(sunPin, GPIO.IN)

sensor = LC709203F(board.I2C())
sensor._write_word(0x14, int(threshold*1000)) 

def netcatDay():
    os.system("nc -w 3 62.240.154.68 8080 < log/" +logDay()+ ".txt")

def netcat(port, msg):
    os.system('echo -n ' +msg+ ' | nc -w 3 62.240.154.68 ' + str(port))

def shutdown():
    print("60 seconds to shutdown")
    time.sleep(60)
    print("shutdown")
    os.system("sudo shutdown -h now")

def average(lst):
    return sum(lst) / len(lst)

def check(volt, sun):
    global playDC
    if sun:
        print(volt)
        print(type(playDC))
        if volt > (threshold - 0.05):
            append(volt, sun, "P")
            if playDC == False:
                playDC = True
                start_tropos()
            #if isDayTime():
                #if playing == 0:
                    #playing = 1
                    #start_tropos()
        else:
            append(volt, sun, "S")
            if pcm:
                stopsound()
                shutdown()
    else:
        if volt > (threshold + 0.05):
            append(volt, sun, "S")
            if pcm:
                stopsound()
                shutdown()
        else:
            if volt > (threshold - 0.05):
                append(volt, sun, "W")
                if playDC == True:
                    stop_tropos()
                    playDC = False
            else:
                append(volt, sun, "S")
                if pcm:
                    stopsound()
                    shutdown()

def loop():
    sampleIndex = 0
    while True:
        sun = not GPIO.input(sunPin)
        try:
            volt = sensor.cell_voltage
            volts[sampleIndex] = volt
            suns[sampleIndex] = sun
            sampleIndex+=1
            print("volt: " + str(volt) + " sun: " + str(sun))
            if sampleIndex == length:
                check(average(volts), suns.count(True) > 0)
                print("volt: " + str(format(average(volts), '.3f')) + " sun: " + str(suns.count(True) > 0) + " (mean)")
                sampleIndex = 0
        except:
            #log(6.666, sun, "FAIL") #skip when reading error
            print("RuntimeError: CRC failure on reading word")
            pass
        time.sleep(samplingInterval)
        
def init():
    GPIO.setup(sunPin, GPIO.IN)
    if GPIO.input(sunPin):
        print("no sun")
    else:
        print("sun")
    
def main():
    init()
    print("!!!!!!!!!!!!!!INIT")
    log()
    time.sleep(1)
    print("!!!!!!!!!!!!!!LOOP")
    loop()


print(type(playDC))
print("klangpi id is: kp" + pid)

if pcm:
    print("starting PCM on kp" + pid)
else:
    print("PCM on kp" + pid + " not active")

time.sleep(2)

print(logTime())
print("starting sound and main loop")

time.sleep(2)
setamixer()
stopsound()
startjack()
#start_tropos()
time.sleep(10)
main()
