FROM flink:1.13.6
WORKDIR /opt/flink
COPY flink-on-k8s-demo-1.0-SNAPSHOT.jar /opt/flink/flink-on-k8s-demo-1.0-SNAPSHOT.jar
ENTRYPOINT ["/docker-entrypoint.sh"]
EXPOSE 6123 8081
CMD ["help"]