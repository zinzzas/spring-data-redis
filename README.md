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

lazyfree parameter yes
- 레디스는 default 싱글 쓰레드로 동작하기에 많은 트래픽 유입시 블락될 상황에 대비해 lazyfree 쓰레드가 백그라운드로 동작하여 키를 삭제하기 때문에, 레디스는 서버 키 삭제가 완료될 때 까지 기다리지 않고 다음 operation을 실행하기 때문에 응답 속도가 빨라짐
- LAZYFREE 파라미터는 5개가 있고, 레디스 서버 4.0에서 4개, 6.0에서 1개가 추가되었음.
```
lazyfree-lazy-eviction yes  >> unlink로 삭제하고 새키를 저장하기 위해 yes를 권장한다고 함(unlink로 삭제시 메인 쓰레드에서 삭제하지 않고 sub thread에서 삭제하기 때문에 블록킹이 발생하지 않음)
lazyfree-lazy-expire yes  >> unlink로 만료된 키를 제거하기 위한 설정
lazyfree-lazy-server-del yes  >> unlink로 데이터를 변경하기 위한 설정
lazyfree-lazy-user-del yes
lazyfree-lazy-user-flush yes
```

클러스터 설정을 위한 ON
```
cluster-enabled yes
cluster-config-file nodes.conf
```


