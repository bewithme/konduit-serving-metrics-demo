FROM skymind-ai-dl4j-demo:v0.0.1
MAINTAINER xuwenfeng

WORKDIR /srv/app
 
ADD . konduit-serving-metrics-demo

         
RUN  git clone --single-branch --branch "sa/grafana_demo" https://github.com/KonduitAI/konduit-serving 

WORKDIR /srv/app/konduit-serving
     
RUN  mvn -Puberjar,tensorflow  install -Dmaven.test.skip=true -Djavacpp.platform=linux-x86_64 -Dchip=gpu  --settings  /srv/app/konduit-serving-metrics-demo/settings_aliyun.xml -Pgpu,intel -Ppython -Ppmml -Dspin.version=all -Denforcer.skip=true

WORKDIR /srv/app/konduit-serving-metrics-demo

RUN  mvn -Plinux  install --settings  settings_aliyun.xml

WORKDIR /srv/app/konduit-serving-metrics-demo/target

RUN unzip konduit-metrics-demo-1.0-SNAPSHOT-bin.zip


EXPOSE 9008

#CMD ["java","-jar","/srv/app/konduit-serving-metrics-demo/target/konduit-metrics-demo-1.0-SNAPSHOT/konduit-metrics-demo-1.0-SNAPSHOT.jar"]


#nvidia-docker build -t='konduit-metrics-demo:v0.0.1' .

#nvidia-docker run --gpus '"device=0"'  -it  -v /skymind:/skymind  -d c8b664e21ba6 <image_id>