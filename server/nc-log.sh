#!/bin/bash

sudo nc -k -l -p 8080 > /var/www/html/log/$(date +"%y%m%d").log
