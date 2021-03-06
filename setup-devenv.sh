#!/bin/bash
sudo apt-get install -y openjdk-8-jdk
# java will be installed in /usr/lib/jvm/java-8-openjdk-amd64/
echo 'ID=$(curl "http://metadata.google.internal/computeMetadata/v1/project/project-id" -H "Metadata-Flavor: Google")' | sudo tee -a /etc/bash.bashrc
echo 'export PROJECTID=$ID' | sudo tee -a /etc/bash.bashrc
export PROJECTID=$(curl "http://metadata.google.internal/computeMetadata/v1/project/project-id" -H "Metadata-Flavor: Google")
echo 'export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64/' | sudo tee -a /etc/bash.bashrc
echo 'export PATH=$PATH:/usr/lib/jvm/java-8-openjdk-amd64/bin' | sudo tee -a /etc/bash.bashrc
sudo mkdir -p /home/devtools/
cd /home/devtools
sudo -p /home/devtools/ wget http://www-us.apache.org/dist/maven/maven-3/3.5.3/binaries/apache-maven-3.5.3-bin.tar.gz
sudo tar xzvf /home/devtools/apache-maven-3.5.3-bin.tar.gz
cp -R ~/dataflow-lab/.m2 ~/
echo 'export PATH=$PATH:/home/devtools/apache-maven-3.5.3/bin' | sudo tee -a /etc/bash.bashrc
gcloud pubsub topics create iotdata
export TOPIC_URI=projects/$PROJECTID/topics/iotdata
echo 'export TOPIC_URI=projects/$PROJECTID/topics/iotdata' | sudo tee -a /etc/bash.bashrc
gsutil mb -c regional -l us-west1 gs://$PROJECTID-imagesin
echo 'export BUCKET_IN_PATH=gs://$PROJECTID-imagesin' | sudo tee -a /etc/bash.bashrc
export BUCKET_IN_PATH=gs://$PROJECTID-imagesin
gsutil mb -c regional -l us-west1 gs://$PROJECTID-imagesout
echo 'export BUCKET_OUT_PATH=gs://$PROJECTID-imagesout' | sudo tee -a /etc/bash.bashrc
gsutil mb -c regional -l us-west1 gs://$PROJECTID-dataflowstagging
gsutil mb -c regional -l us-west1 gs://$PROJECTID-temp
gcloud iot registries create iotregistry --project=$PROJECTID --region=us-central1 --event-notification-config=topic=$TOPIC_URI
gcloud iam service-accounts create sa-iotdevice --display-name 'sa-iotdevice'
gcloud projects add-iam-policy-binding $PROJECTID --member serviceAccount:sa-iotdevice@$PROJECTID.iam.gserviceaccount.com --role roles/storage.objectCreator
gcloud iam service-accounts keys create ~/sa-key.p12 --key-file-type='p12' --iam-account=sa-iotdevice@$PROJECTID.iam.gserviceaccount.com
gsutil cp ~/sa-key.p12 gs://$PROJECTID-temp/sa-key.p12
source /etc/bash.bashrc

