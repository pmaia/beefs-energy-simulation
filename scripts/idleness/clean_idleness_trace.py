#!/usr/bin/python

import sys

line = sys.stdin.readline()
tokens = line.split()
previous_timestamp = int(tokens[0])
previous_idleness_time = int(tokens[1]) / 1000

for line in sys.stdin:
	tokens = line.split()
	timestamp = int(tokens[0])
	idleness_time = int(tokens[1]) / 1000
	
	if idleness_time < previous_idleness_time:
		print "idleness\t" + str(previous_timestamp - previous_idleness_time) + "\t" + str(previous_idleness_time)

	previous_timestamp = timestamp
	previous_idleness_time = idleness_time

print "idleness\t" + str(timestamp - idleness_time) + "\t" + str(idleness_time)
