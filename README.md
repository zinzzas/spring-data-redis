# spring-data-redis

## redis container env

#### docker-compose(require)

```
기본 파일 (-d 백그라운드 실행시) 
docker-compose up -d

다른 이름의 docker-compose 파일 실행(-f 플래그 사용)
docker-compose -f docker-compose-redis-standalone.yml up
```

#### redis.conf 구성  
- 클러스터 구성이므로 각 port별 동일한 설정으로 추가 필요


redis container별 port 설정
```
port 6379
```

서버 접속 구성 all bind (보안에 취약 하지만 로컬 개발 용도)
```
bind 0.0.0.0
```

reids 백업 정책 (rdb + aof)
```
rdbcompression yes
rdbchecksum yes

appendonly.aof yes
appendfilename "appendonly.aof"
aof-load-truncated yes
```

lazyfree 옵션 ON
- 레디스는 default 싱글 쓰레드로 동작하기에 많은 트래픽 유입시 블락될 상황에 대비해 백그라운드에서 수행되도록 설정
```
lazyfree-lazy-eviction yes
lazyfree-lazy-expire yes
lazyfree-lazy-server-del yes
lazyfree-lazy-user-del yes
lazyfree-lazy-user-flush yes
```

클러스터 설정을 위한 ON
```
cluster-enabled yes
cluster-config-file nodes.conf
```


