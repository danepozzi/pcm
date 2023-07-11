import os
import glob
import math
import re
import random
import datetime

today = datetime.date.today().strftime("%y%m%d")    
path = os.path.dirname(os.path.abspath(__file__))
res = 60 #seconds
new = []
logs = []
start_times = []
end_times = []

def timeSeries(start):
    for i in range(len(times)):
        s = (start % 60 * 60) + (i * res)
        m = s / 60
        h = m / 60
        s = s % 60
        m = m % 60
        h = math.floor(h+round(start/60)-1)
        m = math.floor(m)
        s = math.floor(s)
        #times[i] = [str("%02d" % h)+':'+str("%02d" % m)+':'+str("%02d" %s), 'nan']
        times[i] = [str("%02d" % h)+':'+str("%02d" % m), 'nan']

def day():
    day =  date.today()
    day = day.strftime("%y%m%d")    
    return day

def writeLog(day):
    #remove unknown TCP activities from raw log
    #it takes .raw_ as input (raw log) and creates .log (clean log)
    with open(day+'.raw_') as f:
        loglines = f.readlines()
    
    open(path+day+'.log', 'w')
    pattern = re.compile(r"[0-9]{2}:[0-9]{2}:[0-9]{2}\t[0-9].[0-9]{3}\t")
    
    for i in loglines:
        if(pattern.match(i)):
            with open(path+day+'.log', 'a') as f:
                f.write(i)

def listLogFiles(day):
    os.chdir(path + "/" + str(day))
    logsRaw = glob.glob('*.log')
    pattern = re.compile(r"[0-9]{2}.log")
    for i in logsRaw:
        if(pattern.match(i)):
            logs.append(i[:2])
    
def getData(day, pi):
    new = []
    with open(path + "/" + str(day) + "/" + pi + '.log') as f:
        lines = f.readlines()

    for i in lines:
        new.append(i.split())
    
    for i in new:
        temp = i[0].split(':')
        i[0] = (int(temp[2]) + (int(temp[1])*60) + (int(temp[0])*60*60)) / res
        i[0] = round(i[0]) - start
        times[int(i[0])][1] = i[1]
    
def calcUpTime(day, pi):
    with open(path + "/" + str(day) + "/" + pi + '.log') as f:
        for line in f:
            pass
        last = line
        f = open(path + "/" + str(day) + "/" + pi + '.log')
        first = f.readline()
        temp1 = first.split()[0].split(':');
        temp2 = last.split()[0].split(':');
        start = (int(temp1[2]) + (int(temp1[1])*60) + (int(temp1[0])*60*60)) / res
        end = (int(temp2[2]) + (int(temp2[1])*60) + (int(temp2[0])*60*60)) / res
        start_times.append(round(start))
        end_times.append(round(end))

def writeJS(day, pi):
    #writes js arrays to be used in chart
    with open(path + "/" + str(day) + "/" + 'label.js', 'w') as f:
        f.write("const time = [")
    
    for i in range(len(times)):
        with open(path + "/" + str(day) + "/" + 'label.js', 'a') as f:
            f.write("\""+times[i][0]+"\",")

    with open(path + "/" + str(day) + "/" + 'label.js', 'a') as f:
        f.write("];")
    
    with open(path + "/" + str(day) + "/" + pi + '.js', 'w') as f:
        f.write("const pi" + pi + " = [")
    
    for i in range(len(times)):
        with open(path + "/" + str(day) + "/" + pi + '.js', 'a') as f:
            times[i][1] 
            f.write("\""+str(times[i][1])+"\",")

    with open(path + "/" + str(day) + "/" + pi + '.js', 'a') as f:
        f.write("];")

def writeJStimeline(day):
    #writes timeline sequence
    with open(path + "/" + str(day) + "/" + 'label.js', 'w') as f:
        f.write("const time = [")
    
    for i in range(len(times)):
        with open(path + "/" + str(day) + "/" + 'label.js', 'a') as f:
            f.write("\""+times[i][0]+"\",")

    with open(path + "/" + str(day) + "/" + 'label.js', 'a') as f:
        f.write("];")

def writeChartJS(day):
    with open(path + "/" + str(day) + "/" + 'chart.js', 'w') as f:
        f.write("new Chart(document.getElementById(\"line-chart\"), {\ntype: 'line',\ndata: {\nlabels: time,\ndatasets: [")
    for log in logs:
        with open(path + "/" + str(day) + "/" + 'chart.js', 'a') as f:
            f.write("{\ndata: pi"+log+",\nlabel: \""+log+"\",\nborderColor: \"" + "#"+''.join([random.choice('0123456789ABCDEF') for j in range(6)]) + "\",\nfill: true\n},")
    with open(path + "/" + str(day) + "/" + 'chart.js', 'a') as f:
        f.write(" ]\n},\noptions: {\ntitle: {\ndisplay: false,\ntext: 'klangpis " + str(day) + "'\n}\n}\n});")

def writeHTML(day):
    logfolder = "http://62.240.154.68/log/"
    today = datetime.date.today()
    yesterday = (today - datetime.timedelta(days=1)).strftime("%y%m%d")
    tomorrow = (today + datetime.timedelta(days=1)).strftime("%y%m%d")
    with open(path + "/" + str(day) + "/" + 'index.html', 'w') as f:
        f.write("<!DOCTYPE html>\n<html>\n<head>\n<meta http-equiv=\"refresh\" content=\"30\">\n<script src=\"https://cdnjs.cloudflare.com/ajax/libs/Chart.js/2.9.4/Chart.js\"></script>\n</head>\n<body>\n")         
    with open(path + "/" + str(day) + "/" + 'index.html', 'a') as f:
        f.write("<center>")
        f.write("<a href=" + logfolder + yesterday + "/><< " + yesterday + "<<</a>   \n")
        f.write("___" + str(day) + "___")
        f.write("   <a href=" + logfolder + tomorrow + "/>>>" + tomorrow + ">></a>")
        f.write("</center>\n")
        f.write("<canvas id=\"line-chart\" style=\"width:100%\"></canvas>\n")
    for log in logs:
        with open(path + "/" + str(day) + "/" + 'index.html', 'a') as f:
            f.write("<script src=\""+log+".js\"></script>\n")
    with open(path + "/" + str(day) + "/" + 'index.html', 'a') as f:
        f.write("<script src=\"label.js\"></script>\n<script src=\"chart.js\"></script>\n</body>\n</html>")

def emptyHTML(day):
    logfolder = "http://62.240.154.68/log/"
    today = datetime.date.today()
    yesterday = (today - datetime.timedelta(days=1)).strftime("%y%m%d")
    tomorrow = (today + datetime.timedelta(days=1)).strftime("%y%m%d")
    with open(path + "/" + str(day) + "/" + 'index.html', 'w') as f:
        f.write("<!DOCTYPE html>\n<html>\n<body>\n")         
    with open(path + "/" + str(day) + "/" + 'index.html', 'a') as f:
        f.write("<a href=" + logfolder + yesterday + "/><< " + yesterday + "</a>\n")
        f.write("<a href=" + logfolder + tomorrow + "/>" + tomorrow + ">></a>\n")
        f.write("klangpis didn't log in yet. probably it has been very cloudy lately.\n")
    with open(path + "/" + str(day) + "/" + 'index.html', 'a') as f:
        f.write("\n</body>\n</html>")

#main
listLogFiles(today)

for log in logs:
    calcUpTime(today, log)
        
start = min(start_times)
end = max(end_times)
times = ["nan"] * (end-start+1)

for log in logs:
    timeSeries(start)
    getData(today, log)
    writeJS(today, log)
  
writeJStimeline(today)
writeChartJS(today)
writeHTML(today)

