# Chapter 5

Example code for Chapter 5, section 2

https://github.com/awsdocs/aws-lambda-developer-guide/tree/main/sample-apps/java-events


## Prerequisites

* Created: An AWS account, and an S3 bucket for storing temporary deployment artifacts (referred to as $CF_BUCKET below)
* Installed: AWS CLI, Maven, SAM CLI
* Configured: AWS credentials in your terminal

## Usage

To build:

```
$ mvn package
```

To deploy:

```
$ sam deploy --s3-bucket $CF_BUCKET --stack-name ChapterFiveApi --capabilities CAPABILITY_IAM
```
or OpenTofu, the open source fork of Terraform
```bash
$ tofu fmt
$ tofu init 
$ tofu validate
$ tofu apply
```
* Some problem in -> Deploy serverless applications with AWS Lambda and API Gateway -> 
  * https://github.com/hashicorp/learn-terraform-lambda-api-gateway/tree/main
  * https://serverlessland.com/content/guides/building-serverless-applications-with-terraform/05-api-gateway
  * https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/using-samcli-terraform.html

###  Testing the API locally with SAM
* Run the following command from the directory where the main.tf file is located.
  ```bash
  sam local start-api --hook-name terraform
  ```
* This is what output will look like


# TEARING DOWN RESOURCES
When you run `sam deploy`, it creates or updates a CloudFormation `stack`—a set of resources that has a name, which you’ve seen already with the `--stack-name` parameter of `sam deploy`.

When you want to clean up your AWS account after trying an example, the simplest method is to find the corresponding CloudFormation stack in the AWS Web Console (in the CloudFormation section) and delete the stack using the **Delete** button.

Alternatively, you can tear down the stack from the command line. For example, to tear down the **ChapterFiveApi** stack, run the following:
```bash
$ aws cloudformation delete-stack --stack-name ChapterFiveApi
```
or OpenTofu, the open source fork of Terraform
```bash
$ tofu destroy
```

## Test
First, let’s send some data. The base of the URL is the one from the API Gateway console, but we append /events. We can call our API using curl, for example, as follows (substitute in your URL):

```bash
$  curl -v -d '{"locationName":"Brooklyn, NY", "temperature":91, "timestamp":1564428897, "latitude": 40.70, "longitude": -73.99}' \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -X POST https://v4kh23prz3.execute-api.ap-northeast-1.amazonaws.com/Prod/events

Brooklyn, NY

$ curl -d '{"locationName":"Oxford, UK", "temperature":64,  "timestamp":1564428898, "latitude": 51.75, "longitude": -1.25}' \
  -H "Content-Type: application/json" \
  -X POST   https://v4kh23prz3.execute-api.ap-northeast-1.amazonaws.com/Prod/events
 
Oxford, UK
```

This has saved two new events to DynamoDB. You can prove that to yourself by clicking on the DynamoDB table from the Serverless Application console, and then clicking on the **Items** tab once you’re in the DynamoDB console

And now we can use the final part of our application—reading from the API. We can use curl for that again, adding /locations to the API Gateway console URL, for example:

```bash
$ curl  https://v4kh23prz3.execute-api.ap-northeast-1.amazonaws.com/Prod/locations
[{"locationName":"Oxford, UK","temperature":64.0,"timestamp":1564428898,
  "longitude":-1.25,"latitude":51.75},
  {"locationName":"Brooklyn, NY","temperature":91.0,
  "timestamp":1564428897,"longitude":-73.99,"latitude":40.7}]

```
As expected, this returns the list of locations that we’ve stored weather for.

Congratulations! You’ve built your first full serverless application! While it has only one simple feature, think of all the nonfunctional capabilities it has—it auto-scales up to handle a vast load and then back down when not in use, it’s fault-tolerant across multiple availability zones, it has infrastructure that is automatically updated to include critical security patches, and it has a whole lot more besides.

Now let’s look at a different type of application, using a couple of other different AWS services.