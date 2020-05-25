#!/bin/sh

set -e

(hostname -I || ipconfig getifaddr en1 || ipconfig getifaddr en0) 2>/dev/null | awk '{print $1}'
