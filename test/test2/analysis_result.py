#!/usr/bin/python
import sys
from os import listdir
from os.path import isfile, join

mypath = sys.argv[1]
onlyfiles = [f for f in listdir(mypath) if isfile(join(mypath, f))]

all_content = []
for fname in onlyfiles:
    with open(mypath + fname) as f:
        content = f.readlines();
        all_content.extend([x.strip() for x in content])

logs = [ [int(t.split(":")[0]), int(t.split(":")[1]), t.split(":")[2]] for t in all_content]

message_to_time = {}

for entry in logs:
    if message_to_time.has_key(entry[2]):
        message_to_time[entry[2]].append(entry)
    else:
        message_to_time[entry[2]] = [entry]

for key in message_to_time.keys():
    message_to_time[key].sort(key=lambda entry:entry[1])
    entry = message_to_time[key]
    msg_cnt = len(entry)
    print "Result for message: " + key
    print "10% " , (entry[msg_cnt/10][1] - entry[msg_cnt / 10][0]) 
    print "50% " , (entry[msg_cnt/2][1] - entry[msg_cnt/2][0]) 
    print "100% " , (entry[-1][1] - entry[-1][0]) 
