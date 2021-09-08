#!/bin/sh

kubectl port-forward -n msc svc/msc-rmq 5672:5672
