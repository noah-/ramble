#!/usr/bin/python

import string
import random

chars=string.ascii_uppercase + string.digits

for j in range(0,1024):
  for i in range(0,512):
    print random.choice(chars),
  print ""

