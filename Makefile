upload-dis:
	 aws s3 cp ./target/dis-0.0.1-SNAPSHOT.jar s3://globalintegration/dataintegration/unnati/

upload-props:
	 aws s3 cp ./src/main/resources/application.yaml s3://globalintegration/dataintegration/unnati/
