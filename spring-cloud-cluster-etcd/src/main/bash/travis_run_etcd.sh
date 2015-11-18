#!/usr/bin/env bash

./etcd-dist/etcd --name 'spring-cloud-cluster-etcd' --data-dir /tmp/etcd/spring-cloud-cluster-etcd &

sleep 2

#./etcd-dist/etcdctl set /test/messages 'data'
