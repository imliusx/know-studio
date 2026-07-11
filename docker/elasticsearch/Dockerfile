ARG ELASTICSEARCH_VERSION=8.15.3
FROM docker.elastic.co/elasticsearch/elasticsearch:${ELASTICSEARCH_VERSION}

ARG IK_VERSION=8.15.3
RUN bin/elasticsearch-plugin install --batch \
    "https://get.infini.cloud/elasticsearch/analysis-ik/${IK_VERSION}"
