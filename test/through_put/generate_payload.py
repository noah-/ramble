#!/usr/bin/python

import string
import random

chars=string.ascii_uppercase + string.digits

for j in range(0,4096):
  for i in range(0,120):
    print random.choice(chars),
  print ""

